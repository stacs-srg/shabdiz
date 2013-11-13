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
import static uk.ac.standrews.cs.shabdiz.evaluation.analysis.AnalyticsUtil.decorateKillPortion;
import static uk.ac.standrews.cs.shabdiz.evaluation.analysis.AnalyticsUtil.decorateManagerAsApplicationName;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class TimeToReachStatePerKillPortionAnalyzer implements Analyser {

    private final Properties[] experiment_properties;
    private final String name;
    private final String duration_property;

    private TimeToReachStatePerKillPortionAnalyzer(String name, File results_path, String duration_property) throws IOException {

        this.name = name;
        this.duration_property = duration_property;
        experiment_properties = AnalyticsUtil.getAllExperimentPropertiesInPath(results_path);
    }

    @Override
    public String getName() {

        return "Time to reach " + name + " per kill portion";
    }

    @Override
    public JFreeChart getChart() throws IOException {

        final Map<String, Statistics> stats_by_application = AnalyticsUtil.getPropertyStatistics(duration_property, experiment_properties, NANOSECOND_TO_SECOND, new String[]{Constants.MANAGER_PROPERTY, Constants.KILL_PORTION_PROPERTY});
        final DefaultStatisticalCategoryDataset dataset = new DefaultStatisticalCategoryDataset();
        for (Map.Entry<String, Statistics> entry : stats_by_application.entrySet()) {
            final Statistics statistics = entry.getValue();
            final double mean = statistics.getMean().doubleValue();
            final double ci = statistics.getConfidenceInterval95Percent().doubleValue();

            final String[] groups = entry.getKey().split(GROUP_DELIMITER);
            final String manager = decorateManagerAsApplicationName(groups[0]);
            final String kill_portion = decorateKillPortion(groups[1]);
            dataset.add(mean, ci, manager, kill_portion);
        }

        final JFreeChart chart = ChartFactory.createLineChart(getName(), "Application", "Time to reach " + name + " (s)", dataset, PlotOrientation.VERTICAL, true, false, false);
        final StatisticalBarRenderer renderer = new StatisticalBarRenderer();
        chart.getCategoryPlot().setRenderer(renderer);
        chart.getCategoryPlot().getRangeAxis().setLowerBound(0);
        PlainChartTheme.applyTheme(chart);
        return chart;
    }

    static TimeToReachStatePerKillPortionAnalyzer auth(File results_path) throws IOException {

        return new TimeToReachStatePerKillPortionAnalyzer("AUTH", results_path, Constants.TIME_TO_REACH_AUTH_DURATION);
    }

    static TimeToReachStatePerKillPortionAnalyzer running(File results_path) throws IOException {

        return new TimeToReachStatePerKillPortionAnalyzer("RUNNING", results_path, Constants.TIME_TO_REACH_RUNNING_DURATION);
    }

    static TimeToReachStatePerKillPortionAnalyzer authAfterKill(File results_path) throws IOException {

        return new TimeToReachStatePerKillPortionAnalyzer("AUTH after kill", results_path, Constants.TIME_TO_REACH_AUTH_AFTER_KILL_DURATION);
    }

    static TimeToReachStatePerKillPortionAnalyzer runningAfterKill(File results_path) throws IOException {

        return new TimeToReachStatePerKillPortionAnalyzer("RUNNING after kill", results_path, Constants.TIME_TO_REACH_RUNNING_AFTER_KILL_DURATION);
    }
}
