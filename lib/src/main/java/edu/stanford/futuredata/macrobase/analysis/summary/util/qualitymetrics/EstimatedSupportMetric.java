package edu.stanford.futuredata.macrobase.analysis.summary.util.qualitymetrics;

import sketches.CMomentSketch;

import java.util.Arrays;
import java.util.Collections;

/**
 * Measures how large a subgroup is relative to a global count
 */
public class EstimatedSupportMetric extends EstimatedQualityMetric {
    public EstimatedSupportMetric(double quantile, int ka, int kb) {
        super(quantile, ka, kb);
    }

    @Override
    public String name() {
        return "est_support";
    }

    @Override
    public double value(double[] aggregates) {
        CMomentSketch ms = sketchFromAggregates(aggregates);
        if (ka > 0) {
            return ms.estimateGreaterThanThreshold(cutoff) * aggregates[powerSumsBaseIdx] / globalOutlierCount;
        } else {
            return ms.estimateGreaterThanThreshold(cutoff) * aggregates[logSumsBaseIdx] / globalOutlierCount;
        }
    }

    public double getOutlierRateNeeded(double[] aggregates, double threshold) {
        if (ka > 0) {
            return threshold * globalOutlierCount / aggregates[powerSumsBaseIdx];
        } else {
            return threshold * globalOutlierCount / aggregates[logSumsBaseIdx];
        }
    }

    @Override
    public boolean isMonotonic() {
        return true;
    }
}
