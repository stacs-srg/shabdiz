package uk.ac.standrews.cs.shabdiz.evaluation;

import java.io.IOException;
import java.util.Collection;
import javax.inject.Provider;
import org.junit.runners.Parameterized;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.util.Combinations;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class RunningToRunningAfterKillExperimentTest extends ExperimentTest {

    public RunningToRunningAfterKillExperimentTest(int network_size, final Provider<Host> host_provider, ExperimentManager manager, boolean cold, final float kill_portion) throws IOException {

        super(new RunningToRunningAfterKillExperiment(network_size, host_provider, manager, cold, kill_portion));
    }

    @Parameterized.Parameters(name = "{index}: network_size: {0}, host_provider: {1}, manager: {2}, cold: {3}")
    public static Collection<Object[]> data() {

        return Combinations.generateArgumentCombinations(new Object[][]{NETWORK_SIZES, HOST_PROVIDERS, Experiment.APPLICATION_MANAGERS, Experiment.HOT_COLD, KILL_PORTIONS});
    }
}
