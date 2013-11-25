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
import static uk.ac.standrews.cs.shabdiz.evaluation.analysis.AnalyticsUtil.decorateManagerAsDeploymentStrategy;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class TimeToReachStatePerDeploymentStrategyAnalyzer extends TimeToReachStateAnalyzer {

    private TimeToReachStatePerDeploymentStrategyAnalyzer(String name, File results_path, String duration_property) throws IOException {

        super(name, results_path, duration_property);
        showLegend = true;
        x_axis_label = "Deployment Strategy";

    }

    @Override
    public String getName() {

        return "Time to reach " + name + " per Deployment Strategy";
    }

    @Override
    protected DefaultStatisticalCategoryDataset getStatisticalCategoryDataset() {

        final Map<String, Statistics> stats_by_application = AnalyticsUtil.getPropertyStatistics(duration_property, experiment_properties, NANOSECOND_TO_SECOND, new String[]{Constants.MANAGER_PROPERTY});
        final DefaultStatisticalCategoryDataset dataset = new DefaultStatisticalCategoryDataset();
        for (Map.Entry<String, Statistics> entry : stats_by_application.entrySet()) {
            final Statistics statistics = entry.getValue();
            final double mean = statistics.getMean().doubleValue();
            final double ci = statistics.getConfidenceInterval95Percent().doubleValue();
            final String[] groups = entry.getKey().split(GROUP_DELIMITER);
            final String application = decorateManagerAsApplicationName(groups[0]);
            final String strategy = decorateManagerAsDeploymentStrategy(groups[0]);
            dataset.add(mean, ci, application, strategy);
        }
        return dataset;
    }

    static TimeToReachStatePerDeploymentStrategyAnalyzer auth(File results_path) throws IOException {

        return new TimeToReachStatePerDeploymentStrategyAnalyzer("AUTH", results_path, Constants.TIME_TO_REACH_AUTH_DURATION);
    }

    static TimeToReachStatePerDeploymentStrategyAnalyzer running(File results_path) throws IOException {

        return new TimeToReachStatePerDeploymentStrategyAnalyzer("RUNNING", results_path, Constants.TIME_TO_REACH_RUNNING_DURATION);
    }

    static TimeToReachStatePerDeploymentStrategyAnalyzer authAfterKill(File results_path) throws IOException {

        return new TimeToReachStatePerDeploymentStrategyAnalyzer("AUTH after kill", results_path, Constants.TIME_TO_REACH_AUTH_AFTER_KILL_DURATION);
    }

    static TimeToReachStatePerDeploymentStrategyAnalyzer runningAfterKill(File results_path) throws IOException {

        return new TimeToReachStatePerDeploymentStrategyAnalyzer("RUNNING after kill", results_path, Constants.TIME_TO_REACH_RUNNING_AFTER_KILL_DURATION);
    }
}
