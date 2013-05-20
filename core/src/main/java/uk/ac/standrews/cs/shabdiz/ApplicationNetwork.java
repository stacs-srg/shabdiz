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
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.LoggerFactory;

import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.util.Duration;
import uk.ac.standrews.cs.shabdiz.util.HashCodeUtil;

/**
 * Maintains a set of {@link ApplicationDescriptor application descriptors}.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class ApplicationNetwork implements Iterable<ApplicationDescriptor> {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ApplicationNetwork.class);
    private static final int DEFAULT_SCANNER_EXECUTOR_THREAD_POOL_SIZE = 5;
    private static final Duration DEFAULT_SCANNER_CYCLE_DELAY = new Duration(2, TimeUnit.SECONDS);
    private static final Duration DEFAULT_SCANNER_CYCLE_TIMEOUT = new Duration(15, TimeUnit.SECONDS);

    private final String application_name;
    private final ConcurrentSkipListSet<ApplicationDescriptor> application_descriptors;
    private final HashMap<Scanner, ScheduledFuture<?>> scheduled_scanners;
    private final ScheduledExecutorService scanner_scheduler;
    private final ExecutorService concurrent_scanner_executor;
    private final AutoKillScanner auto_kill_scanner;
    private final AutoDeployScanner auto_deploy_scanner;
    private final AutoRemoveScanner auto_remove_scanner;
    private final StatusScanner status_scanner;

    /**
     * Instantiates a new application network.
     * 
     * @param application_name the name of the application
     */
    public ApplicationNetwork(final String application_name) {

        this.application_name = application_name;
        application_descriptors = new ConcurrentSkipListSet<ApplicationDescriptor>();
        scheduled_scanners = new HashMap<Scanner, ScheduledFuture<?>>();
        scanner_scheduler = createScannerScheduledExecutorService();
        concurrent_scanner_executor = createScannerExecutorService();

        auto_kill_scanner = new AutoKillScanner(DEFAULT_SCANNER_CYCLE_DELAY, DEFAULT_SCANNER_CYCLE_TIMEOUT);
        auto_deploy_scanner = new AutoDeployScanner(DEFAULT_SCANNER_CYCLE_DELAY);
        auto_remove_scanner = new AutoRemoveScanner(DEFAULT_SCANNER_CYCLE_DELAY);
        status_scanner = new StatusScanner(DEFAULT_SCANNER_CYCLE_DELAY);

        addScanner(auto_kill_scanner);
        addScanner(auto_deploy_scanner);
        addScanner(auto_remove_scanner);
        addScanner(status_scanner);
    }

    protected ScheduledExecutorService createScannerScheduledExecutorService() {

        return new ScheduledThreadPoolExecutor(DEFAULT_SCANNER_EXECUTOR_THREAD_POOL_SIZE);
    }

    protected ExecutorService createScannerExecutorService() {

        return Executors.newCachedThreadPool();
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
     * Attempts to sequentially {@link #deploy(ApplicationDescriptor) deploy} each of the application instances that are maintained by this network.
     * 
     * @throws Exception if any of the deployments fails
     */
    public void deployAll() throws Exception {

        for (final ApplicationDescriptor application_descriptor : application_descriptors)
            deploy(application_descriptor);
    }

    /**
     * Attempts to deploy an application instance and sets the {@link ApplicationDescriptor#getApplicationReference() application reference} of the given application descriptor.
     * 
     * @param descriptor the application descriptor to deploy
     * @throws Exception if deployment fails
     */
    public void deploy(final ApplicationDescriptor descriptor) throws Exception {

        final ApplicationManager manager = descriptor.getApplicationManager();
        final Object application_reference = manager.deploy(descriptor);
        descriptor.setApplicationReference(application_reference);
    }

    /**
     * Attempts to {@link #kill(ApplicationDescriptor) terminate} all the application instances that their {@link ApplicationDescriptor#getHost() host} is equal to the given {@code host}.
     * 
     * @param host the host on which to terminate the application instances
     * @throws Exception the termination fails
     */
    public void killAllOnHost(final Host host) throws Exception {

        for (final ApplicationDescriptor applciation_descriptor : application_descriptors)
            if (applciation_descriptor.getHost().equals(host)) kill(applciation_descriptor);
    }

    /**
     * Attempts to {@link Process#destroy() terminate} the {@link ApplicationDescriptor#getProcesses() processes} of the given {@code application_descriptor}.
     * 
     * @param descriptor the application descriptor to kill
     * @throws Exception the exception
     */
    public void kill(final ApplicationDescriptor descriptor) throws Exception {

        descriptor.getApplicationManager().kill(descriptor);
    }

    /**
     * Causes the current thread to wait until all the {@link ApplicationDescriptor instances} managed by this network reach one of the given {@code states} at least once, unless the thread is {@link Thread#interrupt() interrupted}.
     * 
     * @param states the states which application instances must reach at least once
     * @throws InterruptedException if the current thread is {@link Thread#interrupt() interrupted} while waiting
     */
    public void awaitAnyOfStates(final ApplicationState... states) throws InterruptedException {

        // FIXME This implementation checks sequentially; consider checking concurrently
        for (final ApplicationDescriptor descriptor : application_descriptors)
            descriptor.awaitAnyOfStates(states);
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
            return isAddable(scanner) ? scanner instanceof AbstractConcurrentScanner ? injectExecutorAndAdd(scanner) : add(scanner) : false;
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
        synchronized (scheduled_scanners) {
            scheduled_scanner = scheduled_scanners.remove(scanner);
        }
        return scheduled_scanner != null && scheduled_scanner.cancel(true);
    }

    /**
     * Sets the policy on whether the scanners of this network should be {@link Scanner#setEnabled(boolean) enabled}.
     * 
     * @param enabled if {@code true} enables all the scanners of this network, disables all the scanners otherwise
     */
    public void setScanEnabled(final boolean enabled) {

        synchronized (scheduled_scanners) {
            for (final Scanner scanner : scheduled_scanners.keySet())
                scanner.setEnabled(enabled);
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
     * Attempts to kill all application processes and {@link Host#close() close} the hosts of application instances.
     * Removes all the hooks that are maintained by this network.
     * After this method is called, this network is no longer usable.
     */
    public void shutdown() {

        scanner_scheduler.shutdownNow();
        concurrent_scanner_executor.shutdownNow();
        cancelScheduledScanners();
        killAllSilently();
        closeHosts();
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
     */
    public void remove(final ApplicationDescriptor descriptor) {

        application_descriptors.remove(descriptor);
    }

    /**
     * Returns the first application descriptor to be added.
     * 
     * @return the first application descriptor to be added
     */
    public ApplicationDescriptor first() {

        return application_descriptors.first();
    }

    /**
     * Attempts to terminate all the application instances that are managed by this network.
     * 
     * @throws Exception the exception
     */
    public void killAll() throws Exception {

        //FIXME implement concurrent kill
        for (final ApplicationDescriptor application_descriptor : application_descriptors)
            kill(application_descriptor);
    }

    @Override
    public Iterator<ApplicationDescriptor> iterator() {

        return application_descriptors.iterator();
    }

    @Override
    public boolean equals(final Object other) {

        if (this == other) return true;
        if (!(other instanceof ApplicationNetwork) || !super.equals(other)) return false;
        final ApplicationNetwork that = (ApplicationNetwork) other;
        return application_name.equals(that.application_name) && scheduled_scanners.equals(that.scheduled_scanners);
    }

    @Override
    public int hashCode() {

        return HashCodeUtil.generate(super.hashCode(), application_name.hashCode(), scheduled_scanners.hashCode());
    }

    private void closeHosts() {

        for (final ApplicationDescriptor application_descriptor : application_descriptors) {

            final Host host = application_descriptor.getHost();
            if (host != null) try {
                host.close();
            }
            catch (final IOException e) {
                LOGGER.warn("failed to close host", e);
            }
        }
    }

    private void killAllSilently() {

        try {
            killAll();
        }
        catch (final Exception e) {
            LOGGER.warn("failed to kill all managed application descriptors", e);
        }
    }

    private void cancelScheduledScanners() {

        synchronized (scheduled_scanners) {
            for (final ScheduledFuture<?> scheduled_scanner : scheduled_scanners.values())
                scheduled_scanner.cancel(true);
        }
    }

    private boolean injectExecutorAndAdd(final Scanner scanner) {

        AbstractConcurrentScanner.class.cast(scanner).injectExecutorService(concurrent_scanner_executor);
        return add(scanner);
    }

    private boolean add(final Scanner scanner) {

        final ScheduledFuture<?> scheduled_scanner = scheduleScanner(scanner);
        return scheduled_scanners.put(scanner, scheduled_scanner) == null;
    }

    private boolean isAddable(final Scanner scanner) {

        return !scheduled_scanners.containsKey(scanner);
    }

    private ScheduledFuture<?> scheduleScanner(final Scanner scanner) {

        final Duration cycle_delay = scanner.getCycleDelay();
        final long cycle_delay_length = cycle_delay.getLength();
        return scanner_scheduler.scheduleWithFixedDelay(new Runnable() {

            @Override
            public void run() {

                if (scanner.isEnabled()) scanner.scan(ApplicationNetwork.this);
            }
        }, cycle_delay_length, cycle_delay_length, cycle_delay.getTimeUnit());
    }
}
