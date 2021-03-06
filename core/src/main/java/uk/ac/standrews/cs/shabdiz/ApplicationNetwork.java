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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.util.Duration;
import uk.ac.standrews.cs.shabdiz.util.FormattedNameThreadFactory;
import uk.ac.standrews.cs.shabdiz.util.HashCodeUtil;

/**
 * Maintains a set of {@link ApplicationDescriptor application descriptors}.
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class ApplicationNetwork implements Iterable<ApplicationDescriptor> {

    private static final String SCANNER_EXECUTOR_THREAD_NAME_FORMAT = "_scanner_executor_%d";
    private static final String SCANNER_SCHEDULER_THREAD_NAME_FORMAT = "_scanner_scheduler_%d";
    private static final String NETWORK_EXECUTOR_THREAD_NAME_FORMAT = "_network_executor_service_%d";
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationNetwork.class);
    private static final int DEFAULT_SCANNER_EXECUTOR_THREAD_POOL_SIZE = 10;
    private static final Duration DEFAULT_SCANNER_CYCLE_DELAY = new Duration(5, TimeUnit.SECONDS);
    private static final Duration DEFAULT_SCANNER_CYCLE_TIMEOUT = new Duration(1, TimeUnit.MINUTES);
    protected final ConcurrentSkipListSet<ApplicationDescriptor> application_descriptors;
    protected final HashMap<Scanner, ScheduledFuture<?>> scheduled_scanners;
    protected final AutoKillScanner auto_kill_scanner;
    protected final AutoDeployScanner auto_deploy_scanner;
    protected final AutoRemoveScanner auto_remove_scanner;
    protected final StatusScanner status_scanner;
    private final String application_name;
    private final ScheduledExecutorService scanner_scheduler;
    private final ExecutorService concurrent_scanner_executor;
    private final ExecutorService network_executor_service;
    private final ScannerEnabledPropertyChangeListener enabled_change_listener = new ScannerEnabledPropertyChangeListener();

    /**
     * Instantiates a new application network with default scanner interval, scanner timeout and scanner thread pool size.
     *
     * @param application_name the name of the application
     */
    public ApplicationNetwork(final String application_name) {

        this(application_name, DEFAULT_SCANNER_CYCLE_DELAY, DEFAULT_SCANNER_CYCLE_TIMEOUT, DEFAULT_SCANNER_EXECUTOR_THREAD_POOL_SIZE, Integer.MAX_VALUE);
    }

    /**
     * Instantiates a new Application network.
     *
     * @param application_name the application name
     * @param scanner_interval the scanner interval for all defaults scanners
     * @param scanner_timeout the scanner timeout for all defaults scanners
     * @param scanner_thread_pool_size the scanner scheduler thread pool size
     * @param concurrent_scanner_thread_pool_size the size of the thread pool used by {@link ConcurrentScanner concurrent scans}
     */
    public ApplicationNetwork(final String application_name, final Duration scanner_interval, final Duration scanner_timeout, final int scanner_thread_pool_size, final int concurrent_scanner_thread_pool_size) {

        this.application_name = application_name;
        application_descriptors = new ConcurrentSkipListSet<>();
        scheduled_scanners = new HashMap<>();
        scanner_scheduler = createScannerScheduledExecutorService(scanner_thread_pool_size);
        concurrent_scanner_executor = createScannerExecutorService(concurrent_scanner_thread_pool_size);
        network_executor_service = createNetworkExecutorService();

        auto_kill_scanner = new AutoKillScanner(scanner_interval, scanner_timeout);
        auto_deploy_scanner = new AutoDeployScanner(scanner_interval, scanner_timeout);
        auto_remove_scanner = new AutoRemoveScanner(scanner_interval, scanner_timeout);
        status_scanner = new StatusScanner(scanner_interval);

        addScanner(auto_kill_scanner);
        addScanner(auto_deploy_scanner);
        addScanner(auto_remove_scanner);
        addScanner(status_scanner);
    }

    /**
     * Gets the name of this application.
     *
     * @return the name of this application
     */
    public String getApplicationName() {

        return application_name;
    }

    /**
     * Attempts to deploy an application instance and sets the {@link ApplicationDescriptor#getApplicationReference() application reference} of the given application descriptor.
     * In the case where the descriptor is not added to this network, this method does not add the given descriptor to this network.
     *
     * @param descriptor the application descriptor to deploy
     * @throws Exception if deployment fails
     */
    public void deploy(final ApplicationDescriptor descriptor) throws Exception {

        final ApplicationManager manager = descriptor.getApplicationManager();
        final Object application_reference = manager.deploy(descriptor);
        descriptor.setApplicationReference(application_reference);
        descriptor.setApplicationState(ApplicationState.DEPLOYED);
    }

    /**
     * Attempts to concurrently {@link #deploy(ApplicationDescriptor) deploy} each of the application instances that are maintained by this network.
     * If one deployment task fails all deployment tasks are cancelled.
     *
     * @throws Exception if any of the deployments fails
     */
    public void deployAll() throws Exception {

        final List<CompletableFuture<Void>> deployments = new ArrayList<>();
        for (final ApplicationDescriptor application_descriptor : application_descriptors) {
            final CompletableFuture<Void> deployment = CompletableFuture.runAsync(() -> {

                try {
                    deploy(application_descriptor);
                }
                catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, network_executor_service);
            deployments.add(deployment);
        }

        awaitCompletion(deployments);
    }

    /**
     * Attempts to terminate the application instance that is described by the given {@code application_descriptor} as defined by its {@link ApplicationDescriptor#getApplicationManager()  manager}.
     *
     * @param descriptor the application descriptor to kill
     * @throws Exception the exception
     */
    public void kill(final ApplicationDescriptor descriptor) throws Exception {

        descriptor.getApplicationManager().kill(descriptor);
        descriptor.setApplicationState(ApplicationState.KILLED);
    }

    /**
     * Attempts to concurrently terminate all the application instances that are managed by this network.
     * If one termination task fails, all the termination tasks are cancelled.
     *
     * @throws Exception the exception
     */
    public void killAll() throws Exception {

        final List<CompletableFuture<Void>> terminations = new ArrayList<>();
        for (final ApplicationDescriptor application_descriptor : application_descriptors) {
            final CompletableFuture<Void> termination = CompletableFuture.runAsync(() -> {

                try {
                    kill(application_descriptor);
                }
                catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, network_executor_service);
            terminations.add(termination);
        }

        awaitCompletion(terminations);
    }

    /**
     * Attempts to concurrently {@link #kill(ApplicationDescriptor) terminate} all the application instances that their {@link ApplicationDescriptor#getHost() host} is equal to the given {@code host}.
     *
     * @param host the host on which to terminate the application instances
     * @throws Exception the termination fails
     */
    public void killAllOnHost(final Host host) throws Exception {

        final List<CompletableFuture<Void>> terminations = new ArrayList<>();
        application_descriptors.stream().filter(application_descriptor -> host.equals(application_descriptor.getHost())).forEach(application_descriptor -> {
            final CompletableFuture<Void> termination = CompletableFuture.runAsync(() -> {

                try {
                    kill(application_descriptor);
                }
                catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, network_executor_service);
            terminations.add(termination);
        });
        awaitCompletion(terminations);
    }

    /**
     * Causes the current thread to wait until all the {@link ApplicationDescriptor instances} managed by this network reach one of the given {@code states} at least once, unless the thread is {@link Thread#interrupt() interrupted}.
     *
     * @param states the states which application instances must reach at least once
     * @throws InterruptedException if the current thread is {@link Thread#interrupt() interrupted} while waiting
     */
    public void awaitAnyOfStates(final ApplicationState... states) throws InterruptedException {

        final List<CompletableFuture<Void>> awaiting_state_futures = new ArrayList<>();
        for (final ApplicationDescriptor descriptor : application_descriptors) {
            final CompletableFuture<Void> awaiting_state = CompletableFuture.runAsync(() -> {

                try {
                    descriptor.awaitAnyOfStates(states);
                }
                catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }, network_executor_service);
            awaiting_state_futures.add(awaiting_state);
        }

        try {
            awaitCompletion(awaiting_state_futures);
        }
        catch (ExecutionException e) {
            LOGGER.error("failure occurred while awaiting uniform state", e);
            throw new InterruptedException("failure occurred while awaiting uniform state");
        }
    }

    /**
     * Adds the given {@code scanner} to the collection of this network's scanners.
     * This method has no effect if the given {@code scanner} has already been added.
     *
     * @param scanner the scanner to add
     * @return true, if successfully added
     */
    public boolean addScanner(final Scanner scanner) {

        synchronized (scheduled_scanners) {

            final boolean added;

            if (!isAdded(scanner)) {
                scanner.addEnabledPropertyChangeListener(enabled_change_listener);
                final ScheduledFuture<?> scheduled_scanner = scanner.isEnabled() ? scheduleScanner(scanner) : null;
                scheduled_scanners.put(scanner, scheduled_scanner);

                added = true;
            }
            else {
                added = false;
            }

            return added;
        }
    }

    /**
     * Removes the given {@code scanner} from the collection of this network's scanners.
     * This method has no effect if the given {@code scanner} does not exist in the collection of this network's scanners.
     *
     * @param scanner the scanner to remove
     * @return true, if successfully removed
     */
    public boolean removeScanner(final Scanner scanner) {

        final ScheduledFuture<?> scheduled_scanner;
        final boolean removable;
        synchronized (scheduled_scanners) {
            removable = isAdded(scanner);
            scheduled_scanner = removable ? scheduled_scanners.remove(scanner) : null;
        }

        if (scheduled_scanner != null) {
            scheduled_scanner.cancel(true);
        }
        if (removable) {
            scanner.removeEnabledPropertyChangeListener(enabled_change_listener);
        }
        return removable;
    }

    /**
     * Sets the policy on whether the all scanners of this network should be {@link Scanner#setEnabled(boolean) enabled}.
     *
     * @param enabled if {@code true} enables all the scanners of this network, disables all the scanners otherwise
     */
    public void setScanEnabled(final boolean enabled) {

        synchronized (scheduled_scanners) {
            for (final Scanner scanner : scheduled_scanners.keySet()) {
                scanner.setEnabled(enabled);
            }
        }
    }

    /**
     * Sets the auto kill enabled.
     *
     * @param enabled the new auto kill enabled
     */
    public void setAutoKillEnabled(final boolean enabled) {

        auto_kill_scanner.setEnabled(enabled);
    }

    /**
     * Sets the auto deploy enabled.
     *
     * @param enabled the new auto deploy enabled
     */
    public void setAutoDeployEnabled(final boolean enabled) {

        auto_deploy_scanner.setEnabled(enabled);
    }

    /**
     * Sets the auto remove enabled.
     *
     * @param enabled the new auto remove enabled
     */
    public void setAutoRemoveEnabled(final boolean enabled) {

        auto_remove_scanner.setEnabled(enabled);
    }

    /**
     * Sets whether the {@link StatusScanner} of this network should be enabled.
     *
     * @param enabled whether the status scanner should be enabled
     */
    public void setStatusScannerEnabled(final boolean enabled) {

        status_scanner.setEnabled(enabled);
    }

    /**
     * Attempts to kill all application processes and {@link Host#close() close} the hosts of application instances.
     * Removes all the hooks that are maintained by this network.
     * After this method is called, this network is no longer usable.
     */
    public void shutdown() {

        setScanEnabled(false);
        cancelScheduledScanners();
        killAllSilently();
        closeHosts();
        scanner_scheduler.shutdownNow();
        network_executor_service.shutdownNow();
        concurrent_scanner_executor.shutdownNow();
        application_descriptors.clear();
    }

    /**
     * Adds an application descriptor to the set.
     *
     * @param descriptor the descriptor to be added
     * @return true if the set did not already contain the specified descriptor
     */
    public boolean add(final ApplicationDescriptor descriptor) {

        return application_descriptors.add(descriptor);
    }

    /**
     * Removes the specified application descriptor if it is present.
     *
     * @param descriptor the descriptor to be removed
     * @return whether the set of descriptors belonging to this network has changed
     */
    public boolean remove(final ApplicationDescriptor descriptor) {

        return application_descriptors.remove(descriptor);
    }

    /**
     * Returns the first application descriptor that has been added to this network, or {@code null} if this network is empty.
     *
     * @return the first application descriptor that has been added to this network, or {@code null} if this network is empty
     */
    public ApplicationDescriptor first() {

        try {
            return application_descriptors.first();
        }
        catch (final NoSuchElementException e) {
            return null;
        }
    }

    /**
     * Gets a copy of this network's application descriptors.
     *
     * @return a copy of this network's application descriptors
     */
    public Set<ApplicationDescriptor> getApplicationDescriptors() {

        return new CopyOnWriteArraySet<>(application_descriptors);
    }

    /**
     * Checks whether this network contains the given {@code descriptor}.
     *
     * @param descriptor the descriptor that its presence is checked
     * @return {@code true} if the given {@code descriptor} is present in this network
     */
    public boolean contains(final ApplicationDescriptor descriptor) {

        return application_descriptors.contains(descriptor);
    }

    /**
     * Returns the size of the network.
     *
     * @return the size of the network
     */
    public int size() {

        return application_descriptors.size();
    }

    @Override
    public Iterator<ApplicationDescriptor> iterator() {

        return application_descriptors.iterator();
    }

    @Override
    public int hashCode() {

        return HashCodeUtil.generate(super.hashCode(), application_name.hashCode(), scheduled_scanners.hashCode());
    }

    @Override
    public boolean equals(final Object other) {

        if (this == other) { return true; }
        if (!(other instanceof ApplicationNetwork) || !super.equals(other)) { return false; }
        final ApplicationNetwork that = (ApplicationNetwork) other;
        return application_name.equals(that.application_name) && scheduled_scanners.equals(that.scheduled_scanners);
    }

    @Override
    public String toString() {

        return application_name;
    }

    private void awaitCompletion(final List<CompletableFuture<Void>> deployments) throws InterruptedException, ExecutionException {

        try {
            CompletableFuture.allOf(deployments.toArray(new CompletableFuture[deployments.size()])).get();
        }
        finally {
            for (final CompletableFuture<Void> future : deployments) {
                future.cancel(true);
            }
        }
    }

    protected ScheduledExecutorService createScannerScheduledExecutorService(final int thread_pool_size) {

        return new ScheduledThreadPoolExecutor(thread_pool_size, createThreadFactory(SCANNER_SCHEDULER_THREAD_NAME_FORMAT));
    }

    protected ExecutorService createScannerExecutorService(final int pool_size) {

        final ThreadFactory thread_factory = createThreadFactory(SCANNER_EXECUTOR_THREAD_NAME_FORMAT);
        return pool_size != Integer.MAX_VALUE ? Executors.newFixedThreadPool(pool_size, thread_factory) : Executors.newCachedThreadPool(thread_factory);
    }

    protected ThreadFactory createThreadFactory(final String format) {

        return new FormattedNameThreadFactory(application_name + format);
    }

    protected ExecutorService createNetworkExecutorService() {

        return Executors.newCachedThreadPool(createThreadFactory(NETWORK_EXECUTOR_THREAD_NAME_FORMAT));
    }

    ExecutorService getConcurrentScannerExecutor() {

        return concurrent_scanner_executor;
    }

    private void closeHosts() {

        final List<CompletableFuture<Void>> host_closures = new ArrayList<>();
        for (final ApplicationDescriptor application_descriptor : application_descriptors) {
            final CompletableFuture<Void> host_closure = CompletableFuture.runAsync(() -> {

                final Host host = application_descriptor.getHost();
                if (host != null) {
                    try {
                        host.close();
                    }
                    catch (final IOException e) {
                        LOGGER.debug("failed to close host", e);
                    }
                }
            }, network_executor_service);
            host_closures.add(host_closure);
        }

        awaitCompletionSilently(host_closures);
    }

    private void awaitCompletionSilently(final List<CompletableFuture<Void>> host_closures) {

        try {
            awaitCompletion(host_closures);
        }
        catch (InterruptedException | ExecutionException e) {
            LOGGER.debug("failed to kill all managed application descriptors", e);
        }
    }

    private void killAllSilently() {

        final List<CompletableFuture<Void>> terminations = new ArrayList<>();
        for (final ApplicationDescriptor application_descriptor : application_descriptors) {
            final CompletableFuture<Void> termination = CompletableFuture.runAsync(() -> {
                try {
                    kill(application_descriptor);
                }
                catch (final Exception e) {
                    LOGGER.debug("failed to kill all managed application descriptors", e);
                }
            }, network_executor_service);
            terminations.add(termination);
        }

        awaitCompletionSilently(terminations);
    }

    private void cancelScheduledScanners() {

        synchronized (scheduled_scanners) {
            scheduled_scanners.values().stream().filter(scheduled_scanner -> scheduled_scanner != null).forEach(scheduled_scanner -> {
                scheduled_scanner.cancel(true);
            });
        }
    }

    private boolean isAdded(final Scanner scanner) {

        return scheduled_scanners.containsKey(scanner);
    }

    private ScheduledFuture<?> scheduleScanner(final Scanner scanner) {

        final Duration cycle_delay = scanner.getCycleDelay();
        final long cycle_delay_length = cycle_delay.getLength();
        return scanner_scheduler.scheduleWithFixedDelay(() -> scanner.scan(ApplicationNetwork.this), 0, cycle_delay_length, cycle_delay.getTimeUnit());
    }

    private class ScannerEnabledPropertyChangeListener implements PropertyChangeListener {

        @Override
        public void propertyChange(final PropertyChangeEvent event) {

            final Object source = event.getSource();

            if (source instanceof Scanner) {

                final Scanner scanner = (Scanner) source;
                final Boolean enabled = (Boolean) event.getNewValue();
                synchronized (scheduled_scanners) {

                    if (enabled) {
                        final ScheduledFuture<?> future = scheduleScanner(scanner);
                        final ScheduledFuture<?> old_future = scheduled_scanners.put(scanner, future);

                        if (old_future != null && !old_future.isDone()) {
                            LOGGER.warn("expected the old future of scanner {} to be done", scanner);
                            old_future.cancel(true);
                        }
                    }
                    else {
                        final ScheduledFuture<?> future = scheduled_scanners.get(scanner);
                        if (future != null) {
                            future.cancel(true);
                        }
                    }
                }
            }
            else {
                LOGGER.warn("expected the source {} to be of type Scanner", source);
            }
        }
    }
}
