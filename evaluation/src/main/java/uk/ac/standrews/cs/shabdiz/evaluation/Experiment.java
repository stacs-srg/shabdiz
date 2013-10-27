package uk.ac.standrews.cs.shabdiz.evaluation;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.inject.Provider;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.internal.AssumptionViolatedException;
import org.junit.rules.TestWatcher;
import org.junit.rules.Timeout;
import org.junit.runner.Description;
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
import uk.ac.standrews.cs.shabdiz.evaluation.util.ApplciationStateCounters;
import uk.ac.standrews.cs.shabdiz.evaluation.util.BlubHostProvider;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.util.Duration;
import uk.ac.standrews.cs.shabdiz.util.ProcessUtil;

import static org.mashti.jetson.util.CloseableUtil.closeQuietly;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
@RunWith(ExperiementRunner.class)
public abstract class Experiment {

    public static final String EXPERIMENT_STATUS = "status";
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILURE = "FAILURE";
    public static final String EXPERIMENT_FAILURE_CAUSE = "failure.cause";
    public static final String EXPERIMENT_DURATION_NANOS = "experiment.duration_nanos";
    public static final int MAX_RETRY_COUNT = 5;
    public static final String EXPERIMENT_START_TIME_NANOS = "experiment.start_time_nanos";
    public static final String USER_PROPERTY = "user";
    public static final String NETWORK_SIZE_PROPERTY = "network_size";
    public static final String MANAGER_PROPERTY = "manager";
    public static final String HOST_PROVIDER_PROPERTY = "host_provider";
    public static final String WORKING_DIRECTORY_PROPERTY = "working_directory";
    public static final String REPORT_INTERVAL_PROPERTY = "report_interval";
    public static final String TIME_TO_REACH_AUTH = "time_to_reach_auth";
    public static final String TIME_TO_REACH_RUNNING = "time_to_reach_running";
    public static final String PROPERTOES_FILE_NAME = "experiment.properties";
    public static final Integer[] NETWORK_SIZES = {10, 20, 30, 40, 48};
    public static final Duration REPORT_INTERVAL = new Duration(5, TimeUnit.SECONDS);
    static final File RESULTS_HOME = new File("results");
    static final int TIMEOUT = 1000 * 60 * 30; // 30 minutes timeout for an experiment
    static final int REPETITIONS = 5;
    static final Provider<Host>[] BLUB_HOST_PROVIDER = new Provider[]{new BlubHostProvider()};
    static final ExperimentManager[] ALL_APPLICATION_MANAGERS = {ChordManager.FILE_BASED_COLD, ChordManager.FILE_BASED_WARM, ChordManager.URL_BASED, ChordManager.MAVEN_BASED_COLD, ChordManager.MAVEN_BASED_WARM, EchoManager.FILE_BASED_COLD, EchoManager.FILE_BASED_WARM, EchoManager.URL_BASED,
                    EchoManager.MAVEN_BASED_COLD, EchoManager.MAVEN_BASED_WARM};
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
    public Timeout experiment_timeout = new Timeout(TIMEOUT);
    @Rule
    public TestWatcher watcher = new TestWatcher() {

        @Override
        protected void succeeded(final Description description) {

            super.succeeded(description);
            setProperty("watcher." + description, "succeeded");
            LOGGER.info("succeeded test {}", description);
        }

        @Override
        protected void failed(final Throwable e, final Description description) {

            super.failed(e, description);
            setProperty("watcher." + description, "failed: " + e);
            LOGGER.error("failed test {} due to error", description);
            LOGGER.error("failed test error", e);
        }

        @Override
        protected void skipped(final AssumptionViolatedException e, final Description description) {

            super.skipped(e, description);
            setProperty("watcher." + description.getMethodName(), "skipped: " + e);
            LOGGER.warn("skipped test {} due to assumption violation", description);
            LOGGER.warn("assumption violation", e);
        }

        @Override
        protected void finished(final Description description) {

            super.finished(description);

            LOGGER.info("persisting experiment properties...");
            try {
                persistProperties();
            }
            catch (IOException e) {
                LOGGER.error("failed to persist properties of " + description, e);
            }
        }
    };

    protected Experiment(Integer network_size, Provider<Host> host_provider, ExperimentManager manager) {

        this(network_size, host_provider, manager, new Duration(5, TimeUnit.SECONDS), ExperimentManager.PROCESS_START_TIMEOUT, 10);
    }

    public Experiment(final Integer network_size, final Provider<Host> host_provider, final ExperimentManager manager, final Duration scanner_interval, final Duration scanner_timeout, final int scanner_thread_pool_size) {

        this.network_size = network_size;
        this.host_provider = host_provider;
        this.manager = manager;
        network = new ApplicationNetwork(getClass().getSimpleName(), scanner_interval, scanner_timeout, scanner_thread_pool_size);
        registry = new MetricRegistry(getClass().getSimpleName());
        reporter = new CsvReporter(registry);
    }

    @Before
    public void setUp() throws Exception {

        populateProperties();
        disableAllNetworkScanners();
        populateNetwork();
        if (manager != null) {
            manager.configure(network);
        }
        registerMetrics();
        LOGGER.info("starting experimentation...");
        startReporter();
    }

    @Test
    @Category(Experiment.class)
    public final void experiment() throws Throwable {

        final long start = System.nanoTime();
        setProperty(EXPERIMENT_START_TIME_NANOS, start);
        try {
            doExperiment();
            setProperty(EXPERIMENT_STATUS, SUCCESS);
        }
        catch (Throwable e) {
            setProperty(EXPERIMENT_STATUS, FAILURE);
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
            LOGGER.error("error occured while taring down experiment", e);
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
            return InetAddress.getLocalHost().getHostName().equals("blub-cs.st-andrews.ac.uk");
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
            LOGGER.error("faield to kill all java processes on blub", e);
        }
    }

    private void registerMetrics() {

        final ApplciationStateCounters application_state_counters = new ApplciationStateCounters(network);
        application_state_counters.registerTo(registry);
        registerMetric("memory_gauge", memory_gauge);
        registerMetric("cpu_gauge", cpu_gauge);
        registerMetric("thread_count_gauge", thread_count_gauge);
        registerMetric("system_load_average_gauge", system_load_average_gauge);
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

    private void persistProperties() throws IOException {

        BufferedOutputStream out = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(new File(PROPERTOES_FILE_NAME), false));
            properties.store(out, "");
        }
        finally {
            closeQuietly(out);
        }
    }

    protected void disableAllNetworkScanners() {

        network.setScanEnabled(false);
    }

    protected long timeUniformNetworkStateInNanos(ApplicationState state) throws InterruptedException {

        final Timer.Time time = timer.time();
        network.awaitAnyOfStates(state);
        return time.stop();
    }

    protected Object setProperty(String key, Object value) {

        return properties.setProperty(key, String.valueOf(value));
    }

    protected Timer.Time time() {

        return timer.time();
    }

    protected long nanosToSeconds(final long nanos) {

        return TimeUnit.SECONDS.convert(nanos, TimeUnit.NANOSECONDS);
    }
}
