package edu.stanford.futuredata.macrobase.analysis.summary.util.qualitymetrics;

import sketches.CMomentSketch;

import java.util.Arrays;
import java.util.Collections;

/**
 * Measures how large a subgroup is relative to a global count
 */
public class EstimatedSupportMetric extends EstimatedQualityMetric {
    public EstimatedSupportMetric(int minIdx, int maxIdx, int logMinIdx, int logMaxIdx, int momentsBaseIdx,
                                  int logMomentsBaseIdx, double quantile) {
        super(minIdx, maxIdx, logMinIdx, logMaxIdx, momentsBaseIdx, logMomentsBaseIdx, quantile);
    }

    @Override
    public String name() {
        return "est_support";
    }

    @Override
    public double value(double[] aggregates) {
        CMomentSketch ms = sketchFromAggregates(aggregates);
        return ms.estimateGreaterThanThreshold(cutoff) * aggregates[momentsBaseIdx] / globalOutlierCount;
    }

    public double getOutlierRateNeeded(double[] aggregates, double threshold) {
        return threshold * globalOutlierCount / aggregates[momentsBaseIdx];
    }

    @Override
    public boolean isMonotonic() {
        return true;
    }
}
