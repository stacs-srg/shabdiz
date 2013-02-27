/*
 * shabdiz Library
 * Copyright (C) 2013 Networks and Distributed Systems Research Group
 * <http://www.cs.st-andrews.ac.uk/research/nds>
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
package uk.ac.standrews.cs.shabdiz;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.nds.util.ErrorHandling;
import uk.ac.standrews.cs.shabdiz.api.ApplicationManager;
import uk.ac.standrews.cs.shabdiz.api.State;
import uk.ac.standrews.cs.shabdiz.api.Scanner;
import uk.ac.standrews.cs.shabdiz.api.MadfaceManager;
import uk.ac.standrews.cs.shabdiz.scanners.DeployScanner;
import uk.ac.standrews.cs.shabdiz.scanners.DropScanner;
import uk.ac.standrews.cs.shabdiz.scanners.KillScanner;
import uk.ac.standrews.cs.shabdiz.scanners.StatusScanner;

/**
 * Madface server implementation. The method {@link #shutdown()} should be called before disposing of an instance, to avoid thread leakage.
 * 
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public final class DefaultMadfaceManager implements MadfaceManager {

    private static final int SCANNER_SCHEDULER_TRHEAD_POOL_SIZE = 10;

    /** Success status message. */
    public static final String RESULT_OK = "ok";

    /** Error message for invalid preference. */
    public static final String NO_PREFERENCE_WITH_NAME = "no preference with name: ";

    // -------------------------------------------------------------------------------------------------------

    private ApplicationManager application_manager;

    private StatusScanner status_scanner;
    private DeployScanner deploy_scanner;
    private DropScanner drop_scanner;
    private KillScanner kill_scanner;

    private Duration status_scanner_min_cycle_time;
    private Duration deploy_scanner_min_cycle_time;
    private Duration drop_scanner_min_cycle_time;
    private Duration kill_scanner_min_cycle_time;

    private Duration state_wait_delay;
    private Duration ssh_check_timeout;

    private final SortedSet<HostDescriptor> host_descriptors;
    private final Map<String, Scanner> scanner_map;
    private final Map<String, Scanner> headless_scanner_map;
    private final ScheduledExecutorService scheduled_executor;
    private final ExecutorService concurrent_scanner_executor;
    private final List<ScheduledFuture<?>> scheduled_scanners;

    protected DefaultMadfaceManager() {

        this(Configuration.DEFAULT_CONFIGURATION);
    }

    protected DefaultMadfaceManager(final Configuration configuration) {

        setConfigurationParameters(configuration);
        scheduled_scanners = new ArrayList<ScheduledFuture<?>>();
        scheduled_executor = Executors.newScheduledThreadPool(SCANNER_SCHEDULER_TRHEAD_POOL_SIZE);
        concurrent_scanner_executor = Executors.newCachedThreadPool();
        host_descriptors = new ConcurrentSkipListSet<HostDescriptor>();
        scanner_map = Collections.synchronizedMap(new HashMap<String, Scanner>());
        headless_scanner_map = Collections.synchronizedMap(new HashMap<String, Scanner>());

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
    public Map<String, Scanner> getScannerMap() {

        return scanner_map;
    }

    /**
     * Returns the application manager.
     * 
     * @return the application manager
     */
    @Override
    public ApplicationManager getApplicationManager() {

        return application_manager;
    }

    @Override
    public void setApplicationManager(final ApplicationManager application_manager) {

        this.application_manager = application_manager;
        configureApplicationSpecificScanners();
    }

    @Override
    public void add(final HostDescriptor host_descriptor) {

        host_descriptors.add(host_descriptor);
    }

    @Override
    public void drop(final HostDescriptor host_descriptor) {

        Diagnostic.traceNoSource(DiagnosticLevel.FINAL, "dropping: " + host_descriptor.getHost());

        host_descriptors.remove(host_descriptor);
        host_descriptor.shutdown();
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

        for (final HostDescriptor host_descriptor : host_descriptors) {
            kill(host_descriptor, kill_all_instances);
        }
    }

    @Override
    public void deployAll() throws Exception {

        for (final HostDescriptor host_descriptor : host_descriptors) {
            deploy(host_descriptor);
        }
    }

    @Override
    public void dropAll() {

        host_descriptors.clear();
    }

    @Override
    public String setPrefEnabled(final String pref_name, final boolean enabled) {

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
    public void waitForHostToReachState(final HostDescriptor host_descriptor, final State state_to_reach) throws InterruptedException {

        waitForHost(host_descriptor, state_to_reach, true);
    }

    @Override
    public void waitForHostToReachStateThatIsNot(final HostDescriptor host_descriptor, final State state_to_not_reach) throws InterruptedException {

        waitForHost(host_descriptor, state_to_not_reach, false);
    }

    @Override
    public void waitForAllToReachState(final State state_to_reach) throws InterruptedException {

        waitForAll(state_to_reach, true);
    }

    @Override
    public void waitForAllToReachStateThatIsNot(final State state_to_not_reach) throws InterruptedException {

        waitForAll(state_to_not_reach, false);
    }

    @Override
    public void shutdown() {

        shutdownScanners();
        shutdownHostDescriptors();
        shutdownApplicationManager();
    }

    private void shutdownApplicationManager() {

        if (application_manager != null) {
            application_manager.shutdown();
        }
    }

    @Override
    public SortedSet<HostDescriptor> getHostDescriptors() {

        return host_descriptors;
    }

    @Override
    public HostDescriptor findHostDescriptorByName(final String host) throws UnknownHostException {

        for (final HostDescriptor host_descriptor : host_descriptors) {
            if (host_descriptor.getHost().equals(host)) { return host_descriptor; }
        }

        throw new UnknownHostException();
    }

    private void shutdownHostDescriptors() {

        for (final HostDescriptor host_descriptor : host_descriptors) {
            host_descriptor.shutdown();
        }
    }

    private String setScannerEnabled(final String pref_name, final boolean enabled) {

        final Scanner scanner = getScannerMap().get(pref_name);
        if (scanner == null) { return NO_PREFERENCE_WITH_NAME + pref_name; }

        scanner.setEnabled(enabled);
        return RESULT_OK;
    }

    private void configureGenericScanners() {

        status_scanner = new StatusScanner(concurrent_scanner_executor, this, status_scanner_min_cycle_time, StatusScanner.DEFAULT_STATUS_CHECK_TIMEOUT, ssh_check_timeout);
        deploy_scanner = new DeployScanner(concurrent_scanner_executor, this, deploy_scanner_min_cycle_time);
        drop_scanner = new DropScanner(concurrent_scanner_executor, this, drop_scanner_min_cycle_time);
        kill_scanner = new KillScanner(concurrent_scanner_executor, this, kill_scanner_min_cycle_time, KillScanner.DEFAULT_KILL_CHECK_TIMEOUT);

        deploy_scanner.syncWith(status_scanner);
        drop_scanner.syncWith(status_scanner);
        kill_scanner.syncWith(status_scanner);

        scanner_map.put(status_scanner.getToggleLabel(), status_scanner);
        scanner_map.put(deploy_scanner.getToggleLabel(), deploy_scanner);
        scanner_map.put(drop_scanner.getToggleLabel(), drop_scanner);
        scanner_map.put(kill_scanner.getToggleLabel(), kill_scanner);
    }

    private void configureApplicationSpecificScanners() {

        final List<Scanner> global_host_scanners = application_manager.getScanners();

        if (global_host_scanners != null) {
            for (final Scanner scanner : global_host_scanners) {
                scanner.syncWith(status_scanner);
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

    private void startScanners() {

        for (final Scanner scanner : scanner_map.values()) {

            final Duration cycle_delay = scanner.getCycleDelay();
            final long cycle_delay_length = cycle_delay.getLength();
            final ScheduledFuture<?> scheduled_scanner = scheduled_executor.scheduleWithFixedDelay(new Runnable() {

                @Override
                public void run() {

                    final CopyOnWriteArraySet<HostDescriptor> host_descriptors_copy = new CopyOnWriteArraySet<HostDescriptor>(host_descriptors);
                    scanner.scan(host_descriptors_copy);
                }
            }, cycle_delay_length, cycle_delay_length, cycle_delay.getTimeUnit());

            scheduled_scanners.add(scheduled_scanner);
        }
    }

    private void shutdownScanners() {

        shutdownScanners(scanner_map);
        shutdownScanners(headless_scanner_map);
        concurrent_scanner_executor.shutdownNow();
        scheduled_executor.shutdownNow();
    }

    private void shutdownScanners(final Map<String, Scanner> map) {

        for (final ScheduledFuture<?> scheduled_scanner : scheduled_scanners) {
            scheduled_scanner.cancel(true);
        }
    }

    private void waitForHost(final HostDescriptor host_descriptor, final State state, final boolean match) throws InterruptedException {

        while (!Thread.currentThread().isInterrupted()) {

            if (!xor(match, host_descriptor.getHostState().equals(state))) { return; }
            state_wait_delay.sleep();
        }
        if (Thread.currentThread().isInterrupted()) { throw new InterruptedException(); }
    }

    private void waitForAll(final State state, final boolean match) throws InterruptedException {

        while (!Thread.currentThread().isInterrupted()) {
            boolean all_at_termination_condition = true;

            for (final HostDescriptor host_descriptor : host_descriptors) {
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

        if (parameter_name.equals(Configuration.SSH_CHECK_THREAD_TIMEOUT_KEY)) {
            ssh_check_timeout = new Duration(parameter_value.getValue(), TimeUnit.SECONDS);
        }
        else if (parameter_name.equals(Configuration.SCANNER_MIN_CYCLE_TIME_KEY)) {

            final Duration min_cycle_time = new Duration(parameter_value.getValue(), TimeUnit.SECONDS);

            status_scanner_min_cycle_time = min_cycle_time;
            deploy_scanner_min_cycle_time = min_cycle_time;
            drop_scanner_min_cycle_time = min_cycle_time;
            kill_scanner_min_cycle_time = min_cycle_time;
        }
        else if (parameter_name.equals(Configuration.STATE_WAIT_DELAY_KEY)) {
            state_wait_delay = new Duration(parameter_value.getValue(), TimeUnit.SECONDS);
        }
        else {
            ErrorHandling.hardError("unknown parameter: " + parameter_name);
        }
    }
}
