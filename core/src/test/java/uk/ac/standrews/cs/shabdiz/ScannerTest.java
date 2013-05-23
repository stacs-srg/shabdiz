package uk.ac.standrews.cs.shabdiz;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.Timeout;

public abstract class ScannerTest {

    protected static final int SCAN_TEST_TIMEOUT = 30 * 1000;
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
