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
import static uk.ac.standrews.cs.shabdiz.evaluation.analysis.AnalyticsUtil.decorateManagerAsApplicationName;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class TimeToReachStatePerApplicationAnalyzer implements Analyser {

    private final Properties[] experiment_properties;
    private final String name;
    private final String duration_property;

    private TimeToReachStatePerApplicationAnalyzer(String name, File results_path, String duration_property) throws IOException {

        this.name = name;
        this.duration_property = duration_property;
        experiment_properties = AnalyticsUtil.getAllExperimentPropertiesInPath(results_path);
    }

    @Override
    public String getName() {

        return "Time to reach " + name + " per Application";
    }

    @Override
    public JFreeChart getChart() throws IOException {

        final Map<String, Statistics> stats_by_application = AnalyticsUtil.getPropertyStatistics(duration_property, experiment_properties, NANOSECOND_TO_SECOND, new String[]{Constants.MANAGER_PROPERTY});
        final DefaultStatisticalCategoryDataset dataset = new DefaultStatisticalCategoryDataset();
        for (Map.Entry<String, Statistics> entry : stats_by_application.entrySet()) {
            final Statistics statistics = entry.getValue();
            final double mean = statistics.getMean().doubleValue();
            final double ci = statistics.getConfidenceInterval95Percent().doubleValue();

            final String manager = decorateManagerAsApplicationName(entry.getKey().split(GROUP_DELIMITER)[0]);
            dataset.add(mean, ci, "Application", manager);
        }

        final JFreeChart chart = ChartFactory.createLineChart(getName(), "Application", "Time to reach " + name + " (s)", dataset, PlotOrientation.VERTICAL, false, false, false);
        final StatisticalBarRenderer renderer = new StatisticalBarRenderer();
        chart.getCategoryPlot().setRenderer(renderer);
        chart.getCategoryPlot().getRangeAxis().setLowerBound(0);
        PlainChartTheme.applyTheme(chart);
        return chart;
    }

    static TimeToReachStatePerApplicationAnalyzer auth(File results_path) throws IOException {

        return new TimeToReachStatePerApplicationAnalyzer("AUTH", results_path, Constants.TIME_TO_REACH_AUTH_DURATION);
    }

    static TimeToReachStatePerApplicationAnalyzer running(File results_path) throws IOException {

        return new TimeToReachStatePerApplicationAnalyzer("RUNNING", results_path, Constants.TIME_TO_REACH_RUNNING_DURATION);
    }

    static TimeToReachStatePerApplicationAnalyzer authAfterKill(File results_path) throws IOException {

        return new TimeToReachStatePerApplicationAnalyzer("AUTH after kill", results_path, Constants.TIME_TO_REACH_AUTH_AFTER_KILL_DURATION);
    }

    static TimeToReachStatePerApplicationAnalyzer runningAfterKill(File results_path) throws IOException {

        return new TimeToReachStatePerApplicationAnalyzer("RUNNING after kill", results_path, Constants.TIME_TO_REACH_RUNNING_AFTER_KILL_DURATION);
    }
}
