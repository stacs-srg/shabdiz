package uk.ac.standrews.cs.shabdiz.evaluation;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.inject.Provider;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.mashti.gauge.Metric;
import org.mashti.gauge.MetricRegistry;
import org.mashti.gauge.Timer;
import org.mashti.gauge.jvm.MemoryUsageGauge;
import org.mashti.gauge.jvm.SystemLoadAverageGauge;
import org.mashti.gauge.jvm.ThreadCountGauge;
import org.mashti.gauge.jvm.ThreadCpuUsageGauge;
import org.mashti.gauge.reporter.CsvReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.ApplicationNetwork;
import uk.ac.standrews.cs.shabdiz.ApplicationState;
import uk.ac.standrews.cs.shabdiz.evaluation.util.ApplicationStateCounters;
import uk.ac.standrews.cs.shabdiz.evaluation.util.BlubBytesInGangliaGauge;
import uk.ac.standrews.cs.shabdiz.evaluation.util.BlubBytesOutGangliaGauge;
import uk.ac.standrews.cs.shabdiz.evaluation.util.BlubPacketsInGangliaGauge;
import uk.ac.standrews.cs.shabdiz.evaluation.util.BlubPacketsOutGangliaGauge;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.util.Duration;
import uk.ac.standrews.cs.shabdiz.util.ProcessUtil;

import static org.mashti.jetson.util.CloseableUtil.closeQuietly;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.CONCURRENT_SCANNER_THREAD_POOL_SIZE_PROPERTY;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.EXPERIMENT_DURATION_NANOS;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.EXPERIMENT_FAILURE_CAUSE;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.EXPERIMENT_START_TIME_NANOS;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.EXPERIMENT_STATUS_PROPERTY;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.EXPERIMENT_TIMEOUT;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.FAILURE;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.HOST_PROVIDER_PROPERTY;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.MANAGER_PROPERTY;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.NETWORK_SIZE_PROPERTY;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.PROPERTIES_FILE_NAME;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.REPORT_INTERVAL;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.REPORT_INTERVAL_PROPERTY;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.SCANNER_INTERVAL_PROPERTY;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.SCANNER_SCHEDULER_THREAD_POOL_SIZE_PROPERTY;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.SCANNER_TIMEOUT_PROPERTY;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.SUCCESS;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.USER_PROPERTY;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.WORKING_DIRECTORY_PROPERTY;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
@RunWith(ExperimentRunner.class)
public abstract class Experiment {

    private static final Logger LOGGER = LoggerFactory.getLogger(Experiment.class);
    protected final ApplicationNetwork network;
    protected final Integer network_size;
    private final Timer timer = new Timer();
    private final MetricRegistry registry;
    private final MemoryUsageGauge memory_gauge = new MemoryUsageGauge();
    private final ThreadCpuUsageGauge cpu_gauge = new ThreadCpuUsageGauge();
    private final ThreadCountGauge thread_count_gauge = new ThreadCountGauge();
    private final SystemLoadAverageGauge system_load_average_gauge = new SystemLoadAverageGauge();
    private final Properties properties = new Properties();
    private final Provider<Host> host_provider;
    private final ExperimentManager manager;
    private final CsvReporter reporter;
    @Rule
    public Timeout experiment_timeout = new Timeout(EXPERIMENT_TIMEOUT);
    @Rule
    public ExperimentWatcher watcher = new ExperimentWatcher(this);
    private BlubBytesInGangliaGauge ganglia_bytes_in;
    private BlubBytesOutGangliaGauge ganglia_bytes_out;
    private BlubPacketsInGangliaGauge ganglia_packets_in;
    private BlubPacketsOutGangliaGauge ganglia_packets_out;

    protected Experiment(final Integer network_size, final Provider<Host> host_provider, final ExperimentManager manager, final Duration scanner_interval, final Duration scanner_timeout, final int scanner_scheduler_thread_pool_size, final int concurrent_scanner_thread_pool_size) {

        this.network_size = network_size;
        this.host_provider = host_provider;
        this.manager = manager;
        network = new ApplicationNetwork(getClass().getSimpleName(), scanner_interval, scanner_timeout, scanner_scheduler_thread_pool_size) {

            @Override
            protected ExecutorService createScannerExecutorService() {

                final ThreadPoolExecutor executor = (ThreadPoolExecutor) super.createScannerExecutorService();
                executor.setMaximumPoolSize(concurrent_scanner_thread_pool_size);
                return executor;
            }
        };
        registry = new MetricRegistry(getClass().getSimpleName());
        reporter = new CsvReporter(registry);

        setProperty(SCANNER_INTERVAL_PROPERTY, scanner_interval);
        setProperty(SCANNER_TIMEOUT_PROPERTY, scanner_timeout);
        setProperty(SCANNER_SCHEDULER_THREAD_POOL_SIZE_PROPERTY, scanner_scheduler_thread_pool_size);
        setProperty(CONCURRENT_SCANNER_THREAD_POOL_SIZE_PROPERTY, concurrent_scanner_thread_pool_size);
    }

    @Before
    public void setUp() throws Exception {

        ganglia_bytes_in = new BlubBytesInGangliaGauge();
        ganglia_bytes_out = new BlubBytesOutGangliaGauge();
        ganglia_packets_in = new BlubPacketsInGangliaGauge();
        ganglia_packets_out = new BlubPacketsOutGangliaGauge();
        populateProperties();
        disableAllNetworkScanners();
        LOGGER.info("populating network...");
        populateNetwork();
        LOGGER.info("finished populating network size of {}", network_size);
        if (manager != null) {
            manager.configure(network);
        }
        registerMetrics();
    }

    @Test
    @Category(Experiment.class)
    public final void experiment() throws Throwable {

        LOGGER.info("starting experimentation...");
        final long start = System.nanoTime();
        setProperty(EXPERIMENT_START_TIME_NANOS, start);
        startReporter();
        try {
            doExperiment();
            setProperty(EXPERIMENT_STATUS_PROPERTY, SUCCESS);
        }
        catch (Throwable e) {
            setProperty(EXPERIMENT_STATUS_PROPERTY, FAILURE);
            setProperty(EXPERIMENT_FAILURE_CAUSE, e);
            throw e;
        }
        finally {
            final long duration = System.nanoTime() - start;
            setProperty(EXPERIMENT_DURATION_NANOS, duration);
        }
    }

    public abstract void doExperiment() throws Exception;

    @After
    public void tearDown() {

        try {
            LOGGER.info("stopping reporter...");
            reporter.stop();
            LOGGER.info("shutting down the network...");
            network.shutdown();
        }
        catch (Throwable e) {
            LOGGER.error("error occurred while taring down experiment", e);
        }
        finally {
            if (isLocalHostBlubHeadNode()) {
                LOGGER.info("killing all java processes on blub nodes...");
                killAllJavaProcessesOnBlubNodes();
            }
            LOGGER.info("done");
        }
    }

    static boolean isLocalHostBlubHeadNode() {

        try {
            return "blub-cs.st-andrews.ac.uk".equals(InetAddress.getLocalHost().getHostName());
        }
        catch (UnknownHostException e) {
            LOGGER.error("failed to check if the local host is blub head node", e);
            return false;
        }
    }

    static void killAllJavaProcessesOnBlubNodes() {

        final ProcessBuilder builder = new ProcessBuilder("bash", "-c", "rocks run host \"killall java\";");
        builder.redirectErrorStream(true);
        final Process start;
        try {
            start = builder.start();
            final String output = ProcessUtil.awaitNormalTerminationAndGetOutput(start);
            LOGGER.info("output from killing all java processes on blub nodes: \n{}", output);
        }
        catch (Throwable e) {
            LOGGER.error("failed to kill all java processes on blub", e);
        }
    }

    private void registerMetrics() {

        final ApplicationStateCounters application_state_counters = new ApplicationStateCounters(network);
        application_state_counters.registerTo(registry);
        registerMetric("memory_gauge", memory_gauge);
        registerMetric("cpu_gauge", cpu_gauge);
        registerMetric("thread_count_gauge", thread_count_gauge);
        registerMetric("system_load_average_gauge", system_load_average_gauge);
        registerMetric("ganglia_bytes_in", ganglia_bytes_in);
        registerMetric("ganglia_bytes_out", ganglia_bytes_out);
        registerMetric("ganglia_packets_in", ganglia_packets_in);
        registerMetric("ganglia_packets_out", ganglia_packets_out);
    }

    private void populateProperties() {

        setProperty(USER_PROPERTY, System.getProperty("user.name"));
        setProperty(NETWORK_SIZE_PROPERTY, network_size);
        setProperty(MANAGER_PROPERTY, manager);
        setProperty(HOST_PROVIDER_PROPERTY, host_provider);
        setProperty(WORKING_DIRECTORY_PROPERTY, System.getProperty("user.dir"));
        setProperty(REPORT_INTERVAL_PROPERTY, REPORT_INTERVAL);
    }

    protected void registerMetric(final String metric_name, final Metric metric) {

        registry.register(metric_name, metric);
    }

    private void startReporter() {

        reporter.start(REPORT_INTERVAL.getLength(), REPORT_INTERVAL.getTimeUnit());
    }

    protected void populateNetwork() throws IOException {

        for (int i = 0; i < network_size; i++) {
            final Host host = host_provider.get();
            addHostToNetwork(host);
        }
    }

    protected void addHostToNetwork(final Host host) {

        network.add(new ApplicationDescriptor(host, manager));
    }

    protected void persistProperties(String comment) throws IOException {

        BufferedOutputStream out = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(new File(PROPERTIES_FILE_NAME), false));
            properties.store(out, comment);
        }
        finally {
            closeQuietly(out);
        }
    }

    protected void disableAllNetworkScanners() {

        network.setScanEnabled(false);
    }

    protected long timeUniformNetworkState(String start_property, String duration_property, ApplicationState... states) throws InterruptedException {

        final String states_as_string = Arrays.toString(states);
        LOGGER.info("awaiting {} state(s)...", states_as_string);
        final Timer.Time time = timer.time();
        network.awaitAnyOfStates(states);
        final long duration = time.stop();
        final long start = time.getStartTimeInNanos();

        setProperty(duration_property, duration);
        setProperty(start_property, start);
        LOGGER.info("reached {} state(s) in {} seconds", states_as_string, toSeconds(duration));

        return duration;
    }

    protected Object setProperty(String key, Object value) {

        return properties.setProperty(key, String.valueOf(value));
    }

    protected Timer.Time time() {

        return timer.time();
    }

    protected long toSeconds(final long nanoseconds) {

        return TimeUnit.SECONDS.convert(nanoseconds, TimeUnit.NANOSECONDS);
    }
}
