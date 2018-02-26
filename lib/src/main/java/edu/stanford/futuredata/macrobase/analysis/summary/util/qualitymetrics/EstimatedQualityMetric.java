package edu.stanford.futuredata.macrobase.analysis.summary.util.qualitymetrics;

import macrobase.MarkovBound;
import sketches.CMomentSketch;

import java.util.Arrays;
import java.util.Collections;

/**
 * Quality metric used in the power cube pipeline. Uses min, max and moments.
 */
public abstract class EstimatedQualityMetric implements QualityMetric {
    int ka;
    int kb;
    private int minIdx;
    private int maxIdx;
    private int logMinIdx;
    private int logMaxIdx;
    int powerSumsBaseIdx;
    int logSumsBaseIdx;
    double quantile;  // eg, 0.99
    double cutoff;
    double globalOutlierCount;
    private double tolerance = 1e-9;
    private boolean useCascade = true;

    EstimatedQualityMetric(double quantile, int ka, int kb) {
        this.quantile = quantile;
        this.ka = ka;
        this.kb = kb;
    }

    CMomentSketch sketchFromAggregates(double[] aggregates) {
        CMomentSketch ms = new CMomentSketch(tolerance);

        double min = 0;
        double max = 1;
        double logMin = 0;
        double logMax = 1;
        double[] powerSums = new double[]{1};
        double[] logSums = new double[]{1};

        if (ka > 0) {
            min = aggregates[minIdx];
            max = aggregates[maxIdx];
            powerSums = Arrays.copyOfRange(aggregates, powerSumsBaseIdx, powerSumsBaseIdx + ka);
        }
        if (kb > 0) {
            logMin = aggregates[logMinIdx];
            logMax = aggregates[logMaxIdx];
            logSums = Arrays.copyOfRange(aggregates, logSumsBaseIdx, logSumsBaseIdx + kb);
        }

        ms.setStats(min, max, logMin, logMax, powerSums, logSums);
        return ms;
    }

    @Override
    public QualityMetric initialize(double[] globalAggregates) {
        if (ka > 0) {
            globalOutlierCount = globalAggregates[powerSumsBaseIdx] * (1.0 - quantile);
        } else {
            globalOutlierCount = globalAggregates[logSumsBaseIdx] * (1.0 - quantile);
        }

        CMomentSketch ms = sketchFromAggregates(globalAggregates);
        try {
            cutoff = ms.getQuantiles(Collections.singletonList(quantile))[0];
        } catch (Exception e) {
            if (ka > 0) {
                cutoff = quantile * (globalAggregates[maxIdx] - globalAggregates[minIdx]) + globalAggregates[minIdx];
            } else {
                cutoff = quantile * (Math.exp(globalAggregates[logMaxIdx]) - Math.exp(globalAggregates[logMinIdx])) +
                        Math.exp(globalAggregates[minIdx]);
            }
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

    private Action actionIfBelowThreshold() {
        if (isMonotonic()) {
            return Action.PRUNE;
        } else {
            return Action.NEXT;
        }
    }

    private Action getActionCascade(double[] aggregates, double threshold) {
        double outlierRateNeeded = getOutlierRateNeeded(aggregates, threshold);

        // Stage 1: simple checks on min and max
        if (ka > 0) {
            if (aggregates[maxIdx] < cutoff || outlierRateNeeded > 1.0) {
                return actionIfBelowThreshold();
            }
            if (aggregates[minIdx] >= cutoff && outlierRateNeeded <= 1.0) {
                return Action.KEEP;
            }
        } else {
            if (Math.exp(aggregates[logMaxIdx]) < cutoff || outlierRateNeeded > 1.0) {
                return actionIfBelowThreshold();
            }
        }

        CMomentSketch ms = sketchFromAggregates(aggregates);

        // Stage 2: Markov bounds
        double[] markovBounds = ms.boundGreaterThanThresholdMarkov(cutoff);
        if (markovBounds[1] < outlierRateNeeded) {
            return actionIfBelowThreshold();
        }
        if (markovBounds[0] >= outlierRateNeeded) {
            return Action.KEEP;
        }

        // Stage 3: Racz bounds
        double[] raczBounds = ms.boundGreaterThanThresholdRacz(cutoff);
        if (raczBounds[1] < outlierRateNeeded) {
            return actionIfBelowThreshold();
        }
        if (raczBounds[0] >= outlierRateNeeded) {
            return Action.KEEP;
        }

        // Stage 4: MaxEnt estimate
        double outlierRateEstimate = ms.estimateGreaterThanThreshold(cutoff);
        return (outlierRateEstimate >= outlierRateNeeded) ? Action.KEEP : actionIfBelowThreshold();
    }

    private Action getActionMaxent(double[] aggregates, double threshold) {
        double outlierRateNeeded = getOutlierRateNeeded(aggregates, threshold);;
        CMomentSketch ms = sketchFromAggregates(aggregates);
        double outlierRateEstimate = ms.estimateGreaterThanThreshold(cutoff);
        return (outlierRateEstimate >= outlierRateNeeded) ? Action.KEEP : actionIfBelowThreshold();
    }

    public void setUseCascade(boolean useCascade) { this.useCascade = useCascade; }
    public void setTolerance(double tolerance) { this.tolerance = tolerance; }
    
    public void setStandardIndices(int minIdx, int maxIdx, int powerSumsBaseIdx) {
        this.minIdx = minIdx;
        this.maxIdx = maxIdx;
        this.powerSumsBaseIdx = powerSumsBaseIdx;
    }
    public void setLogIndices(int logMinIdx, int logMaxIdx, int logSumsBaseIdx) {
        this.logMinIdx = logMinIdx;
        this.logMaxIdx = logMaxIdx;
        this.logSumsBaseIdx = logSumsBaseIdx;
    }
}
