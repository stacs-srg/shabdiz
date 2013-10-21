package uk.ac.standrews.cs.shabdiz;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.standrews.cs.shabdiz.util.TimeoutExecutorService;

public class AutoRemoveScannerTest extends ScannerFunctionalityTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoRemoveScannerTest.class);
    public static final int NETWORK_EMPTINESS_CHECK_DELAY_MILLIS = 1000;

    @Override
    public void setUp() throws Exception {

        super.setUp();
        network.setAutoRemoveEnabled(true);
    }

    @Test
    public void testRemovableUnreachable() throws Exception {

        testRemovableInState(ApplicationState.UNREACHABLE);
    }

    @Test
    public void testRemovableInvalid() throws Exception {

        testRemovableInState(ApplicationState.INVALID);
    }

    @Test
    public void testUnremovable() throws Exception {

        final int expected_size = network.size();
        try {
            testRemovableInState(ApplicationState.RUNNING);
            Assert.fail("must timeout since there is nothing to remove");
        }
        catch (TimeoutException e) {

            LOGGER.debug("expected timeout exception", e);
            Assert.assertEquals(expected_size, network.size());
        }
    }

    private void testRemovableInState(final ApplicationState state) throws InterruptedException, ExecutionException, TimeoutException {

        network.manager.setProbeStateResult(state);
        network.awaitAnyOfStates(state);
        awaitEmptyNetwork();
        network.assertEmptiness();
    }

    private void awaitEmptyNetwork() throws ExecutionException, InterruptedException, TimeoutException {

        TimeoutExecutorService.awaitCompletion(new Callable<Void>() {

            @Override
            public Void call() throws Exception {

                while (network.size() != 0) {
                    Thread.sleep(NETWORK_EMPTINESS_CHECK_DELAY_MILLIS);
                }
                return null;
            }
        }, AWAIT_STATE_TIMEOUT);
    }
}
