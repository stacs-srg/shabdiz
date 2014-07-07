package uk.ac.standrews.cs.shabdiz;

import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.Timeout;
import uk.ac.standrews.cs.shabdiz.util.Duration;

public abstract class ScannerFunctionalityTest {

    protected static final Duration AWAIT_STATE_TIMEOUT = new Duration(10, TimeUnit.SECONDS);
    protected static final int SCAN_TEST_TIMEOUT = 60 * 1000;
    @Rule
    public Timeout timeout = new Timeout(SCAN_TEST_TIMEOUT);
    protected MockApplicationNetwork network;

    @Before
    public void setUp() throws Exception {

        network = new MockApplicationNetwork();
        network.populate();
    }

    @After
    public void tearDown() throws Exception {

        network.shutdown();
    }
}
