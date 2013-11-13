package uk.ac.standrews.cs.shabdiz.evaluation.analysis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.jfree.chart.JFreeChart;
import org.mashti.sight.ChartExportUtils;

import static uk.ac.standrews.cs.shabdiz.evaluation.analysis.AnalyticsUtil.listSubDirectoriesExcluding;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class Analysis {

    public static final String ANALYSIS_DIR_NAME = "analysis";
    private static final int DEFAULT_SVG_WIDTH = 1024;
    private static final int DEFAULT_SVG_HEIGHT = 768;
    private static final File RESULTS_HOME = new File("/Users/masih/Desktop/results/");

    public static void main(String[] args) throws IOException {

        if (!RESULTS_HOME.isDirectory()) { throw new FileNotFoundException("cannot find results home directory: " + RESULTS_HOME); }

        final File network_size_results = new File(RESULTS_HOME, "Experiment 1 Network Size");
        generateGenericChartsForSubDirectories(network_size_results);
        saveAsSVG(network_size_results, TimeToReachStatePerNetworkSizeAnalyzer.auth(network_size_results));
        saveAsSVG(network_size_results, TimeToReachStatePerNetworkSizeAnalyzer.running(network_size_results));
        saveAsSVG(network_size_results, TimeToReachStatePerNetworkSizeAnalyzer.runningAfterKill(network_size_results));
        saveAsSVG(network_size_results, TimeToReachStatePerNetworkSizeAnalyzer.authAfterKill(network_size_results));

        final File application_size_results = new File(RESULTS_HOME, "Experiment 2 Application Size");
        generateGenericChartsForSubDirectories(application_size_results);
        saveAsSVG(application_size_results, TimeToReachStatePerApplicationAnalyzer.auth(application_size_results));
        saveAsSVG(application_size_results, TimeToReachStatePerApplicationAnalyzer.running(application_size_results));
        saveAsSVG(application_size_results, TimeToReachStatePerApplicationAnalyzer.runningAfterKill(application_size_results));
        saveAsSVG(application_size_results, TimeToReachStatePerApplicationAnalyzer.authAfterKill(application_size_results));

        final File deployment_strategy_results = new File(RESULTS_HOME, "Experiment 3 Deployment Strategy");
        generateGenericChartsForSubDirectories(deployment_strategy_results);
        saveAsSVG(deployment_strategy_results, TimeToReachStatePerDeploymentStrategyAnalyzer.auth(deployment_strategy_results));
        saveAsSVG(deployment_strategy_results, TimeToReachStatePerDeploymentStrategyAnalyzer.running(deployment_strategy_results));
        saveAsSVG(deployment_strategy_results, TimeToReachStatePerDeploymentStrategyAnalyzer.runningAfterKill(deployment_strategy_results));
        saveAsSVG(deployment_strategy_results, TimeToReachStatePerDeploymentStrategyAnalyzer.authAfterKill(deployment_strategy_results));

        final File scanner_interval_results = new File(RESULTS_HOME, "Experiment 4 Scanner Intervals");
        generateGenericChartsForSubDirectories(scanner_interval_results);
        saveAsSVG(scanner_interval_results, TimeToReachStatePerScannerIntervalAnalyzer.auth(scanner_interval_results));
        saveAsSVG(scanner_interval_results, TimeToReachStatePerScannerIntervalAnalyzer.running(scanner_interval_results));
        saveAsSVG(scanner_interval_results, TimeToReachStatePerScannerIntervalAnalyzer.stabilize(scanner_interval_results));
        saveAsSVG(scanner_interval_results, TimeToReachStatePerScannerIntervalAnalyzer.runningAfterKill(scanner_interval_results));
        saveAsSVG(scanner_interval_results, TimeToReachStatePerScannerIntervalAnalyzer.authAfterKill(scanner_interval_results));
        saveAsSVG(scanner_interval_results, TimeToReachStatePerScannerIntervalAnalyzer.stabilizeAfterKill(scanner_interval_results));

        final File thread_pool_results = new File(RESULTS_HOME, "Experiment 5 Thread Pool Size Effect");
        generateGenericChartsForSubDirectories(thread_pool_results);
        saveAsSVG(thread_pool_results, TimeToReachStatePerThreadPoolSizeAnalyzer.auth(thread_pool_results));
        saveAsSVG(thread_pool_results, TimeToReachStatePerThreadPoolSizeAnalyzer.running(thread_pool_results));
        saveAsSVG(thread_pool_results, TimeToReachStatePerThreadPoolSizeAnalyzer.runningAfterKill(thread_pool_results));
        saveAsSVG(thread_pool_results, TimeToReachStatePerThreadPoolSizeAnalyzer.authAfterKill(thread_pool_results));

        final File kill_portion_results = new File(RESULTS_HOME, "Experiment 6 Kill Portion Effect");
        generateGenericChartsForSubDirectories(kill_portion_results);
        saveAsSVG(kill_portion_results, TimeToReachStatePerKillPortionAnalyzer.auth(kill_portion_results));
        saveAsSVG(kill_portion_results, TimeToReachStatePerKillPortionAnalyzer.running(kill_portion_results));
        saveAsSVG(kill_portion_results, TimeToReachStatePerKillPortionAnalyzer.runningAfterKill(kill_portion_results));
        saveAsSVG(kill_portion_results, TimeToReachStatePerKillPortionAnalyzer.authAfterKill(kill_portion_results));

        final File cross_lab_results = new File(RESULTS_HOME, "Experiment 7 Cross-lab");
        generateGenericChartsForSubDirectories(cross_lab_results);
        saveAsSVG(cross_lab_results, new TimeToReachStateAllInOneAnalyzer(cross_lab_results));

    }

    private static void generateGenericChartsForSubDirectories(final File results_path) throws IOException {

        final File[] sub_directories = listSubDirectoriesExcluding(results_path, ANALYSIS_DIR_NAME);

        for (File sub_directory : sub_directories) {
            generateGenericCharts(sub_directory);
        }
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
        final File analysis_dir = makeAnalysisDirectory(destination_directory);
        ChartExportUtils.saveAsSVG(chart, DEFAULT_SVG_WIDTH, DEFAULT_SVG_HEIGHT, new File(analysis_dir, analyzer.getName() + ".svg"));
    }

    private static File makeAnalysisDirectory(final File parent) throws IOException {

        final File analysis_dir = new File(parent, ANALYSIS_DIR_NAME);
        if (!analysis_dir.isDirectory()) {
            FileUtils.forceMkdir(analysis_dir);
        }
        return analysis_dir;
    }
}
