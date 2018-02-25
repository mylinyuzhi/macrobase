package edu.stanford.futuredata.macrobase.analysis.summary.util.qualitymetrics;

import macrobase.MarkovBound;
import sketches.CMomentSketch;

import java.util.Arrays;
import java.util.Collections;

/**
 * Quality metric used in the power cube pipeline. Uses min, max and moments.
 */
public abstract class EstimatedQualityMetric implements QualityMetric {
    private int minIdx = 0;
    private int maxIdx = 1;
    private int logMinIdx = 2;
    private int logMaxIdx = 3;
    int momentsBaseIdx = 4;
    private int logMomentsBaseIdx = 4 + 9;
    double quantile;  // eg, 0.99
    double cutoff;
    double globalOutlierCount;
    private double tolerance = 1e-9;
    private boolean useCascade = true;

    EstimatedQualityMetric(int minIdx, int maxIdx, int logMinIdx, int logMaxIdx, int momentsBaseIdx,
                           int logMomentsBaseIdx, double quantile) {
        this.minIdx = minIdx;
        this.maxIdx = maxIdx;
        this.logMinIdx = logMinIdx;
        this.logMaxIdx = logMaxIdx;
        this.momentsBaseIdx = momentsBaseIdx;
        this.logMomentsBaseIdx = logMomentsBaseIdx;
        this.quantile = quantile;
    }

    CMomentSketch sketchFromAggregates(double[] aggregates) {
        CMomentSketch ms = new CMomentSketch(tolerance);
        double min = aggregates[minIdx];
        double max = aggregates[maxIdx];
        double logMin = aggregates[logMinIdx];
        double logMax = aggregates[logMaxIdx];
        double[] powerSums = Arrays.copyOfRange(aggregates, momentsBaseIdx, logMomentsBaseIdx);
        double[] logSums = Arrays.copyOfRange(aggregates, logMomentsBaseIdx, aggregates.length);
        ms.setStats(min, max, logMin, logMax, powerSums, logSums);
        return ms;
    }

    @Override
    public QualityMetric initialize(double[] globalAggregates) {
        globalOutlierCount = globalAggregates[momentsBaseIdx] * (1.0 - quantile);
        CMomentSketch ms = sketchFromAggregates(globalAggregates);
        try {
            cutoff = ms.getQuantiles(Collections.singletonList(quantile))[0];
        } catch (Exception e) {
            cutoff = quantile * (globalAggregates[maxIdx] - globalAggregates[minIdx]) + globalAggregates[minIdx];
        }
        return this;
    }

    abstract double getOutlierRateNeeded(double[] aggregates, double threshold);

    @Override
    public Action getAction(double[] aggregates, double threshold) {
        Action action;
        if (useCascade) {
            action = getActionCascade(aggregates, threshold);
        } else {
            action = getActionMaxent(aggregates, threshold);
        }
        return action;
    }

    private Action getActionCascade(double[] aggregates, double threshold) {
        double outlierRateNeeded = getOutlierRateNeeded(aggregates, threshold);

        // Stage 1: simple checks on min and max
        if (aggregates[maxIdx] < cutoff || outlierRateNeeded > 1.0) {
            return Action.PRUNE;
        }
        if (aggregates[minIdx] >= cutoff && outlierRateNeeded <= 1.0) {
            return Action.KEEP;
        }

        double min = aggregates[minIdx];
        double max = aggregates[maxIdx];
        double logMin = aggregates[logMinIdx];
        double logMax = aggregates[logMaxIdx];
        double[] powerSums = Arrays.copyOfRange(aggregates, momentsBaseIdx, logMomentsBaseIdx);
        double[] logSums = Arrays.copyOfRange(aggregates, logMomentsBaseIdx, aggregates.length);

        // Stage 2: Markov bounds
//        Action action = MarkovBound.isPastThreshold(outlierRateNeeded, cutoff, min, max, logMin, logMax, powerSums, logSums);
        Action action = Action.KEEP;
        if (action != null) {
            return action;
        }

        CMomentSketch ms = new CMomentSketch(tolerance);
        ms.setStats(min, max, logMin, logMax, powerSums, logSums);

        // Stage 3: Racz bounds
        double[] bounds = ms.boundGreaterThanThreshold(cutoff);
        if (bounds[1] < outlierRateNeeded) {
            return Action.PRUNE;
        }
        if (bounds[0] >= outlierRateNeeded) {
            return Action.KEEP;
        }

        // Stage 4: MaxEnt estimate
        double outlierRateEstimate = ms.estimateGreaterThanThreshold(cutoff);
        return (outlierRateEstimate >= outlierRateNeeded) ? Action.KEEP : Action.PRUNE;
    }

    private Action getActionMaxent(double[] aggregates, double threshold) {
        double outlierRateNeeded = getOutlierRateNeeded(aggregates, threshold);;
        CMomentSketch ms = sketchFromAggregates(aggregates);
        double outlierRateEstimate = ms.estimateGreaterThanThreshold(cutoff);
        return (outlierRateEstimate >= outlierRateNeeded) ? Action.KEEP : Action.PRUNE;
    }

    public void setUseCascade(boolean useCascade) { this.useCascade = useCascade; }
    public void setTolerance(double tolerance) { this.tolerance = tolerance; }
}
