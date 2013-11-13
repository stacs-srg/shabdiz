package uk.ac.standrews.cs.shabdiz.evaluation.analysis;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.StatisticalBarRenderer;
import org.jfree.data.statistics.DefaultStatisticalCategoryDataset;
import org.mashti.sight.PlainChartTheme;
import org.mashti.sina.distribution.statistic.Statistics;
import uk.ac.standrews.cs.shabdiz.evaluation.Constants;

import static uk.ac.standrews.cs.shabdiz.evaluation.analysis.AnalyticsUtil.GROUP_DELIMITER;
import static uk.ac.standrews.cs.shabdiz.evaluation.analysis.AnalyticsUtil.NANOSECOND_TO_SECOND;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class TimeToReachStatePerScannerIntervalAnalyzer implements Analyser {

    private final Properties[] experiment_properties;
    private final String name;
    private final String duration_property;

    private TimeToReachStatePerScannerIntervalAnalyzer(String name, File results_path, String duration_property) throws IOException {

        this.name = name;
        this.duration_property = duration_property;
        experiment_properties = AnalyticsUtil.getAllExperimentPropertiesInPath(results_path);
    }

    @Override
    public String getName() {

        return "Time to reach " + name + " per Scanner Interval";
    }

    @Override
    public JFreeChart getChart() throws IOException {

        final Map<String, Statistics> stats_by_application = AnalyticsUtil.getPropertyStatistics(duration_property, experiment_properties, NANOSECOND_TO_SECOND, new String[]{Constants.SCANNER_INTERVAL_PROPERTY});
        final DefaultStatisticalCategoryDataset dataset = new DefaultStatisticalCategoryDataset();
        for (Map.Entry<String, Statistics> entry : stats_by_application.entrySet()) {
            final Statistics statistics = entry.getValue();
            final double mean = statistics.getMean().doubleValue();
            final double ci = statistics.getConfidenceInterval95Percent().doubleValue();

            final String[] groups = entry.getKey().split(GROUP_DELIMITER);
            final String pool_size = groups[0];
            dataset.add(mean, ci, "", pool_size);
        }

        final JFreeChart chart = ChartFactory.createLineChart(getName(), "Application", "Time to reach " + name + " (s)", dataset, PlotOrientation.VERTICAL, false, false, false);
        final StatisticalBarRenderer renderer = new StatisticalBarRenderer();
        chart.getCategoryPlot().setRenderer(renderer);
        chart.getCategoryPlot().getRangeAxis().setLowerBound(0);
        PlainChartTheme.applyTheme(chart);
        return chart;
    }

    static TimeToReachStatePerScannerIntervalAnalyzer auth(File results_path) throws IOException {

        return new TimeToReachStatePerScannerIntervalAnalyzer("AUTH", results_path, Constants.TIME_TO_REACH_AUTH_DURATION);
    }

    static TimeToReachStatePerScannerIntervalAnalyzer running(File results_path) throws IOException {

        return new TimeToReachStatePerScannerIntervalAnalyzer("RUNNING", results_path, Constants.TIME_TO_REACH_RUNNING_DURATION);
    }

    static TimeToReachStatePerScannerIntervalAnalyzer stabilize(File results_path) throws IOException {

        return new TimeToReachStatePerScannerIntervalAnalyzer("stabilized ring", results_path, Constants.TIME_TO_REACH_STABILIZED_RING_DURATION);
    }

    static TimeToReachStatePerScannerIntervalAnalyzer authAfterKill(File results_path) throws IOException {

        return new TimeToReachStatePerScannerIntervalAnalyzer("AUTH after kill", results_path, Constants.TIME_TO_REACH_AUTH_AFTER_KILL_DURATION);
    }

    static TimeToReachStatePerScannerIntervalAnalyzer runningAfterKill(File results_path) throws IOException {

        return new TimeToReachStatePerScannerIntervalAnalyzer("RUNNING after kill", results_path, Constants.TIME_TO_REACH_RUNNING_AFTER_KILL_DURATION);
    }

    static TimeToReachStatePerScannerIntervalAnalyzer stabilizeAfterKill(File results_path) throws IOException {

        return new TimeToReachStatePerScannerIntervalAnalyzer("stabilized ring after kill", results_path, Constants.TIME_TO_REACH_STABILIZED_RING_AFTER_KILL_DURATION);
    }
}
