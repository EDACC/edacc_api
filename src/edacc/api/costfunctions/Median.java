package edacc.api.costfunctions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edacc.model.ExperimentResult;

public class Median implements CostFunction {

	@Override
	public float calculateCost(List<ExperimentResult> results) {
		if (results.size() == 0) return 0;
		List<Float> vals = new ArrayList<Float>();
		for (ExperimentResult res: results) {
			vals.add(res.getResultTime());
		}
		Collections.sort(vals);
		return vals.get(vals.size() / 2);
	}

	@Override
	public String databaseRepresentation() {
		return "median";
	}

}
