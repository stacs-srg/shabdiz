package uk.ac.standrews.cs.shabdiz;

import org.junit.Test;

public class AutoRemoveScannerTest extends ScannerTest {

    @Override
    public void setUp() throws Exception {

        super.setUp();
        network.setAutoRemoveEnabled(true);
    }

    @Test
    public void testRemovableUnreachable() throws Exception {

        network.manager.setProbeStateResult(ApplicationState.UNREACHABLE);
        network.awaitAnyOfStates(ApplicationState.UNREACHABLE);
        network.assertAllKilled();
    }
}
