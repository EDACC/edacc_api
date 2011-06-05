package edacc.api;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.sql.SQLException;
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
	
	public static void main(String[] args) throws InstanceClassMustBeSourceException, SQLException, IOException, JAXBException {
		API api = new API();
		ParameterGraph pg = api.loadParameterGraph("src/sparrow_parameterspace.xml");
		System.out.println(pg.getRandomConfiguration(new Random()).toString());
		
		/*Set<edacc.parameterspace.Parameter> parameters = new HashSet<edacc.parameterspace.Parameter>();
		edacc.parameterspace.Parameter param_c1 = new edacc.parameterspace.Parameter("c1", new IntegerDomain(1, 10));
		parameters.add(param_c1);
		edacc.parameterspace.Parameter param_c2 = new edacc.parameterspace.Parameter("c2", new IntegerDomain(1, 10));
		parameters.add(param_c2);
		edacc.parameterspace.Parameter param_c3 = new edacc.parameterspace.Parameter("c3", new IntegerDomain(1, 10));
		parameters.add(param_c3);
		edacc.parameterspace.Parameter param_ps = new edacc.parameterspace.Parameter("ps", new RealDomain(0.0, 1.0));
		parameters.add(param_ps);
		
		Set<Node> nodes = new HashSet<Node>();
		AndNode start = new AndNode(null, null); nodes.add(start);
		OrNode c1 = new OrNode(param_c1); nodes.add(c1);
		OrNode c2 = new OrNode(param_c2); nodes.add(c2);
		OrNode c3 = new OrNode(param_c3); nodes.add(c3);
		OrNode ps = new OrNode(param_ps); nodes.add(ps);
		AndNode c1_vals = new AndNode(param_c1, param_c1.getDomain()); nodes.add(c1_vals);
		AndNode c2_vals = new AndNode(param_c2, param_c2.getDomain()); nodes.add(c2_vals);
		AndNode c3_vals = new AndNode(param_c3, param_c3.getDomain()); nodes.add(c3_vals);
		AndNode ps_vals = new AndNode(param_ps, param_ps.getDomain()); nodes.add(ps_vals);
		
		List<Edge> edges = new LinkedList<Edge>();
		edges.add(new Edge(start, c1, 0));
		edges.add(new Edge(start, c2, 0));
		edges.add(new Edge(start, c3, 0));
		edges.add(new Edge(start, ps, 0));
		edges.add(new Edge(c1, c1_vals, 0));
		edges.add(new Edge(c2, c2_vals, 0));
		edges.add(new Edge(c3, c3_vals, 0));
		edges.add(new Edge(ps, ps_vals, 0));
		ParameterGraph pspace = new ParameterGraph(nodes, edges, parameters, start);
		Random rng = new Random();
		
		API api = new API();
		api.connect("localhost", 3306, "EDACC", "edacc", "edaccteam");
		LinkedList<Instance> instances = InstanceDAO.getAllByExperimentId(8);
		LinkedList<Integer> job_ids = new LinkedList<Integer>();
		for (int i = 0; i < 100; i++) {
			int solver_config_id = api.createSolverConfig(8, 10, pspace.getRandomConfiguration(rng), i);
			for (Instance inst: instances) {
				int run = 0;
				for (int j = 0; j < 1; j++) {
					job_ids.add(api.launchJob(8, solver_config_id, inst.getId(), BigInteger.valueOf(rng.nextInt(234567892)), 10, run++));
				}
			}
		}*/
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
	
	public ParameterGraph loadParameterGraph(String xmlFileName) throws FileNotFoundException, JAXBException {
		FileInputStream fis = new FileInputStream(xmlFileName);
		ParameterGraph unm = unmarshal(ParameterGraph.class, fis);
		unm.buildAdjacencyList();
		return unm;
	}
	
	public <T> T unmarshal( Class<T> docClass, InputStream inputStream )
    	throws JAXBException {
		JAXBContext jc = JAXBContext.newInstance( docClass);
		Unmarshaller u = jc.createUnmarshaller();
		return (T)u.unmarshal(inputStream);
	}
}
