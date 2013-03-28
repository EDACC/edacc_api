package edacc.api;

import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import edacc.api.costfunctions.Average;
import edacc.api.costfunctions.CostFunction;
import edacc.api.costfunctions.Median;
import edacc.api.costfunctions.PARX;
import edacc.api.costfunctions.PenalizedGeometricMeanX;
import edacc.model.*;
import edacc.parameterspace.domain.*;
import edacc.parameterspace.graph.ParameterGraph;
import edacc.parameterspace.ParameterConfiguration;

/**
 * API implementation
 */
public class APIImpl implements API {
    private static DatabaseConnector db = DatabaseConnector.getInstance();
    // internal configuration scenario cache
    private Map<Integer, ConfigurationScenario> csCache = new HashMap<Integer, ConfigurationScenario>();
    // internal parameter graph cache
    private Map<Integer, ParameterGraph> pgCache = new HashMap<Integer, ParameterGraph>();
    // internal solver binaries cache
    private Map<Integer, SolverBinaries> sbCache = new HashMap<Integer, SolverBinaries>();
    // internal experiment cache
    private Map<Integer, Experiment> expCache = new HashMap<Integer, Experiment>();
    
    public static final String[] constSolverParameters = {"instance", "seed", "tempdir", "db_host", "db_port", "db_db", "db_username", "db_password"};
    /**
     * Checks if the given parameter name is a 'magic' solver parameter.
     * @param parameterName
     * @return true, iff <code>parameterName</code> is a 'magic' solver parameter name.
     */
    public static boolean isMagicSolverParameter(String parameterName) {
        for (String p : constSolverParameters) {
            if (p.equals(parameterName)) {
                return true;
            }
        }
        return false;
    }
    
    
    
    public synchronized boolean connect(String hostname, int port, String database, String username, String password)
            throws Exception {
    	return connect(hostname, port, database, username, password, false);
    }
    
    public synchronized boolean connect(String hostname, int port, String database, String username, String password, boolean compress)
            throws Exception {
        db.connect(hostname, port, username, database, password, false, compress, 8, false, false);
        return db.isConnected();
    }

    public synchronized void disconnect() {
        csCache.clear();
        pgCache.clear();
        sbCache.clear();
        db.disconnect();
    }

    public synchronized String getCanonicalName(int idExperiment, ParameterConfiguration config) throws Exception {
        ConfigurationScenario cs = getConfigScenario(idExperiment);
        StringBuilder name = new StringBuilder();
        List<ConfigurationScenarioParameter> params = cs.getParameters();
        // sort parameters in ascending order of their name
        Collections.sort(params);
        for (ConfigurationScenarioParameter param : params) {
            if (isMagicSolverParameter(param.getParameter().getName()))
                continue;
            if (param.isConfigurable()) {
                edacc.parameterspace.Parameter config_param = null;
                for (edacc.parameterspace.Parameter p : config.getParameter_instances().keySet()) {
                    if (p.getName().equals(param.getParameter().getName())) {
                        config_param = p;
                        break;
                    }
                }
                if (config_param == null) {
                    continue;
                }
                if (config.getParameterValue(config_param) == null) continue;

                if (OptionalDomain.OPTIONS.NOT_SPECIFIED.equals(config.getParameterValue(config_param)))
                    continue;
                else if (FlagDomain.FLAGS.ON.equals(config.getParameterValue(config_param))) {
                    if (param.getParameter().getPrefix() != null) name.append(param.getParameter().getPrefix());
                    if (param != params.get(params.size() - 1))
                        name.append(" ");
                } else {
                    if (param.getParameter().getPrefix() != null) name.append(param.getParameter().getPrefix());
                    if (param.getParameter().getSpace())
                        name.append(" ");
                    name.append(config.getValueRepresentation(config.getParameterValue(config_param)));
                    if (param != params.get(params.size() - 1))
                        name.append(" ");
                }
            }

        }
        return name.toString();
    }
    
    public synchronized List<edacc.parameterspace.Parameter> getConfigurableParameters(int idExperiment) throws Exception {
        ConfigurationScenario cs = getConfigScenario(idExperiment);
        List<edacc.parameterspace.Parameter> configurableParameters = new ArrayList<edacc.parameterspace.Parameter>();
        for (ConfigurationScenarioParameter param : cs.getParameters()) {
            if (isMagicSolverParameter(param.getParameter().getName()))
                continue;
            if (param.isConfigurable()) {
                edacc.parameterspace.Parameter config_param = null;
                for (edacc.parameterspace.Parameter p : getParamGraph(idExperiment).getParameterSet()) {
                    if (p.getName().equals(param.getParameter().getName())) {
                        config_param = p;
                        break;
                    }
                }
                if (config_param != null) configurableParameters.add(config_param);
            }
        }
        return configurableParameters;
    }

    public synchronized int createSolverConfig(int idExperiment, ParameterConfiguration config, String name) throws Exception {
        ConfigurationScenario cs = getConfigScenario(idExperiment);
        SolverBinaries solver_binary = getSolverBinary(cs.getIdSolverBinary());

        SolverConfiguration solver_config = SolverConfigurationDAO.createSolverConfiguration(solver_binary, idExperiment, 0,
                name, "", null, null, calculateParameterConfigHash(idExperiment, config));
        SolverConfigurationDAO.clearCache(); // should probably not cache in the
                                             // first place ...

        List<ParameterInstance> parameter_instances = createParameterInstancesList(idExperiment, solver_config, config);

        ParameterInstanceDAO.saveBulk(parameter_instances);

        return solver_config.getId();
    }
    
    public synchronized List<Integer> createSolverConfigs(int idExperiment, List<ParameterConfiguration> configs, List<String> names) throws Exception {
        if (configs.size() != names.size()) {
            throw new IllegalArgumentException("Number of configs and names has to be the same");
        }
        List<Integer> solverConfigIds = new ArrayList<Integer>();
        if (configs.isEmpty()) {
            return solverConfigIds;
        }
        try {
            db.getConn().setAutoCommit(false);
            ConfigurationScenario cs = getConfigScenario(idExperiment);
            SolverBinaries solver_binary = getSolverBinary(cs.getIdSolverBinary());
            
            List<SolverConfiguration> solverConfigurations = new ArrayList<SolverConfiguration>();
            int name_i = 0;
            for (ParameterConfiguration config: configs) {
                SolverConfiguration solver_config = new SolverConfiguration();
                solver_config.setHint("");
                solver_config.setSolverBinary(solver_binary);
                solver_config.setExperiment_id(idExperiment);
                solver_config.setSeed_group(0);
                solver_config.setName(names.get(name_i++));
                solver_config.setParameter_hash(calculateParameterConfigHash(idExperiment, config));
                solver_config.setCost(null);
                solver_config.setCost_function(null);
                solverConfigurations.add(solver_config);
                
            }
            
            SolverConfigurationDAO.saveAll(solverConfigurations);
            for (SolverConfiguration sc: solverConfigurations) {
                solverConfigIds.add(sc.getId());
            }
            
            SolverConfigurationDAO.clearCache();
            
            List<ParameterInstance> parameter_instances = new ArrayList<ParameterInstance>();
            int configIx = 0;
            for (SolverConfiguration solver_config: solverConfigurations) {
                ParameterConfiguration config = configs.get(configIx++);
                parameter_instances.addAll(createParameterInstancesList(idExperiment, solver_config, config));
            }
            
            ParameterInstanceDAO.saveBulk(parameter_instances);
        } catch (Exception e) {
            db.getConn().rollback();
            throw e;
        } finally {
            db.getConn().setAutoCommit(true);
        }
        
        return solverConfigIds;
    }
    
    private synchronized String calculateParameterConfigHash(int idExperiment, ParameterConfiguration config) throws Exception {
        ConfigurationScenario cs = getConfigScenario(idExperiment);
        MessageDigest md = MessageDigest.getInstance("SHA");
        // calculate the checksum of the parameter configuration in the context of the experiment's
        // configuration scenario, i.e. consider only configurable parameter values.
        List<ConfigurationScenarioParameter> params = cs.getParameters();
        Collections.sort(params);
        for (ConfigurationScenarioParameter param : params) {
            if (isMagicSolverParameter(param.getParameter().getName()))
                continue;
            if (param.isConfigurable()) {
                edacc.parameterspace.Parameter config_param = null;
                for (edacc.parameterspace.Parameter p : config.getParameter_instances().keySet()) {
                    if (p.getName().equals(param.getParameter().getName())) {
                        config_param = p;
                        break;
                    }
                }
                if (config_param == null) {
                    continue;
                }

                if (config.getParameterValue(config_param) != null
                        && !(config.getParameterValue(config_param) instanceof OptionalDomain.OPTIONS)
                        && !(config.getParameterValue(config_param).equals(FlagDomain.FLAGS.OFF))) {
                    md.update(config.getValueRepresentation(config.getParameterValue(config_param)).getBytes());
                    
                }
            }
        }
        
        return toHex(md.digest());
    }
    
    private synchronized List<ParameterInstance> createParameterInstancesList(int idExperiment, SolverConfiguration solver_config, ParameterConfiguration config) throws Exception {
        ConfigurationScenario cs = getConfigScenario(idExperiment);
        List<ParameterInstance> parameter_instances = new ArrayList<ParameterInstance>();
        for (ConfigurationScenarioParameter param : cs.getParameters()) {
            ParameterInstance pi = new ParameterInstance();
            if (isMagicSolverParameter(param.getParameter().getName())) {
                pi.setSolverConfiguration(solver_config);
                pi.setValue("");
                pi.setParameter_id(param.getParameter().getId());
                
            } else if (!param.isConfigurable()) {
                if (param.getParameter().getHasValue()) {
                    pi.setSolverConfiguration(solver_config);
                    pi.setValue(param.getFixedValue());
                    pi.setParameter_id(param.getParameter().getId());
                } else { // flag
                    pi.setSolverConfiguration(solver_config);
                    pi.setValue("");
                    pi.setParameter_id(param.getParameter().getId());
                }
            } else if (param.isConfigurable()) {
                edacc.parameterspace.Parameter config_param = null;
                for (edacc.parameterspace.Parameter p : config.getParameter_instances().keySet()) {
                    if (p.getName().equals(param.getParameter().getName())) {
                        config_param = p;
                        break;
                    }
                }
                if (config_param == null) {
                    continue;
                }
                if (config.getParameterValue(config_param) == null) continue;

                if (OptionalDomain.OPTIONS.NOT_SPECIFIED.equals(config.getParameterValue(config_param)))
                    continue;
                else if (FlagDomain.FLAGS.OFF.equals(config.getParameterValue(config_param)))
                    continue;
                else {
                    pi.setSolverConfiguration(solver_config);
                    pi.setValue(config.getValueRepresentation(config.getParameterValue(config_param)));
                    pi.setParameter_id(param.getParameter().getId());
                }
            }
            parameter_instances.add(pi);
        }
        return parameter_instances;
    }

    public synchronized int launchJob(int idExperiment, int idSolverConfig, int idInstance, BigInteger seed, int cpuTimeLimit, int wallClockTimeLimit)
            throws Exception {
        return launchJob(idExperiment, idSolverConfig, idInstance, seed, cpuTimeLimit, wallClockTimeLimit, 0);
    }
    
    public synchronized int launchJob(int idExperiment, int idSolverConfig, int idInstance, BigInteger seed, int cpuTimeLimit, int wallClockTimeLimit,
            int priority) throws Exception {
        ExperimentResult job = ExperimentResultDAO.createExperimentResult(getCurrentMaxRun(idSolverConfig, idInstance) + 1,
                priority, 0, StatusCode.NOT_STARTED, seed.intValue(), ResultCode.UNKNOWN, 0, 0, 0, idSolverConfig, idExperiment,
                idInstance, null, cpuTimeLimit, -1, wallClockTimeLimit, -1);
        ArrayList<ExperimentResult> l = new ArrayList<ExperimentResult>();
        l.add(job);
        ExperimentResultDAO.batchSave(l);
        return job.getId();
    }

    private synchronized void extendCourse(ConfigurationScenario cs, Random rng) throws Exception {
        List<Instance> instances = new ArrayList<Instance>();
        for (int i = 0; i < cs.getCourse().getInitialLength(); i++) {
            instances.add(cs.getCourse().get(i).instance);
        }
        Collections.shuffle(instances, rng);
        int oldLength = cs.getCourse().getLength();
        PreparedStatement st = DatabaseConnector.getInstance().getConn().prepareStatement(
                "INSERT INTO Course (ConfigurationScenario_idConfigurationScenario, Instances_idInstance, seed, `order`) VALUES (?, ?, ?, ?)");
        for (int i = 0; i < cs.getCourse().getInitialLength(); i++) {
            int seed = rng.nextInt(Integer.MAX_VALUE);
            st.setInt(1, cs.getId());
            st.setInt(2, instances.get(i).getId());
            st.setInt(3, seed);
            st.setInt(4, oldLength + i);
            st.addBatch();
            cs.getCourse().add(new InstanceSeed(instances.get(i), seed));
        }
        st.executeBatch();
        st.close();
    }

    public synchronized int launchJob(int idExperiment, int idSolverConfig, int cpuTimeLimit, int wallClockTimeLimit, Random rng) throws Exception {
        return launchJob(idExperiment, idSolverConfig, cpuTimeLimit, wallClockTimeLimit, 0, rng);
    }

    public synchronized int launchJob(int idExperiment, int idSolverConfig, int cpuTimeLimit, int wallClockTimeLimit, int priority, Random rng)
            throws Exception {
        ConfigurationScenario cs = getConfigScenario(idExperiment);
        if (cs == null)
            return 0;
        List<ExperimentResult> jobs = ExperimentResultDAO.getAllBySolverConfiguration(SolverConfigurationDAO
                .getSolverConfigurationById(idSolverConfig));
        Course course = cs.getCourse();
        int courseLength = 0;
        for (int cix = 0; cix < course.getLength(); cix++) {
            boolean matchingJob = false;
            for (ExperimentResult er : jobs) {
                matchingJob |= er.getInstanceId() == course.get(cix).instance.getId() && er.getSeed() == course.get(cix).seed;
            }
            if (!matchingJob) break;
            courseLength++;
        }
        if (courseLength == course.getLength()) {
            extendCourse(cs, rng);
        }
        InstanceSeed is = course.get(courseLength);
        return launchJob(idExperiment, idSolverConfig, is.instance.getId(), BigInteger.valueOf(is.seed), cpuTimeLimit, wallClockTimeLimit, priority);
    }

    public int getCourseLength(int idExperiment) throws Exception {
        ConfigurationScenario cs = getConfigScenario(idExperiment);
        if (cs == null)
            return 0;
        return cs.getCourse().getLength();
    }

    public synchronized List<Integer> launchJob(int idExperiment, int idSolverConfig, int[] cpuTimeLimit, int[] wallClockTimeLimit, int numberRuns,
            Random rng) throws Exception {
        int[] priority = new int[numberRuns];
        for (int i = 0; i < numberRuns; i++)
            priority[i] = 0;
        return launchJob(idExperiment, idSolverConfig, cpuTimeLimit, wallClockTimeLimit, numberRuns, priority, rng);
    }

    public synchronized List<Integer> launchJob(int idExperiment, int idSolverConfig, int[] cpuTimeLimit, int[] wallClockTimeLimit, int numberRuns,
            int[] priority, Random rng) throws Exception {
        ConfigurationScenario cs = getConfigScenario(idExperiment);
        List<ExperimentResult> jobs = ExperimentResultDAO.getAllBySolverConfiguration(SolverConfigurationDAO
                .getSolverConfigurationById(idSolverConfig));
        Course course = cs.getCourse();
        int courseLength = 0;
        for (int cix = 0; cix < course.getLength(); cix++) {
            boolean matchingJob = false;
            for (ExperimentResult er : jobs) {
                matchingJob |= er.getInstanceId() == course.get(cix).instance.getId() && er.getSeed() == course.get(cix).seed;
            }
            if (!matchingJob) break;
            courseLength++;
        }
        while (course.getLength() < courseLength + numberRuns)
            extendCourse(cs, rng);

        Map<Integer, Integer> maxRun = new HashMap<Integer, Integer>();
        ArrayList<ExperimentResult> l = new ArrayList<ExperimentResult>();
        for (int i = 0; i < numberRuns; i++) {
            int idInstance = course.get(courseLength + i).instance.getId();
            int seed = course.get(courseLength + i).seed;
            if (!maxRun.containsKey(idInstance))
                maxRun.put(idInstance, getCurrentMaxRun(idSolverConfig, idInstance));
            else
                maxRun.put(idInstance, maxRun.get(idInstance) + 1);
            l.add(ExperimentResultDAO.createExperimentResult(maxRun.get(idInstance) + 1, priority[i], 0, StatusCode.NOT_STARTED,
                    seed, ResultCode.UNKNOWN, 0, 0, 0, idSolverConfig, idExperiment, idInstance, null, cpuTimeLimit[i], -1, wallClockTimeLimit[i], -1));
        }
        ExperimentResultDAO.batchSave(l);

        List<Integer> ids = new ArrayList<Integer>();
        for (ExperimentResult er : l)
            ids.add(er.getId());
        return ids;
    }

    public synchronized ParameterConfiguration getParameterConfiguration(int idExperiment, int idSolverConfig) throws Exception {
        ParameterGraph graph = getParamGraph(idExperiment);
        ParameterConfiguration config = new ParameterConfiguration(graph.getParameterSet());
        ConfigurationScenario cs = getConfigScenario(idExperiment);
        SolverConfiguration solver_config = SolverConfigurationDAO.getSolverConfigurationById(idSolverConfig);
        if (solver_config == null)
            return null;

        // map ParameterID -> Parameter
        Map<Integer, edacc.model.Parameter> parameter_map = new HashMap<Integer, edacc.model.Parameter>();
        for (edacc.model.Parameter p : ParameterDAO.getParameterFromSolverId(solver_config.getSolverBinary().getIdSolver())) {
            parameter_map.put(p.getId(), p);
        }

        // map Parameter name -> Parameter Instance (value)
        Map<String, ParameterInstance> solver_config_param_map = new HashMap<String, ParameterInstance>();
        for (ParameterInstance p : ParameterInstanceDAO.getBySolverConfig(solver_config)) {
            solver_config_param_map.put(parameter_map.get(p.getParameter_id()).getName(), p);
        }

        Map<String, edacc.parameterspace.Parameter> pgraph_map = graph.getParameterMap();

        for (ConfigurationScenarioParameter param : cs.getParameters()) {
            if (isMagicSolverParameter(param.getParameter().getName())) continue;
            if (!pgraph_map.containsKey(param.getParameter().getName())) continue;
            
            String parameter_name = param.getParameter().getName();
            if (param.isConfigurable()) {
                if (!param.getParameter().getHasValue()) { // this is a flag
                    if (!solver_config_param_map.containsKey(param.getParameter().getName())) {
                        config.setParameterValue(parameter_name, FlagDomain.FLAGS.OFF);
                    } else {
                        config.setParameterValue(parameter_name, FlagDomain.FLAGS.ON);
                    }
                    
                } else { // standard parameter with a value
                    if (solver_config_param_map.get(param.getParameter().getName()) == null) continue;
                    
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
                            } catch (NumberFormatException e2) {
                                config.setParameterValue(parameter_name, value);
                            }
                        }
                    }
                }
            } else {
                if (pgraph_map.get(param.getParameter().getName()).getDomain().contains(param.getFixedValue())) {
                    // string should be fine for this domain
                    config.setParameterValue(parameter_name, param.getFixedValue());
                } else {
                    try {
                        int i = Integer.valueOf(param.getFixedValue());
                        config.setParameterValue(parameter_name, i);
                    } catch (NumberFormatException e) {
                        try {
                            double f = Double.valueOf(param.getFixedValue());
                            config.setParameterValue(parameter_name, f);
                        } catch (NumberFormatException e2) {
                            config.setParameterValue(parameter_name, param.getFixedValue());
                        }
                    }
                }
            }
        }
        config.updateChecksum();
        return config;
    }

    public synchronized int exists(int idExperiment, ParameterConfiguration config) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA");
        ConfigurationScenario cs = getConfigScenario(idExperiment);
        List<ConfigurationScenarioParameter> params = cs.getParameters();
        Collections.sort(params);
        for (ConfigurationScenarioParameter param : params) {
            if (isMagicSolverParameter(param.getParameter().getName()))
                continue;
            if (param.isConfigurable()) {
                edacc.parameterspace.Parameter config_param = null;
                for (edacc.parameterspace.Parameter p : config.getParameter_instances().keySet()) {
                    if (p.getName().equals(param.getParameter().getName())) {
                        config_param = p;
                        break;
                    }
                }
                if (config_param == null) {
                    continue;
                }

                if (config.getParameterValue(config_param) != null
                        && !(config.getParameterValue(config_param) instanceof OptionalDomain.OPTIONS)
                        && !(config.getParameterValue(config_param).equals(FlagDomain.FLAGS.OFF))) {
                    md.update(config.getValueRepresentation(config.getParameterValue(config_param)).getBytes());
                }
            }

        }

        SolverConfiguration sc = SolverConfigurationDAO.getByParameterHash(idExperiment, toHex(md.digest()));
        if (sc != null)
            return sc.getId();
        return 0;
    }

    public synchronized int getNumJobs(int idSolverConfig) throws Exception {
        return ExperimentResultDAO.getNumJobsBySolverConfigurationId(idSolverConfig);
    }

    public synchronized void updateSolverConfigurationCost(int idSolverConfig, double cost, CostFunction func) throws Exception {
        PreparedStatement st = db.getConn().prepareStatement(
                "UPDATE SolverConfig SET cost=?, cost_function=? WHERE idSolverConfig=?");
        st.setDouble(1, cost);
        st.setString(2, func.databaseRepresentation());
        st.setInt(3, idSolverConfig);
        st.executeUpdate();
        st.close();
    }

    public synchronized CostFunction getCostFunction(int idSolverConfig) throws Exception {
        PreparedStatement st = db.getConn().prepareStatement("SELECT cost_function, Experiment_idExperiment FROM SolverConfig WHERE idSolverConfig=?");
        st.setInt(1, idSolverConfig);
        ResultSet rs = st.executeQuery();
        if (rs.next()) {
            String func = rs.getString("cost_function");
            int idExperiment = rs.getInt("Experiment_idExperiment");
            rs.close();
            st.close();
            return costFunctionByExperiment(idExperiment, func);
        }
        rs.close();
        st.close();
        return null;
    }

    public synchronized Double getSolverConfigurationCost(int idSolverConfig) throws Exception {
        PreparedStatement st = db.getConn().prepareStatement("SELECT cost FROM SolverConfig WHERE idSolverConfig=?");
        st.setInt(1, idSolverConfig);
        ResultSet rs = st.executeQuery();
        if (rs.next()) {
            Double cost = rs.getDouble("cost");
            if (rs.wasNull()) cost = null;
            rs.close();
            st.close();
            return cost;
        }
        rs.close();
        st.close();
        return null;
    }

    public synchronized ExperimentResult getJob(int idJob) throws Exception {
        return ExperimentResultDAO.getByIdWithoutAssign(idJob);
    }

    public synchronized ExperimentResult killJob(int idJob) throws Exception {
        ExperimentResult er = ExperimentResultDAO.getById(idJob);
        if (!(er.getStatus().equals(StatusCode.NOT_STARTED) || er.getStatus().equals(StatusCode.RUNNING)))
            return er;
        if (er.getIdClient() != null) {
            ClientDAO.sendMessage(er.getIdClient(), "kill " + er.getId());
        }
        return null;
    }

    public void restartJob(int idJob, int CPUTimeLimit) throws Exception {
        ExperimentResult er = ExperimentResultDAO.getById(idJob);
        if (er == null)
            return;
        er.setCPUTimeLimit(CPUTimeLimit);
        ArrayList<ExperimentResult> jobs = new ArrayList<ExperimentResult>();
        jobs.add(er);
        ExperimentResultDAO.batchUpdateCPUTimeLimit(jobs);
        ExperimentResultDAO.batchUpdateStatus(jobs, StatusCode.NOT_STARTED);
    }

    public synchronized boolean deleteResult(int idJob) throws Exception {
        ExperimentResult er = ExperimentResultDAO.getByIdWithoutAssign(idJob);
        if (er == null)
            return false;
        ArrayList<ExperimentResult> l = new ArrayList<ExperimentResult>();
        l.add(er);
        ExperimentResultDAO.deleteExperimentResults(l);
        return true;
    }
    //returns runs only fromt the course
    public synchronized ArrayList<ExperimentResult> getRuns(int idExperiment, int idSolverConfig) throws Exception {
        ConfigurationScenario cs = getConfigScenario(idExperiment);
        ArrayList<ExperimentResult> orderedResults = new ArrayList<ExperimentResult>();
        List<ExperimentResult> results = ExperimentResultDAO.getAllBySolverConfiguration(SolverConfigurationDAO
                .getSolverConfigurationById(idSolverConfig));
        for (InstanceSeed isp : cs.getCourse().getInstanceSeedList()) {
            for (ExperimentResult res : results) {
                if (res.getInstanceId() == isp.instance.getId() && res.getSeed() == isp.seed) {
                    orderedResults.add(res);
                    break;
                }
            }
        }
        return orderedResults;
    }
    //returns all runs    
    public synchronized ArrayList<ExperimentResult> getAllRuns(int idExperiment, int idSolverConfig) throws Exception {
        ConfigurationScenario cs = getConfigScenario(idExperiment);
        ArrayList<ExperimentResult> orderedResults = new ArrayList<ExperimentResult>();
        List<ExperimentResult> results = ExperimentResultDAO.getAllBySolverConfiguration(SolverConfigurationDAO
                .getSolverConfigurationById(idSolverConfig));
        //for (InstanceSeed isp : cs.getCourse().getInstanceSeedList()) {
            for (ExperimentResult res : results) {
             //   if (res.getInstanceId() == isp.instance.getId() && res.getSeed() == isp.seed) {
                    orderedResults.add(res);
          //          break;
            //    }
            //}
        }
        return orderedResults;
    }

    public synchronized Map<Integer, ExperimentResult> getJobsByIDs(List<Integer> ids) throws Exception {
        Map<Integer, ExperimentResult> jobs = new HashMap<Integer, ExperimentResult>();
        List<ExperimentResult> results = ExperimentResultDAO.getByIds(ids);
        for (ExperimentResult result : results) {
            jobs.put(result.getId(), result);
        }
        return jobs;
    }

    public synchronized List<Instance> getExperimentInstances(int idExperiment) throws Exception {
        return InstanceDAO.getAllByExperimentId(idExperiment);
    }

    public synchronized String getSolverConfigName(int idSolverConfig) throws Exception {
        SolverConfiguration config = SolverConfigurationDAO.getSolverConfigurationById(idSolverConfig);
        if (config != null)
            return config.getName();
        return null;
    }

    public synchronized int getBestConfiguration(int idExperiment, CostFunction func) throws Exception {
        PreparedStatement st = db
                .getConn()
                .prepareStatement(
                        "SELECT idSolverConfig FROM SolverConfig WHERE Experiment_idExperiment=? AND cost_function=? AND cost IS NOT NULL ORDER BY cost LIMIT 1");
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

    public List<Integer> getBestConfigurations(int idExperiment, CostFunction func, int no) throws Exception {
        PreparedStatement st = db
                .getConn()
                .prepareStatement(
                        "SELECT idSolverConfig FROM SolverConfig WHERE Experiment_idExperiment=? AND cost_function=? AND cost IS NOT NULL ORDER BY cost LIMIT ?");
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

    public synchronized ParameterGraph loadParameterGraphFromDB(int idExperiment) throws Exception {
        ConfigurationScenario cs = getConfigScenario(idExperiment);
        Statement st = db.getConn().createStatement();

        ResultSet rs = st
                .executeQuery("SELECT serializedGraph FROM ConfigurationScenario JOIN SolverBinaries ON SolverBinaries_idSolverBinary=idSolverBinary JOIN ParameterGraph ON SolverBinaries.idSolver=ParameterGraph.Solver_idSolver WHERE Experiment_idExperiment = "
                        + idExperiment);
        try {
            if (rs.next()) {
                ParameterGraph pg = unmarshal(ParameterGraph.class, rs.getBlob("serializedGraph").getBinaryStream());
                pg.buildAdjacencyList();

                Map<edacc.parameterspace.Parameter, Object> fixedParams = new HashMap<edacc.parameterspace.Parameter, Object>();
                Map<String, edacc.parameterspace.Parameter> pgraph_map = pg.getParameterMap();
                List<ConfigurationScenarioParameter> params = cs.getParameters();
                Collections.sort(params);
                for (ConfigurationScenarioParameter param : params) {
                    if (isMagicSolverParameter(param.getParameter().getName()))
                        continue;
                    if (!param.isConfigurable()) {
                        // fixed parameter
                        edacc.parameterspace.Parameter config_param = null;
                        for (edacc.parameterspace.Parameter p : pg.getParameterSet()) {
                            if (p.getName().equals(param.getParameter().getName())) {
                                config_param = p;
                                break;
                            }
                        }
                        if (config_param == null) {
                            continue;
                        }
                        
                        // guess the right type
                        if (pgraph_map.get(param.getParameter().getName()).getDomain().contains(param.getFixedValue())) {
                            // string should be fine for this domain
                            fixedParams.put(config_param, param.getFixedValue());
                        } else {
                            try {
                                int i = Integer.valueOf(param.getFixedValue());
                                fixedParams.put(config_param, i);
                            } catch (NumberFormatException e) {
                                try {
                                    double f = Double.valueOf(param.getFixedValue());
                                    fixedParams.put(config_param, f);
                                } catch (NumberFormatException e2) {
                                    fixedParams.put(config_param, param.getFixedValue());
                                }
                            }
                        }
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

    @SuppressWarnings("unchecked")
    private <T> T unmarshal(Class<T> docClass, InputStream inputStream) throws JAXBException {
        JAXBContext jc = JAXBContext.newInstance(docClass);
        Unmarshaller u = jc.createUnmarshaller();
        return (T) u.unmarshal(inputStream);
    }

    private synchronized int getCurrentMaxRun(int idSolverConfig, int idInstance) throws Exception {
        PreparedStatement ps = db.getConn().prepareStatement(
                "SELECT MAX(run) FROM ExperimentResults WHERE SolverConfig_idSolverConfig=? AND Instances_idInstance=?");
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
        if (bytes == null)
            return "";
        BigInteger bi = new BigInteger(1, bytes);
        return String.format("%0" + (bytes.length << 1) + "X", bi);
    }

    /*public CostFunction costFunctionByName(String databaseRepresentation) {
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
    }*/
    
    public CostFunction costFunctionByExperiment(int idExperiment, String databaseRepresentation) throws Exception {
        Experiment exp = getExperiment(idExperiment);
        Experiment.Cost cost = exp.getDefaultCost();
        if ("average".equals(databaseRepresentation)) {
            return new Average(cost, exp.getMinimize());
        } else if ("median".equals(databaseRepresentation)) {
            return new Median(cost, exp.getMinimize());
        } else if (databaseRepresentation != null && databaseRepresentation.startsWith("par")) {
            try {
                int penaltyFactor = Integer.valueOf(databaseRepresentation.substring(3));
                return new PARX(cost, exp.getMinimize(), exp.getCostPenalty() == null ? 0.f : exp.getCostPenalty(), penaltyFactor);
            } catch (Exception e) {
                return null;
            }
        } else if (databaseRepresentation != null && databaseRepresentation.startsWith("PenalizedGeometricMean")) {
            try {
                String[] split = databaseRepresentation.split("_");
                int penaltyFactor = Integer.valueOf(split[1]);
                double shift = Double.valueOf(split[2]);
                return new PenalizedGeometricMeanX(cost, exp.getMinimize(), shift, exp.getCostPenalty() == null ? 0.f : exp.getCostPenalty(), penaltyFactor);
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

    @Override
    public int getComputationCoreCount(int idExperiment) throws Exception {
        Statement st = db.getConn().createStatement();
        ResultSet rs = st
                .executeQuery("SELECT IFNULL(SUM(ceil(gridQueue.numCPUs / gridQueue.numCPUsPerJob)), 0) FROM Client JOIN gridQueue ON (idgridQueue = Client.gridQueue_idgridQueue) JOIN Experiment_has_gridQueue ON (idgridQueue = Experiment_has_gridQueue.gridQueue_idgridQueue) WHERE Experiment_idExperiment = "
                        + idExperiment);
        if (rs.next()) {
            try {
                return rs.getInt(1);
            } finally {
                rs.close();
                st.close();
            }
        }
        rs.close();
        st.close();
        return 0;
    }

    @Override
    public int getComputationJobCount(int idExperiment) throws Exception {
        Statement st = db.getConn().createStatement();
        ResultSet rs = st
                .executeQuery("SELECT COUNT(idJob) FROM ExperimentResults WHERE (status = 0 OR status = -1) AND priority >= 0 AND Experiment_idExperiment = "
                        + idExperiment);
        if (rs.next()) {
            try {
                return rs.getInt(1);
            } finally {
                rs.close();
                st.close();
            }
        }
        rs.close();
        st.close();
        return 0;
    }

    @Override
    public void setJobPriority(int idJob, int priority) throws Exception {
        Statement st = db.getConn().createStatement();
        st.executeUpdate("UPDATE ExperimentResults SET priority = " + priority + " WHERE idJob = " + idJob);
        st.close();
    }

    @Override
    public List<Integer> getSolverConfigurations(int idExperiment) throws Exception {
        Statement st = db.getConn().createStatement();
        ResultSet rs = st.executeQuery("SELECT idSolverConfig FROM SolverConfig WHERE Experiment_idExperiment = " + idExperiment);
        List<Integer> res = new ArrayList<Integer>();
        while (rs.next()) {
            res.add(rs.getInt(1));
        }
        rs.close();
        st.close();
        return res;
    }

    @Override
    public List<Integer> getSolverConfigurations(int idExperiment, String hint) throws Exception {
        PreparedStatement st = db.getConn().prepareStatement(
                "SELECT idSolverConfig FROM SolverConfig WHERE hint LIKE ? AND Experiment_idExperiment = ?");
        st.setString(1, hint);
        st.setInt(2, idExperiment);
        ResultSet rs = st.executeQuery();
        List<Integer> res = new ArrayList<Integer>();
        while (rs.next()) {
            res.add(rs.getInt(1));
        }
        rs.close();
        st.close();
        return res;
    }

    @Override
    public void setSolverConfigurationHint(int idExperiment, int idSolverConfig, String hint) throws Exception {
        PreparedStatement st = db.getConn().prepareStatement(
                "UPDATE SolverConfig SET hint = ? WHERE Experiment_idExperiment = ? AND idSolverConfig = ?");
        st.setString(1, hint);
        st.setInt(2, idExperiment);
        st.setInt(3, idSolverConfig);
        st.executeUpdate();
        st.close();
    }

    private synchronized ConfigurationScenario getConfigScenario(int idExperiment) throws Exception {
        if (csCache.containsKey(idExperiment))
            return csCache.get(idExperiment);
        csCache.put(idExperiment, ConfigurationScenarioDAO.getConfigurationScenarioByExperimentId(idExperiment));
        return csCache.get(idExperiment);
    }
    
    private synchronized Experiment getExperiment(int idExperiment) throws Exception {
        if (expCache.containsKey(idExperiment))
            return expCache.get(idExperiment);
        expCache.put(idExperiment, ExperimentDAO.getById(idExperiment));
        return expCache.get(idExperiment);
    }

    private synchronized ParameterGraph getParamGraph(int idExperiment) throws Exception {
        if (pgCache.containsKey(idExperiment))
            return pgCache.get(idExperiment);
        pgCache.put(idExperiment, loadParameterGraphFromDB(idExperiment));
        return pgCache.get(idExperiment);
    }

    private synchronized SolverBinaries getSolverBinary(int idSolverBinary) throws Exception {
        if (sbCache.containsKey(idSolverBinary))
            return sbCache.get(idSolverBinary);
        sbCache.put(idSolverBinary, SolverBinariesDAO.getById(idSolverBinary));
        return sbCache.get(idSolverBinary);
    }

    public float getTotalCPUTime(int idExperiment) throws Exception {
        PreparedStatement st = db.getConn().prepareStatement(
                "SELECT SUM(resultTime) FROM ExperimentResults WHERE Experiment_idExperiment = ? AND status != -1 AND status != 0");
        st.setInt(1, idExperiment);
        ResultSet rs = st.executeQuery();  
        if (rs.next()) {
            try {
                return rs.getFloat(1);
            } finally {
                rs.close();
                st.close();
            }
        }
        rs.close();
        st.close();
        return 0;
    }

    @Override
    public Course getCourse(int idExperiment) throws Exception {
        return getConfigScenario(idExperiment).getCourse();
    }

	@Override
	public void setOutput(int idExperiment, String output) throws Exception {
		PreparedStatement st = db.getConn().prepareStatement(
				"UPDATE `ConfigurationScenario` SET configuratorOutput = ? WHERE Experiment_idExperiment = ?");
		st.setString(1, output);
		st.setInt(2, idExperiment);
		st.executeUpdate();
		st.close();
	}

	@Override
	public void addOutput(int idExperiment, String text) throws Exception {
		PreparedStatement st = db.getConn().prepareStatement(
				"UPDATE `ConfigurationScenario` SET configuratorOutput = CONCAT(configuratorOutput, ?) WHERE Experiment_idExperiment = ?");
		st.setString(1, text);
		st.setInt(2, idExperiment);
		st.executeUpdate();
		st.close();
	}



	@Override
	public ExperimentResult updateCPUTimeLimit(int idJob, int cputimelimit, StatusCode statusCode, ResultCode resultCode) throws Exception {
		Statement st = DatabaseConnector.getInstance().getConn().createStatement();
		st.executeUpdate("UPDATE ExperimentResults SET status = " + statusCode.getStatusCode() + ", resultCode = " + resultCode.getResultCode() + ", CPUTimeLimit = " + cputimelimit + " WHERE idJob = " + idJob);
		st.close();
		return getJob(idJob);
	}
}
