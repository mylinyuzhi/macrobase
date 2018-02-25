package edu.stanford.futuredata.macrobase.analysis.summary.aplinear;

import edu.stanford.futuredata.macrobase.analysis.summary.util.qualitymetrics.EstimatedSupportMetric;
import edu.stanford.futuredata.macrobase.analysis.summary.util.qualitymetrics.QualityMetric;
import edu.stanford.futuredata.macrobase.analysis.summary.util.AttributeEncoder;
import edu.stanford.futuredata.macrobase.analysis.summary.util.qualitymetrics.AggregationOp;
import edu.stanford.futuredata.macrobase.analysis.summary.util.qualitymetrics.EstimatedGlobalRatioMetric;
import edu.stanford.futuredata.macrobase.datamodel.DataFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Summarizer that works over both cube and row-based labeled ratio-based
 * outlier summarization.
 */
public class APLMomentSummarizer extends APLSummarizer {
    private Logger log = LoggerFactory.getLogger("APLMomentSummarizer");
    private String minColumn = null;
    private String maxColumn = null;
    private String logMinColumn = null;
    private String logMaxColumn = null;
    private List<String> momentColumns;
    private List<String> logMomentColumns;
    private double percentile;
    private boolean useCascade;
    private boolean useSupport = true;
    private boolean useGlobalRatio = true;

    @Override
    public List<String> getAggregateNames() {
        ArrayList<String> aggregateNames = new ArrayList<>();
        aggregateNames.add("Minimum");
        aggregateNames.add("Maximum");
        aggregateNames.add("Log Minimum");
        aggregateNames.add("Log Maximum");
        aggregateNames.addAll(momentColumns);
        aggregateNames.addAll(logMomentColumns);
        return aggregateNames;
    }

    @Override
    public double[][] getAggregateColumns(DataFrame input) {
        double[][] aggregateColumns = new double[4+momentColumns.size()+logMomentColumns.size()][];
        int curCol = 0;
        aggregateColumns[curCol++] = input.getDoubleColumnByName(minColumn);
        aggregateColumns[curCol++] = input.getDoubleColumnByName(maxColumn);
        aggregateColumns[curCol++] = input.getDoubleColumnByName(logMinColumn);
        aggregateColumns[curCol++] = input.getDoubleColumnByName(logMaxColumn);
        for (int i = 0; i < momentColumns.size(); i++) {
            aggregateColumns[curCol++] = input.getDoubleColumnByName(momentColumns.get(i));
        }
        for (int i = 0; i < logMomentColumns.size(); i++) {
            aggregateColumns[curCol++] = input.getDoubleColumnByName(logMomentColumns.get(i));
        }

        processCountCol(input, momentColumns.get(0), aggregateColumns[0].length);
        return aggregateColumns;
    }

    @Override
    public AggregationOp[] getAggregationOps() {
        AggregationOp[] aggregationOps = new AggregationOp[4+momentColumns.size()+logMomentColumns.size()];
        aggregationOps[0] = AggregationOp.MIN;
        aggregationOps[1] = AggregationOp.MAX;
        aggregationOps[2] = AggregationOp.MIN;
        aggregationOps[3] = AggregationOp.MAX;
        for (int i = 4; i < aggregationOps.length; i++) {
            aggregationOps[i] = AggregationOp.SUM;
        }
        return aggregationOps;
    }

    @Override
    public int[][] getEncoded(List<String[]> columns, DataFrame input) {
        return encoder.encodeAttributesAsArray(columns);
    }

    @Override
    public List<QualityMetric> getQualityMetricList() {
        List<QualityMetric> qualityMetricList = new ArrayList<>();
        if (useSupport) {
            EstimatedSupportMetric metric = new EstimatedSupportMetric(0, 1, 2, 3, 4,
                    4+momentColumns.size(),(100.0 - percentile) / 100.0);
            metric.setUseCascade(true);
            qualityMetricList.add(metric);
        }
        if (useGlobalRatio) {
            EstimatedGlobalRatioMetric metric = new EstimatedGlobalRatioMetric(0, 1, 2, 3, 4,
                    4+momentColumns.size(),(100.0 - percentile) / 100.0);
            metric.setUseCascade(true);
            qualityMetricList.add(metric);
        }
        return qualityMetricList;
    }

    @Override
    public List<Double> getThresholds() {
        List<Double> thresholds = new ArrayList<>();
        if (useSupport) {
            thresholds.add(minOutlierSupport);
        }
        if (useGlobalRatio) {
            thresholds.add(minRatioMetric);
        }
        return thresholds;
    }

    @Override
    public double getNumberOutliers(double[][] aggregates) {
        double count = 0.0;
        double[] counts = aggregates[2];
        for (int i = 0; i < counts.length; i++) {
            count += counts[i];
        }
        return count * percentile / 100.0;
    }

    public String getMinColumn() {
        return minColumn;
    }
    public void setMinColumn(String minColumn) {
        this.minColumn = minColumn;
    }
    public String getMaxColumn() {
        return maxColumn;
    }
    public void setMaxColumn(String maxColumn) {
        this.maxColumn = maxColumn;
    }
    public void setLogMinColumn(String logMinColumn) {
        this.logMinColumn = logMinColumn;
    }
    public void setLogMaxColumn(String logMaxColumn) {
        this.logMaxColumn = logMaxColumn;
    }
    public List<String> getMomentColumns() {
        return momentColumns;
    }
    public void setMomentColumns(List<String> momentColumns) {
        this.momentColumns = momentColumns;
    }
    public void setLogMomentColumns(List<String> logMomentColumns) {
        this.logMomentColumns = logMomentColumns;
    }
    public void setPercentile(double percentile) {
        this.percentile = percentile;
    }
    public double getMinRatioMetric() {
        return minRatioMetric;
    }
    public void setUseCascade(boolean useCascade) { this.useCascade = useCascade; }
    public void setUseSupport(boolean useSupport) { this.useSupport = useSupport; }
    public void setUseGlobalRatio(boolean useGlobalRatio) { this.useGlobalRatio = useGlobalRatio; }
}
