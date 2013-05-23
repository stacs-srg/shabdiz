package uk.ac.standrews.cs.shabdiz;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public abstract class ScannerTest {

    protected static final long SCAN_TEST_TIMEOUT = 10 * 1000;
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

    @Test
    public abstract void testScan() throws Exception;
}
