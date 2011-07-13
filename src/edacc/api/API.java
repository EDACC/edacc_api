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
			SolverConfiguration solver_config = SolverConfigurationDAO.createSolverConfiguration(solver_binary, idExperiment, 0, name);

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
}
