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
import java.net.MalformedURLException;
import java.net.UnknownHostException;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests MadfaceManager functionality using a dummy application.
 */
public class ApplicationManagerTests extends ApplicationNetworkTestBase {

    private static final int SCANNER_INTERVAL_TEST_TIMEOUT = 10000;
    private static final int MANAGER_TESTS_TIMEOUT = 10000;

    /**
     * Tests the method to set the application to a given instance.
     * This is not accessible via the IMadfaceManager interface, but is used in the madface project providing
     * a web front-end.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void setApplicationManager() throws Exception {

        Assert.fail("unimplemented ");
    }

    /**
     * Tests the method to set the application manager class to a given named class.
     * This is not accessible via the IMadfaceManager interface, but is used in the madface project providing
     * a web front-end.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void configureApplicationManagerClass() throws Exception {

        Assert.fail("unimplemented ");
    }

    /**
     * Tests the method to set the application manager library URLs, with valid URLs.
     * This is not accessible via the IMadfaceManager interface, but is used in the madface project providing
     * a web front-end.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void configureApplicationUrls() throws Exception {

        Assert.fail("unimplemented ");
    }

    /**
     * Tests the method to set the application manager library URLs, with empty URLs.
     * This is not accessible via the IMadfaceManager interface, but is used in the madface project providing
     * a web front-end.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void configureEmptyApplicationUrls() throws Exception {

        Assert.fail("unimplemented ");
    }

    /**
     * Tests the method to set the application manager library URLs, with malformed URLs.
     * This is not accessible via the IMadfaceManager interface, but is used in the madface project providing
     * a web front-end.
     * 
     * @throws Exception if the test fails
     */
    @Test(expected = MalformedURLException.class)
    public void configureMalformedApplicationUrls() throws Exception {

        Assert.fail("unimplemented ");
    }

    /**
     * Tests the method to set the application manager library URLs, with inaccessible URLs.
     * This is not accessible via the IMadfaceManager interface, but is used in the madface project providing
     * a web front-end.
     * 
     * @throws Exception if the test fails
     */
    @Test(expected = IOException.class)
    public void configureInaccessibleApplicationUrls() throws Exception {

        Assert.fail("unimplemented ");
    }

    /**
     * Tests the method to get the application manager's scanner map.
     * This is not accessible via the IMadfaceManager interface, but is used in the madface project providing
     * a web front-end.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void getScannerMap() throws Exception {

        Assert.fail("unimplemented ");
    }

    /**
     * Configures the application using a specified (empty) set of URLs.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void configureApplicationWithEmptyURLSet() throws Exception {

        Assert.fail("unimplemented ");
    }

    /**
     * Configures application using an incorrect URL.
     * 
     * @throws Exception IOException expected, or other exception if the test fails
     */
    @Test
    public void configureApplicationWithValidURLs() throws Exception {

        Assert.fail("unimplemented ");
    }

    /**
     * Configures application using an incorrect URL.
     * 
     * @throws Exception IOException expected, or other exception if the test fails
     */
    @Test(expected = IOException.class)
    public void configureApplicationWithMalformedURLs() throws Exception {

        Assert.fail("unimplemented ");
    }

    /**
     * Configures application using an incorrect URL.
     * 
     * @throws Exception IOException expected, or other exception if the test fails
     */
    @Test(expected = IOException.class)
    public void configureApplicationWithInaccessibleURLs() throws Exception {

        Assert.fail("unimplemented ");
    }

    /**
     * Configures the application using a specified set of URLs.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void configureApplicationWithBaseURL() throws Exception {

        Assert.fail("unimplemented ");
    }

    /**
     * Configures the application using a specified set of URLs.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void configureApplicationWithBaseURLAndEmptySet() throws Exception {

        Assert.fail("unimplemented ");
    }

    /**
     * Configures the application using a specified application manager.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void configureApplicationManager() throws Exception {

        Assert.fail("unimplemented ");
    }

    /**
     * Gets URLs used by configured application.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void getApplicationURLs() throws Exception {

        Assert.fail("unimplemented ");
    }

    /**
     * Gets URLs used by configured application.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void getApplicationEntrypoint() throws Exception {

        Assert.fail("unimplemented ");
    }

    /**
     * Gets the name of the configured application.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void getApplicationName() throws Exception {

        Assert.fail("unimplemented ");
    }

    /**
     * Gets the name of the unconfigured application.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void getApplicationNameForUnconfiguredManager() throws Exception {

        Assert.fail("unimplemented ");
    }

    /**
     * Gets the default host list, expected to be empty.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void emptyHostList() throws Exception {

        Assert.fail("unimplemented ");
    }

    /**
     * Adds a set of hosts via a pattern, then checks that they are all present in the host descriptor list.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void addHosts() throws Exception {

        Assert.fail("unimplemented ");
    }

    /**
     * Retrieves a host descriptor for a given host name.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void getHostDescriptor() throws Exception {

        Assert.fail("unimplemented ");
    }

    /**
     * Drops a host, then checks that it has been removed from the host descriptor list.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void dropHost() throws Exception {

        Assert.fail("unimplemented ");
    }

    /**
     * Drops all hosts, then checks that the host descriptor list is empty.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void dropAllHosts() throws Exception {

        Assert.fail("unimplemented ");
    }

    /**
     * Adds a host and waits for it to be recognized as authorized.
     * 
     * @throws Exception if the test fails
     */
    @Test(timeout = MANAGER_TESTS_TIMEOUT)
    public void scanUndeployed() throws Exception {

        Assert.fail("unimplemented ");
    }

    /**
     * Deploys a host, waits for it to be recognized as running, then kills it and waits for that to be recognized.
     * 
     * @throws Exception if the test fails
     */
    @Test(timeout = MANAGER_TESTS_TIMEOUT)
    public void deployAndKill() throws Exception {

        Assert.fail("unimplemented ");
    }

    /**
     * Same as {@link #deployAndKill()} but uses deployAll() and killAll() variants.
     * 
     * @throws Exception if the test fails
     */
    @Test(timeout = MANAGER_TESTS_TIMEOUT)
    public void deployAndKillAll() throws Exception {

        Assert.fail("unimplemented ");
    }

    /**
     * Same as {@link #deployAndKill()} but uses autoDeploy and autoKill variants.
     * 
     * @throws Exception if the test fails
     */
    @Test(timeout = MANAGER_TESTS_TIMEOUT)
    public void autoDeployAndKill() throws Exception {

        Assert.fail("unimplemented ");
    }

    /**
     * Tests the ability to wait for a host to reach a state different from one specified.
     * 
     * @throws Exception if the test fails
     */
    @Test(timeout = MANAGER_TESTS_TIMEOUT)
    public void waitForAntiStates() throws Exception {

        Assert.fail("unimplemented ");
    }

    /**
     * Deploys and kills a host, then waits for appropriate status change callbacks.
     * 
     * @throws Exception if the test fails
     */
    @Test(timeout = MANAGER_TESTS_TIMEOUT)
    public void hostStatusCallbacks() throws Exception {

        Assert.fail("unimplemented ");
    }

    /**
     * Deploys a host, then waits for appropriate attribute value callback.
     * 
     * @throws Exception if the test fails
     */
    @Test(timeout = MANAGER_TESTS_TIMEOUT)
    public void attributeCallbacks() throws Exception {

        Assert.fail("unimplemented ");
    }

    /**
     * Tests satisfactory handling of attempt to set an invalid preference.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void setInvalidPreference() throws Exception {

        Assert.fail("unimplemented ");
    }

    /**
     * Tests status checking and deployment when specific scanner frequencies are set.
     * 
     * @throws Exception if the test fails
     */
    @Test(timeout = SCANNER_INTERVAL_TEST_TIMEOUT)
    public void setScannerIntervals() throws Exception {

        Assert.fail("unimplemented ");
    }

    @Test
    public void disableHostScanning() throws Exception {

        Assert.fail("unimplemented ");
    }

    @Test
    public void shutdownManager() throws Exception {

        Assert.fail("unimplemented ");
    }

    @Test(expected = UnknownHostException.class)
    public void unknownHost() throws Exception {

        Assert.fail("unimplemented ");
    }

    @Test(timeout = MANAGER_TESTS_TIMEOUT)
    public void dummyApplicationScanners() throws Exception {

        Assert.fail("unimplemented ");
    }

    @Test(timeout = MANAGER_TESTS_TIMEOUT)
    public void interruptWait() throws Exception {

        Assert.fail("unimplemented ");
    }

    @Test(timeout = MANAGER_TESTS_TIMEOUT)
    public void interruptWaitForAll() throws Exception {

        Assert.fail("unimplemented ");
    }
}
