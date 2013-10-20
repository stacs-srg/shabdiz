package uk.ac.standrews.cs.shabdiz.evaluation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.inject.Provider;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.ApplicationState;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.util.Combinations;
import uk.ac.standrews.cs.shabdiz.util.Duration;

import static uk.ac.standrews.cs.shabdiz.ApplicationState.AUTH;

/**
 * Investigates how long it takes for a network to recognise availablility of added hosts.
 * A host is considered available if it is in {@link ApplicationState#AUTH} state as defined by {@link ExperimentManager}.
 * For a given network size and a host provider:
 * - Adds all hosts to a network
 * - enables status scanner
 * - awaits {@link ApplicationState#AUTH} state
 * - shuts down the network
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class HostAvailabilityRecognitionExperiment extends Experiment {

    private static final Logger LOGGER = LoggerFactory.getLogger(HostAvailabilityRecognitionExperiment.class);
    private static final Duration[] SCANNER_INTERVALS = {
            new Duration(1, TimeUnit.SECONDS), new Duration(3, TimeUnit.SECONDS), new Duration(5, TimeUnit.SECONDS), new Duration(7, TimeUnit.SECONDS), new Duration(9, TimeUnit.SECONDS), new Duration(20, TimeUnit.SECONDS)
    };

    private static final Duration[] SCANNER_TIMEOUTS = {
            new Duration(1, TimeUnit.MINUTES)
    };

    private static final Integer[] SCANNER_THREAD_POOL_SIZES = {10, 100};
    public static final String SCANNER_INTERVAL_PROPERTY = "scanner.interval";
    public static final String SCANNER_TIMEOUT_PROPERTY = "scanner.timeout";
    public static final String SCANNER_THREAD_POOL_SIZE_PROPERTY = "scanner.thread_pool_size";

    public HostAvailabilityRecognitionExperiment(final Integer network_size, final Provider<Host> host_provider, Duration scanner_interval, Duration scanner_timeout, Integer thread_pool_size) {

        super(network_size, host_provider, new NoOpExperimentManager(), scanner_interval, scanner_timeout, thread_pool_size);
        setProperty(SCANNER_INTERVAL_PROPERTY, scanner_interval);
        setProperty(SCANNER_TIMEOUT_PROPERTY, scanner_timeout);
        setProperty(SCANNER_THREAD_POOL_SIZE_PROPERTY, thread_pool_size);

    }

    @Parameterized.Parameters(name = "network_size_{0}__on_{1}_interval_{2}__timeout_{3}__pool_size_{4}")
    public static Collection<Object[]> getParameters() {

        final List<Object[]> parameters = new ArrayList<Object[]>();
        final List<Object[]> combinations = Combinations.generateArgumentCombinations(new Object[][] {NETWORK_SIZES, BLUB_HOST_PROVIDER, SCANNER_INTERVALS, SCANNER_TIMEOUTS, SCANNER_THREAD_POOL_SIZES});
        for (int i = 0; i < REPETITIONS; i++) {
            parameters.addAll(combinations);
        }
        return parameters;
    }

    public void doExperiment() throws Exception {

        LOGGER.info("enabling status scanner");
        network.setStatusScannerEnabled(true);

        LOGGER.info("awaiting AUTH state...");
        final long time_to_reach_auth = timeUniformNetworkStateInNanos(AUTH);
        setProperty(TIME_TO_REACH_AUTH, String.valueOf(time_to_reach_auth));
        LOGGER.info("reached AUTH state in {} seconds", nanosToSeconds(time_to_reach_auth));
    }

    static class NoOpExperimentManager extends ExperimentManager {

        @Override
        public Object deploy(final ApplicationDescriptor descriptor) throws Exception {

            return null;
        }

        @Override
        protected void attemptApplicationCall(final ApplicationDescriptor descriptor) throws Exception {

            throw new Exception("no application");
        }
    }
}
