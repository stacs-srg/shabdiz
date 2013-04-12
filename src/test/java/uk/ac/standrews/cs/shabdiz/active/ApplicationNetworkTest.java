/*
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
package uk.ac.standrews.cs.shabdiz.active;

import java.io.IOException;
import java.net.UnknownHostException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

/**
 * Tests requiring authentication, not intended to be run automatically.
 */
public class ApplicationNetworkTest extends ApplicationNetworkTestBase {

    private static final int TEST_TIMEOUT = 100000;

    @Rule
    public Timeout global_timeout = new Timeout(TEST_TIMEOUT);

    /**
     * Initializes a host descriptor interactively.
     * 
     * @throws IOException if an error occurs
     */
    @BeforeClass
    public static void setupHost() throws IOException {

    }

    @Before
    @Override
    public void setup() throws Exception {

    }

    /**
     * Adds an invalid host and tests that the host state is eventually INVALID.
     * 
     * @throws Exception if the test fails
     */
    @Test(expected = UnknownHostException.class)
    public void addInvalidHost() throws Exception {

        Assert.fail("unimplemented ");
    }

    /**
     * Adds a valid host with invalid credentials and tests that the host state is eventually NO_AUTH.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void addHostWithInvalidCredentials() throws Exception {

        Assert.fail("unimplemented ");
    }

    /**
     * Adds a valid host with valid credentials and tests that the host state is eventually AUTH.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void addHostWithValidCredentials() throws Exception {

        Assert.fail("unimplemented ");
    }

    /**
     * Deploys to a valid host and tests that the host state is eventually RUNNING.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void deploy() throws Exception {

        Assert.fail("unimplemented ");
    }

    /**
     * Adds a valid host, sets auto-deploy and tests that the host state is eventually RUNNING.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void autoDeploy() throws Exception {

        Assert.fail("unimplemented ");
    }

    /**
     * Deploys to a valid host, then kills and tests that the host state is eventually AUTH.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void kill() throws Exception {

        Assert.fail("unimplemented ");
    }

    /**
     * Deploys to a valid host, sets auto-kill and tests that the host state is eventually AUTH.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void autoKill() throws Exception {

        Assert.fail("unimplemented ");
    }

    /**
     * Deploys to a valid host, then kills and tests that the host state is eventually not RUNNING.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void waitForNotRunning() throws Exception {

        Assert.fail("unimplemented ");
    }

    /**
     * Deploys to a valid host, then tests that a status callback is eventually received.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void statusCallback() throws Exception {

        Assert.fail("unimplemented ");
    }

    /**
     * Deploys to a valid host, then tests that an attribute callback is eventually received.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void attributeCallback() throws Exception {

        Assert.fail("unimplemented ");
    }
}
