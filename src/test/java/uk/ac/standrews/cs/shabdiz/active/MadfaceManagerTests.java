/***************************************************************************
 *                                                                         *
 * nds Library                                                             *
 * Copyright (C) 2005-2011 Distributed Systems Architecture Research Group *
 * University of St Andrews, Scotland                                      *
 * http://www-systems.cs.st-andrews.ac.uk/                                 *
 *                                                                         *
 * This file is part of nds, a package of utility classes.                 *
 *                                                                         *
 * nds is free software: you can redistribute it and/or modify             *
 * it under the terms of the GNU General Public License as published by    *
 * the Free Software Foundation, either version 3 of the License, or       *
 * (at your option) any later version.                                     *
 *                                                                         *
 * nds is distributed in the hope that it will be useful,                  *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of          *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the           *
 * GNU General Public License for more details.                            *
 *                                                                         *
 * You should have received a copy of the GNU General Public License       *
 * along with nds.  If not, see <http://www.gnu.org/licenses/>.            *
 *                                                                         *
 ***************************************************************************/
package uk.ac.standrews.cs.shabdiz.active;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.CountDownLatch;

import org.junit.Ignore;
import org.junit.Test;

import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.shabdiz.Configuration;
import uk.ac.standrews.cs.shabdiz.DefaultMadfaceManager;
import uk.ac.standrews.cs.shabdiz.HostDescriptor;
import uk.ac.standrews.cs.shabdiz.HostState;
import uk.ac.standrews.cs.shabdiz.ParameterValue;
import uk.ac.standrews.cs.shabdiz.api.HostScanner;
import uk.ac.standrews.cs.shabdiz.util.URL;

/**
 * Tests MadfaceManager functionality using a dummy application.
 * 
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public class MadfaceManagerTests extends MadfaceManagerTestBase {

    private static final int SCANNER_INTERVAL_TEST_TIMEOUT = 10000;
    private static final int MANAGER_TESTS_TIMEOUT = 10000;
    private static final String NDS_URL_ROOT = "https://builds.cs.st-andrews.ac.uk/job/nds/lastSuccessfulBuild/artifact/";
    private static final String NDS_JAR_URL_STRING = NDS_URL_ROOT + "bin/nds.jar";
    private static final String JSON_JAR_URL_STRING = NDS_URL_ROOT + "lib/json.jar";
    private static final String NDS_URL_CLASSPATH = NDS_JAR_URL_STRING + ";" + JSON_JAR_URL_STRING;
    private static final String INACCESSIBLE_URL_STRING = "http://www.google.com/invalid.jar";
    private static final String MALFORMED_URL_STRING = "FISH URL";

    private static final int TEST_APP_PORT = 53728;

    /**
     * Tests the method to set the application to a given instance.
     * This is not accessible via the IMadfaceManager interface, but is used in the madface project providing
     * a web front-end.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void setApplicationManager() throws Exception {

        outputTestName();

        final TestAppManager appManager = new TestAppManager();
        ((DefaultMadfaceManager) manager).setApplicationManager(appManager);

        assertEquals(manager.getApplicationManager(), appManager);
    }

    /**
     * Tests the method to set the application manager class to a given named class.
     * This is not accessible via the IMadfaceManager interface, but is used in the madface project providing
     * a web front-end.
     * 
     * @throws Exception if the test fails
     */
    @Test
    @Ignore
    public void configureApplicationManagerClass() throws Exception {

        outputTestName();
        //
        //        ((DefaultMadfaceManager) manager).configureApplicationManagerClass(TestAppManager.class.getCanonicalName());
        //
        //        assertThat(manager.getApplicationEntrypoint(), is(notNullValue()));
        //        assertEquals(manager.getApplicationEntrypoint(), TestAppManager.class);
    }

    /**
     * Tests the method to set the application manager library URLs, with valid URLs.
     * This is not accessible via the IMadfaceManager interface, but is used in the madface project providing
     * a web front-end.
     * 
     * @throws Exception if the test fails
     */
    @Test
    @Ignore
    public void configureApplicationUrls() throws Exception {

        outputTestName();

        //        ((DefaultMadfaceManager) manager).configureApplicationUrls(NDS_URL_CLASSPATH);
    }

    /**
     * Tests the method to set the application manager library URLs, with empty URLs.
     * This is not accessible via the IMadfaceManager interface, but is used in the madface project providing
     * a web front-end.
     * 
     * @throws Exception if the test fails
     */
    @Test
    @Ignore
    public void configureEmptyApplicationUrls() throws Exception {

        outputTestName();

        //        ((DefaultMadfaceManager) manager).configureApplicationUrls("");
    }

    /**
     * Tests the method to set the application manager library URLs, with malformed URLs.
     * This is not accessible via the IMadfaceManager interface, but is used in the madface project providing
     * a web front-end.
     * 
     * @throws Exception if the test fails
     */
    @Test(expected = MalformedURLException.class)
    @Ignore
    public void configureMalformedApplicationUrls() throws Exception {

        outputTestName();

        //        ((DefaultMadfaceManager) manager).configureApplicationUrls(MALFORMED_URL_STRING);
    }

    /**
     * Tests the method to set the application manager library URLs, with inaccessible URLs.
     * This is not accessible via the IMadfaceManager interface, but is used in the madface project providing
     * a web front-end.
     * 
     * @throws Exception if the test fails
     */
    @Test(expected = IOException.class)
    @Ignore
    public void configureInaccessibleApplicationUrls() throws Exception {

        outputTestName();

        //        ((DefaultMadfaceManager) manager).configureApplicationUrls(INACCESSIBLE_URL_STRING);
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

        outputTestName();

        final DefaultMadfaceManager concrete_manager = (DefaultMadfaceManager) manager;
        concrete_manager.setApplicationManager(new TestAppManager());
        final Map<String, HostScanner> scanner_map = concrete_manager.getScannerMap();

        assertThat(scanner_map, is(notNullValue()));
    }

    // -------------------------------------------------------------------------------------------------------

    /**
     * Configures the application using a specified (empty) set of URLs.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void configureApplicationWithEmptyURLSet() throws Exception {

        outputTestName();

        configureManager();

        assertThat(manager.getApplicationManager(), is(notNullValue()));
        assertEquals(manager.getApplicationManager().getClass(), TestAppManager.class);
    }

    /**
     * Configures application using an incorrect URL.
     * 
     * @throws Exception IOException expected, or other exception if the test fails
     */
    @Test
    public void configureApplicationWithValidURLs() throws Exception {

        outputTestName();

        final Set<URL> valid_urls = new HashSet<URL>();
        valid_urls.add(new URL(NDS_JAR_URL_STRING));
        configureManager();

        assertThat(manager.getApplicationManager(), is(notNullValue()));
        assertEquals(manager.getApplicationManager().getClass(), TestAppManager.class);
    }

    /**
     * Configures application using an incorrect URL.
     * 
     * @throws Exception IOException expected, or other exception if the test fails
     */
    @Test(expected = IOException.class)
    public void configureApplicationWithMalformedURLs() throws Exception {

        outputTestName();

        final Set<URL> incorrect_urls = new HashSet<URL>();
        incorrect_urls.add(new URL(MALFORMED_URL_STRING));

        configureManager();
    }

    /**
     * Configures application using an incorrect URL.
     * 
     * @throws Exception IOException expected, or other exception if the test fails
     */
    @Test(expected = IOException.class)
    public void configureApplicationWithInaccessibleURLs() throws Exception {

        outputTestName();

        final Set<URL> incorrect_urls = new HashSet<URL>();
        incorrect_urls.add(new URL(INACCESSIBLE_URL_STRING));
        configureManager();
    }

    //    /**
    //     * Configures the application using a specified set of URLs.
    //     *
    //     * @throws Exception if the test fails
    //     */
    //    @Test
    //    public void configureApplicationWithBaseURL() throws Exception {
    //
    //        outputTestName();
    //
    //        final Set<String> jar_names = new HashSet<String>();
    //        final Set<String> lib_names = new HashSet<String>();
    //        jar_names.add("bin/nds.jar");
    //        lib_names.add("lib/json.jar");
    //        manager.configureApplication(TestAppManager.class, new URL(NDS_URL_ROOT), jar_names, lib_names);
    //
    //        assertThat(manager.getApplicationEntrypoint(), is(notNullValue()));
    //        assertEquals(manager.getApplicationEntrypoint(), TestAppManager.class);
    //    }

    //    /**
    //     * Configures the application using a specified set of URLs.
    //     *
    //     * @throws Exception if the test fails
    //     */
    //    @Test
    //    public void configureApplicationWithBaseURLAndEmptySet() throws Exception {
    //
    //        outputTestName();
    //
    //        manager.configureApplication(TestAppManager.class, new URL(NDS_URL_ROOT), new HashSet<String>(), new HashSet<String>());
    //
    //        assertThat(manager.getApplicationEntrypoint(), is(notNullValue()));
    //        assertEquals(manager.getApplicationEntrypoint(), TestAppManager.class);
    //    }

    /**
     * Configures the application using a specified application manager.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void configureApplicationManager() throws Exception {

        outputTestName();

        manager.setApplicationManager(new TestAppManager());

        assertThat(manager.getApplicationManager(), is(notNullValue()));
        assertEquals(manager.getApplicationManager().getClass(), TestAppManager.class);
    }

    /**
     * Gets URLs used by configured application.
     * 
     * @throws Exception if the test fails
     */
    @Test
    @Ignore
    public void getApplicationURLs() throws Exception {

        outputTestName();

        configureManager();

        //        assertThat(manager.getApplicationUrls(), is(equalTo(application_urls)));
    }

    /**
     * Gets URLs used by configured application.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void getApplicationEntrypoint() throws Exception {

        outputTestName();

        configureManager();

        assertEquals(manager.getApplicationManager().getClass(), TestAppManager.class);
    }

    /**
     * Gets the name of the configured application.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void getApplicationName() throws Exception {

        outputTestName();

        configureManager();

        assertThat(manager.getApplicationManager().getApplicationName(), is(equalTo(TestAppManager.APPLICATION_NAME)));
    }

    /**
     * Gets the name of the unconfigured application.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void getApplicationNameForUnconfiguredManager() throws Exception {

        outputTestName();

        assertThat(manager.getApplicationManager().getApplicationName(), is(equalTo("")));
    }

    /**
     * Gets the default host list, expected to be empty.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void emptyHostList() throws Exception {

        outputTestName();

        configureManager();

        assertThat(manager.getHostDescriptors().size(), is(equalTo(0)));
    }

    /**
     * Adds a set of hosts via a pattern, then checks that they are all present in the host descriptor list.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void addHosts() throws Exception {

        outputTestName();

        configureManager();
        addFiveHosts();

        final SortedSet<HostDescriptor> host_descriptors = manager.getHostDescriptors();

        final int expected_number_of_hosts = 5;

        for (int i = 1; i <= expected_number_of_hosts; i++) {
            assertThat(contains(host_descriptors, "beast.cs.st-andrews.ac.uk"), is(true));
        }
    }

    /**
     * Retrieves a host descriptor for a given host name.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void getHostDescriptor() throws Exception {

        outputTestName();

        configureManager();
        addFiveHosts();

        final String host_name = "compute-0-3";
        final HostDescriptor host_descriptor = manager.findHostDescriptorByName(host_name);

        assertThat(host_descriptor.getHost(), is(equalTo(host_name)));
    }

    /**
     * Drops a host, then checks that it has been removed from the host descriptor list.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void dropHost() throws Exception {

        outputTestName();

        configureManager();
        addFiveHosts();

        manager.drop(manager.findHostDescriptorByName("compute-0-3"));

        final SortedSet<HostDescriptor> host_descriptors = manager.getHostDescriptors();

        final int expected_number_of_hosts = 4;
        assertThat(host_descriptors.size(), is(equalTo(expected_number_of_hosts)));

        assertThat(contains(host_descriptors, "compute-0-3"), is(false));
    }

    /**
     * Drops all hosts, then checks that the host descriptor list is empty.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void dropAllHosts() throws Exception {

        outputTestName();

        configureManager();
        addFiveHosts();

        manager.dropAll();

        final SortedSet<HostDescriptor> host_descriptors = manager.getHostDescriptors();

        assertThat(host_descriptors.isEmpty(), is(equalTo(true)));
    }

    /**
     * Adds a host and waits for it to be recognized as authorized.
     * 
     * @throws Exception if the test fails
     */
    @Test(timeout = MANAGER_TESTS_TIMEOUT)
    public void scanUndeployed() throws Exception {

        outputTestName();

        configureManager();
        final HostDescriptor host_descriptor = new HostDescriptor();

        manager.add(host_descriptor);
        manager.waitForHostToReachState(host_descriptor, HostState.AUTH);
    }

    /**
     * Deploys a host, waits for it to be recognized as running, then kills it and waits for that to be recognized.
     * 
     * @throws Exception if the test fails
     */
    @Test(timeout = MANAGER_TESTS_TIMEOUT)
    public void deployAndKill() throws Exception {

        outputTestName();

        configureManager();
        final HostDescriptor host_descriptor = makeHostDescriptorAndAddToManager();

        explicitDeployAndWait(host_descriptor);
        explicitKillAndWait(host_descriptor);
    }

    /**
     * Same as {@link #deployAndKill()} but uses deployAll() and killAll() variants.
     * 
     * @throws Exception if the test fails
     */
    @Test(timeout = MANAGER_TESTS_TIMEOUT)
    public void deployAndKillAll() throws Exception {

        outputTestName();

        configureManager();
        final HostDescriptor host_descriptor = makeHostDescriptorAndAddToManager();

        explicitDeployAllAndWait(host_descriptor);
        explicitKillAllAndWait(host_descriptor);
    }

    /**
     * Same as {@link #deployAndKill()} but uses autoDeploy and autoKill variants.
     * 
     * @throws Exception if the test fails
     */
    @Test(timeout = MANAGER_TESTS_TIMEOUT)
    public void autoDeployAndKill() throws Exception {

        outputTestName();

        configureManager();
        final HostDescriptor host_descriptor = makeHostDescriptorAndAddToManager();

        autoDeployAndWait(host_descriptor);
        autoKillAndWait(host_descriptor);
    }

    /**
     * Tests the ability to wait for a host to reach a state different from one specified.
     * 
     * @throws Exception if the test fails
     */
    @Test(timeout = MANAGER_TESTS_TIMEOUT)
    public void waitForAntiStates() throws Exception {

        outputTestName();

        configureManager();
        final HostDescriptor host_descriptor = makeHostDescriptorAndAddToManager();
        autoDeployAndWait(host_descriptor);

        manager.setAutoDeploy(false);
        manager.setAutoKill(true);
        manager.waitForHostToReachStateThatIsNot(host_descriptor, HostState.RUNNING);
    }

    /**
     * Deploys and kills a host, then waits for appropriate status change callbacks.
     * 
     * @throws Exception if the test fails
     */
    @Test(timeout = MANAGER_TESTS_TIMEOUT)
    public void hostStatusCallbacks() throws Exception {

        outputTestName();

        configureManager();
        makeHostDescriptorAndAddToManager();

        final CountDownLatch state_running_latch = new CountDownLatch(1);

        for (final HostDescriptor h : manager.getHostDescriptors()) {
            h.addPropertyChangeListener(HostDescriptor.HOST_STATE_PROPERTY_NAME, new PropertyChangeListener() {

                @Override
                public void propertyChange(final PropertyChangeEvent evt) {

                    if (evt.getNewValue().equals(HostState.RUNNING)) {
                        state_running_latch.countDown();
                    }

                }
            });
        }

        manager.setAutoDeploy(true);
        state_running_latch.await();

        manager.setAutoDeploy(false);
        final CountDownLatch state_auth_latch = new CountDownLatch(1);

        for (final HostDescriptor h : manager.getHostDescriptors()) {
            h.addPropertyChangeListener(HostDescriptor.HOST_STATE_PROPERTY_NAME, new PropertyChangeListener() {

                @Override
                public void propertyChange(final PropertyChangeEvent evt) {

                    if (evt.getNewValue().equals(HostState.AUTH)) {
                        state_auth_latch.countDown();
                    }

                }
            });
        }
        manager.setAutoKill(true);
        state_auth_latch.await();
    }

    /**
     * Deploys a host, then waits for appropriate attribute value callback.
     * 
     * @throws Exception if the test fails
     */
    @Test(timeout = MANAGER_TESTS_TIMEOUT)
    @Ignore
    public void attributeCallbacks() throws Exception {

        outputTestName();

        configureManager();
        final HostDescriptor host_descriptor = makeHostDescriptorAndAddToManager();

        final CountDownLatch latch = new CountDownLatch(1);
        //        manager.addAttributesCallback(new AttributesCallback() {
        //
        //            @Override
        //            public void attributesChange(final HostDescriptor host_descriptor) {
        //
        //                final String attribute_value = host_descriptor.getAttributes().get("test attribute");
        //                if (attribute_value != null && attribute_value.equals("test value")) {
        //                    latch.countDown();
        //                }
        //            }
        //        });
        //
        //        autoDeployAndWait(host_descriptor);
        //        latch.await();
        //
        //        autoKillAndWait(host_descriptor);
    }

    /**
     * Tests satisfactory handling of attempt to set an invalid preference.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void setInvalidPreference() throws Exception {

        outputTestName();

        configureManager();

        assertThat(manager.setPrefEnabled("FISH", true).startsWith(DefaultMadfaceManager.NO_PREFERENCE_WITH_NAME), is(true));
    }

    /**
     * Tests status checking and deployment when specific scanner frequencies are set.
     * 
     * @throws Exception if the test fails
     */
    @Test(timeout = SCANNER_INTERVAL_TEST_TIMEOUT)
    public void setScannerIntervals() throws Exception {

        outputTestName();

        configuration = new Configuration().addParameter(new ParameterValue(Configuration.SCANNER_MIN_CYCLE_TIME_KEY, 10));

        configureManager();

        final HostDescriptor host_descriptor = makeHostDescriptorAndAddToManager();

        autoDeployAndWait(host_descriptor);
        autoKillAndWait(host_descriptor);
    }

    @Test
    public void disableHostScanning() throws Exception {

        outputTestName();

        configureManager();
        manager.setHostScanning(false);

        final HostDescriptor host_descriptor = makeHostDescriptorAndAddToManager();
        SECONDS.sleep(10);

        assertThat(host_descriptor.getHostState(), is(not(equalTo(HostState.AUTH))));
    }

    @Test
    public void shutdownManager() throws Exception {

        outputTestName();

        configureManager();

        final HostDescriptor host_descriptor = new HostDescriptor();
        host_descriptor.port(TEST_APP_PORT);

        manager.shutdown();

        manager.add(host_descriptor);
        SECONDS.sleep(10);

        assertThat(host_descriptor.getHostState(), is(not(equalTo(HostState.AUTH))));
    }

    @Test(expected = UnknownHostException.class)
    public void unknownHost() throws Exception {

        outputTestName();

        configureManager();
        manager.findHostDescriptorByName("FISH");
    }

    @Test(timeout = MANAGER_TESTS_TIMEOUT)
    public void dummyApplicationScanners() throws Exception {

        outputTestName();

        manager.setApplicationManager(new TestAppManager(true));
        manager.setHostScanning(true);

        final HostDescriptor host_descriptor = makeHostDescriptorAndAddToManager();

        autoDeployAndWait(host_descriptor);
        autoKillAndWait(host_descriptor);
    }

    @Test(timeout = MANAGER_TESTS_TIMEOUT)
    public void interruptWait() throws Exception {

        outputTestName();

        configureManager();
        final HostDescriptor host_descriptor = makeHostDescriptorAndAddToManager();

        final CountDownLatch latch = new CountDownLatch(1);

        final Thread t = new Thread(new Runnable() {

            @Override
            public void run() {

                try {
                    manager.waitForHostToReachState(host_descriptor, HostState.RUNNING);
                }
                catch (final InterruptedException e) {

                }
                finally {
                    latch.countDown();
                }
            }
        });

        t.start();
        t.interrupt();
        latch.await();
    }

    @Test(timeout = MANAGER_TESTS_TIMEOUT)
    public void interruptWaitForAll() throws Exception {

        outputTestName();

        configureManager();
        makeHostDescriptorAndAddToManager();

        final CountDownLatch latch = new CountDownLatch(1);

        final Thread t = new Thread(new Runnable() {

            @Override
            public void run() {

                try {
                    manager.waitForAllToReachState(HostState.RUNNING);
                }
                catch (final InterruptedException e) {

                }
                finally {
                    latch.countDown();
                }
            }
        });

        t.start();
        SECONDS.sleep(5);
        t.interrupt();
        latch.await();
    }

    // -------------------------------------------------------------------------------------------------------

    private void explicitDeployAndWait(final HostDescriptor host_descriptor) throws Exception {

        manager.waitForHostToReachState(host_descriptor, HostState.AUTH);
        manager.deploy(host_descriptor);
        manager.waitForHostToReachState(host_descriptor, HostState.RUNNING);
    }

    private void explicitDeployAllAndWait(final HostDescriptor host_descriptor) throws Exception {

        manager.waitForHostToReachState(host_descriptor, HostState.AUTH);

        manager.deployAll();
        manager.waitForHostToReachState(host_descriptor, HostState.RUNNING);
    }

    private void explicitKillAndWait(final HostDescriptor host_descriptor) throws Exception {

        manager.kill(host_descriptor, true);
        manager.waitForHostToReachState(host_descriptor, HostState.AUTH);
    }

    private void explicitKillAllAndWait(final HostDescriptor host_descriptor) throws Exception {

        manager.killAll(true);
        manager.waitForHostToReachState(host_descriptor, HostState.AUTH);
    }

    private void autoDeployAndWait(final HostDescriptor host_descriptor) throws InterruptedException {

        manager.setAutoDeploy(true);
        manager.waitForHostToReachState(host_descriptor, HostState.RUNNING);
    }

    private void autoKillAndWait(final HostDescriptor host_descriptor) throws InterruptedException {

        manager.setAutoDeploy(false);
        manager.setAutoKill(true);
        manager.waitForHostToReachState(host_descriptor, HostState.AUTH);
    }

    private HostDescriptor makeHostDescriptorAndAddToManager() throws IOException {

        final HostDescriptor host_descriptor = new HostDescriptor();

        host_descriptor.port(TEST_APP_PORT);

        manager.add(host_descriptor);
        return host_descriptor;
    }

    private void addFiveHosts() throws IOException {

        //        manager.add("beast.cs.st-andrews.ac.uk", PublicKeyCredentials.getDefaultRSACredentials(null));
        //        manager.add("beast.cs.st-andrews.ac.uk", PublicKeyCredentials.getDefaultRSACredentials(null));
        //        manager.add("beast.cs.st-andrews.ac.uk", PublicKeyCredentials.getDefaultRSACredentials(null));
        //        manager.add("beast.cs.st-andrews.ac.uk", PublicKeyCredentials.getDefaultRSACredentials(null));
        //        manager.add("beast.cs.st-andrews.ac.uk", PublicKeyCredentials.getDefaultRSACredentials(null));
    }

    private boolean contains(final SortedSet<HostDescriptor> host_descriptors, final String host) {

        for (final HostDescriptor host_descriptor : host_descriptors) {
            if (host_descriptor.getHost().equals(host)) { return true; }
        }
        return false;
    }

    private void outputTestName() {

        System.out.println(Diagnostic.getMethodInCallChain(2));
    }
}
