package edacc.api;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Vector;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import edacc.model.*;
import edacc.parameterspace.domain.*;
import edacc.parameterspace.graph.AndNode;
import edacc.parameterspace.graph.Edge;
import edacc.parameterspace.graph.Node;
import edacc.parameterspace.graph.OrNode;
import edacc.parameterspace.graph.ParameterGraph;
import edacc.parameterspace.ParameterConfiguration;

public class API {
	private static DatabaseConnector db = DatabaseConnector.getInstance();
	
	public static double cost_func(API api, ParameterConfiguration config, Random rng) throws Exception {
		LinkedList<Instance> instances = InstanceDAO.getAllByExperimentId(8);
		LinkedList<Integer> job_ids = new LinkedList<Integer>();
		int solver_config_id = api.createSolverConfig(8, 10, config, 0);
		for (Instance inst: instances) {
			int run = 0;
			for (int j = 0; j < 1; j++) {
				job_ids.add(api.launchJob(8, solver_config_id, inst.getId(), BigInteger.valueOf(rng.nextInt(234567892)), 5, run++));
			}
		}
		while (true) {
			//System.out.println("awaiting results ...");
			Thread.sleep(2000);
			boolean all_done = true;
			for (int id: job_ids) {
				ExperimentResult res = api.getJob(id);
				all_done &= res.getStatus().getStatusCode() >= 1;
			}
			if (all_done) break;
		}
		
		double total_time = 0.0;
		for (int id: job_ids) {
			ExperimentResult res = api.getJob(id);
			total_time += res.getResultTime();
		}
		return total_time / job_ids.size();
	}
	
	public static void main(String[] args) throws Exception {
		API api = new API();
		api.connect("localhost", 3306, "EDACC", "edacc", "edaccteam");
		
		ParameterGraph pspace = api.loadParameterGraphFromFile("src/sparrow_parameterspace.xml");
		Random rng = new Random();
		
		int pop_size = 10, generations = 10;
		double best_cost = Double.MAX_VALUE;
		ParameterConfiguration best_configuration = null;
		Vector<Double> cost = new Vector<Double>();
		Vector<ParameterConfiguration> population = new Vector<ParameterConfiguration>();
		for (int i = 0; i < pop_size; i++) {
			population.add(pspace.getRandomConfiguration(rng));
			cost.add(cost_func(api, population.get(i), rng));
			if (cost.get(i) < best_cost) {
				best_cost = cost.get(i);
				best_configuration = new ParameterConfiguration(population.get(i));
			}
		}
		System.out.println("initial generation:");
		for (int i = 0; i < pop_size; i++) {
			System.out.println(population.get(i).toString() + " (cost: " + cost.get(i) + ")");
		}
		
		for (int g = 0; g < generations; g++) {
			Vector<ParameterConfiguration> children = new Vector<ParameterConfiguration>();
			for (int i = 0; i < pop_size; i++) {
				children.add(new ParameterConfiguration(population.get(i)));
				pspace.mutateParameterConfiguration(rng, children.get(i));
				double child_cost = cost_func(api, children.get(i), rng);
				if (child_cost < cost.get(i)) { // replace parent
					population.set(i, children.get(i));
					cost.set(i, child_cost);
				}
				
				if (cost.get(i) < best_cost) {
					best_cost = cost.get(i);
					best_configuration = new ParameterConfiguration(population.get(i));
				}
			}
			
			System.out.println("new generation:");
			for (int i = 0; i < pop_size; i++) {
				System.out.println(population.get(i).toString() + " (cost: " + cost.get(i) + ")");
			}
			
			System.out.println("best configuration found so far:" + best_configuration.toString() + " (cost " + best_cost + ")");
		}
		
		System.out.println("best configuration:" + best_configuration.toString() + " (cost " + best_cost + ")");
		
		/*
		LinkedList<Instance> instances = InstanceDAO.getAllByExperimentId(8);
		LinkedList<Integer> job_ids = new LinkedList<Integer>();
		for (int i = 0; i < 200; i++) {
			int solver_config_id = api.createSolverConfig(8, 10, pspace.getRandomConfiguration(rng), i);
			for (Instance inst: instances) {
				int run = 0;
				for (int j = 0; j < 1; j++) {
					job_ids.add(api.launchJob(8, solver_config_id, inst.getId(), BigInteger.valueOf(rng.nextInt(234567892)), 5, run++));
				}
			}
		}*/
		
		api.disconnect();
	}
	
	public boolean connect(String hostname, int port, String database, String username, String password) {
		try {
			db.connect(hostname, port, username, database, password, false, false, 8);
			return true;
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
	public void disconnect() {
		db.disconnect();
	}
	
	public int createSolverConfig(int experiment_id, int solver_binary_id, ParameterConfiguration config, int seed_group) {
		try {
			SolverBinaries solver_binary = SolverBinariesDAO.getById(solver_binary_id);
			SolverConfiguration solver_config = SolverConfigurationDAO.createSolverConfiguration(solver_binary, experiment_id, seed_group, solver_binary.getBinaryName() + String.valueOf(seed_group), 0);
			Vector<Parameter> parameters = ParameterDAO.getParameterFromSolverId(solver_binary.getIdSolver()); 
			for (edacc.parameterspace.Parameter p: config.getParameter_instances().keySet()) {
				Parameter db_parameter = null;
				for (Parameter dbp: parameters) if (dbp.getName().equals(p.getName())) db_parameter = dbp;
				if (OptionalDomain.OPTIONS.NOT_SPECIFIED.equals(config.getParameterValue(p))) continue;
				else if (FlagDomain.FLAGS.OFF.equals(config.getParameterValue(p))) continue;
				else {
					ParameterInstance pi = ParameterInstanceDAO.createParameterInstance(db_parameter.getId(), solver_config, config.getParameterValue(p).toString());
					ParameterInstanceDAO.save(pi);
				}
			}
			for (Parameter db_parameter: parameters) {
				// check if parameter is part of the search space
				boolean search_param = false;
				for (edacc.parameterspace.Parameter p: config.getParameter_instances().keySet()) {
					if (p.getName().equals(db_parameter.getName())) search_param = true; 
				}
				if (!search_param) {
					if ("instance".equals(db_parameter.getName()) || "seed".equals(db_parameter.getName()) || db_parameter.isMandatory()) {
						ParameterInstance pi = ParameterInstanceDAO.createParameterInstance(db_parameter.getId(), solver_config, "");
						ParameterInstanceDAO.save(pi);
					}
				}
			}
			return solver_config.getId();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}

	
	public int launchJob(int experiment_id, int solver_config_id, int instance_id, BigInteger seed, int cpu_time_limit, int run) {
		try {
			ExperimentResult job = ExperimentResultDAO.createExperimentResult(run, 0, 0, StatusCode.NOT_STARTED, seed.intValue(), ResultCode.UNKNOWN, 0, solver_config_id, experiment_id, instance_id, null, cpu_time_limit, -1, -1, -1, -1, -1);
			ArrayList<ExperimentResult> l = new ArrayList<ExperimentResult>();
			l.add(job);
			ExperimentResultDAO.batchSave(l);
			return job.getId();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	public ExperimentResult getJob(int job_id) {
		try {
			return ExperimentResultDAO.getById(job_id);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public ParameterGraph loadParameterGraphFromDB(int solverID) {
		try {
            Statement st = db.getConn().createStatement();
    
            ResultSet rs = st.executeQuery("SELECT serializedGraph FROM ParameterGraph WHERE Solver_idSolver = " + solverID);
            try {
                if (rs.next()) {
                    return unmarshal(ParameterGraph.class, rs.getBlob("serializedGraph").getBinaryStream());
                }
            } catch (JAXBException e) {
				e.printStackTrace();
				return null;
			} finally {
            	rs.close();
                st.close();
            }
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
		return null;
	}
	
	public ParameterGraph loadParameterGraphFromFile(String xmlFileName) throws FileNotFoundException {
		FileInputStream fis = new FileInputStream(xmlFileName);
		ParameterGraph unm;
		try {
			unm = unmarshal(ParameterGraph.class, fis);
		} catch (JAXBException e) {
			e.printStackTrace();
			return null;
		}
		unm.buildAdjacencyList();
		return unm;
	}
	
	private <T> T unmarshal( Class<T> docClass, InputStream inputStream )
    	throws JAXBException {
		JAXBContext jc = JAXBContext.newInstance( docClass);
		Unmarshaller u = jc.createUnmarshaller();
		return (T)u.unmarshal(inputStream);
	}
}
