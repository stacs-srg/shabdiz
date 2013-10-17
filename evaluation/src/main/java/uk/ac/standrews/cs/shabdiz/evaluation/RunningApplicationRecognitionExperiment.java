package uk.ac.standrews.cs.shabdiz.evaluation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.inject.Provider;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.ApplicationState;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.util.Combinations;

import static uk.ac.standrews.cs.shabdiz.ApplicationState.AUTH;
import static uk.ac.standrews.cs.shabdiz.ApplicationState.RUNNING;
import static uk.ac.standrews.cs.shabdiz.ApplicationState.UNKNOWN;

/**
 * Investigates how long it takes for a network to recognise already running instances of an application as defined by its manager.
 * For a given network size, a host provider and a manager:
 * - Adds all hosts to a network
 * - enables status scanner
 * - awaits {@link ApplicationState#AUTH} state
 * - enables auto-deploy
 * - awaits {@link ApplicationState#RUNNING} state
 * - disables status scanner
 * - resets all application states to {@link ApplicationState#UNKNOWN}
 * - awaits {@link ApplicationState#RUNNING} state
 * - shuts down the network
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class RunningApplicationRecognitionExperiment extends Experiment {

    private static final Logger LOGGER = LoggerFactory.getLogger(RunningApplicationRecognitionExperiment.class);
    static final String TIME_TO_REACH_RUNNING_AFTER_RESET = "time_to_reach_running_after_reset";
    private static final ExperimentManager[] APPLICATION_MANAGERS = {ChordManager.MAVEN_BASED_COLD, EchoManager.MAVEN_BASED_COLD};

    public RunningApplicationRecognitionExperiment(final Integer network_size, final Provider<Host> host_provider, ExperimentManager manager) {

        super(network_size, host_provider, manager);
    }

    @Parameterized.Parameters(name = "network_size_{0}__on_{1}__{2}")
    public static Collection<Object[]> getParameters() {

        final List<Object[]> parameters = new ArrayList<Object[]>();
        final List<Object[]> combinations = Combinations.generateArgumentCombinations(new Object[][] {NETWORK_SIZES, BLUB_HOST_PROVIDER, APPLICATION_MANAGERS});
        for (int i = 0; i < REPETITIONS; i++) {
            parameters.addAll(combinations);
        }
        return parameters;
    }

    public void doExperiment() throws Exception {

        LOGGER.info("enabling status scanner");
        network.setStatusScannerEnabled(true);

        LOGGER.info("awaiting AUTH state");
        network.awaitAnyOfStates(AUTH);

        LOGGER.info("enabling auto deploy");
        network.setAutoDeployEnabled(true);

        LOGGER.info("awaiting RUNNING state");
        network.awaitAnyOfStates(RUNNING);

        LOGGER.info("disabling all network scanners");
        disableAllNetworkScanners();

        LOGGER.info("resetting all application descriptor states to UNKNOWN");
        resetApplicationDescriptorsStatusTo(UNKNOWN);

        LOGGER.info("re-enabling status scanner");
        network.setStatusScannerEnabled(true);

        LOGGER.info("awaiting RUNNING state after reset");
        final long time_to_reach_running_after_reset = timeUniformNetworkStateInNanos(RUNNING);
        setProperty(TIME_TO_REACH_RUNNING_AFTER_RESET, time_to_reach_running_after_reset);
        LOGGER.info("reached RUNNING state after reset in {} seconds", nanosToSeconds(time_to_reach_running_after_reset));
    }

    private void resetApplicationDescriptorsStatusTo(final ApplicationState status) {

        for (ApplicationDescriptor descriptor : network) {
            descriptor.setApplicationState(status);
        }
    }
}
