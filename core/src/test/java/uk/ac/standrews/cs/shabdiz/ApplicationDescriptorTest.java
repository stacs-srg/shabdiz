/*
 * Copyright 2013 University of St Andrews School of Computer Science
 *
 * This file is part of Shabdiz.
 *
 * Shabdiz is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Shabdiz is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Shabdiz.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.standrews.cs.shabdiz;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.host.LocalHost;
import uk.ac.standrews.cs.shabdiz.host.exec.Commands;
import uk.ac.standrews.cs.shabdiz.platform.Platform;
import uk.ac.standrews.cs.shabdiz.util.AttributeKey;

/**
 * Tests {@link ApplicationDescriptor}.
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class ApplicationDescriptorTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationDescriptorTest.class);
    private static final int CONCURRENCY_TEST_TIMEOUT_IN_SECONDS = 5;
    private ExecutorService executor;

    /** Prepares the environment for a test. */
    @Before
    public void setUp() {

        executor = Executors.newCachedThreadPool();
    }

    /** Cleans up after a test. */
    @After
    public void tearDown() {

        executor.shutdownNow();
    }

    /** Tests {@link ApplicationDescriptor#compareTo(ApplicationDescriptor)}. */
    @Test
    public void testCompareTo() {

        final ApplicationDescriptor one = newApplicationDescriptor();
        final ApplicationDescriptor other = newApplicationDescriptor();
        Assert.assertTrue(one.compareTo(one) == 0);
        Assert.assertTrue(one.compareTo(other) != 0);
    }

    private ApplicationDescriptor newApplicationDescriptor() {

        return new ApplicationDescriptor(null);
    }

    /**
     * Tests {@link ApplicationDescriptor#getProcesses()}.
     * The set of {@link ApplicationDescriptor} processes must be updated automatically as the process are executed on its host.
     *
     * @throws IOException if failure occurs while resolving local address
     */
    @Test
    public void testGetProcesses() throws IOException {

        final ApplicationDescriptor descriptor = new ApplicationDescriptor(new LocalHost(), null);
        final Host local_host = descriptor.getHost();
        final Platform local_platform = local_host.getPlatform();
        Assert.assertTrue(descriptor.getProcesses().isEmpty());

        final Process process = local_host.execute(Commands.ECHO.get(local_platform));
        Assert.assertTrue(descriptor.getProcesses().contains(process));
        Assert.assertTrue(descriptor.removeProcess(process));
        Assert.assertTrue(descriptor.getProcesses().isEmpty());

        process.destroy();
        local_host.close();
    }

    /** Tests {@link ApplicationDescriptor#getAttribute(AttributeKey)} and {@link ApplicationDescriptor#setAttribute(AttributeKey, Object)}. */
    @Test
    public void testGetSetAttribute() {

        final AttributeKey<String> key = new AttributeKey<String>();
        final String value = "QWERTYUIOP{}\":LKJHGFDSXCVBNM<>";
        final ApplicationDescriptor descriptor = newApplicationDescriptor();
        Assert.assertNull(descriptor.setAttribute(key, value));
        Assert.assertEquals(value, descriptor.getAttribute(key));
        Assert.assertEquals(value, descriptor.setAttribute(key, null));
        Assert.assertNull(descriptor.getAttribute(key));
    }

    /**
     * Tests {@link ApplicationDescriptor#awaitAnyOfStates(ApplicationState...)} for when many concurrent state changes occur.
     *
     * @throws InterruptedException if interrupted while waiting for the test to complete
     */
    @Test
    public void testAwaitAnyOfStatesForStress() throws InterruptedException {

        final List<ApplicationDescriptor> descriptors = createDescriptors(100);
        awaitStateChanges(descriptors, ApplicationState.RUNNING);
    }

    private void awaitStateChanges(final List<ApplicationDescriptor> descriptors, final ApplicationState target_state) throws InterruptedException {

        final int descriptors_size = descriptors.size();
        final CountDownLatch start_latch = new CountDownLatch(1);
        final CountDownLatch state_update_end_latch = new CountDownLatch(descriptors_size);
        final CountDownLatch listeners_end_latch = new CountDownLatch(descriptors_size);

        LOGGER.info("Attempting to await sate changes of {} descriptors...", descriptors_size);
        for (final ApplicationDescriptor descriptor : descriptors) {
            executor.submit(new Callable<Void>() {

                @Override
                public Void call() throws Exception {

                    start_latch.await();
                    descriptor.awaitAnyOfStates(target_state);
                    listeners_end_latch.countDown();
                    return null;
                }
            });

            executor.submit(new Callable<Void>() {

                @Override
                public Void call() throws Exception {

                    start_latch.await();
                    descriptor.setApplicationState(target_state);
                    state_update_end_latch.countDown();
                    return null;
                }
            });
        }
        LOGGER.info("Submitted 'setApplicationState' and 'awaitAnyOfStates' jobs");
        LOGGER.info("Releasing start latch...");
        start_latch.countDown();
        LOGGER.info("Awaiting 'setApplicationState' jobs to compelete...");
        state_update_end_latch.await(); // Await until all states have been updated to the target_state

        LOGGER.info("Awaiting 'awaitAnyOfStates' jobs to compelete...");
        if (!listeners_end_latch.await(CONCURRENCY_TEST_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS)) {
            LOGGER.error("The 'awaitAnyOfStates' jobs did not compelete after {} seconds waiting", CONCURRENCY_TEST_TIMEOUT_IN_SECONDS);
            Assert.fail();
        }
        LOGGER.info("Awaiting 'awaitAnyOfStates' jobs is compelete. \n");
    }

    private List<ApplicationDescriptor> createDescriptors(final int descriptors_count) {

        final List<ApplicationDescriptor> descriptors = new ArrayList<ApplicationDescriptor>();
        for (int i = 0; i < descriptors_count; i++) {
            descriptors.add(newApplicationDescriptor());
        }
        return descriptors;
    }

    /**
     * Tests {@link ApplicationDescriptor#awaitAnyOfStates(ApplicationState...)} for when concurrent state changes occur.
     *
     * @throws InterruptedException if interrupted while waiting for the test to complete
     */
    @Test
    public void testAwaitAnyOfStatesWithConcurrentStateChange() throws InterruptedException {

        final List<ApplicationDescriptor> descriptors = createDescriptors(2);
        awaitStateChanges(descriptors, ApplicationState.RUNNING);
    }
}
