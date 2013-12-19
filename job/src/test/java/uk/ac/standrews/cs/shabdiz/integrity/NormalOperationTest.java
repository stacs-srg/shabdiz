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
package uk.ac.standrews.cs.shabdiz.integrity;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import uk.ac.standrews.cs.shabdiz.ApplicationState;
import uk.ac.standrews.cs.shabdiz.host.AbstractHost;
import uk.ac.standrews.cs.shabdiz.host.LocalHost;
import uk.ac.standrews.cs.shabdiz.host.exec.AgentBasedJavaProcessBuilder;
import uk.ac.standrews.cs.shabdiz.job.Worker;
import uk.ac.standrews.cs.shabdiz.job.WorkerNetwork;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests whether a deployed job returns expected result.
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class NormalOperationTest {

    private static final String TEST_EXCEPTION_MESSAGE = "Test Exception Message";
    private static final String HELLO = "hello";
    protected static AbstractHost host;
    protected static WorkerNetwork network;
    protected static Worker worker;

    /**
     * Sets the up the test.
     *
     * @throws Exception if unable to set up
     */
    @BeforeClass
    public static void setUp() throws Exception {

        host = new LocalHost();
        AgentBasedJavaProcessBuilder.clearCachedFilesOnHost(host);
        network = new WorkerNetwork();
        network.add(host);
        network.addMavenDependency("uk.ac.standrews.cs.shabdiz", "job", "1.0-SNAPSHOT", "tests");
        network.deployAll();
        network.awaitAnyOfStates(ApplicationState.RUNNING);
        worker = network.first().getApplicationReference();
    }

    /**
     * Cleans up after tests.
     *
     * @throws Exception if unable to clean up
     */
    @AfterClass
    public static void tearDown() throws Exception {

        network.shutdown();
        host.close();
    }

    /**
     * Test.
     *
     * @throws Exception the exception
     */
    @Test
    public void sayHelloTest() throws Exception {

        final Future<String> future = worker.submit(TestJobRemoteFactory.makeEchoJob(HELLO));
        assertEquals(HELLO, future.get());
    }

    /**
     * Test.
     *
     * @throws Exception the exception
     */
    @Test
    public void throwExceptionTest() throws Exception {

        final NullPointerException npe = new NullPointerException(TEST_EXCEPTION_MESSAGE);
        final Future<String> future = worker.submit(TestJobRemoteFactory.makeThrowExceptionJob(npe));

        try {
            future.get(); // Expect the execution exception to be thrown
        }
        catch (final ExecutionException e) {
            final Throwable cause = e.getCause();
            assertNotNull(cause);
            assertEquals(TEST_EXCEPTION_MESSAGE, cause.getMessage());
            assertEquals(NullPointerException.class, cause.getClass());
        }
    }
}
