package edacc.api;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import edacc.api.costfunctions.Average;
import edacc.api.costfunctions.CostFunction;
import edacc.api.costfunctions.Median;
import edacc.api.costfunctions.PARX;
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
public class APIImpl implements API {
	private static DatabaseConnector db = DatabaseConnector.getInstance();

	/**
	 * Establishes the database connection.
	 * @param hostname
	 * @param port
	 * @param database
	 * @param username
	 * @param password
	 * @return
	 */
	public synchronized boolean connect(String hostname, int port, String database, String username, String password) throws Exception {
		db.connect(hostname, port, username, database, password, false, false, 8, false);
		return db.isConnected();
	}
	
	/**
	 * Closes the database connection.
	 */
	public synchronized void disconnect() {
		db.disconnect();
	}
	
	/**
	 * Returns a canonical name of the given parameter configuration in the context
	 * of the configuration experiment.
	 * This means only values that are to be configured are actually appearing in the name.
	 * @param idExperiment
	 * @param config
	 * @return
	 * @throws Exception
	 */
	public synchronized String getCanonicalName(int idExperiment, ParameterConfiguration config) throws Exception {
	    ConfigurationScenario cs = ConfigurationScenarioDAO.getConfigurationScenarioByExperimentId(idExperiment);
	    StringBuilder name = new StringBuilder();
	    List<ConfigurationScenarioParameter> params = cs.getParameters();
	    Collections.sort(params);
	    for (ConfigurationScenarioParameter param: params) {
	        if ("instance".equals(param.getParameter().getName()) || "seed".equals(param.getParameter().getName())) continue;
	        if (param.isConfigurable()) {
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
                else if (FlagDomain.FLAGS.ON.equals(config.getParameterValue(config_param))) {
                    name.append(param.getParameter().getPrefix());
                    if (param != params.get(params.size() - 1)) name.append(" ");
                }
                else {
                    name.append(param.getParameter().getPrefix());
                    if (param.getParameter().getSpace()) name.append(" ");
                    name.append(config.getParameterValue(config_param).toString());
                    if (param != params.get(params.size() - 1)) name.append(" ");
                }
	        }
	        
	    }
	    return name.toString();
	}
	
	/**
	 * Creates a new solver configuration in the database for the experiment specified by the idExperiment argument.
	 * The solver binary of the configuration is determined by the configuration scenario that the user created in the GUI.
	 * The parameters values are assigned by looping over the parameters that were chosen in the GUI for the configuration scenario.
	 * Non-configurable parameters take on the value that the user specified as "fixed value" while configurable parameters
	 * take on the values that are specified in the ParameterConfiguration config that is passed to this function.
	 * @param idExperiment ID of the experiment for which to create a solver configuration.
	 * @param config parameter configuration object that specifies the values of parameters.
	 * @param name name for the new solver configuration (used to display)
	 * @return unique database ID > 0 of the created solver configuration, 0 on errors.
	 */
	public synchronized int createSolverConfig(int idExperiment, ParameterConfiguration config, String name) throws Exception {
		ConfigurationScenario cs = ConfigurationScenarioDAO.getConfigurationScenarioByExperimentId(idExperiment);
		SolverBinaries solver_binary = SolverBinariesDAO.getById(cs.getIdSolverBinary());
		MessageDigest md = MessageDigest.getInstance("SHA");
		
        List<ConfigurationScenarioParameter> params = cs.getParameters();
        Collections.sort(params);
        for (ConfigurationScenarioParameter param: params) {
            if ("instance".equals(param.getParameter().getName()) || "seed".equals(param.getParameter().getName())) continue;
            if (param.isConfigurable()) {
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
                
                if (config.getParameterValue(config_param) != null && 
                        !(config.getParameterValue(config_param) instanceof OptionalDomain.OPTIONS) &&
                        !(config.getParameterValue(config_param).equals(FlagDomain.FLAGS.OFF))) {
                        md.update(config.getParameterValue(config_param).toString().getBytes());
                }
            }
        }
        
        SolverConfiguration solver_config = SolverConfigurationDAO.createSolverConfiguration(solver_binary, idExperiment, 0, name, null, null, toHex(md.digest()));
		
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
				
                if (config.getParameterValue(config_param) != null && 
                        !(config.getParameterValue(config_param) instanceof OptionalDomain.OPTIONS) &&
                        !(config.getParameterValue(config_param).equals(FlagDomain.FLAGS.OFF))) {
                        md.update(config.getParameterValue(config_param).toString().getBytes());
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

	/**
	 * Creates a new job with the given parameters and marks it as ready for computation.
	 * @param idExperiment ID of the experiment that should contain the job.
	 * @param idSolverConfig ID of the solver configuration.
	 * @param idInstance ID of the instance.
	 * @param seed integer seed that is assigned to the seed parameter of the solver configuration, if the seed parameter was activated.
	 * @param cpuTimeLimit time limit of the job in CPU seconds.
	 * @return unique database ID > 0 of the created job, 0 on errors.
	 */
	public synchronized int launchJob(int idExperiment, int idSolverConfig, int idInstance, BigInteger seed, int cpuTimeLimit) throws Exception {
		ExperimentResult job = ExperimentResultDAO.createExperimentResult(getCurrentMaxRun(idSolverConfig, idInstance) + 1, 0, 0, StatusCode.NOT_STARTED, seed.intValue(), ResultCode.UNKNOWN, 0, idSolverConfig, idExperiment, idInstance, null, cpuTimeLimit, -1, -1, -1, -1, -1);
		ArrayList<ExperimentResult> l = new ArrayList<ExperimentResult>();
		l.add(job);
		ExperimentResultDAO.batchSave(l);
		return job.getId();
	}
	
	/**
	 * Creates a new job for the given solver configuration
	 * in the instance-seed parcour of the given experiment.
	 */
	public synchronized int launchJob(int idExperiment, int idSolverConfig, int cpuTimeLimit, Random rng) throws Exception {
	    ConfigurationScenario cs = ConfigurationScenarioDAO.getConfigurationScenarioByExperimentId(idExperiment);
	    if (cs == null) return 0;
	    List<ExperimentResult> jobs = ExperimentResultDAO.getAllBySolverConfiguration(SolverConfigurationDAO.getSolverConfigurationById(idSolverConfig));
	    Course course = cs.getCourse();
	    int courseLength = 0;
	    for (ExperimentResult er: jobs) {
	        for (int cix = 0; cix < course.getLength(); cix++) {
	            if (er.getInstanceId() == course.get(cix).instance.getId() && er.getSeed() == course.get(cix).seed) {
	                courseLength += 1;
	            }
	        }
	    }
	    if (courseLength == course.getLength()) {
	    	// the instances that are part of the initial course are reused in extension
	    	Instance instance = course.get(courseLength % course.getInitialLength()).instance;
	    	int seed = rng.nextInt(Integer.MAX_VALUE);
	    	PreparedStatement st = DatabaseConnector.getInstance().getConn().prepareStatement("INSERT INTO Course (ConfigurationScenario_idConfigurationScenario, Instances_idInstance, seed, `order`) VALUES (?, ?, ?, ?)");
	    	st.setInt(1, cs.getId());
	    	st.setInt(2, instance.getId());
	    	st.setInt(3, seed);
	    	st.setInt(4, courseLength);
	    	st.executeUpdate();
	    	st.close();
	    	course.add(new InstanceSeed(instance, seed));
	    }
	    InstanceSeed is = course.get(courseLength);
	    return launchJob(idExperiment, idSolverConfig, is.instance.getId(), BigInteger.valueOf(is.seed), cpuTimeLimit);
	}
	
    /**
     * returns the length of the instance-seed course of the configuration experiment
     * @return
     * @throws Exception
     */
    public int getCourseLength(int idExperiment) throws Exception {
        ConfigurationScenario cs = ConfigurationScenarioDAO.getConfigurationScenarioByExperimentId(idExperiment);
        if (cs == null) return 0;
        return cs.getCourse().getLength();
    }
	
	/**
	 * Creates numberRuns new jobs for the given solver configuration
	 * in the given experiment.
	 * @param idExperiment
	 * @param idSolverConfig
	 * @param cpuTimeLimit
	 * @param numberRuns
	 * @return
	 * @throws Exception
	 */
	public synchronized List<Integer> launchJob(int idExperiment, int idSolverConfig, int[] cpuTimeLimit, int numberRuns, Random rng) throws Exception {
	    List<Integer> ids = new ArrayList<Integer>();
	    for (int i = 0; i < numberRuns; i++) {
	        ids.add(launchJob(idExperiment, idSolverConfig, cpuTimeLimit[i], rng));
	    }
	    return ids;
	}
	
	/**
	 * Returns the parameter configuration corresponding to the given solver configuration in the DB.
	 * @param idExperiment
	 * @param idSolverConfig
	 */
	public synchronized ParameterConfiguration getParameterConfiguration(int idExperiment, int idSolverConfig) throws Exception {
		ParameterGraph graph = loadParameterGraphFromDB(idExperiment);
		ParameterConfiguration config = new ParameterConfiguration(graph.getParameterSet());
		ConfigurationScenario cs = ConfigurationScenarioDAO.getConfigurationScenarioByExperimentId(idExperiment);
		SolverConfiguration solver_config = SolverConfigurationDAO.getSolverConfigurationById(idSolverConfig);
		if (solver_config == null) return null;
		
		// map ParameterID -> Parameter
		Map<Integer, edacc.model.Parameter> parameter_map = new HashMap<Integer, edacc.model.Parameter>();
		for (edacc.model.Parameter p: ParameterDAO.getParameterFromSolverId(solver_config.getSolverBinary().getIdSolver())) {
			parameter_map.put(p.getId(), p);
		}
		
		// map Parameter name -> Parameter Instance (value)
		Map<String, ParameterInstance> solver_config_param_map = new HashMap<String, ParameterInstance>();
		for (ParameterInstance p: ParameterInstanceDAO.getBySolverConfig(solver_config)) {
			solver_config_param_map.put(parameter_map.get(p.getParameter_id()).getName(), p);
		}
		
		Map<String, edacc.parameterspace.Parameter> pgraph_map = graph.getParameterMap();
		
		for (ConfigurationScenarioParameter param: cs.getParameters()) {
			if (param.isConfigurable() && !"instance".equals(param.getParameter().getName()) && !"seed".equals(param.getParameter().getName())) {
				String parameter_name = param.getParameter().getName();
				if (!param.getParameter().getHasValue()) { // this is a flag which is ON in this solver config
					config.setParameterValue(parameter_name, FlagDomain.FLAGS.ON);
				}
				else { // standard parameter with a value
					String value = solver_config_param_map.get(param.getParameter().getName()).getValue();
					if (pgraph_map.get(param.getParameter().getName()).getDomain().contains(value)) {
					    // string should be fine for this domain
					    config.setParameterValue(parameter_name, value);
					} else {
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
		}
		Random rng = new Random();
		// set parameters that are not part of the configuration scenario to some
		// random values since they should not matter (will be replaced by fixed values or not appear at all)
		for (edacc.parameterspace.Parameter p: pgraph_map.values()) {
		    if (config.getParameterValue(p) == null) config.setParameterValue(p, p.getDomain().randomValue(rng));
		}
		config.updateChecksum();
		return config;
	}
	
	/**
	 * If the given parameter configuration already has a corresponding solver configuration
	 * in the given experiment this function will return the ID of the solver configuration and
	 * 0 otherwise. 
	 * @param idExperiment
	 * @param config
	 * @return
	 */
	public synchronized int exists(int idExperiment, ParameterConfiguration config) throws Exception {
	    MessageDigest md = MessageDigest.getInstance("SHA");
        ConfigurationScenario cs = ConfigurationScenarioDAO.getConfigurationScenarioByExperimentId(idExperiment);
        List<ConfigurationScenarioParameter> params = cs.getParameters();
        Collections.sort(params);
        for (ConfigurationScenarioParameter param: params) {
            if ("instance".equals(param.getParameter().getName()) || "seed".equals(param.getParameter().getName())) continue;
            if (param.isConfigurable()) {
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
                
                if (config.getParameterValue(config_param) != null && 
                        !(config.getParameterValue(config_param) instanceof OptionalDomain.OPTIONS) &&
                        !(config.getParameterValue(config_param).equals(FlagDomain.FLAGS.OFF))) {
                        md.update(config.getParameterValue(config_param).toString().getBytes());
                }
            }
            
        }
        
		SolverConfiguration sc = SolverConfigurationDAO.getByParameterHash(idExperiment, toHex(md.digest()));
		if (sc != null) return sc.getId();
		return 0;
	}
	
	/**
	 * return the current number of jobs of the given solver configuration and 0, if it doesn't exist
	 * @param idSolverConfig
	 * @return
	 */
	public synchronized int getNumJobs(int idSolverConfig) throws Exception {
		return ExperimentResultDAO.getNumJobsBySolverConfigurationId(idSolverConfig);
	}
	
	/**
	 * Updates the cost of the given solver configuration.
	 * The cost function is also saved in the table.
	 * @param idSolverConfig
	 * @param cost
	 * @param func
	 */
	public synchronized void updateSolverConfigurationCost(int idSolverConfig, float cost, CostFunction func) throws Exception {
		PreparedStatement st = db.getConn().prepareStatement("UPDATE SolverConfig SET cost=?, cost_function=? WHERE idSolverConfig=?");
		st.setFloat(1, cost);
		st.setString(2, func.databaseRepresentation());
		st.setInt(3, idSolverConfig);
		st.executeUpdate();
		st.close();
	}
	
	/**
	 * Returns the cost function of the given solver configuration as saved in the database
	 * @param idSolverConfig
	 * @return
	 */
	public synchronized CostFunction getCostFunction(int idSolverConfig) throws Exception {
		PreparedStatement st = db.getConn().prepareStatement("SELECT cost_function FROM SolverConfig WHERE idSolverConfig=?");
		st.setInt(1, idSolverConfig);
		ResultSet rs = st.executeQuery();
		if (rs.next()) {
			String func = rs.getString("cost_function");
			rs.close();
			st.close();
			return costFunctionByName(func);
		}
		rs.close();
		st.close();
		return null;
	}
	
	/**
	 * Returns the current cost value of the solver configuration as saved in the database.
	 * Can return null if the DB value is NULL.
	 * @param idSolverConfig
	 * @return
	 */
	public synchronized Float getSolverConfigurationCost(int idSolverConfig) throws Exception {
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
		return null;
	}
	
	/**
	 * Retrieves and returns a job from the database by it's unique ID regardless
	 * of its computation status.
	 * @param idJob ID of the job
	 * @return the job as edacc.model.ExperimentResult object.
	 */
	public synchronized ExperimentResult getJob(int idJob) throws Exception {
	    return ExperimentResultDAO.getByIdWithoutAssign(idJob);
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
	public synchronized ExperimentResult killJob(int idJob) throws Exception {
		ExperimentResult er = ExperimentResultDAO.getById(idJob);
		if (!(er.getStatus().equals(StatusCode.NOT_STARTED) || er.getStatus().equals(StatusCode.RUNNING))) return er;
		if (er.getIdClient() != null) {
			ClientDAO.sendMessage(er.getIdClient(), "kill " + er.getId());
		}
		return null;
	}
	
    /**
     * reset the job with the given ID to "not started".
     * @param idJob
     * @throws Exception
     */
    public void restartJob(int idJob, int CPUTimeLimit) throws Exception {
        ExperimentResult er = ExperimentResultDAO.getById(idJob);
        if (er == null) return;
        er.setCPUTimeLimit(CPUTimeLimit);
        ArrayList<ExperimentResult> jobs = new ArrayList<ExperimentResult>();
        jobs.add(er);
        ExperimentResultDAO.batchUpdateCPUTimeLimit(jobs);
        ExperimentResultDAO.batchUpdateStatus(jobs, StatusCode.NOT_STARTED);
    }

    /**
	 * Deletes a job no matter its computation status.
	 * @param idJob ID of the job to delete.
	 * @return
	 */
	public synchronized boolean deleteResult(int idJob) throws Exception {
		ExperimentResult er = ExperimentResultDAO.getByIdWithoutAssign(idJob);
		if (er == null) return false;
		ArrayList<ExperimentResult> l = new ArrayList<ExperimentResult>();
		l.add(er);
		ExperimentResultDAO.deleteExperimentResults(l);
		return true;
	}
	
	/**
	 * Returns all jobs of the solver configuration specified by the idSolverConfig argument.
	 * @param idExperiment
	 * @param idSolverConfig
	 * @return
	 */
	public synchronized ArrayList<ExperimentResult> getRuns(int idExperiment, int idSolverConfig) throws Exception {
		ConfigurationScenario cs = ConfigurationScenarioDAO.getConfigurationScenarioByExperimentId(idExperiment);
		ArrayList<ExperimentResult> orderedResults = new ArrayList<ExperimentResult>();
		List<ExperimentResult> results = ExperimentResultDAO.getAllBySolverConfiguration(SolverConfigurationDAO.getSolverConfigurationById(idSolverConfig));
		for (InstanceSeed isp: cs.getCourse().getInstanceSeedList()) {
			for (ExperimentResult res: results) {
				if (res.getInstanceId() == isp.instance.getId() && res.getSeed() == isp.seed) {
					orderedResults.add(res);
					break;
				}
			}
		}
		return orderedResults;
	}
	
	/**
	 * Returns a map with all jobs from the database with IDs specified in the ids argument.
	 * @param ids
	 * @return map with (job_id -> ExperimentResult) mapping
	 */
	public synchronized Map<Integer, ExperimentResult> getJobsByIDs(List<Integer> ids) throws Exception {
		Map<Integer, ExperimentResult> jobs = new HashMap<Integer, ExperimentResult>();
		List<ExperimentResult> results = ExperimentResultDAO.getByIds(ids);
		for (ExperimentResult result: results) {
			jobs.put(result.getId(), result);
		}
		return jobs;
	}
	
	/**
	 * Returns the instances of the given experiment as list.
	 * @param idExperiment
	 * @return
	 * @throws Exception
	 */
	public synchronized List<Instance> getExperimentInstances(int idExperiment) throws Exception {
		return InstanceDAO.getAllByExperimentId(idExperiment);
	}
	
	/**
	 * Returns the name of the configuration with the given ID,
	 * or null, if it doesn't exist
	 * @param idSolverConfig
	 * @return
	 */
	public synchronized String getSolverConfigName(int idSolverConfig) throws Exception {
	    SolverConfiguration config = SolverConfigurationDAO.getSolverConfigurationById(idSolverConfig);
	    if (config != null) return config.getName();
	    return null;
	}
	
	/**
	 * Returns the ID of the best configuration with cost function @func of the given experiment.
	 * Returns 0 if there's no configuration with the given cost function 
	 * @param idExperiment
	 * @param func
	 * @return
	 */
	public synchronized int getBestConfiguration(int idExperiment, CostFunction func) throws Exception {
        PreparedStatement st = db.getConn().prepareStatement("SELECT idSolverConfig FROM SolverConfig WHERE Experiment_idExperiment=? AND cost_function=? AND cost IS NOT NULL ORDER BY cost LIMIT 1");
        st.setInt(1, idExperiment);
        st.setString(2, func.databaseRepresentation());
        ResultSet rs = st.executeQuery();
        if (rs.next()) {
            int id = rs.getInt("idSolverConfig");
            rs.close();
            st.close();
            return id;
        }
        rs.close();
        st.close();
        return 0;
    }

    /**
     * Returns the IDs of the #no best configuration with cost function @func of the given experiment.
     * @param idExperiment
     * @param func
     * @param no how many
     * @return
     */
    public List<Integer> getBestConfigurations(int idExperiment,
    		CostFunction func, int no) throws Exception {
        PreparedStatement st = db.getConn().prepareStatement("SELECT idSolverConfig FROM SolverConfig WHERE Experiment_idExperiment=? AND cost_function=? AND cost IS NOT NULL ORDER BY cost LIMIT ?");
        st.setInt(1, idExperiment);
        st.setString(2, func.databaseRepresentation());
        st.setInt(3, no);
        ResultSet rs = st.executeQuery();
        List<Integer> best = new ArrayList<Integer>(); 
        while (rs.next()) {
            best.add(rs.getInt("idSolverConfig"));
        }
        rs.close();
        st.close();
        return best;
    }

    /**
	 * Loads the parameter graph object of the solver binary selected in the configuration experiment
	 * specified by the idExperiment argument. Parameter graphs objects provide methods
	 * to build, modify and validate solver parameters.
	 * @param idExperiment ID of the configuration experiment
	 * @return parameter graph object providing parameter space methods.
	 */
	public synchronized ParameterGraph loadParameterGraphFromDB(int idExperiment) throws Exception {
	    ConfigurationScenario cs = ConfigurationScenarioDAO.getConfigurationScenarioByExperimentId(idExperiment);
        Statement st = db.getConn().createStatement();

        ResultSet rs = st.executeQuery("SELECT serializedGraph FROM ConfigurationScenario JOIN SolverBinaries ON SolverBinaries_idSolverBinary=idSolverBinary JOIN ParameterGraph ON SolverBinaries.idSolver=ParameterGraph.Solver_idSolver WHERE Experiment_idExperiment = " + idExperiment);
        try {
            if (rs.next()) {
                ParameterGraph pg = unmarshal(ParameterGraph.class, rs.getBlob("serializedGraph").getBinaryStream());
                pg.buildAdjacencyList();

                Set<edacc.parameterspace.Parameter> fixedParams = new HashSet<edacc.parameterspace.Parameter>();
                List<ConfigurationScenarioParameter> params = cs.getParameters();
                Collections.sort(params);
                for (ConfigurationScenarioParameter param: params) {
                    if ("instance".equals(param.getParameter().getName()) || "seed".equals(param.getParameter().getName())) continue;
                    if (!param.isConfigurable()) {
                        // fixed parameter
                        edacc.parameterspace.Parameter config_param = null;
                        for (edacc.parameterspace.Parameter p: pg.getParameterSet()) {
                            if (p.getName().equals(param.getParameter().getName())) {
                                config_param = p;
                                break;
                            }
                        }
                        if (config_param == null) {
                            continue;
                        }

                        fixedParams.add(config_param);
                    }
                }
                pg.setFixedParameters(fixedParams);
                rs.close();
                st.close();
                return pg;
            }
        } catch (JAXBException e) {
			throw e;
		} finally {
        	rs.close();
            st.close();
        }
		return null;
	}

	/**
	 * Loads a parameter graph from a XML file.
	 * @param xmlFileName
	 * @return
	 * @throws FileNotFoundException
	 */
	public synchronized ParameterGraph loadParameterGraphFromFile(String xmlFileName) throws Exception {
		FileInputStream fis = new FileInputStream(xmlFileName);
		ParameterGraph unm;
		try {
			unm = unmarshal(ParameterGraph.class, fis);
		} catch (JAXBException e) {
			throw e;
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
	private synchronized int getCurrentMaxRun(int idSolverConfig, int idInstance) throws Exception {
		PreparedStatement ps = db.getConn().prepareStatement("SELECT MAX(run) FROM ExperimentResults WHERE SolverConfig_idSolverConfig=? AND Instances_idInstance=?");
		ps.setInt(1, idSolverConfig);
		ps.setInt(2, idInstance);
		ResultSet rs = ps.executeQuery();
		if (rs.next()) {
			if (rs.getObject(1) == null) {
				rs.close();
				ps.close();
				return -1;
			}
			int res = rs.getInt(1);
			rs.close();
			ps.close();
			return res;
		}
		rs.close();
		ps.close();
		return -1;
	}

	public String toHex(byte[] bytes) {
		if (bytes == null) return "";
	    BigInteger bi = new BigInteger(1, bytes);
	    return String.format("%0" + (bytes.length << 1) + "X", bi);
	}

	public CostFunction costFunctionByName(
			String databaseRepresentation) {
		if ("average".equals(databaseRepresentation)) {
			return new Average();
		} else if ("median".equals(databaseRepresentation)) {
			return new Median();
		} else if (databaseRepresentation != null && databaseRepresentation.startsWith("par")) {
			try {
				int penaltyFactor = Integer.valueOf(databaseRepresentation.substring(3));
				return new PARX(penaltyFactor);
			} catch (Exception e) {
				return null;
			}
		}
		return null;
	}
	
	public void updateSolverConfigurationName(int idSolverConfig, String name) throws Exception {
		PreparedStatement ps = db.getConn().prepareStatement("UPDATE SolverConfig SET name = ? WHERE idSolverConfig = ?");
		ps.setString(1, name);
		ps.setInt(2, idSolverConfig);
		ps.executeUpdate();
		ps.close();
	}

	@Override
	public void removeSolverConfig(int idSolverConfig) throws Exception {
		Statement st = db.getConn().createStatement();
		st.executeUpdate("DELETE FROM SolverConfig WHERE idSolverConfig = " + idSolverConfig);
		st.close();
	}
}
