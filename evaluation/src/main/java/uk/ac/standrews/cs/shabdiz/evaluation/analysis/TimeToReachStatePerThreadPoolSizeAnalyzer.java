package uk.ac.standrews.cs.shabdiz.evaluation.analysis;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.jfree.data.statistics.DefaultStatisticalCategoryDataset;
import org.mashti.sina.distribution.statistic.Statistics;
import uk.ac.standrews.cs.shabdiz.evaluation.Constants;

import static uk.ac.standrews.cs.shabdiz.evaluation.analysis.AnalyticsUtil.GROUP_DELIMITER;
import static uk.ac.standrews.cs.shabdiz.evaluation.analysis.AnalyticsUtil.NANOSECOND_TO_SECOND;
import static uk.ac.standrews.cs.shabdiz.evaluation.analysis.AnalyticsUtil.decoratePoolSize;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class TimeToReachStatePerThreadPoolSizeAnalyzer extends TimeToReachStateAnalyzer {

    private TimeToReachStatePerThreadPoolSizeAnalyzer(String name, File results_path, String duration_property) throws IOException {

        super(name, results_path, duration_property);
        x_axis_label = "Thread pool Size";
    }

    @Override
    public String getName() {

        return "Time to reach " + name + " per Thread Pool Size";
    }

    @Override
    protected DefaultStatisticalCategoryDataset getStatisticalCategoryDataset() {

        final Map<String, Statistics> stats_by_application = AnalyticsUtil.getPropertyStatistics(duration_property, experiment_properties, NANOSECOND_TO_SECOND, new String[]{Constants.CONCURRENT_SCANNER_THREAD_POOL_SIZE_PROPERTY});
        final DefaultStatisticalCategoryDataset dataset = new DefaultStatisticalCategoryDataset();
        for (Map.Entry<String, Statistics> entry : stats_by_application.entrySet()) {
            final Statistics statistics = entry.getValue();
            final double mean = statistics.getMean().doubleValue();
            final double ci = statistics.getConfidenceInterval95Percent().doubleValue();

            final String[] groups = entry.getKey().split(GROUP_DELIMITER);
            final String pool_size = decoratePoolSize(groups[0]);
            dataset.add(mean, ci, "", pool_size);
        }
        return dataset;
    }

    static TimeToReachStatePerThreadPoolSizeAnalyzer auth(File results_path) throws IOException {

        return new TimeToReachStatePerThreadPoolSizeAnalyzer("AUTH", results_path, Constants.TIME_TO_REACH_AUTH_DURATION);
    }

    static TimeToReachStatePerThreadPoolSizeAnalyzer running(File results_path) throws IOException {

        return new TimeToReachStatePerThreadPoolSizeAnalyzer("RUNNING", results_path, Constants.TIME_TO_REACH_RUNNING_DURATION);
    }

    static TimeToReachStatePerThreadPoolSizeAnalyzer stabilize(File results_path) throws IOException {

        return new TimeToReachStatePerThreadPoolSizeAnalyzer("stabilized ring", results_path, Constants.TIME_TO_REACH_STABILIZED_RING_DURATION);
    }

    static TimeToReachStatePerThreadPoolSizeAnalyzer authAfterKill(File results_path) throws IOException {

        return new TimeToReachStatePerThreadPoolSizeAnalyzer("AUTH after kill", results_path, Constants.TIME_TO_REACH_AUTH_AFTER_KILL_DURATION);
    }

    static TimeToReachStatePerThreadPoolSizeAnalyzer runningAfterKill(File results_path) throws IOException {

        return new TimeToReachStatePerThreadPoolSizeAnalyzer("RUNNING after kill", results_path, Constants.TIME_TO_REACH_RUNNING_AFTER_KILL_DURATION);
    }

    static TimeToReachStatePerThreadPoolSizeAnalyzer stabilizeAfterKill(File results_path) throws IOException {

        return new TimeToReachStatePerThreadPoolSizeAnalyzer("stabilized ring after kill", results_path, Constants.TIME_TO_REACH_STABILIZED_RING_AFTER_KILL_DURATION);
    }
}
