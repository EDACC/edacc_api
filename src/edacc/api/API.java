package edacc.api;

import java.io.FileNotFoundException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import edacc.api.costfunctions.CostFunction;
import edacc.model.ExperimentResult;
import edacc.model.Instance;
import edacc.parameterspace.ParameterConfiguration;
import edacc.parameterspace.graph.ParameterGraph;

/**
 * EDACC Configurator database API.
 * 
 * This API provides methods to create and retrieve solver configurations and
 * jobs for "configuration experiments" that were created using the EDACC GUI application.
 * 
 */
public interface API {
    /**
     * Establishes the database connection.
     * @param hostname
     * @param port
     * @param database
     * @param username
     * @param password
     * @return
     */
    public boolean connect(String hostname, int port, String database, String username, String password) throws Exception;
    
    /**
     * Closes the database connection.
     */
    public void disconnect();
    
    /**
     * Returns a canonical name of the given parameter configuration in the context
     * of the configuration experiment.
     * This means only values that are to be configured are actually appearing in the name.
     * Example: "-c 2 -p1 0.4 -p2 0.8" 
     * @param idExperiment ID of the configuration experiment
     * @param config parameter configuration to be named
     * @return name
     * @throws Exception
     */
    public String getCanonicalName(int idExperiment, ParameterConfiguration config) throws Exception;
    

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
    public int createSolverConfig(int idExperiment, ParameterConfiguration config, String name) throws Exception;
    

    /**
     * Creates a new job with the given parameters and marks it as ready for computation.
     * @param idExperiment ID of the experiment that should contain the job.
     * @param idSolverConfig ID of the solver configuration.
     * @param idInstance ID of the instance.
     * @param seed integer seed that is assigned to the seed parameter of the solver configuration, if the seed parameter was activated.
     * @param cpuTimeLimit time limit of the job in CPU seconds.
     * @return unique database ID > 0 of the created job, 0 on errors.
     */
    public int launchJob(int idExperiment, int idSolverConfig, int idInstance, BigInteger seed, int cpuTimeLimit) throws Exception;

    /**
     * Creates a new job for the given solver configuration
     * in the instance-seed parcour of the given experiment.
     */
    public int launchJob(int idExperiment, int idSolverConfig, int cpuTimeLimit, Random rng) throws Exception;
    
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
    public List<Integer> launchJob(int idExperiment, int idSolverConfig, int[] cpuTimeLimit, int numberRuns, Random rng) throws Exception;
    
    /**
     * Returns the parameter configuration corresponding to the given solver configuration in the DB.
     * @param idExperiment
     * @param idSolverConfig
     */
    public ParameterConfiguration getParameterConfiguration(int idExperiment, int idSolverConfig) throws Exception;
    
    /**
     * If the given parameter configuration already has a corresponding solver configuration
     * in the given experiment this function will return the ID of the solver configuration and
     * 0 otherwise. 
     * @param idExperiment
     * @param config
     * @return
     */
    public int exists(int idExperiment, ParameterConfiguration config) throws Exception;
    
    /**
     * return the current number of jobs of the given solver configuration and 0, if it doesn't exist
     * @param idSolverConfig
     * @return
     */
    public int getNumJobs(int idSolverConfig) throws Exception;
    
    /**
     * Updates the cost of the given solver configuration.
     * The cost function is also saved in the table.
     * @param idSolverConfig
     * @param cost
     * @param func
     */
    public void updateSolverConfigurationCost(int idSolverConfig, float cost, CostFunction func) throws Exception;

    /**
     * Returns the cost function of the given solver configuration as saved in the database
     * @param idSolverConfig
     * @return
     */
    public CostFunction getCostFunction(int idSolverConfig) throws Exception;
    
    /**
     * Returns the current cost value of the solver configuration as saved in the database.
     * Can return null if the DB value is NULL.
     * @param idSolverConfig
     * @return
     */
    public Float getSolverConfigurationCost(int idSolverConfig) throws Exception;
    
    /**
     * Retrieves and returns a job from the database by it's unique ID regardless
     * of its computation status.
     * @param idJob ID of the job
     * @return the job as edacc.model.ExperimentResult object.
     */
    public ExperimentResult getJob(int idJob) throws Exception;
    
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
    public ExperimentResult killJob(int idJob) throws Exception;
    
    /**
     * Deletes a job no matter its computation status.
     * @param idJob ID of the job to delete.
     * @return
     */
    public boolean deleteResult(int idJob) throws Exception;
    
    /**
     * reset the job with the given ID to "not started".
     * @param idJob
     * @throws Exception
     */
    public void restartJob(int idJob, int CPUTimeLimit) throws Exception;
    
    /**
     * Returns all jobs of the solver configuration specified by the idSolverConfig argument.
     * @param idExperiment
     * @param idSolverConfig
     * @return
     */
    public ArrayList<ExperimentResult> getRuns(int idExperiment, int idSolverConfig) throws Exception;
    
    /**
     * returns the length of the instance-seed course of the configuration experiment
     * @return
     * @throws Exception
     */
    public int getCourseLength(int idExperiment) throws Exception;
    
    /**
     * Returns a map with all jobs from the database with IDs specified in the ids argument.
     * @param ids
     * @return map with (job_id -> ExperimentResult) mapping
     */
    public Map<Integer, ExperimentResult> getJobsByIDs(List<Integer> ids) throws Exception;
    
    /**
     * Returns the instances of the given experiment as list.
     * @param idExperiment
     * @return
     * @throws Exception
     */
    public List<Instance> getExperimentInstances(int idExperiment) throws Exception;
    
    /**
     * Returns the name of the configuration with the given ID,
     * or null, if it doesn't exist
     * @param idSolverConfig
     * @return
     */
    public String getSolverConfigName(int idSolverConfig) throws Exception;
    
    /**
     * Returns the ID of the best configuration with cost function @func of the given experiment.
     * Returns 0 if there's no configuration with the given cost function 
     * @param idExperiment
     * @param func
     * @return
     */
    public int getBestConfiguration(int idExperiment, CostFunction func) throws Exception;
    
    /**
     * Returns the IDs of the #no best configuration with cost function @func of the given experiment.
     * @param idExperiment
     * @param func
     * @param no how many
     * @return
     */
    public List<Integer> getBestConfigurations(int idExperiment, CostFunction func, int no) throws Exception;
    
    /**
     * Loads the parameter graph object of the solver binary selected in the configuration experiment
     * specified by the idExperiment argument. Parameter graphs objects provide methods
     * to build, modify and validate solver parameters.
     * @param idExperiment ID of the configuration experiment
     * @return parameter graph object providing parameter space methods.
     */
    public ParameterGraph loadParameterGraphFromDB(int idExperiment) throws Exception;
    
    /**
     * Loads a parameter graph from a XML file.
     * @param xmlFileName
     * @return
     * @throws FileNotFoundException
     */
    public ParameterGraph loadParameterGraphFromFile(String xmlFileName) throws Exception;
   
    /**
     * Returns an instance of a cost function from the given database representation,
     * or null, if no such cost function exists.
     * @param databaseRepresentation
     * @return
     */
    public CostFunction costFunctionByName(String databaseRepresentation);
}
