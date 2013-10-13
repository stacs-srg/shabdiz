package uk.ac.standrews.cs.shabdiz.evaluation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.inject.Provider;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.standrews.cs.shabdiz.ApplicationState;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.util.Combinations;

import static uk.ac.standrews.cs.shabdiz.ApplicationState.AUTH;
import static uk.ac.standrews.cs.shabdiz.ApplicationState.RUNNING;

/**
 * Investigates how long it takes for a network to reach {@link ApplicationState#AUTH} from {@link ApplicationState#RUNNING} state after killing all application instances.
 * For a given network size, a host provider and a manager:
 * - Adds all hosts to a network
 * - enables status scanner
 * - awaits {@link ApplicationState#AUTH} state
 * - enables auto-deploy
 * - awaits {@link ApplicationState#RUNNING} state
 * - disables status scanner
 * - kills all
 * - awaits {@link ApplicationState#AUTH} state
 * - shuts down the network
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class KillTimeExperiment extends Experiment {

    static final ExperimentManager[] APPLICATION_MANAGERS = {ChordManager.MAVEN_BASED_COLD, EchoManager.MAVEN_BASED_COLD};
    private static final Logger LOGGER = LoggerFactory.getLogger(KillTimeExperiment.class);
    private static final String TIME_TO_REACH_AUTH_FROM_RUNNING = "time_to_reach_auth_from_running";

    public KillTimeExperiment(int network_size, final Provider<Host> host_provider, ExperimentManager manager) {

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

    @Override
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

        LOGGER.info("killing all application instances");
        network.killAll();

        LOGGER.info("awaiting AUTH afte after kill...");
        final long time_to_reach_auth_from_running = timeUniformNetworkStateInNanos(AUTH);
        setProperty(TIME_TO_REACH_AUTH_FROM_RUNNING, time_to_reach_auth_from_running);
        LOGGER.info("reached AUTH state after kill all in {} seconds", nanosToSeconds(time_to_reach_auth_from_running));
    }
}
