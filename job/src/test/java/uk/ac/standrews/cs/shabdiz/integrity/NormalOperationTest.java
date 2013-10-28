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
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import uk.ac.standrews.cs.shabdiz.ApplicationState;
import uk.ac.standrews.cs.shabdiz.host.AbstractHost;
import uk.ac.standrews.cs.shabdiz.host.LocalHost;
import uk.ac.standrews.cs.shabdiz.job.Worker;
import uk.ac.standrews.cs.shabdiz.job.WorkerNetwork;

/**
 * Tests whether a deployed job returns expected result.
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class NormalOperationTest {

    private static final String TEST_EXCEPTION_MESSAGE = "Test Exception Message";
    private static final String HELLO = "hello";
    private static AbstractHost localhost;
    private static WorkerNetwork network;
    private static Worker worker;

    /**
     * Sets the up the test.
     *
     * @throws Exception if unable to set up
     */
    @BeforeClass
    public static void setUp() throws Exception {

        localhost = new LocalHost();
        //        final OpenSSHKeyFile key_provider = new OpenSSHKeyFile();
        //        key_provider.init(new File(SSHCredentials.DEFAULT_SSH_HOME, "id_rsa"), new PasswordFinder() {
        //
        //            @Override
        //            public char[] reqPassword(final Resource<?> resource) {
        //
        //                return Input.readPassword("local private key password: ");
        //            }
        //
        //            @Override
        //            public boolean shouldRetry(final Resource<?> resource) {
        //
        //                return false;
        //            }
        //        });
        //
        //        final AuthMethod authentication = new AuthPublickey(key_provider);
        //        localhost = new SSHjHost("project07.cs.st-andrews.ac.uk", authentication);

        network = new WorkerNetwork();
        network.add(localhost);
        network.addMavenDependency("uk.ac.standrews.cs", "shabdiz-job", "1.0-SNAPSHOT", "tests");
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
        localhost.close();
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
}
