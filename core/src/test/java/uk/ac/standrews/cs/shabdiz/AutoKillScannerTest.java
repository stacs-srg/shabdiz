package uk.ac.standrews.cs.shabdiz;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.standrews.cs.shabdiz.util.TimeoutExecutorService;

/**
 * Tests {@link AutoKillScanner}.
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class AutoKillScannerTest extends ScannerFunctionalityTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoKillScannerTest.class);

    @Override
    public void setUp() throws Exception {

        super.setUp();
        network.setAutoKillEnabled(true);
    }

    @Test
    public void testFaultyKillable() throws Exception {

        network.manager.setThrowExceptionOnKill(true);
        network.manager.setProbeStateResult(ApplicationState.RUNNING);
        assertAwaitKilledStateTimeout();
        network.awaitAnyOfStates(ApplicationState.RUNNING);
        network.assertAllInState(ApplicationState.RUNNING);
    }

    @Test
    public void testKillable() throws Exception {

        network.manager.setProbeStateResult(ApplicationState.RUNNING);
        network.awaitAnyOfStates(ApplicationState.KILLED);
        network.assertAllKilled();
    }

    @Test
    @Ignore
    public void testUnkillable() throws Exception {

        network.manager.setProbeStateResult(ApplicationState.AUTH);
        network.awaitAnyOfStates(ApplicationState.AUTH);
        assertAwaitKilledStateTimeout();
        network.assertAllInState(ApplicationState.AUTH);
    }

    private void assertAwaitKilledStateTimeout() throws InterruptedException, ExecutionException {

        try {
            TimeoutExecutorService.awaitCompletion((Callable<Void>) () -> {

                network.awaitAnyOfStates(ApplicationState.KILLED);
                return null;

            }, AWAIT_STATE_TIMEOUT);

            Assert.fail("KILLED state must never be reached since kill is configured to fail");
        }
        catch (TimeoutException e) {

            LOGGER.debug("expected timeout exception", e);
            network.assertAllNotKilled();
        }
    }
}
