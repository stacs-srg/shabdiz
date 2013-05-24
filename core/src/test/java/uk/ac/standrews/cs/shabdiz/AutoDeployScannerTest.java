package uk.ac.standrews.cs.shabdiz;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.standrews.cs.shabdiz.util.TimeoutExecutorService;

public class AutoDeployScannerTest extends ScannerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoDeployScannerTest.class);

    @Override
    public void setUp() throws Exception {

        super.setUp();
        network.setAutoDeployEnabled(true);
    }

    @Test
    public void testFaultyDeployable() throws Exception {

        network.manager.setThrowExceptionOnDeploy(true);
        network.manager.setProbeStateResult(ApplicationState.AUTH);
        assertAwaitDeployedStateTimeout();
        network.assertAllInState(ApplicationState.AUTH);
    }

    @Test
    public void testDeployable() throws Exception {

        network.manager.setProbeStateResult(ApplicationState.AUTH);
        network.awaitAnyOfStates(ApplicationState.DEPLOYED);
        network.assertAllDeployed();
    }

    @Test
    public void testUndeployable() throws Exception {

        network.manager.setProbeStateResult(ApplicationState.NO_AUTH);
        network.awaitAnyOfStates(ApplicationState.NO_AUTH);
        assertAwaitDeployedStateTimeout();
        network.assertAllInState(ApplicationState.NO_AUTH);
    }

    private void assertAwaitDeployedStateTimeout() throws InterruptedException, ExecutionException {
        try {
            TimeoutExecutorService.awaitCompletion(new Callable<Void>() {

                @Override
                public Void call() throws Exception {
                    network.awaitAnyOfStates(ApplicationState.DEPLOYED);
                    return null;
                }
            }, AWAIT_STATE_TIMEOUT);

            Assert.fail("DEPLOYED state must never be reached since deploy is configured to fail");
        }
        catch (TimeoutException e) {

            LOGGER.debug("expected timeout exception", e);
            network.assertAllNotDeployed();
        }
    }
}
