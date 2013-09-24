package uk.ac.standrews.cs.shabdiz.evaluation;

import java.io.IOException;
import java.util.Collection;
import javax.inject.Provider;
import org.junit.runners.Parameterized;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.util.Combinations;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class ChordRunningToRunningAfterKillExperimentTest extends ExperimentTest {

    public ChordRunningToRunningAfterKillExperimentTest(int network_size, final Provider<Host> host_provider, ExperimentManager manager, boolean cold, final float kill_portion) throws IOException {

        super(new ChordRunningToRunningAfterKillExperiment(network_size, host_provider, manager, cold, kill_portion));
    }

    @Parameterized.Parameters(name = "{index}: network_size: {0}, host_provider: {1}, manager: {2}, cold: {3}, kill_portion: {4}")
    public static Collection<Object[]> data() {

        return Combinations.generateArgumentCombinations(new Object[][]{NETWORK_SIZES, HOST_PROVIDERS, Experiment.APPLICATION_MANAGERS, Experiment.HOT_COLD, KILL_PORTIONS});
    }
}
