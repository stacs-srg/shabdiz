package uk.ac.standrews.cs.shabdiz.evaluation.analysis;

import java.io.File;
import java.io.IOException;
import org.jfree.chart.JFreeChart;
import org.mashti.sight.ChartExportUtils;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class Analysis {

    private static final int DEFAULT_SVG_WIDTH = 1024;
    private static final int DEFAULT_SVG_HEIGHT = 768;

    public static void main(String[] args) throws IOException {

        final File application_size_effect = new File("/Users/masih/Desktop/results/Experiment 2 Application Size");
        generateGenericCharts(application_size_effect);
        saveAsSVG(application_size_effect, TimeToReachStatePerApplicationAnalyzer.auth(application_size_effect));
        saveAsSVG(application_size_effect, TimeToReachStatePerApplicationAnalyzer.running(application_size_effect));
        saveAsSVG(application_size_effect, TimeToReachStatePerApplicationAnalyzer.runningAfterKill(application_size_effect));
        saveAsSVG(application_size_effect, TimeToReachStatePerApplicationAnalyzer.authAfterKill(application_size_effect));

        final File cross_lab_results = new File("/Users/masih/Desktop/results/Experiment 7 Cross-lab");
        generateGenericCharts(cross_lab_results);
        saveAsSVG(cross_lab_results, new TimeToReachStateAnalyzer(cross_lab_results));

        final File kill_portion_results = new File("/Users/masih/Desktop/results/Experiment 6 Kill Portion Effect");
        generateGenericCharts(kill_portion_results);
        saveAsSVG(kill_portion_results, TimeToReachStatePerKillPortionAnalyzer.auth(kill_portion_results));
        saveAsSVG(kill_portion_results, TimeToReachStatePerKillPortionAnalyzer.running(kill_portion_results));
        saveAsSVG(kill_portion_results, TimeToReachStatePerKillPortionAnalyzer.runningAfterKill(kill_portion_results));
        saveAsSVG(kill_portion_results, TimeToReachStatePerKillPortionAnalyzer.authAfterKill(kill_portion_results));

        final File thread_pool_results = new File("/Users/masih/Desktop/results/Experiment 5 Thread Pool Size Effect");
        saveAsSVG(thread_pool_results, TimeToReachStatePerThreadPoolSizeAnalyzer.auth(thread_pool_results));
        saveAsSVG(thread_pool_results, TimeToReachStatePerThreadPoolSizeAnalyzer.running(thread_pool_results));
        saveAsSVG(thread_pool_results, TimeToReachStatePerThreadPoolSizeAnalyzer.runningAfterKill(thread_pool_results));
        saveAsSVG(thread_pool_results, TimeToReachStatePerThreadPoolSizeAnalyzer.authAfterKill(thread_pool_results));

        final File scanner_interval_results = new File("/Users/masih/Desktop/results/Experiment 4 Scanner Intervals");
        saveAsSVG(scanner_interval_results, TimeToReachStatePerScannerIntervalAnalyzer.auth(scanner_interval_results));
        saveAsSVG(scanner_interval_results, TimeToReachStatePerScannerIntervalAnalyzer.running(scanner_interval_results));
        saveAsSVG(scanner_interval_results, TimeToReachStatePerScannerIntervalAnalyzer.stabilize(scanner_interval_results));
        saveAsSVG(scanner_interval_results, TimeToReachStatePerScannerIntervalAnalyzer.runningAfterKill(scanner_interval_results));
        saveAsSVG(scanner_interval_results, TimeToReachStatePerScannerIntervalAnalyzer.authAfterKill(scanner_interval_results));
        saveAsSVG(scanner_interval_results, TimeToReachStatePerScannerIntervalAnalyzer.stabilizeAfterKill(scanner_interval_results));

        final File network_size_results = new File("/Users/masih/Desktop/results/Experiment 1 Network Size");
        saveAsSVG(network_size_results, TimeToReachStatePerNetworkSizeAnalyzer.auth(network_size_results));
        saveAsSVG(network_size_results, TimeToReachStatePerNetworkSizeAnalyzer.running(network_size_results));
        saveAsSVG(network_size_results, TimeToReachStatePerNetworkSizeAnalyzer.runningAfterKill(network_size_results));
        saveAsSVG(network_size_results, TimeToReachStatePerNetworkSizeAnalyzer.authAfterKill(network_size_results));

        final File deployment_strategy_results = new File("/Users/masih/Desktop/results/Experiment 3 Deployment Strategy");
        saveAsSVG(deployment_strategy_results, TimeToReachStatePerDeploymentStrategyAnalyzer.auth(deployment_strategy_results));
        saveAsSVG(deployment_strategy_results, TimeToReachStatePerDeploymentStrategyAnalyzer.running(deployment_strategy_results));
        saveAsSVG(deployment_strategy_results, TimeToReachStatePerDeploymentStrategyAnalyzer.runningAfterKill(deployment_strategy_results));
        saveAsSVG(deployment_strategy_results, TimeToReachStatePerDeploymentStrategyAnalyzer.authAfterKill(deployment_strategy_results));
    }

    private static void generateGenericCharts(final File results_path) throws IOException {

        saveAsSVG(results_path, new StateChangeAnalyzer(results_path));
        saveAsSVG(results_path, new SystemLoadAverageAnalyzer(results_path));
        saveAsSVG(results_path, new ThreadCountAnalyzer(results_path));
        saveAsSVG(results_path, new PacketsOutPerSecondAnalyzer(results_path));
        saveAsSVG(results_path, new PacketsInPerSecondAnalyzer(results_path));
        saveAsSVG(results_path, new BytesOutPerSecondAnalyzer(results_path));
        saveAsSVG(results_path, new BytesInPerSecondAnalyzer(results_path));
        saveAsSVG(results_path, new CpuUsageAnalyzer(results_path));
        saveAsSVG(results_path, new MemoryUsageAnalyzer(results_path));
    }

    private static void saveAsSVG(final File destination_directory, Analyser analyzer) throws IOException {

        final JFreeChart chart = analyzer.getChart();
        ChartExportUtils.saveAsSVG(chart, DEFAULT_SVG_WIDTH, DEFAULT_SVG_HEIGHT, new File(destination_directory, analyzer.getName() + ".svg"));
    }

}
