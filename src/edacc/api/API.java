package edacc.api;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import edacc.model.*;
import edacc.parameterspace.domain.*;
import edacc.parameterspace.graph.ParameterGraph;
import edacc.parameterspace.ParameterConfiguration;

/**
 * EDACC Configurator database API.
 * 
 * This API provides methods to create and retrieve solver configurations and
 * jobs for "configuration experiments" that were created using the EDACC GUI application.
 * 
 */
public class API {
	private static DatabaseConnector db = DatabaseConnector.getInstance();
	
	public static enum COST_FUNCTIONS {
		// Enumeration of cost functions.
		// This enumeration (i.e. the values of the toString methods)
		// should match the enumeration specified in the database schema
		AVERAGE {
			@Override
			public String toString() {
				return "average";
			}
		},
		MIN {
			@Override
			public String toString() {
				return "min";
			}
		}
	}

	/**
	 * Establishes the database connection.
	 * @param hostname
	 * @param port
	 * @param database
	 * @param username
	 * @param password
	 * @return
	 */
	public synchronized boolean connect(String hostname, int port, String database, String username, String password) {
		try {
			db.connect(hostname, port, username, database, password, false, false, 8, false);
			return true;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
	/**
	 * Closes the database connection.
	 */
	public synchronized void disconnect() {
		db.disconnect();
	}
	
	/**
	 * Creates a new solver configuration in the database for the experiment specified by the idExperiment argument.
	 * The solver binary of the configuration is determined by the configuration scenario that the user created in the GUI.
	 * The parameters values are assigned by looping over the parameters that were chosen in the GUI for the configuration scenario.
	 * Non-configurable parameters take on the value that the user specified as "fixed value" while configurable parameters
	 * take on the values that are specified in the ParameterConfiguration config that is passed to this function.
	 * @param idExperiment ID of the experiment for which to create a solver configuration.
	 * @param config parameter configuration object that specifies the values of parameters.
	 * @return unique database ID > 0 of the created solver configuration, 0 on errors.
	 */
	public synchronized int createSolverConfig(int idExperiment, ParameterConfiguration config, String name) {
		try {
			ConfigurationScenario cs = ConfigurationScenarioDAO.getConfigurationScenarioByExperimentId(idExperiment);
			SolverBinaries solver_binary = SolverBinariesDAO.getById(cs.getIdSolverBinary());
			config.updateChecksum(); // TODO: needed here?
			SolverConfiguration solver_config = SolverConfigurationDAO.createSolverConfiguration(solver_binary, idExperiment, 0, name, null, null, toHex(config.getChecksum()));

			for (ConfigurationScenarioParameter param: cs.getParameters()) {
				if ("instance".equals(param.getParameter().getName()) || "seed".equals(param.getParameter().getName())) {
					ParameterInstance pi = ParameterInstanceDAO.createParameterInstance(param.getParameter().getId(), solver_config, "");
					ParameterInstanceDAO.save(pi);
				}
				else if (!param.isConfigurable()) {
					if (param.getParameter().getHasValue()) {
						ParameterInstance pi = ParameterInstanceDAO.createParameterInstance(param.getParameter().getId(), solver_config, param.getFixedValue());
						ParameterInstanceDAO.save(pi);
					}
					else { // flag
						ParameterInstance pi = ParameterInstanceDAO.createParameterInstance(param.getParameter().getId(), solver_config, "");
						ParameterInstanceDAO.save(pi);
					}
				}
				else if (param.isConfigurable()) {
					edacc.parameterspace.Parameter config_param = null;
					for (edacc.parameterspace.Parameter p: config.getParameter_instances().keySet()) {
						if (p.getName().equals(param.getParameter().getName())) {
							config_param = p;
							break;
						}
					}
					if (config_param == null) {
						continue;
					}
					
					if (OptionalDomain.OPTIONS.NOT_SPECIFIED.equals(config.getParameterValue(config_param))) continue;
					else if (FlagDomain.FLAGS.OFF.equals(config.getParameterValue(config_param))) continue;
					else {
						ParameterInstance pi = ParameterInstanceDAO.createParameterInstance(param.getParameter().getId(), solver_config, config.getParameterValue(config_param).toString());
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

	/**
	 * Creates a new job with the given parameters and marks it as ready for computation.
	 * @param idExperiment ID of the experiment that should contain the job.
	 * @param idSolverConfig ID of the solver configuration.
	 * @param idInstance ID of the instance.
	 * @param seed integer seed that is assigned to the seed parameter of the solver configuration, if the seed parameter was activated.
	 * @param cpuTimeLimit time limit of the job in CPU seconds.
	 * @return unique database ID > 0 of the created job, 0 on errors.
	 */
	public synchronized int launchJob(int idExperiment, int idSolverConfig, int idInstance, BigInteger seed, int cpuTimeLimit) {
		try {
			ExperimentResult job = ExperimentResultDAO.createExperimentResult(getCurrentMaxRun(idSolverConfig, idInstance) + 1, 0, 0, StatusCode.NOT_STARTED, seed.intValue(), ResultCode.UNKNOWN, 0, idSolverConfig, idExperiment, idInstance, null, cpuTimeLimit, -1, -1, -1, -1, -1);
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
	
	/**
	 * Creates a new job for the given solver configuration
	 * in the instance-seed parcour of the given experiment.
	 */
	public synchronized int launchJob(int idExperiment, int idSolverConfig, int cpuTimeLimit) {
		// TODO: implement
		// find next Instance-Seed pair in the parcour, create job.
		// Count jobs of the solver configuration -> n jobs, find the (n+1)th parcour
		// item. if that doesn't exist extend the parcour, otherwise create new job with
		// that instance/seed pair
		return 0;
	}
	
	/**
	 * Returns the parameter configuration corresponding to the given solver configuration in the DB.
	 * @param idExperiment
	 * @param idSolverConfig
	 */
	public synchronized ParameterConfiguration getParameterConfiguration(int idExperiment, int idSolverConfig) {
		try {
			ParameterGraph graph = loadParameterGraphFromDB(idExperiment);
			ParameterConfiguration config = new ParameterConfiguration(graph.getParameterSet());
			ConfigurationScenario cs = ConfigurationScenarioDAO.getConfigurationScenarioByExperimentId(idExperiment);
			SolverConfiguration solver_config = SolverConfigurationDAO.getSolverConfigurationById(idSolverConfig);
			
			// map ParameterID -> Parameter
			Map<Integer, Parameter> parameter_map = new HashMap<Integer, Parameter>();
			for (Parameter p: ParameterDAO.getParameterFromSolverId(solver_config.getSolverBinary().getIdSolver())) {
				parameter_map.put(p.getId(), p);
			}
			
			// map Parameter name -> Parameter Instance (value)
			Map<String, ParameterInstance> solver_config_param_map = new HashMap<String, ParameterInstance>();
			for (ParameterInstance p: ParameterInstanceDAO.getBySolverConfig(solver_config)) {
				solver_config_param_map.put(parameter_map.get(p.getParameter_id()).getName(), p);
			}
			
			for (ConfigurationScenarioParameter param: cs.getParameters()) {
				if (param.isConfigurable()) {
					String parameter_name = param.getParameter().getName();
					if (!param.getParameter().getHasValue()) { // this is a flag which is ON in this solver config
						config.setParameterValue(parameter_name, FlagDomain.FLAGS.ON);
					}
					else { // standard parameter with a value
						String value = solver_config_param_map.get(param.getParameter().getName()).getValue();
						try {
							int i = Integer.valueOf(value);
							config.setParameterValue(parameter_name, i);
						} catch (NumberFormatException e) {
							try {
								double f = Double.valueOf(value);
								config.setParameterValue(parameter_name, f);
							}
							catch (NumberFormatException e2) {
								config.setParameterValue(parameter_name, value);
							}
						}
					}
				}
			}
			config.updateChecksum();
			return config;
		
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * If the given parameter configuration already has a corresponding solver configuration
	 * in the given experiment this function will return the ID of the solver configuration and
	 * 0 otherwise. 
	 * @param idExperiment
	 * @param config
	 * @return
	 */
	public synchronized int exists(int idExperiment, ParameterConfiguration config) {
		try {
			SolverConfiguration sc = SolverConfigurationDAO.getByParameterHash(idExperiment, toHex(config.getChecksum()));
			if (sc != null) return sc.getId();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;
	}
	
	/**
	 * return the current number of jobs of the given solver configuration and 0, if it doesn't exist
	 * @param idSolverConfig
	 * @return
	 */
	public synchronized int getNumJobs(int idSolverConfig) {
		try {
			return ExperimentResultDAO.getNumJobsBySolverConfigurationId(idSolverConfig);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	/**
	 * Updates the cost of the given solver configuration.
	 * The cost function is also saved in the table.
	 * @param idSolverConfig
	 * @param cost
	 * @param func
	 */
	public synchronized void updateSolverConfigurationCost(int idSolverConfig, float cost, COST_FUNCTIONS func) {
		try {
			PreparedStatement st = db.getConn().prepareStatement("UPDATE SolverConfig SET cost=?, cost_function=? WHERE idSolverConfig=?");
			st.setFloat(1, cost);
			st.setString(2, func.toString());
			st.setInt(3, idSolverConfig);
			st.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Returns the cost function of the given solver configuration as saved in the database
	 * @param idSolverConfig
	 * @return
	 */
	public synchronized COST_FUNCTIONS getCostFunction(int idSolverConfig) {
		try {
			PreparedStatement st = db.getConn().prepareStatement("SELECT cost_function FROM SolverConfig WHERE idSolverConfig=?");
			st.setInt(1, idSolverConfig);
			ResultSet rs = st.executeQuery();
			if (rs.next()) {
				String func = rs.getString("cost_function");
				for (COST_FUNCTIONS f: COST_FUNCTIONS.values()) {
					if (f.equals(func)) {
						rs.close();
						st.close();
						return f;
					}
				}
			}
			rs.close();
			st.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Returns the current cost value of the solver configuration as saved in the database.
	 * Can return null if the DB value is NULL.
	 * @param idSolverConfig
	 * @return
	 */
	public synchronized Float getSolverConfigurationCost(int idSolverConfig) {
		try {
			PreparedStatement st = db.getConn().prepareStatement("SELECT cost FROM SolverConfig WHERE idSolverConfig=?");
			st.setInt(1, idSolverConfig);
			ResultSet rs = st.executeQuery();
			if (rs.next()) {
				float cost = rs.getFloat("cost");
				rs.close();
				st.close();
				return cost;
			}
			rs.close();
			st.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Retrieves and returns a job from the database by it's unique ID regardless
	 * of its computation status.
	 * @param idJob ID of the job
	 * @return the job as edacc.model.ExperimentResult object.
	 */
	public synchronized ExperimentResult getJob(int idJob) {
		try {
			return ExperimentResultDAO.getById(idJob);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Stops a job. If the job has already finished or crashed,
	 * the method returns the job object.
	 * If the job is currently being computed a kill message
	 * will be sent to the computing client.
	 * 
	 * This method does not block to wait until the job is killed. 
	 * There are no guarantees about the time it takes to kill a running job.
	 * 
	 * @param idJob ID of the job to kill.
	 * @return
	 */
	public synchronized ExperimentResult killJob(int idJob) {
		try {
			ExperimentResult er = ExperimentResultDAO.getById(idJob);
			if (!(er.getStatus().equals(StatusCode.NOT_STARTED) || er.getStatus().equals(StatusCode.RUNNING))) return er;
			if (er.getIdClient() != null) {
				ClientDAO.sendMessage(er.getIdClient(), "kill " + er.getId());
			}
			return null;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Deletes a job no matter its computation status.
	 * @param idJob ID of the job to delete.
	 * @return
	 */
	public synchronized boolean deleteResult(int idJob) {
		try {
			ExperimentResult er = ExperimentResultDAO.getById(idJob);
			if (er == null) return false;
			ArrayList<ExperimentResult> l = new ArrayList<ExperimentResult>();
			l.add(er);
			ExperimentResultDAO.deleteExperimentResults(l);
			return true;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return false;
	}
	
	/**
	 * Returns all jobs of the solver configuration specified by the idSolverConfig argument.
	 * @param idSolverConfig
	 * @return
	 */
	public synchronized ArrayList<ExperimentResult> getRuns(int idSolverConfig) {
		try {
			return ExperimentResultDAO.getAllBySolverConfiguration(SolverConfigurationDAO.getSolverConfigurationById(idSolverConfig));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Returns a map with all jobs from the database with IDs specified in the ids argument.
	 * @param ids
	 * @return map with (job_id -> ExperimentResult) mapping
	 */
	public synchronized Map<Integer, ExperimentResult> getJobsByIDs(Collection<Integer> ids) {
		Map<Integer, ExperimentResult> jobs = new HashMap<Integer, ExperimentResult>();
		try {
    		for (Integer id: ids) {
    			jobs.put(id, ExperimentResultDAO.getById(id)); // TODO: optimize
    		}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return jobs;
	}
	
	/**
	 * Loads the parameter graph object of the solver binary selected in the configuration experiment
	 * specified by the idExperiment argument. Parameter graphs objects provide methods
	 * to build, modify and validate solver parameters.
	 * @param idExperiment ID of the configuration experiment
	 * @return parameter graph object providing parameter space methods.
	 */
	public synchronized ParameterGraph loadParameterGraphFromDB(int idExperiment) {
		try {
            Statement st = db.getConn().createStatement();
    
            ResultSet rs = st.executeQuery("SELECT serializedGraph FROM ConfigurationScenario JOIN SolverBinaries ON SolverBinaries_idSolverBinary=idSolverBinary JOIN ParameterGraph ON SolverBinaries.idSolver=ParameterGraph.Solver_idSolver WHERE Experiment_idExperiment = " + idExperiment);
            try {
                if (rs.next()) {
                    ParameterGraph pg = unmarshal(ParameterGraph.class, rs.getBlob("serializedGraph").getBinaryStream());
                    pg.buildAdjacencyList();
                    return pg;
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

	/**
	 * Loads a parameter graph from a XML file.
	 * @param xmlFileName
	 * @return
	 * @throws FileNotFoundException
	 */
	public synchronized ParameterGraph loadParameterGraphFromFile(String xmlFileName) throws FileNotFoundException {
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
	
	/**
	 * Generic XML unmarshalling method.
	 * @param docClass
	 * @param inputStream
	 * @return
	 * @throws JAXBException
	 */
	@SuppressWarnings("unchecked")
	private <T> T unmarshal( Class<T> docClass, InputStream inputStream )
    	throws JAXBException {
		JAXBContext jc = JAXBContext.newInstance( docClass);
		Unmarshaller u = jc.createUnmarshaller();
		return (T)u.unmarshal(inputStream);
	}
	
	/**
	 * Returns "current number of runs - 1" of the solver configuration specified
	 * by the idSolverConfig argument on the instance specified by idInstance.
	 * @param idSolverConfig
	 * @param idInstance
	 * @return
	 */
	private synchronized int getCurrentMaxRun(int idSolverConfig, int idInstance) {
		try {
    		PreparedStatement ps = db.getConn().prepareStatement("SELECT MAX(run) FROM ExperimentResults WHERE SolverConfig_idSolverConfig=? AND Instances_idInstance=?");
    		ps.setInt(1, idSolverConfig);
    		ps.setInt(2, idInstance);
    		ResultSet rs = ps.executeQuery();
    		if (rs.next()) {
    			if (rs.getObject(1) == null) return -1;
    			return rs.getInt(1);
    		}
		} catch (SQLException e) {
			e.printStackTrace();
			return -1;
		}
		return -1;
	}

	public String toHex(byte[] bytes) {
		if (bytes == null) return "";
	    BigInteger bi = new BigInteger(1, bytes);
	    return String.format("%0" + (bytes.length << 1) + "X", bi);
	}

}
