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
import org.mashti.gauge.reporter.CsvReporter;
import org.mashti.jetson.util.CloseableUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.ApplicationManager;
import uk.ac.standrews.cs.shabdiz.ApplicationNetwork;
import uk.ac.standrews.cs.shabdiz.ApplicationState;
import uk.ac.standrews.cs.shabdiz.evaluation.util.BlubHostProvider;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.util.Combinations;
import uk.ac.standrews.cs.shabdiz.util.Duration;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
@RunWith(Parameterized.class)
public abstract class Experiment {

    static final String PROPERTOES_FILE_NAME = "experiment.properties";
    private static final Logger LOGGER = LoggerFactory.getLogger(Experiment.class);
    private static final Duration REPORT_INTERVAL = new Duration(5, TimeUnit.SECONDS);
    private static final int REPETITIONS = 20;
    private static final Integer[] NETWORK_SIZES = {10, 20, 30, 40, 48};
    private static final Provider<Host>[] HOST_PROVIDERS = new Provider[]{new BlubHostProvider()};
    private final MetricRegistry registry;
    private final CsvReporter reporter;
    private final StateCountGauge auth_state_gauge = new StateCountGauge(ApplicationState.AUTH);
    private final StateCountGauge unknown_state_gauge = new StateCountGauge(ApplicationState.UNKNOWN);
    private final StateCountGauge running_state_gauge = new StateCountGauge(ApplicationState.RUNNING);
    private final StateCountGauge other_state_gauge = new StateCountGauge(ApplicationState.DEPLOYED, ApplicationState.INVALID, ApplicationState.KILLED, ApplicationState.LAUNCHED, ApplicationState.NO_AUTH, ApplicationState.UNREACHABLE);
    //TODO add CPU and memory usage gauge
    private final File observations_directory;
    private final String name;
    private final Properties properties = new Properties();
    private final Integer network_size;
    private final Provider<Host> host_provider;
    private final ApplicationManager manager;
    private final ApplicationNetwork network;

    public Experiment(Integer network_size, Provider<Host> host_provider, ApplicationManager manager) throws IOException {

        this.network_size = network_size;
        this.host_provider = host_provider;
        this.manager = manager;
        network = new ApplicationNetwork(getClass().getSimpleName() + " Network");
        name = getClass().getSimpleName() + "_" + network_size;
        observations_directory = new File(new File(name, "repetitions"), String.valueOf(System.currentTimeMillis()));
        FileUtils.forceMkdir(observations_directory);
        registry = new MetricRegistry(getClass().getSimpleName());
        reporter = new CsvReporter(registry, observations_directory);
        populateProperties();
    }

    @Parameterized.Parameters(name = "{index}: network_size: {0}, host_provider: {1}")
    public static Collection<Object[]> data() {

        final List<Object[]> parameters = new ArrayList<Object[]>();
        final List<Object[]> combinations = Combinations.generateArgumentCombinations(new Object[][]{NETWORK_SIZES, HOST_PROVIDERS});
        for (int i = 0; i < REPETITIONS; i++) {
            parameters.addAll(combinations);
        }
        return parameters;
    }

    @Before
    public void setUp() throws Exception {

        registerMetric("auth_state_gauge", auth_state_gauge);
        registerMetric("running_state_gauge", running_state_gauge);
        registerMetric("unknown_state_gauge", unknown_state_gauge);
        registerMetric("other_state_gauge", other_state_gauge);
        persistProperties();

        LOGGER.info("starting experimentation...");
        startReporter();
    }

    @Test
    @Category(Experiment.class)
    public abstract void doExperiment() throws Exception;

    @After
    public void tearDown() throws Exception {

        reporter.stop();
        getNetwork().shutdown();
        LOGGER.info("done, results are stored at {}", observations_directory);
    }

    private void populateProperties() {

        properties.put("name", name);
        properties.put("observations_directory", observations_directory.getAbsolutePath());
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

    protected void populateNetwork() throws IOException {

        for (int i = 0; i < network_size; i++) {
            final Host host = host_provider.get();
            addHostToNetwork(host);
        }
    }

    protected void addHostToNetwork(final Host host) {

        network.add(new ApplicationDescriptor(host, manager));
    }

    protected void disableAllNetworkScanners() {

        getNetwork().setScanEnabled(false);
    }

    private void startReporter() {

        reporter.start(REPORT_INTERVAL.getLength(), REPORT_INTERVAL.getTimeUnit());
    }

    protected Object setProperty(String key, String value) {

        return properties.setProperty(key, value);
    }

    protected void registerMetric(final String metric_name, final Metric metric) {

        registry.register(metric_name, metric);
    }

    protected ApplicationNetwork getNetwork() {

        return network;
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
