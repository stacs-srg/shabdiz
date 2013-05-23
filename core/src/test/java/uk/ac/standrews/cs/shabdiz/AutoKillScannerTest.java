package uk.ac.standrews.cs.shabdiz;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.standrews.cs.shabdiz.util.Duration;
import uk.ac.standrews.cs.shabdiz.util.TimeoutExecutorService;

public class AutoKillScannerTest extends ScannerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoKillScannerTest.class);
    private static final Duration FAULTY_KILLABLE_TEST_TIMEOUT = new Duration(10, TimeUnit.SECONDS);

    @Override
    public void setUp() throws Exception {
        super.setUp();
        network.setAutoKillEnabled(true);
    }

    @Test
    public void testFaultyKillable() throws Exception {

        network.manager.setThrowExceptionOnKill(true);
        network.manager.setProbeStateResult(ApplicationState.RUNNING);

        try {
            TimeoutExecutorService.awaitCompletion(new Callable<Void>() {

                @Override
                public Void call() throws Exception {
                    network.awaitAnyOfStates(ApplicationState.KILLED);
                    return null;
                }
            }, FAULTY_KILLABLE_TEST_TIMEOUT);

            Assert.fail("KILLED state must never be reached since kill is configured to fail");
        }
        catch (TimeoutException e) {

            LOGGER.debug("expected timeout exception", e);
            network.assertAllNotKilled();
            network.assertAllInState(ApplicationState.RUNNING);
        }
    }

    @Test
    public void testKillable() throws Exception {

        network.manager.setProbeStateResult(ApplicationState.RUNNING);
        network.awaitAnyOfStates(ApplicationState.KILLED);
        network.assertAllKilled();
    }

    @Test
    public void testUnkillable() throws Exception {

        network.manager.setProbeStateResult(ApplicationState.AUTH);
        network.awaitAnyOfStates(ApplicationState.AUTH);
        network.assertAllNotKilled();
    }
}
