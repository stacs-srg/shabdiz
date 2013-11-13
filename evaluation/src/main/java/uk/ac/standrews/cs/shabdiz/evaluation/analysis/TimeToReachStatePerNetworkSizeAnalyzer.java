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

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class TimeToReachStatePerNetworkSizeAnalyzer extends TimeToReachStateAnalyzer {

    private TimeToReachStatePerNetworkSizeAnalyzer(String name, File results_path, String duration_property) throws IOException {

        super(name, results_path, duration_property);
        x_axis_label = "Network Size";
        showLegend = true;
    }

    @Override
    public String getName() {

        return "Time to reach " + name + " per Network";
    }

    @Override
    protected DefaultStatisticalCategoryDataset getStatisticalCategoryDataset() {

        final Map<String, Statistics> stats_by_application = AnalyticsUtil.getPropertyStatistics(duration_property, experiment_properties, NANOSECOND_TO_SECOND, new String[]{Constants.MANAGER_PROPERTY, Constants.NETWORK_SIZE_PROPERTY});
        final DefaultStatisticalCategoryDataset dataset = new DefaultStatisticalCategoryDataset();
        for (Map.Entry<String, Statistics> entry : stats_by_application.entrySet()) {
            final Statistics statistics = entry.getValue();
            final double mean = statistics.getMean().doubleValue();
            final double ci = statistics.getConfidenceInterval95Percent().doubleValue();

            final String[] groups = entry.getKey().split(GROUP_DELIMITER);
            final String manager = decorateManagerAsApplicationName(groups[0]);
            final String network_size = groups[1];
            dataset.add(mean, ci, manager, network_size);
        }
        return dataset;
    }

    static TimeToReachStatePerNetworkSizeAnalyzer auth(File results_path) throws IOException {

        return new TimeToReachStatePerNetworkSizeAnalyzer("AUTH", results_path, Constants.TIME_TO_REACH_AUTH_DURATION);
    }

    static TimeToReachStatePerNetworkSizeAnalyzer running(File results_path) throws IOException {

        return new TimeToReachStatePerNetworkSizeAnalyzer("RUNNING", results_path, Constants.TIME_TO_REACH_RUNNING_DURATION);
    }

    static TimeToReachStatePerNetworkSizeAnalyzer authAfterKill(File results_path) throws IOException {

        return new TimeToReachStatePerNetworkSizeAnalyzer("AUTH after kill", results_path, Constants.TIME_TO_REACH_AUTH_AFTER_KILL_DURATION);
    }

    static TimeToReachStatePerNetworkSizeAnalyzer runningAfterKill(File results_path) throws IOException {

        return new TimeToReachStatePerNetworkSizeAnalyzer("RUNNING after kill", results_path, Constants.TIME_TO_REACH_RUNNING_AFTER_KILL_DURATION);
    }
}
