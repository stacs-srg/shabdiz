/*
 * shabdiz Library
 * Copyright (C) 2011 Distributed Systems Architecture Research Group
 * <http://www-systems.cs.st-andrews.ac.uk/>
 *
 * This file is part of shabdiz, a variation of the Chord protocol
 * <http://pdos.csail.mit.edu/chord/>, where each node strives to maintain
 * a list of all the nodes in the overlay in order to provide one-hop
 * routing.
 *
 * shabdiz is a free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * shabdiz is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with shabdiz.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, see <https://builds.cs.st-andrews.ac.uk/job/shabdiz/>.
 */
package uk.ac.standrews.cs.shabdiz.integrity;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import uk.ac.standrews.cs.jetson.exception.JsonRpcException;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.shabdiz.api.ApplicationState;
import uk.ac.standrews.cs.shabdiz.api.Worker;
import uk.ac.standrews.cs.shabdiz.host.AbstractHost;
import uk.ac.standrews.cs.shabdiz.host.LocalHost;
import uk.ac.standrews.cs.shabdiz.jobs.WorkerNetwork;
import uk.ac.standrews.cs.shabdiz.util.TestJobRemoteFactory;

/**
 * Tests whether a deployed job returns expected result.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class NormalOperationTest {

    private static final String TEST_EXCEPTION_MESSAGE = "Test Exception Message";
    private static final String HELLO = "hello";

    private AbstractHost localhost;
    private WorkerNetwork launcher;
    private Worker worker;

    /**
     * Sets the up the test.
     * 
     * @throws Exception if unable to set up
     */
    @Before
    public void setUp() throws Exception {

        Diagnostic.setLevel(DiagnosticLevel.NONE);

        //        localhost = new RemoteSSHHost("localhost", new PasswordCredentials(Input.readPassword("Enter password:")));
        localhost = new LocalHost();
        launcher = new WorkerNetwork();
        launcher.add(localhost);
        launcher.deployAll();
        launcher.awaitAnyOfStates(ApplicationState.RUNNING);
        worker = launcher.first().getApplicationReference();
    }

    /**
     * Test.
     * 
     * @throws Exception the exception
     */
    @Test
    public void sayHelloTest() throws Exception {

        final Future<String> future = worker.submit(TestJobRemoteFactory.makeEchoJob(HELLO));
        Assert.assertEquals(HELLO, future.get());
        try {
            worker.shutdown();
        }
        catch (final Exception e) {
            //            e.printStackTrace();
        }
    }

    /**
     * Test.
     * 
     * @throws Exception the exception
     */
    @Test
    public void throwExeptionTest() throws Exception {

        final NullPointerException npe = new NullPointerException(TEST_EXCEPTION_MESSAGE);
        final Future<String> future = worker.submit(TestJobRemoteFactory.makeThrowExceptionJob(npe));

        try {
            future.get(); // Expect the execution exception to be thrown
        }
        catch (final ExecutionException e) {
            final Throwable cause = e.getCause();
            Assert.assertTrue(cause != null);
            Assert.assertEquals(TEST_EXCEPTION_MESSAGE, cause.getMessage());
            Assert.assertEquals(NullPointerException.class, cause.getClass());
        }
    }

    /**
     * Cleans up after tests.
     * 
     * @throws Exception if unable to clean up
     */
    @After
    public void tearDown() throws Exception {

        try {
            worker.shutdown();
        }
        catch (final JsonRpcException e) {
            //ignore; expected.
        }
        launcher.shutdown();
        localhost.close();
    }
}
