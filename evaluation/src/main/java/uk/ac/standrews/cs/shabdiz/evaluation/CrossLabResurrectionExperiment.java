package uk.ac.standrews.cs.shabdiz.evaluation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import uk.ac.standrews.cs.shabdiz.ApplicationState;
import uk.ac.standrews.cs.shabdiz.evaluation.util.CrossLabHostProvider;
import uk.ac.standrews.cs.shabdiz.util.Combinations;
import uk.ac.standrews.cs.shabdiz.util.Duration;

import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.CONCURRENT_SCANNER_THREAD_POOL_SIZE_5_AND_MAX;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.ECHO_FILE_WARM_MANAGERS;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.KILL_PORTION_50;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.SCANNER_INTERVAL_1_SECOND;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.SCANNER_TIMEOUT_5_MINUTE;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.SCHEDULER_THREAD_POOL_SIZE_10;

/**
 * Investigates how long it takes for a network to reach {@link ApplicationState#RUNNING} state after a portion of application instances are killed.
 * For a given network size, a host provider, a manager and a kill portion:
 * - Adds all hosts to a network
 * - enables status scanner
 * - awaits {@link ApplicationState#AUTH} state
 * - enables auto-deploy
 * - awaits {@link ApplicationState#RUNNING} state
 * - disables auto-deploy
 * - kills a portion of network
 * - re-enables auto-deploy
 * - awaits {@link ApplicationState#RUNNING} state
 * - shuts down the network
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
@RunWith(Parameterized.class)
public class CrossLabResurrectionExperiment extends ResurrectionExperiment {

    public static final CrossLabHostProvider CROSS_LAB_HOST_PROVIDER = new CrossLabHostProvider();

    public CrossLabResurrectionExperiment(final int network_size, ExperimentManager manager, final int kill_portion, Duration scanner_interval, Duration scanner_timeout, int scheduler_pool_size, int concurrent_scanner_pool_size) {

        super(network_size, new CrossLabHostProvider(), manager, kill_portion, scanner_interval, scanner_timeout, scheduler_pool_size, concurrent_scanner_pool_size);
    }

    @Parameterized.Parameters(name = "network_{0}_{1}_{2}_kill_{3}_interval_{4}_timeout_{5}_sch_pool_{6}_conc_pool_{7}")
    public static Collection<Object[]> getParameters() {

        final List<Object[]> parameters = new ArrayList<Object[]>();
        final List<Object[]> combinations = Combinations.generateArgumentCombinations(new Object[][]{new Integer[]{50}, ECHO_FILE_WARM_MANAGERS, KILL_PORTION_50, SCANNER_INTERVAL_1_SECOND, SCANNER_TIMEOUT_5_MINUTE, SCHEDULER_THREAD_POOL_SIZE_10, CONCURRENT_SCANNER_THREAD_POOL_SIZE_5_AND_MAX});
        for (int i = 0; i < 1; i++) {
            parameters.addAll(combinations);
        }
        return parameters;
    }

}
