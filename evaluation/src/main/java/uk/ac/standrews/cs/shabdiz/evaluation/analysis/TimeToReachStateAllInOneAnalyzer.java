package uk.ac.standrews.cs.shabdiz.evaluation.analysis;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.StatisticalBarRenderer;
import org.jfree.data.statistics.DefaultStatisticalCategoryDataset;
import org.json.JSONArray;
import org.json.JSONException;
import org.mashti.sight.PlainChartTheme;
import org.mashti.sina.distribution.statistic.Statistics;
import uk.ac.standrews.cs.shabdiz.evaluation.Constants;

import static uk.ac.standrews.cs.shabdiz.evaluation.analysis.AnalyticsUtil.NANOSECOND_TO_SECOND;
import static uk.ac.standrews.cs.shabdiz.evaluation.analysis.AnalyticsUtil.getAllExperimentPropertiesInPath;
import static uk.ac.standrews.cs.shabdiz.evaluation.analysis.AnalyticsUtil.getPropertyStatistics;

/**
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class TimeToReachStateAllInOneAnalyzer implements Analyser {

    public static final String STATE_GROUP = "state";
    private final File results_path;
    private DefaultStatisticalCategoryDataset dataset;

    public TimeToReachStateAllInOneAnalyzer(File results_path) {

        this.results_path = results_path;
    }

    @Override
    public String getName() {

        return "Time to reach state";
    }

    @Override
    public JFreeChart getChart() throws IOException {


        final JFreeChart chart = ChartFactory.createLineChart("Time to reach uniform state", "Application State", "Time to reach state (s)", getDataset(), PlotOrientation.VERTICAL, false, false, false);
        final CategoryPlot category_plot = chart.getCategoryPlot();
        category_plot.setRenderer(new StatisticalBarRenderer());
        category_plot.getRangeAxis().setLowerBound(0);
        PlainChartTheme.applyTheme(chart);
        return chart;
    }

    @Override
    public synchronized DefaultStatisticalCategoryDataset getDataset() throws IOException {

        if (dataset == null) {
            final Properties[] repetitions_properties = getAllExperimentPropertiesInPath(results_path);
            final Statistics auth_stats = getPropertyStatistics(Constants.TIME_TO_REACH_AUTH_DURATION, repetitions_properties, NANOSECOND_TO_SECOND);
            final Statistics running_stats = getPropertyStatistics(Constants.TIME_TO_REACH_RUNNING_DURATION, repetitions_properties, NANOSECOND_TO_SECOND);
            final Statistics running_after_kill_stats = getPropertyStatistics(Constants.TIME_TO_REACH_RUNNING_AFTER_KILL_DURATION, repetitions_properties, NANOSECOND_TO_SECOND);
            final Statistics auth_after_kill_stats = getPropertyStatistics(Constants.TIME_TO_REACH_AUTH_AFTER_KILL_DURATION, repetitions_properties, NANOSECOND_TO_SECOND);

            dataset = new DefaultStatisticalCategoryDataset();
            dataset.add(auth_stats.getMean(), auth_stats.getConfidenceInterval95Percent(), STATE_GROUP, "AUTH");
            dataset.add(running_stats.getMean(), running_stats.getConfidenceInterval95Percent(), STATE_GROUP, "RUNNING");
            dataset.add(running_after_kill_stats.getMean(), running_after_kill_stats.getConfidenceInterval95Percent(), STATE_GROUP, "RUNNING after kill");
            dataset.add(auth_after_kill_stats.getMean(), auth_after_kill_stats.getConfidenceInterval95Percent(), STATE_GROUP, "AUTH after kill");
        }
        return dataset;
    }

    @Override
    public JSONArray toJSON() throws IOException, JSONException {

        return  DatasetUtils.toJson(getDataset());
    }
}
