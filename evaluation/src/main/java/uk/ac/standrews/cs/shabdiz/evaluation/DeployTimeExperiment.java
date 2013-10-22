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
 * Investigates how long it takes for a network to reach {@link ApplicationState#RUNNING} from {@link ApplicationState#AUTH} state after killing all application instances using file, maven and URL based managers.
 * The initial state of the system may be warm or cold:
 * - warm: makes sure the classpath files already exist on hosts and do not need to be uploaded by the manager
 * - cold: removes any existing classpath files on hosts to force manger to upload any needed files
 * For a given network size, a host provider and a manager:
 * - Adds all hosts to a network
 * - enables status scanner
 * - awaits {@link ApplicationState#AUTH} state
 * - enables auto-deploy
 * - awaits {@link ApplicationState#RUNNING} state
 * - shuts down the network
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class DeployTimeExperiment extends Experiment {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeployTimeExperiment.class);

    public DeployTimeExperiment(int network_size, final Provider<Host> host_provider, ExperimentManager manager) {

        super(network_size, host_provider, manager);
    }

    @Parameterized.Parameters(name = "network_size_{0}__on_{1}__{2}")
    public static Collection<Object[]> getParameters() {

        final List<Object[]> parameters = new ArrayList<Object[]>();
        final List<Object[]> combinations = Combinations.generateArgumentCombinations(new Object[][] {NETWORK_SIZES, BLUB_HOST_PROVIDER, ALL_APPLICATION_MANAGERS});
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
        final long time_to_reach_running = timeUniformNetworkStateInNanos(RUNNING);
        setProperty(TIME_TO_REACH_RUNNING, time_to_reach_running);
        LOGGER.info("reached RUNNING state in {} seconds", nanosToSeconds(time_to_reach_running));
    }
}
