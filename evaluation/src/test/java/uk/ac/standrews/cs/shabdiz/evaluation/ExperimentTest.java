package uk.ac.standrews.cs.shabdiz.evaluation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.inject.Provider;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import uk.ac.standrews.cs.shabdiz.evaluation.util.LocalHostProvider;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.host.LocalHost;
import uk.ac.standrews.cs.shabdiz.testing.junit.ParallelParameterized;

/**
 * Tests if a given experiment starts and ends normally.
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 * */
@RunWith(Parameterized.class)
public abstract class ExperimentTest {

    static final Long TEST_TIMEOUT = TimeUnit.MILLISECONDS.convert(10, TimeUnit.MINUTES);
    static final Integer[] NETWORK_SIZES = {10};
    @SuppressWarnings("unchecked")
    static final Provider<Host>[] HOST_PROVIDERS = new Provider[]{new LocalHostProvider()};
    private final Experiment experiment;
    @Rule
    public Timeout test_timeout = new Timeout(TEST_TIMEOUT.intValue());

    public ExperimentTest(Experiment experiment) {

        this.experiment = experiment;
    }

    @ParallelParameterized.HostProvider(name = "localhost")
    public static Collection<Host> getHosts() throws IOException {

        final List<Host> hosts = new ArrayList<Host>();
        hosts.add(new LocalHost());
        return hosts;
    }

    @Before
    public void setUp() throws Exception {

        experiment.setUp();
    }

    @After
    public void tearDown() throws Exception {

        experiment.tearDown();
    }

    @Test
    public void testDoExperiment() throws Exception {

        experiment.doExperiment();
    }
}
