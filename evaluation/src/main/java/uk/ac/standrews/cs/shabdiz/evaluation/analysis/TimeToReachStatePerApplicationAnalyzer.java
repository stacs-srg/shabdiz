package uk.ac.standrews.cs.shabdiz.evaluation.analysis;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.jfree.data.statistics.DefaultStatisticalCategoryDataset;
import org.mashti.sina.distribution.statistic.Statistics;
import uk.ac.standrews.cs.shabdiz.evaluation.Constants;

import static uk.ac.standrews.cs.shabdiz.evaluation.analysis.AnalyticsUtil.GROUP_DELIMITER;
import static uk.ac.standrews.cs.shabdiz.evaluation.analysis.AnalyticsUtil.NANOSECOND_TO_SECOND;
import static uk.ac.standrews.cs.shabdiz.evaluation.analysis.AnalyticsUtil.decorateManagerAsApplicationName;

/**
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class TimeToReachStatePerApplicationAnalyzer extends TimeToReachStateAnalyzer {

    private TimeToReachStatePerApplicationAnalyzer(String name, File results_path, String duration_property) throws IOException {

        super(name, results_path, duration_property);
        showLegend = false;
    }

    @Override
    public String getName() {

        return "Time to reach " + name + " per Application";
    }

    @Override
    public DefaultStatisticalCategoryDataset getDataset() {

        final Map<String, Statistics> stats_by_application = AnalyticsUtil.getPropertyStatistics(duration_property, experiment_properties, NANOSECOND_TO_SECOND, new String[]{Constants.MANAGER_PROPERTY});
        final DefaultStatisticalCategoryDataset dataset = new DefaultStatisticalCategoryDataset();
        for (Map.Entry<String, Statistics> entry : stats_by_application.entrySet()) {
            final Statistics statistics = entry.getValue();
            final double mean = statistics.getMean().doubleValue();
            final double ci = statistics.getConfidenceInterval95Percent().doubleValue();
            final String manager = decorateManagerAsApplicationName(entry.getKey().split(GROUP_DELIMITER)[0]);
            dataset.add(mean, ci, "Application", manager);
        }
        return dataset;
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
