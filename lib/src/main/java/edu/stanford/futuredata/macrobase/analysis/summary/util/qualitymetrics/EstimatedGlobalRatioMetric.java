package edu.stanford.futuredata.macrobase.analysis.summary.util.qualitymetrics;

import sketches.CMomentSketch;

import java.util.Arrays;
import java.util.Collections;

/**
 * Measures the relative outlier rate w.r.t. the global outlier rate
 */
public class EstimatedGlobalRatioMetric extends EstimatedQualityMetric {
    public EstimatedGlobalRatioMetric(int minIdx, int maxIdx, int logMinIdx, int logMaxIdx, int momentsBaseIdx,
                                      int logMomentsBaseIdx, double quantile) {
        super(minIdx, maxIdx, logMinIdx, logMaxIdx, momentsBaseIdx, logMomentsBaseIdx, quantile);
    }

    @Override
    public String name() {
        return "est_global_ratio";
    }

    @Override
    public double value(double[] aggregates) {
        CMomentSketch ms = sketchFromAggregates(aggregates);
        return ms.estimateGreaterThanThreshold(cutoff) / (1.0 - quantile);
    }

    public double getOutlierRateNeeded(double[] aggregates, double threshold) {
        return threshold * (1.0 - quantile);
    }

    @Override
    public boolean isMonotonic() {
        return false;
    }
}
