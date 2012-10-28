package edacc.api.costfunctions;

import java.util.List;

import edacc.model.Experiment;
import edacc.model.ExperimentResult;

public class PenalizedGeometricMeanX implements CostFunction {
    private boolean minimize;
    private Experiment.Cost costType;
    private double shift;
    private double costPenaltyValue;
    private int penaltyFactor;
    
    public PenalizedGeometricMeanX(Experiment.Cost costType, boolean minimize, double shift, double costPenaltyValue, int penaltyFactor) {
        this.minimize = minimize;
        this.costType = costType;
        this.shift = shift;
        this.costPenaltyValue = costPenaltyValue;
        if (penaltyFactor <= 0)
            throw new IllegalArgumentException("penalty factor should be greater than 0");
        this.penaltyFactor = penaltyFactor;
    }
    
    @Override
    public double singleCost(ExperimentResult job) {
        if (String.valueOf(job.getResultCode().getResultCode()).startsWith("1")) {
            if (costType.equals(Experiment.Cost.resultTime))
                return job.getResultTime();
            else if (costType.equals(Experiment.Cost.wallTime)) 
                return job.getWallTime();
            else
                return job.getCost();
        } else {
            if (costType.equals(Experiment.Cost.resultTime))
                return job.getCPUTimeLimit() * (double) penaltyFactor;
            else if (costType.equals(Experiment.Cost.wallTime)) 
                return job.getWallClockTimeLimit() * (double) penaltyFactor;
            else
                return costPenaltyValue * penaltyFactor;
        }
    }

    @Override
    public double calculateCost(List<ExperimentResult> results) {
        if (results.size() == 0) throw new IllegalArgumentException("Can't calculate geometric mean of 0 results");
        double logsum = 0.0;
        for (ExperimentResult run: results) logsum += Math.log(singleCost(run) + shift);
        logsum /= results.size();
        return Math.exp(logsum) - shift; 
    }

    @Override
    public double calculateCumulatedCost(List<ExperimentResult> results) {
        throw new RuntimeException("TODO: cumulated cost don't make sense for this cost function?");
    }

    @Override
    public String databaseRepresentation() {
        return "PenalizedGeometricMean" + String.valueOf(penaltyFactor);
    }

    @Override
    public boolean isSingleCostPenalized(ExperimentResult job) {
        if (String.valueOf(job.getResultCode().getResultCode()).startsWith("1")) {
            if (costType.equals(Experiment.Cost.resultTime))
                return false;
            else if (costType.equals(Experiment.Cost.wallTime)) 
                return false;
            else
                return Math.abs(job.getCost() - costPenaltyValue) < 1e-10;
        } else {
            return true;
        }
    }

    @Override
    public boolean getMinimize() {
        return minimize;
    }

}
