package edacc.api;

import java.util.List;

public interface CostFunction {
	public float calculateCost(List<edacc.model.ExperimentResult> results);
}
