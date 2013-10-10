package uk.ac.standrews.cs.shabdiz.evaluation;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
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

    protected static final String TIME_TO_REACH_AUTH = "time_to_reach_auth";
    protected static final String TIME_TO_REACH_RUNNING = "time_to_reach_running";
    static final File RESULTS_HOME = new File("results");
    static final int TIMEOUT = 1000 * 60 * 20; // 20 minutes timeout for an experiment
    static final String PROPERTOES_FILE_NAME = "experiment.properties";
    static final int REPETITIONS = 10;
    static final Integer[] NETWORK_SIZES = {10/*, 20, 30, 40, 48*/};
    //    static final Provider<Host>[] BLUB_HOST_PROVIDER = new Provider[]{new LocalHostProvider()};
    static final Provider<Host>[] BLUB_HOST_PROVIDER = new Provider[] {new BlubHostProvider()};
    static final ExperimentManager[] ALL_APPLICATION_MANAGERS = {
            ChordManager.FILE_BASED_COLD, ChordManager.FILE_BASED_WARM, ChordManager.URL_BASED, ChordManager.MAVEN_BASED_COLD, ChordManager.MAVEN_BASED_WARM, EchoManager.FILE_BASED_COLD, EchoManager.FILE_BASED_WARM, EchoManager.URL_BASED, EchoManager.MAVEN_BASED_COLD, EchoManager.MAVEN_BASED_WARM
    };
    private static final Logger LOGGER = LoggerFactory.getLogger(Experiment.class);
    private static final Duration REPORT_INTERVAL = new Duration(5, TimeUnit.SECONDS);
    protected final ApplicationNetwork network;
    protected final Integer network_size;
    private final Timer timer = new Timer();
    private final MetricRegistry registry;
    private final MemoryUsageGauge memory_gauge = new MemoryUsageGauge();
    private final ThreadCpuUsageGauge cpu_gauge = new ThreadCpuUsageGauge();
    private final ThreadCountGauge thread_count_gauge = new ThreadCountGauge();
    private final Properties properties = new Properties();
    private final Provider<Host> host_provider;
    private final ExperimentManager manager;
    private final CsvReporter reporter;
    @Rule
    public Timeout experiment_timeout = new Timeout(TIMEOUT);

    protected Experiment(Integer network_size, Provider<Host> host_provider, ExperimentManager manager) {

        this.network_size = network_size;
        this.host_provider = host_provider;
        this.manager = manager;
        network = new ApplicationNetwork(getClass().getSimpleName());
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
    public abstract void doExperiment() throws Exception;

    @After
    public void tearDown() throws Exception {

        persistProperties();
        reporter.stop();
        network.shutdown();
        if (host_provider instanceof BlubHostProvider) {
            LOGGER.info("killing all java processes on blub nodes...");
            killAllJavaProcessesOnBlubNodes();
        }
        LOGGER.info("done");
    }

    private static void killAllJavaProcessesOnBlubNodes() throws IOException, InterruptedException {

        final ProcessBuilder builder = new ProcessBuilder("bash", "-c", "rocks run host \"killall java\";");
        builder.redirectErrorStream(true);
        final Process start = builder.start();
        final String output = ProcessUtil.awaitNormalTerminationAndGetOutput(start);
        LOGGER.info("output from killing all java processes on blub nodes: \n{}", output);
    }

    private void registerMetrics() {

        final ApplciationStateCounters application_state_counters = new ApplciationStateCounters(network);
        application_state_counters.registerTo(registry);
        registerMetric("memory_gauge", memory_gauge);
        registerMetric("cpu_gauge", cpu_gauge);
        registerMetric("thread_count_gauge", thread_count_gauge);
    }

    protected String constructName() {

        return getClass().getSimpleName() + '_' + network_size + '_' + host_provider + '_' + manager;
    }

    private void populateProperties() {

        setProperty("user", System.getProperty("user.name"));
        setProperty("network_size", network_size);
        setProperty("manager", manager);
        setProperty("host_provider", host_provider);
        setProperty("working_directory", System.getProperty("user.dir"));
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
