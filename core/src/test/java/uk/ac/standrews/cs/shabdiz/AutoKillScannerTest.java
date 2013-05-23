package uk.ac.standrews.cs.shabdiz;

import org.junit.Test;

public class AutoKillScannerTest extends ScannerTest {

    @Override
    @Test(timeout = SCAN_TEST_TIMEOUT)
    public void testScan() throws Exception {

        network.setAutoKillEnabled(false);
        network.manager.setProbeStateResult(ApplicationState.RUNNING);
        network.awaitAnyOfStates(ApplicationState.RUNNING);
        network.setAutoKillEnabled(true);
        network.awaitAnyOfStates(ApplicationState.KILLED);
        network.assertAllKilled();
    }
}
