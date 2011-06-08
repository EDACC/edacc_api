package edacc.api;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Vector;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import edacc.model.*;
import edacc.parameterspace.domain.*;
import edacc.parameterspace.graph.ParameterGraph;
import edacc.parameterspace.ParameterConfiguration;

public class API {
	private static DatabaseConnector db = DatabaseConnector.getInstance();

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
	
	/**
	 * Erstellt eine DB-SolverConfiguration aus einer experiment_id und einer ParameterConfiguration
	 * und gibt eine eindeutige ID zurück.
	 * Bisher sind auch noch solver_binary_id und seed_group anzugeben, das sollte aber dann wegfallen
	 * wenn diese Infos auch in der DB an dem Experiment hängen.  
	 */
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

	/**
	 * Startet einen Job im gegebenen Experiment für die DB-SolverConfiguration (referenziert durch solver_config_id) auf
	 * der Instanz (instance_id). Die run Zahl wird benutzt, falls mehrere Durchläufe pro Instanz und SolverConfig gemacht werden
	 * und kann später auch noch automatisch von der DB ermittelt und gesetzt werden.
	 * 
	 * Gibt eine eindeutige ID für diesen Job zurück
	 */
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
	
	/**
	 * Job nach ID aus der DB laden. Siehe die Klasse edacc.models.ExperimentResult aus der EDACC GUI (eingebunden über JAR).
	 * Dazu könnte noch eine eigene Wrapperklasse um ExperimentResult geschrieben werden, die für Konfiguratoren unwichtige Dinge weglässt.
	 */
	public ExperimentResult getJob(int job_id) {
		try {
			return ExperimentResultDAO.getById(job_id);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Lädt eine Parameterspace Beschreibung (also eine Instanz eines Parametergraphs) aus der DB.
	 * Das wird dann statt über solverID auch direkt über Experiment ID gehen.
	 */
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
	
	/**
	 * .. aus einer Datei zum Testen.
	 */
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
}
