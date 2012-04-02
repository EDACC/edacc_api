package edacc.api;

import java.io.FileNotFoundException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Random;

import edacc.api.costfunctions.CostFunction;
import edacc.model.Course;
import edacc.model.ExperimentResult;
import edacc.model.Instance;
import edacc.parameterspace.ParameterConfiguration;
import edacc.parameterspace.graph.ParameterGraph;

/**
 * EDACC Configurator database API.
 * 
 * This API provides methods to create and retrieve solver configurations and
 * jobs for "configuration experiments" that were created using the EDACC GUI
 * application.
 * 
 */
public interface API {
    /**
     * Establishes the database connection.
     * 
     * @param hostname
     * @param port
     * @param database
     * @param username
     * @param password
     * @return
     */
    public boolean connect(String hostname, int port, String database, String username, String password) throws Exception;
    public boolean connect(String hostname, int port, String database, String username, String passwd, boolean compress) throws Exception;

    /**
     * Closes the database connection and clears internal caches.
     */
    public void disconnect();

    /**
     * Returns a canonical name of the given parameter configuration in the
     * context of the configuration experiment. This means only values that are
     * to be configured are actually appearing in the name. The name will consist
     * of parameter prefix - value pairs in ascending order of the parameter names.
     * Example:
     * "-c 2 -p1 0.4 -p2 0.8"
     * 
     * @param idExperiment
     *            ID of the configuration experiment
     * @param config
     *            parameter configuration to be named
     * @return name
     * @throws Exception
     */
    public String getCanonicalName(int idExperiment, ParameterConfiguration config) throws Exception;

    /**
     * Creates a new solver configuration in the database for the experiment
     * specified by the <code>idExperiment</code> argument. The solver binary of the
     * configuration is determined by the configuration scenario that the user
     * created in the GUI. The parameter values are assigned by iterating over
     * the parameters that were chosen in the GUI for the configuration
     * scenario. Non-configurable parameters take on the value that the user
     * specified as "fixed value" in the GUI, while configurable parameters take on the
     * values that are specified in the ParameterConfiguration <code>config</code> that is
     * passed to this function.
     * 
     * @param idExperiment
     *            ID of the experiment for which to create a solver
     *            configuration.
     * @param config
     *            parameter configuration object that specifies the values of
     *            parameters.
     * @param name
     *            name for the new solver configuration (used to display)
     * @return database unique ID > 0 of the created solver configuration, 0 on
     *         errors.
     */
    public int createSolverConfig(int idExperiment, ParameterConfiguration config, String name) throws Exception;
    
    /**
     * Returns a list of all configurable parameters for the parameter graph of the given experiment.
     * @param idExperiment
     * @return
     * @throws Exception
     */
    public List<edacc.parameterspace.Parameter> getConfigurableParameters(int idExperiment) throws Exception;
    
    public List<Integer> createSolverConfigs(int idExperiment, List<ParameterConfiguration> configs, List<String> names) throws Exception;

    /**
     * Removes the solver configuration with the specified ID.
     * Also removes all jobs and parameters of the solver configuration.
     */
    public void removeSolverConfig(int idSolverConfig) throws Exception;

    /**
     * Returns the solver config ids with the specified hint.
     * 
     * @param idExperiment
     * @param idSolverConfig
     * @return
     * @throws Exception
     */
    public List<Integer> getSolverConfigurations(int idExperiment, String hint) throws Exception;

    /**
     * Updates the <code>hint</code> of the solver configuration specified by
     * <code>idSolverConfig</code>.
     * Hints can be used by configuration tools to store some
     * information about the solver configuration in the database. This
     * hint will also be shown in the GUI.
     * There's a length limit on the database hint column (currently 1024).
     * 
     * @param idExperiment
     * @param idSolverConfig
     * @param hint
     * @throws Exception
     */
    public void setSolverConfigurationHint(int idExperiment, int idSolverConfig, String hint) throws Exception;

    /**
     * Creates a new job with the given parameters and marks it as ready for
     * computation.
     * 
     * It should be ensured by the API user that the instance is actually part
     * of the experiment, i.e. selected in the GUI.
     * 
     * @param idExperiment
     *            ID of the experiment that should contain the job.
     * @param idSolverConfig
     *            ID of the solver configuration.
     * @param idInstance
     *            ID of the instance.
     * @param seed
     *            integer seed that is assigned to the seed parameter of the
     *            solver configuration, if the seed parameter was activated.
     * @param cpuTimeLimit
     *            time limit of the job in CPU seconds.
     * @return unique database ID > 0 of the created job, 0 on errors.
     */
    public int launchJob(int idExperiment, int idSolverConfig, int idInstance, BigInteger seed, int cpuTimeLimit, int wallClockTimeLimit) throws Exception;

    /**
     * Creates a new job with the given parameters and marks it as ready for
     * computation.
     * 
     * @param idExperiment
     *            ID of the experiment that should contain the job.
     * @param idSolverConfig
     *            ID of the solver configuration.
     * @param idInstance
     *            ID of the instance.
     * @param seed
     *            integer seed that is assigned to the seed parameter of the
     *            solver configuration, if the seed parameter was activated.
     * @param cpuTimeLimit
     *            time limit of the job in CPU seconds.
     * @param priority
     *            Priority of the job, only jobs with priority >= 0 will be considered for
     *            computation.
     * @return unique database ID > 0 of the created job, 0 on errors.
     */
    public int launchJob(int idExperiment, int idSolverConfig, int idInstance, BigInteger seed, int cpuTimeLimit, int wallClockTimeLimit, int priority)
            throws Exception;

    /**
     * Creates a new job for the given solver configuration in the instance-seed
     * course of the given experiment. That means, the job will be created
     * for the given solver configuration on the next instance and seed slot
     * of the course.
     * 
     * @param idExperiment ID of the experiment that should contain the job.
     * @param idSolverConfig ID of the solver configuration.
     * @param cpuTimeLimit time limit of the job in CPU seconds.
     * @param rng Random number generator instance that should be used to create seeds if the course has to be extended.
     * @return unique database ID > 0 of the created job, 0 on errors.
     * @throws Exception
     */
    public int launchJob(int idExperiment, int idSolverConfig, int cpuTimeLimit, int wallClockTimeLimit, Random rng) throws Exception;

    /**
     * Creates a new job for the given solver configuration in the instance-seed
     * course of the given experiment. That means, the job will be created
     * for the given solver configuration on the next instance and seed slot
     * of the course.
     * 
     * @param idExperiment ID of the experiment that should contain the job.
     * @param idSolverConfig ID of the solver configuration.
     * @param cpuTimeLimit time limit of the job in CPU seconds.
     * @param rng Random number generator instance that should be used to create seeds if the course has to be extended.
     * @param priority Priority of the job, only jobs with priority >= 0 will be considered for computation.
     * @return unique database ID > 0 of the created job, 0 on errors.
     * @throws Exception
     */
    public int launchJob(int idExperiment, int idSolverConfig, int cpuTimeLimit, int wallClockTimeLimit, int priority, Random rng) throws Exception;

    /**
     * Creates numberRuns new jobs for the given solver configuration in the
     * instance-seed course of the experiment.
     * 
     * @param idExperiment ID of the experiment that should contain the jobs.
     * @param idSolverConfig ID of the solver configuration.
     * @param cpuTimeLimit time limits of the jobs in CPU seconds.
     * @param rng Random number generator instance that should be used to create seeds if the course has to be extended.
     * @param numberRuns how many jobs to generate
     * @return List of unique database IDs
     * @throws Exception
     */
    public List<Integer> launchJob(int idExperiment, int idSolverConfig, int[] cpuTimeLimit, int[] wallClockTimeLimit, int numberRuns, Random rng) throws Exception;

    /**
     * Creates numberRuns new jobs for the given solver configuration in the
     * instance-seed course of the experiment.
     * 
     * @param idExperiment ID of the experiment that should contain the jobs.
     * @param idSolverConfig ID of the solver configuration.
     * @param cpuTimeLimit time limits of the jobs in CPU seconds.
     * @param priority priorities of the jobs.
     * @param numberRuns how many jobs to generate
     * @param rng Random number generator instance that should be used to create seeds if the course has to be extended.
     * @return List of unique database IDs
     * @throws Exception
     */
    public List<Integer> launchJob(int idExperiment, int idSolverConfig, int[] cpuTimeLimit, int[] wallClockTimeLimit, int numberRuns, int[] priority, Random rng)
            throws Exception;

    /**
     * Returns the parameter configuration corresponding to the given solver
     * configuration in the DB.
     * 
     * @param idExperiment
     * @param idSolverConfig
     */
    public ParameterConfiguration getParameterConfiguration(int idExperiment, int idSolverConfig) throws Exception;

    /**
     * If the given parameter configuration already has a corresponding solver
     * configuration in the given experiment this function will return the ID of
     * the solver configuration and 0 otherwise.
     * 
     * @param idExperiment
     * @param config
     * @return
     */
    public int exists(int idExperiment, ParameterConfiguration config) throws Exception;

    /**
     * return the current number of jobs of the given solver configuration and
     * 0, if it doesn't exist
     * 
     * @param idSolverConfig
     * @return
     */
    public int getNumJobs(int idSolverConfig) throws Exception;

    /**
     * Updates the cost of the given solver configuration. The cost function is
     * also saved in the table.
     * 
     * The API user is responsible for keeping this value updated.
     * 
     * @param idSolverConfig
     * @param cost
     * @param func
     */
    public void updateSolverConfigurationCost(int idSolverConfig, float cost, CostFunction func) throws Exception;

    /**
     * Returns the cost function of the given solver configuration as saved in
     * the database
     * 
     * The API user is responsible for keeping this value updated.
     * 
     * @param idSolverConfig
     * @return
     */
    public CostFunction getCostFunction(int idSolverConfig) throws Exception;

    /**
     * Returns the current cost value of the solver configuration as saved in
     * the database. Can return null if the DB value is NULL.
     * 
     * The API user is responsible for keeping this value updated.
     * 
     * @param idSolverConfig
     * @return
     */
    public Float getSolverConfigurationCost(int idSolverConfig) throws Exception;

    /**
     * Retrieves and returns a job from the database by it's unique ID
     * regardless of its computation status.
     * 
     * @param idJob
     *            ID of the job
     * @return the job as edacc.model.ExperimentResult object.
     */
    public ExperimentResult getJob(int idJob) throws Exception;

    /**
     * Stops a job. If the job has already finished or crashed, the method
     * returns the job object. If the job is currently being computed a kill
     * message will be sent to the computing client.
     * 
     * This method does not block to wait until the job is killed. There are no
     * guarantees about the time it takes to kill a running job.
     * 
     * @param idJob
     *            ID of the job to kill.
     * @return
     */
    public ExperimentResult killJob(int idJob) throws Exception;

    /**
     * Immediately deletes a job no matter its computation status.
     * If the job is currently being computed by some client
     * a message will be sent to the client to stop the computation.
     * 
     * This method does not block to wait until the job is killed. There are no
     * guarantees about the time it takes to kill a running job.
     * 
     * @param idJob
     *            ID of the job to delete.
     * @return
     */
    public boolean deleteResult(int idJob) throws Exception;

    /**
     * Reset the job with the given ID to "not started".
     * 
     * @param idJob
     * @throws Exception
     */
    public void restartJob(int idJob, int CPUTimeLimit) throws Exception;

    /**
     * Returns all jobs of the solver configuration specified by the
     * idSolverConfig argument.
     * 
     * @param idExperiment
     * @param idSolverConfig
     * @return
     */
    public List<ExperimentResult> getRuns(int idExperiment, int idSolverConfig) throws Exception;

    /**
     * returns the length of the instance-seed course of the configuration
     * experiment
     * 
     * @return
     * @throws Exception
     */
    public int getCourseLength(int idExperiment) throws Exception;

    /**
     * Returns a map with all jobs from the database with IDs specified in the
     * ids argument.
     * 
     * @param ids
     * @return map with (job_id -> ExperimentResult) mapping
     */
    public Map<Integer, ExperimentResult> getJobsByIDs(List<Integer> ids) throws Exception;

    /**
     * Returns the instances of the given experiment as list.
     * 
     * @param idExperiment
     * @return
     * @throws Exception
     */
    public List<Instance> getExperimentInstances(int idExperiment) throws Exception;

    /**
     * Returns the name of the configuration with the given ID, or null, if it
     * doesn't exist
     * 
     * @param idSolverConfig
     * @return
     */
    public String getSolverConfigName(int idSolverConfig) throws Exception;

    /**
     * Updates the name of the solver configuration with the given ID.
     * 
     * @param idSolverConfig
     * @param name
     * @throws Exception
     */
    public void updateSolverConfigurationName(int idSolverConfig, String name) throws Exception;

    /**
     * Returns the ID of the best configuration with cost function @func of the
     * given experiment. Returns 0 if there's no configuration with the given
     * cost function
     * 
     * @param idExperiment
     * @param func
     * @return
     */
    public int getBestConfiguration(int idExperiment, CostFunction func) throws Exception;

    /**
     * Returns the IDs of the #no best configuration with cost function @func of
     * the given experiment.
     * 
     * @param idExperiment
     * @param func
     * @param no
     *            how many
     * @return
     */
    public List<Integer> getBestConfigurations(int idExperiment, CostFunction func, int no) throws Exception;

    /**
     * Returns the IDs of all solver configurations for this experiment.
     * 
     * @param idExperiment
     * @return
     * @throws Exception
     */
    public List<Integer> getSolverConfigurations(int idExperiment) throws Exception;

    /**
     * Loads the parameter graph object of the solver binary selected in the
     * configuration experiment specified by the idExperiment argument.
     * Parameter graphs objects provide methods to build, modify and validate
     * solver parameters.
     * 
     * @param idExperiment
     *            ID of the configuration experiment
     * @return parameter graph object providing parameter space methods.
     */
    public ParameterGraph loadParameterGraphFromDB(int idExperiment) throws Exception;

    /**
     * Loads a parameter graph from a XML file.
     * 
     * @param xmlFileName
     * @return
     * @throws FileNotFoundException
     */
    public ParameterGraph loadParameterGraphFromFile(String xmlFileName) throws Exception;

    /**
     * Returns an instance of a cost function from the given database
     * representation, or null, if no such cost function exists.
     * 
     * @param databaseRepresentation
     * @return
     */
    //public CostFunction costFunctionByName(String databaseRepresentation);
    
    /**
     * Returns an instance of a cost function from the given database
     * representation, or null, if no such cost function exists.
     * 
     * @param idExperiment experiment id
     * @return
     */
    public CostFunction costFunctionByExperiment(int idExperiment, String databaseRepresentation) throws Exception;

    /**
     * Returns the number of cores that can be used for the computation for the
     * experiment specified by <code>idExperiment</code>.
     * 
     * @param idExperiment
     * @return number of computation cores
     * @throws Exception
     */
    public int getComputationCoreCount(int idExperiment) throws Exception;

    /**
     * Returns the number of jobs that have to be computed to finish all jobs in
     * the experiment specified by <code>idExperiment</code>.<br>
     * i.e. the number of jobs with status code <code>RUNNING</code> or
     * <code>NOT_STARTED</code>.
     * 
     * @param idExperiment
     * @return
     * @throws Exception
     */
    public int getComputationJobCount(int idExperiment) throws Exception;

    /**
     * Sets the priority of the job specified by <code>idJob</code> to the
     * priority given by <code>priority</code>.
     * 
     * @param idJob
     * @param priority
     * @throws Exception
     */
    public void setJobPriority(int idJob, int priority) throws Exception;
    
    /**
     * Returns the total amount of CPU time used by the finished jobs of the
     * given experiment.
     * 
     * @param idExperiment
     * @return
     * @throws Exception
     */
    public float getTotalCPUTime(int idExperiment) throws Exception;
    
    /**
     * Returns the Instance-Seed course of the configuration scenario of
     * the given experiment. Do NOT modify the returned course.
     * @param idExperiment
     * @return
     * @throws Exception
     */
    public Course getCourse(int idExperiment) throws Exception;
}
