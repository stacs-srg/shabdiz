package uk.ac.standrews.cs.shabdiz.evaluation;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.inject.Provider;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mashti.gauge.Gauge;
import org.mashti.gauge.Metric;
import org.mashti.gauge.MetricRegistry;
import org.mashti.gauge.Timer;
import org.mashti.gauge.jvm.MemoryUsageGauge;
import org.mashti.gauge.jvm.ThreadCountGauge;
import org.mashti.gauge.jvm.ThreadCpuUsageGauge;
import org.mashti.gauge.reporter.CsvReporter;
import org.mashti.jetson.util.CloseableUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.ApplicationNetwork;
import uk.ac.standrews.cs.shabdiz.ApplicationState;
import uk.ac.standrews.cs.shabdiz.evaluation.util.BlubHostProvider;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.util.Combinations;
import uk.ac.standrews.cs.shabdiz.util.Duration;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
@RunWith(Parallelized.class)
//@RunWith(Parameterized.class)
public abstract class Experiment {

    //TODO fix the state change over time gauge. one for each state : use property change listener
    //TODO fix the key-value pair CSV output

    protected static final String TIME_TO_REACH_AUTH = "time_to_reach_auth";
    protected static final String TIME_TO_REACH_RUNNING = "time_to_reach_running";
    static final String PROPERTOES_FILE_NAME = "experiment.properties";
    static final int REPETITIONS = 1;
    static final Integer[] NETWORK_SIZES = {10, 20, 30, 40, 48};
    //    static final Provider<Host>[] HOST_PROVIDERS = new Provider[]{new LocalHostProvider()}; //new BlubHostProvider()};
    static final Provider<Host>[] HOST_PROVIDERS = new Provider[]{new BlubHostProvider()};
    static final ExperimentManager[] APPLICATION_MANAGERS = {ChordManager.FILE_BASED, ChordManager.URL_BASED, ChordManager.MAVEN_BASED, EchoManager.FILE_BASED, EchoManager.URL_BASED, EchoManager.MAVEN_BASED};
    static final Boolean[] HOT_COLD = {Boolean.FALSE, Boolean.TRUE};
    private static final Logger LOGGER = LoggerFactory.getLogger(Experiment.class);
    private static final Duration REPORT_INTERVAL = new Duration(5, TimeUnit.SECONDS);
    protected final ApplicationNetwork network;
    protected final Integer network_size;
    protected final Timer timer = new Timer();
    private final MetricRegistry registry;
    private CsvReporter reporter;
    private final StateCountGauge auth_state_gauge = new StateCountGauge(ApplicationState.AUTH);
    private final StateCountGauge unknown_state_gauge = new StateCountGauge(ApplicationState.UNKNOWN);
    private final StateCountGauge running_state_gauge = new StateCountGauge(ApplicationState.RUNNING);
    private final StateCountGauge other_state_gauge = new StateCountGauge(ApplicationState.DEPLOYED, ApplicationState.INVALID, ApplicationState.KILLED, ApplicationState.LAUNCHED, ApplicationState.NO_AUTH, ApplicationState.UNREACHABLE);
    private final MemoryUsageGauge memory_gauge = new MemoryUsageGauge();
    private final ThreadCpuUsageGauge cpu_gauge = new ThreadCpuUsageGauge();
    private final ThreadCountGauge thread_count_gauge = new ThreadCountGauge();
    private File observations_directory;
    private String name;
    private final Properties properties = new Properties();
    private final Provider<Host> host_provider;
    private final ExperimentManager manager;
    private final boolean cold;

    public Experiment(Integer network_size, Provider<Host> host_provider, ExperimentManager manager, boolean cold) throws IOException {

        this.network_size = network_size;
        this.host_provider = host_provider;
        this.manager = manager;
        this.cold = cold;
        network = new ApplicationNetwork(getClass().getSimpleName());
        registry = new MetricRegistry(getClass().getSimpleName());
    }

    @Parameterized.Parameters(name = "{index}: network_size: {0}, host_provider: {1}, manager: {2}, cold: {3}")
    public static Collection<Object[]> getParameters() {

        final List<Object[]> parameters = new ArrayList<Object[]>();
        final List<Object[]> combinations = Combinations.generateArgumentCombinations(new Object[][]{NETWORK_SIZES, HOST_PROVIDERS, APPLICATION_MANAGERS, HOT_COLD});
        for (int i = 0; i < REPETITIONS; i++) {
            parameters.addAll(combinations);
        }
        return parameters;
    }

    @Before
    public void setUp() throws Exception {

        name = constructName();
        observations_directory = new File(new File(name, "repetitions"), String.valueOf(System.currentTimeMillis()));
        FileUtils.forceMkdir(observations_directory);
        reporter = new CsvReporter(registry, observations_directory);
        populateProperties();

        disableAllNetworkScanners();
        populateNetwork();
        manager.configure(network, cold);

        registerMetric("auth_state_gauge", auth_state_gauge);
        registerMetric("running_state_gauge", running_state_gauge);
        registerMetric("unknown_state_gauge", unknown_state_gauge);
        registerMetric("other_state_gauge", other_state_gauge);
        registerMetric("memory_gauge", memory_gauge);
        registerMetric("cpu_gauge", cpu_gauge);
        registerMetric("thread_count_gauge", thread_count_gauge);
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
        getNetwork().shutdown();
        LOGGER.info("done, results are stored at {}", observations_directory);
    }

    protected String constructName() {

        return getClass().getSimpleName() + "_" + network_size + "_" + host_provider + "_" + manager + "_" + cold;
    }

    private void populateProperties() {

        setProperty("name", name);
        setProperty("observations_directory", observations_directory.getAbsolutePath());
        setProperty("network_size", network_size);
        setProperty("manager", manager);
        setProperty("cold", cold);
        setProperty("host_provider", host_provider);

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
            out = new BufferedOutputStream(new FileOutputStream(new File(observations_directory, PROPERTOES_FILE_NAME), false));

            properties.store(out, "");
        }
        finally {
            CloseableUtil.closeQuietly(out);
        }
    }

    protected void disableAllNetworkScanners() {

        getNetwork().setScanEnabled(false);
    }

    protected ApplicationNetwork getNetwork() {

        return network;
    }

    protected long timeUniformNetworkStateInNanos(ApplicationState state) throws InterruptedException {

        final Timer.Time time = timer.time();
        getNetwork().awaitAnyOfStates(state);
        return time.stop();
    }

    protected Object setProperty(String key, Object value) {

        return properties.setProperty(key, String.valueOf(value));
    }

    protected class StateCountGauge implements Gauge<Integer> {

        private final ApplicationState[] target_states;

        StateCountGauge(ApplicationState... target_states) {

            this.target_states = target_states;
        }

        @Override
        public Integer get() {

            int count = 0;
            for (ApplicationDescriptor descriptor : getNetwork()) {

                if (descriptor.isInAnyOfStates(target_states)) {
                    count++;
                }
            }
            return count;
        }
    }
}
