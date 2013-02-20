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

import java.io.IOException;
import java.net.URLClassLoader;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;

import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.nds.util.ErrorHandling;
import uk.ac.standrews.cs.shabdiz.active.interfaces.ApplicationManager;
import uk.ac.standrews.cs.shabdiz.active.interfaces.IAttributesCallback;
import uk.ac.standrews.cs.shabdiz.active.interfaces.IGlobalHostScanner;
import uk.ac.standrews.cs.shabdiz.active.interfaces.IHostScanner;
import uk.ac.standrews.cs.shabdiz.active.interfaces.IHostStatusCallback;
import uk.ac.standrews.cs.shabdiz.active.interfaces.IMadfaceManager;
import uk.ac.standrews.cs.shabdiz.active.interfaces.ISingleHostScanner;
import uk.ac.standrews.cs.shabdiz.active.scanners.DeployScanner;
import uk.ac.standrews.cs.shabdiz.active.scanners.DropScanner;
import uk.ac.standrews.cs.shabdiz.active.scanners.GlobalHostScannerThread;
import uk.ac.standrews.cs.shabdiz.active.scanners.HostScannerThread;
import uk.ac.standrews.cs.shabdiz.active.scanners.KillScanner;
import uk.ac.standrews.cs.shabdiz.active.scanners.SingleHostScannerThread;
import uk.ac.standrews.cs.shabdiz.active.scanners.StatusScanner;

/**
 * Madface server implementation. The method {@link #shutdown()} should be called before disposing of an instance, to avoid thread leakage.
 * 
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public final class MadfaceManager implements IMadfaceManager {

    public static final String STATE_WAIT_DELAY_KEY = "state_wait_delay";

    public static final String SCANNER_MIN_CYCLE_TIME_KEY = "scanner_min_cycle_time";

    public static final String SSH_CHECK_THREAD_TIMEOUT_KEY = "ssh_check_thread_timeout";

    public static final String DEPLOY_CHECK_THREAD_COUNT_KEY = "deploy_check_thread_count";

    public static final String SSH_CHECK_THREAD_COUNT_KEY = "ssh_check_thread_count";

    public static final String STATUS_CHECK_THREAD_COUNT_KEY = "status_check_thread_count";

    /** Success status message. */
    public static final String RESULT_OK = "ok";

    /** Separator character for URL paths. */
    public static final String URL_PATH_SEPARATOR = ";";

    /** Error message for invalid preference. */
    public static final String NO_PREFERENCE_WITH_NAME = "no preference with name: ";

    // -------------------------------------------------------------------------------------------------------

    /** The default scanner thread pool size. */
    public static final int DEFAULT_THREAD_POOL_SIZE = 10;

    /** The default minimum scanner cycle time. */
    public static final Duration DEFAULT_SCANNER_MIN_CYCLE_TIME = new Duration(2, TimeUnit.SECONDS);

    /** The default delay between host state checks. */
    public static final Duration DEFAULT_STATE_WAIT_DELAY = new Duration(1, TimeUnit.SECONDS);

    public static final Configuration DEFAULT_CONFIGURATION;
    static {
        DEFAULT_CONFIGURATION = new Configuration();

        DEFAULT_CONFIGURATION.addParameter(new ParameterValue(STATUS_CHECK_THREAD_COUNT_KEY, StatusScanner.DEFAULT_SCANNER_THREAD_POOL_SIZE));
        DEFAULT_CONFIGURATION.addParameter(new ParameterValue(DEPLOY_CHECK_THREAD_COUNT_KEY, DeployScanner.DEFAULT_SCANNER_THREAD_POOL_SIZE));
        DEFAULT_CONFIGURATION.addParameter(new ParameterValue(SSH_CHECK_THREAD_COUNT_KEY, StatusScanner.DEFAULT_SSH_CHECK_THREAD_POOL_SIZE));
        DEFAULT_CONFIGURATION.addParameter(new ParameterValue(SSH_CHECK_THREAD_TIMEOUT_KEY, (int) StatusScanner.DEFAULT_SSH_CHECK_TIMEOUT.getLength(TimeUnit.SECONDS)));
        DEFAULT_CONFIGURATION.addParameter(new ParameterValue(SCANNER_MIN_CYCLE_TIME_KEY, (int) DEFAULT_SCANNER_MIN_CYCLE_TIME.getLength(TimeUnit.SECONDS)));
        DEFAULT_CONFIGURATION.addParameter(new ParameterValue(STATE_WAIT_DELAY_KEY, (int) DEFAULT_STATE_WAIT_DELAY.getLength(TimeUnit.SECONDS)));
    }

    // -------------------------------------------------------------------------------------------------------

    private static final String DISCARD_ERRORS = "Discard Errors";
    private static final String NEWLINE_REGEX = "[\\r\\n]+";
    private static final long serialVersionUID = -8525716807644819016L;

    private Class<? extends ApplicationManager> application_manager_class;
    private Set<URL> application_urls;

    private ApplicationManager application_manager;

    private StatusScanner status_scanner;
    private DeployScanner deploy_scanner;
    private DropScanner drop_scanner;
    private KillScanner kill_scanner;

    private int status_scanner_thread_pool_size;
    private int deploy_scanner_thread_pool_size;
    private static final int drop_scanner_thread_pool_size = DEFAULT_THREAD_POOL_SIZE;
    private static final int kill_scanner_thread_pool_size = DEFAULT_THREAD_POOL_SIZE;

    private Duration status_scanner_min_cycle_time;
    private Duration deploy_scanner_min_cycle_time;
    private Duration drop_scanner_min_cycle_time;
    private Duration kill_scanner_min_cycle_time;

    private Duration state_wait_delay;
    private Duration ssh_check_timeout;
    private static final Duration kill_check_timeout = KillScanner.DEFAULT_KILL_CHECK_TIMEOUT;
    private static final Duration status_check_timeout = StatusScanner.DEFAULT_STATUS_CHECK_TIMEOUT;

    private int ssh_check_thread_pool_size;

    private Set<IHostStatusCallback> host_status_callbacks;
    private Set<IAttributesCallback> attributes_callbacks;

    private final SortedSet<HostDescriptor> host_state_list;
    private final List<HostScannerThread> scanner_list;
    private final Map<String, IHostScanner> scanner_map;
    private final Map<String, IHostScanner> headless_scanner_map;

    private boolean discard_errors;

    // -------------------------------------------------------------------------------------------------------

    /**
     * Initializes the service implementation.
     */
    public MadfaceManager() {

        this(DEFAULT_CONFIGURATION);
    }

    /**
     * Initializes the service implementation.
     */
    public MadfaceManager(final Configuration configuration) {

        setConfigurationParameters(configuration);

        host_state_list = new ConcurrentSkipListSet<HostDescriptor>();
        scanner_list = Collections.synchronizedList(new ArrayList<HostScannerThread>());
        scanner_map = Collections.synchronizedMap(new HashMap<String, IHostScanner>());
        headless_scanner_map = Collections.synchronizedMap(new HashMap<String, IHostScanner>());

        application_urls = Collections.synchronizedSet(new HashSet<URL>());

        configureCallbacks();
        configureGenericScanners();
        startScanners();
    }

    private void setConfigurationParameters(final Configuration configuration) {

        if (configuration != null) {
            for (final ParameterValue parameter_value : configuration.getValues()) {
                setParameterValue(parameter_value);
            }
        }
    }

    // -------------------------------------------------------------------------------------------------------

    /**
     * Returns the manager's scanner map.
     * 
     * @return the scanner map
     */
    public Map<String, IHostScanner> getScannerMap() {

        return scanner_map;
    }

    /**
     * Returns the application manager.
     * 
     * @return the application manager
     */
    public ApplicationManager getApplicationManager() {

        return application_manager;
    }

    /**
     * Returns the host status callbacks.
     * 
     * @return the host status callbacks.
     */
    public Set<IHostStatusCallback> getHostStatusCallbacks() {

        return host_status_callbacks;
    }

    /**
     * Returns the host attribute callbacks.
     * 
     * @return the host attribute callbacks.
     */
    public Set<IAttributesCallback> getAttributesCallbacks() {

        return attributes_callbacks;
    }

    /**
     * Sets the application manager.
     * 
     * @param application_manager the application manager
     */
    public void setApplicationManager(final ApplicationManager application_manager) {

        this.application_manager = application_manager;
        if (application_manager != null) {
            application_manager_class = application_manager.getClass();
        }
    }

    /**
     * Configures the application manager class.
     * 
     * @param application_manager_class_name the application manager class name
     * @throws ClassNotFoundException if the class cannot be resolved
     */
    public void configureApplicationManagerClass(final String application_manager_class_name) throws ClassNotFoundException {

        application_manager_class = getApplicationManagerClass(application_manager_class_name);
    }

    /**
     * Configures the URLS for the application.
     * 
     * @param application_url_classpath the URL classpath
     * @throws IOException if any of the URLs are invalid
     */
    public void configureApplicationUrls(final String application_url_classpath) throws IOException {

        configureApplication(pathToUrls(application_url_classpath));
    }

    /**
     * Configures the application and any application-specific scanners.
     * 
     * @throws InstantiationException if the application manager cannot be instantiated
     * @throws IllegalAccessException if the application manager cannot be instantiated
     */
    public void configureApplication() throws InstantiationException, IllegalAccessException {

        configureApplicationManager();
        configureApplicationSpecificScanners();
    }

    // -------------------------------------------------------------------------------------------------------

    @Override
    public void configureApplication(final Class<? extends ApplicationManager> application_manager_class, final Set<URL> application_urls) throws InstantiationException, IllegalAccessException {

        this.application_manager_class = application_manager_class;
        configureApplication(application_urls);

        configureApplication();
    }

    @Override
    public void configureApplication(final Class<? extends ApplicationManager> application_manager_class, final URL url_base, final Set<String> jar_names, final Set<String> lib_names) throws IOException, InstantiationException, IllegalAccessException {

        final Set<URL> application_url_classpath = makeURLs(url_base, jar_names, lib_names);

        configureApplication(application_manager_class, application_url_classpath);
    }

    @Override
    public void configureApplication(final ApplicationManager application_manager) {

        setApplicationManager(application_manager);
        configureApplicationSpecificScanners();
    }

    @Override
    public void configureApplication(final Set<URL> application_urls) {

        this.application_urls = application_urls;
    }

    @Override
    public Set<URL> getApplicationUrls() {

        return application_urls;
    }

    @Override
    public Class<? extends ApplicationManager> getApplicationEntrypoint() {

        return application_manager_class;
    }

    @Override
    public String getApplicationName() {

        return application_manager != null ? application_manager.getApplicationName() : "";
    }

    @Override
    public void add(final HostDescriptor host_descriptor) {

        host_state_list.add(host_descriptor);
        host_descriptor.applicationURLs(application_urls);
    }

    @Override
    public void add(final String multi_line_host_patterns, final Credentials credentials) throws IOException {

        final String[] host_patterns = splitAtNewLines(multi_line_host_patterns);

        add(host_patterns, credentials);
    }

    @Override
    public void drop(final HostDescriptor host_descriptor) {

        Diagnostic.traceNoSource(DiagnosticLevel.FINAL, "dropping: " + host_descriptor.getHost());

        host_state_list.remove(host_descriptor);
        host_descriptor.shutdown();
    }

    @Override
    public void addHostStatusCallback(final IHostStatusCallback host_status_callback) {

        host_status_callbacks.add(host_status_callback);
    }

    @Override
    public void addAttributesCallback(final IAttributesCallback attributes_callback) {

        attributes_callbacks.add(attributes_callback);
    }

    @Override
    public void deploy(final HostDescriptor host_descriptor) throws Exception {

        Diagnostic.traceNoSource(DiagnosticLevel.FINAL, "deploying to: " + host_descriptor.getHost());
        application_manager.deployApplication(host_descriptor);
    }

    @Override
    public void kill(final HostDescriptor host_descriptor, final boolean kill_all_instances) throws Exception {

        Diagnostic.traceNoSource(DiagnosticLevel.FINAL, "killing application on: " + host_descriptor.getHost());
        application_manager.killApplication(host_descriptor, kill_all_instances);
    }

    @Override
    public void killAll(final boolean kill_all_instances) throws Exception {

        for (final HostDescriptor host_descriptor : host_state_list) {
            kill(host_descriptor, kill_all_instances);
        }
    }

    @Override
    public void deployAll() throws Exception {

        for (final HostDescriptor host_descriptor : host_state_list) {
            deploy(host_descriptor);
        }
    }

    @Override
    public void dropAll() {

        host_state_list.clear();
    }

    @Override
    public String setPrefEnabled(final String pref_name, final boolean enabled) {

        if (pref_name.equals(DISCARD_ERRORS)) {

            discard_errors = enabled;
            return RESULT_OK;
        }

        return setScannerEnabled(pref_name, enabled);
    }

    @Override
    public void setAutoDeploy(final boolean auto_deploy) {

        setPrefEnabled(DeployScanner.AUTO_DEPLOY_KEY, auto_deploy);
    }

    @Override
    public void setAutoDrop(final boolean auto_drop) {

        setPrefEnabled(DropScanner.AUTO_DROP_KEY, auto_drop);
    }

    @Override
    public void setAutoKill(final boolean auto_kill) {

        setPrefEnabled(KillScanner.AUTO_KILL_KEY, auto_kill);
    }

    @Override
    public void setHostScanning(final boolean enabled) {

        setPrefEnabled(StatusScanner.STATUS_SCANNER_KEY, enabled);
    }

    @Override
    public void waitForHostToReachState(final HostDescriptor host_descriptor, final HostState state_to_reach) throws InterruptedException {

        waitForHost(host_descriptor, state_to_reach, true);
    }

    @Override
    public void waitForHostToReachStateThatIsNot(final HostDescriptor host_descriptor, final HostState state_to_not_reach) throws InterruptedException {

        waitForHost(host_descriptor, state_to_not_reach, false);
    }

    @Override
    public void waitForAllToReachState(final HostState state_to_reach) throws InterruptedException {

        waitForAll(state_to_reach, true);
    }

    @Override
    public void waitForAllToReachStateThatIsNot(final HostState state_to_not_reach) throws InterruptedException {

        waitForAll(state_to_not_reach, false);
    }

    @Override
    public void shutdown() {

        shutdownScannerThreads();
        shutdownScanners();
        shutdownHostDescriptors();
        shutdownApplicationManager();
    }

    private void shutdownApplicationManager() {

        if (application_manager != null) {
            application_manager.shutdown();
        }
    }

    private void shutdownScannerThreads() {

        for (final HostScannerThread scanner_thread : scanner_list) {
            scanner_thread.shutdown();
        }
    }

    @Override
    public SortedSet<HostDescriptor> getHostDescriptors() {

        return host_state_list;
    }

    @Override
    public HostDescriptor getHostDescriptor(final String host) throws UnknownHostException {

        for (final HostDescriptor host_descriptor : host_state_list) {
            if (host_descriptor.getHost().equals(host)) { return host_descriptor; }
        }

        throw new UnknownHostException();
    }

    private void shutdownHostDescriptors() {

        for (final HostDescriptor host_descriptor : host_state_list) {
            host_descriptor.shutdown();
        }
    }

    // -------------------------------------------------------------------------------------------------------

    /**
     * Returns true if error output should be discarded.
     * 
     * @return true if error output should be discarded
     */
    public boolean errorsAreDiscarded() {

        return discard_errors;
    }

    // -------------------------------------------------------------------------------------------------------

    private String setScannerEnabled(final String pref_name, final boolean enabled) {

        final IHostScanner scanner = getScannerMap().get(pref_name);
        if (scanner == null) { return NO_PREFERENCE_WITH_NAME + pref_name; }

        scanner.setEnabled(enabled);
        return RESULT_OK;
    }

    private void configureApplicationManager() throws InstantiationException, IllegalAccessException {

        application_manager = getApplicationManager(application_manager_class);
    }

    private void configureGenericScanners() {

        status_scanner = new StatusScanner(this, status_scanner_min_cycle_time, status_scanner_thread_pool_size, ssh_check_thread_pool_size, status_check_timeout, ssh_check_timeout);
        deploy_scanner = new DeployScanner(this, deploy_scanner_thread_pool_size, deploy_scanner_min_cycle_time);
        drop_scanner = new DropScanner(this, drop_scanner_thread_pool_size, drop_scanner_min_cycle_time);
        kill_scanner = new KillScanner(this, kill_scanner_thread_pool_size, kill_scanner_min_cycle_time, kill_check_timeout);

        deploy_scanner.syncWith(status_scanner);
        drop_scanner.syncWith(status_scanner);
        kill_scanner.syncWith(status_scanner);

        scanner_list.add(new SingleHostScannerThread(this, status_scanner));
        scanner_list.add(new SingleHostScannerThread(this, deploy_scanner));
        scanner_list.add(new SingleHostScannerThread(this, drop_scanner));
        scanner_list.add(new SingleHostScannerThread(this, kill_scanner));

        scanner_map.put(status_scanner.getToggleLabel(), status_scanner);
        scanner_map.put(deploy_scanner.getToggleLabel(), deploy_scanner);
        scanner_map.put(drop_scanner.getToggleLabel(), drop_scanner);
        scanner_map.put(kill_scanner.getToggleLabel(), kill_scanner);
    }

    private IHostScanner getStatusScanner() {

        return scanner_map.get(status_scanner.getToggleLabel());
    }

    private IHostScanner getDeployScanner() {

        return scanner_map.get(deploy_scanner.getToggleLabel());
    }

    private IHostScanner getDropScanner() {

        return scanner_map.get(drop_scanner.getToggleLabel());
    }

    private IHostScanner getKillScanner() {

        return scanner_map.get(kill_scanner.getToggleLabel());
    }

    private void configureApplicationSpecificScanners() {

        configureSingleScanners();
        configureGlobalScanners();
    }

    private void configureSingleScanners() {

        final List<ISingleHostScanner> single_host_scanners = application_manager.getSingleScanners();

        if (single_host_scanners != null) {

            for (final ISingleHostScanner scanner : single_host_scanners) {

                scanner.syncWith(status_scanner);
                final SingleHostScannerThread thread = new SingleHostScannerThread(this, scanner);
                scanner_list.add(thread);
                thread.start();

                final String toggle_label = scanner.getToggleLabel();
                if (toggle_label != null) {
                    scanner_map.put(toggle_label, scanner);
                }
                else {
                    headless_scanner_map.put(scanner.getName(), scanner);
                }
            }
        }
    }

    private void configureGlobalScanners() {

        final List<IGlobalHostScanner> global_host_scanners = application_manager.getGlobalScanners();

        if (global_host_scanners != null) {
            for (final IGlobalHostScanner scanner : global_host_scanners) {

                scanner.syncWith(status_scanner);
                final GlobalHostScannerThread thread = new GlobalHostScannerThread(this, scanner);
                scanner_list.add(thread);
                thread.start();

                final String toggle_label = scanner.getToggleLabel();
                if (toggle_label != null) {
                    scanner_map.put(toggle_label, scanner);
                }
                else {
                    headless_scanner_map.put(scanner.getName(), scanner);
                }
            }
        }
    }

    private void configureCallbacks() {

        host_status_callbacks = new HashSet<IHostStatusCallback>();
        attributes_callbacks = new HashSet<IAttributesCallback>();
    }

    private void startScanners() {

        for (final Thread scanner : scanner_list) {
            scanner.start();
        }
    }

    private void shutdownScanners() {

        shutdownScanners(scanner_map);
        shutdownScanners(headless_scanner_map);
    }

    private void shutdownScanners(final Map<String, IHostScanner> map) {

        for (final Entry<String, IHostScanner> entry : map.entrySet()) {
            entry.getValue().shutdown();
        }
    }

    private void add(final String[] host_patterns, final Credentials credentials) throws IOException {

        for (final String host_pattern : host_patterns) {

            // Each element of the array may contain a pattern that denotes multiple hosts.
            for (final String host : Patterns.resolveHostPattern(host_pattern)) {

                final HostDescriptor host_descriptor = new HostDescriptor(host, credentials);
                add(host_descriptor);
            }
        }
    }

    private String[] splitAtNewLines(final String hosts) {

        return hosts.split(NEWLINE_REGEX);
    }

    private Set<URL> pathToUrls(final String application_url_classpath) throws IOException {

        final String[] url_strings = application_url_classpath.split(URL_PATH_SEPARATOR);
        final Set<URL> result = new HashSet<URL>(url_strings.length);

        for (final String s : url_strings) {
            if (!s.equals("")) {
                result.add(new URL(s));
            }
        }

        return result;
    }

    private Set<URL> makeURLs(final URL url_base, final Set<String> jar_names, final Set<String> lib_names) throws IOException {

        final Set<URL> urls = new HashSet<URL>();

        urls.addAll(makeUrls(url_base, jar_names));
        urls.addAll(makeUrls(url_base, lib_names));

        return urls;
    }

    private Set<URL> makeUrls(final URL url_base, final Set<String> paths) throws IOException {

        final Set<URL> urls = new HashSet<URL>();

        for (final String path : paths) {
            urls.add(new URL(url_base + path));
        }

        return urls;
    }

    @SuppressWarnings("unchecked")
    private Class<? extends ApplicationManager> getApplicationManagerClass(final String application_entrypoint_name) throws ClassNotFoundException {

        final ClassLoader parent_loader = Thread.currentThread().getContextClassLoader();
        final java.net.URL[] url_array = URL.toArrayAsRealURLs(application_urls);
        final ClassLoader url_class_loader = new URLClassLoader(url_array, parent_loader);

        return (Class<? extends ApplicationManager>) Class.forName(application_entrypoint_name, true, url_class_loader);
    }

    private ApplicationManager getApplicationManager(final Class<? extends ApplicationManager> application_manager_class) throws InstantiationException, IllegalAccessException {

        return application_manager_class.newInstance();
    }

    private void waitForHost(final HostDescriptor host_descriptor, final HostState state, final boolean match) throws InterruptedException {

        while (!Thread.currentThread().isInterrupted()) {

            if (!xor(match, host_descriptor.getHostState().equals(state))) { return; }
            state_wait_delay.sleep();
        }
        if (Thread.currentThread().isInterrupted()) { throw new InterruptedException(); }
    }

    private void waitForAll(final HostState state, final boolean match) throws InterruptedException {

        while (!Thread.currentThread().isInterrupted()) {
            boolean all_at_termination_condition = true;

            for (final HostDescriptor host_descriptor : host_state_list) {
                if (xor(match, host_descriptor.getHostState().equals(state))) {
                    all_at_termination_condition = false;
                    break;
                }
            }

            if (all_at_termination_condition) { return; }
            state_wait_delay.sleep();
        }
        if (Thread.currentThread().isInterrupted()) { throw new InterruptedException(); }
    }

    private boolean xor(final boolean first, final boolean second) {

        return first && !second || !first && second;
    }

    private void setParameterValue(final ParameterValue parameter_value) {

        final String parameter_name = parameter_value.getParameterName();

        if (parameter_name.equals(STATUS_CHECK_THREAD_COUNT_KEY)) {
            status_scanner_thread_pool_size = parameter_value.getValue();
        }
        else if (parameter_name.equals(SSH_CHECK_THREAD_COUNT_KEY)) {
            ssh_check_thread_pool_size = parameter_value.getValue();
        }
        else if (parameter_name.equals(DEPLOY_CHECK_THREAD_COUNT_KEY)) {
            deploy_scanner_thread_pool_size = parameter_value.getValue();
        }
        else if (parameter_name.equals(SSH_CHECK_THREAD_TIMEOUT_KEY)) {
            ssh_check_timeout = new Duration(parameter_value.getValue(), TimeUnit.SECONDS);
        }
        else if (parameter_name.equals(SCANNER_MIN_CYCLE_TIME_KEY)) {

            final Duration min_cycle_time = new Duration(parameter_value.getValue(), TimeUnit.SECONDS);

            status_scanner_min_cycle_time = min_cycle_time;
            deploy_scanner_min_cycle_time = min_cycle_time;
            drop_scanner_min_cycle_time = min_cycle_time;
            kill_scanner_min_cycle_time = min_cycle_time;
        }
        else if (parameter_name.equals(STATE_WAIT_DELAY_KEY)) {
            state_wait_delay = new Duration(parameter_value.getValue(), TimeUnit.SECONDS);
        }
        else {
            ErrorHandling.hardError("unknown parameter: " + parameter_name);
        }
    }
}
