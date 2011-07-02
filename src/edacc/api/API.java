package edacc.api;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
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
import edacc.properties.PropertyTypeNotExistException;

public class API {
	private static DatabaseConnector db = DatabaseConnector.getInstance();

	public boolean connect(String hostname, int port, String database, String username, String password) {
		try {
			db.connect(hostname, port, username, database, password, false, false, 8);
			return true;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
	public void disconnect() {
		db.disconnect();
	}
	
	public int createSolverConfig(int experiment_id, ParameterConfiguration config) {
		try {
			ConfigurationScenario cs = ConfigurationScenarioDAO.getConfigurationScenarioByExperimentId(experiment_id);
			SolverBinaries solver_binary = SolverBinariesDAO.getById(cs.getIdSolverBinary());
			SolverConfiguration solver_config = SolverConfigurationDAO.createSolverConfiguration(solver_binary, experiment_id, 0, solver_binary.getBinaryName());

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
				else {
					edacc.parameterspace.Parameter config_param = null;
					for (edacc.parameterspace.Parameter p: config.getParameter_instances().keySet()) {
						if (p.getName().equals(param.getParameter().getName())) {
							config_param = p;
							break;
						}
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

	public int launchJob(int experiment_id, int solver_config_id, int instance_id, BigInteger seed, int cpu_time_limit) {
		try {
			ExperimentResult job = ExperimentResultDAO.createExperimentResult(getCurrentMaxRun(solver_config_id, instance_id) + 1, 0, 0, StatusCode.NOT_STARTED, seed.intValue(), ResultCode.UNKNOWN, 0, solver_config_id, experiment_id, instance_id, null, cpu_time_limit, -1, -1, -1, -1, -1);
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
	
	public ExperimentResult killJob(int job_id) {
		try {
			ExperimentResult er = ExperimentResultDAO.getById(job_id);
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
	
	public boolean deleteResult(int job_id) {
		try {
			ExperimentResult er = ExperimentResultDAO.getById(job_id);
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
	
	public ArrayList<ExperimentResult> getRuns(int solver_config_id) {
		try {
			return ExperimentResultDAO.getAllBySolverConfiguration(SolverConfigurationDAO.getSolverConfigurationById(solver_config_id));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public ParameterGraph loadParameterGraphFromDB(int experiment_id) {
		try {
            Statement st = db.getConn().createStatement();
    
            ResultSet rs = st.executeQuery("SELECT serializedGraph FROM ConfigurationScenario JOIN SolverBinaries ON SolverBinaries_idSolverBinary=idSolverBinary JOIN ParameterGraph ON SolverBinaries.idSolver=ParameterGraph.Solver_idSolver WHERE Experiment_idExperiment = " + experiment_id);
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
	
	@SuppressWarnings("unchecked")
	private <T> T unmarshal( Class<T> docClass, InputStream inputStream )
    	throws JAXBException {
		JAXBContext jc = JAXBContext.newInstance( docClass);
		Unmarshaller u = jc.createUnmarshaller();
		return (T)u.unmarshal(inputStream);
	}
	
	private int getCurrentMaxRun(int solver_config_id, int instance_id) {
		try {
    		PreparedStatement ps = db.getConn().prepareStatement("SELECT MAX(run) FROM ExperimentResults WHERE SolverConfig_idSolverConfig=? AND Instances_idInstance=?");
    		ps.setInt(1, solver_config_id);
    		ps.setInt(2, instance_id);
    		ResultSet rs = ps.executeQuery();
    		if (rs.next()) {
    			return rs.getInt(1);
    		}
		} catch (SQLException e) {
			e.printStackTrace();
			return -1;
		}
		return -1;
	}
}
