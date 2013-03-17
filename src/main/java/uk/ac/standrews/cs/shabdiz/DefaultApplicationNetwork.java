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
package uk.ac.standrews.cs.shabdiz;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.shabdiz.api.ApplicationManager;
import uk.ac.standrews.cs.shabdiz.api.ApplicationNetwork;
import uk.ac.standrews.cs.shabdiz.api.ApplicationState;
import uk.ac.standrews.cs.shabdiz.api.Host;
import uk.ac.standrews.cs.shabdiz.api.Scanner;

public class DefaultApplicationNetwork extends ConcurrentSkipListSet<ApplicationDescriptor> implements ApplicationNetwork {

    private static final long serialVersionUID = 6709443520736431534L;

    private static final Logger LOGGER = Logger.getLogger(DefaultApplicationNetwork.class.getName());

    private static final int DEFAULT_SCANNER_EXECUTOR_THREAD_POOL_SIZE = 5;
    private static final Duration DEFAULT_SCANNER_CYCLE_DELAY = new Duration(2, TimeUnit.SECONDS);
    private static final Duration DEFAULT_SCANNER_CYCLE_TIMEOUT = new Duration(15, TimeUnit.SECONDS);
    private final String application_name;
    private final Map<Scanner, ScheduledFuture<?>> scheduled_scanners;
    private final ScheduledThreadPoolExecutor scanner_executor;
    private final ExecutorService concurrent_scanner_executor;

    private final AutoKillScanner auto_kill_scanner;
    private final AutoDeployScanner auto_deploy_scanner;
    private final AutoRemoveScanner auto_remove_scanner;
    private final StatusScanner status_scanner;

    public DefaultApplicationNetwork(final String application_name) {

        this.application_name = application_name;
        scheduled_scanners = new HashMap<Scanner, ScheduledFuture<?>>();
        scanner_executor = new ScheduledThreadPoolExecutor(DEFAULT_SCANNER_EXECUTOR_THREAD_POOL_SIZE);
        concurrent_scanner_executor = Executors.newCachedThreadPool();

        auto_kill_scanner = new AutoKillScanner(DEFAULT_SCANNER_CYCLE_DELAY, DEFAULT_SCANNER_CYCLE_TIMEOUT);
        auto_deploy_scanner = new AutoDeployScanner(DEFAULT_SCANNER_CYCLE_DELAY);
        auto_remove_scanner = new AutoRemoveScanner(DEFAULT_SCANNER_CYCLE_DELAY);
        status_scanner = new StatusScanner(DEFAULT_SCANNER_CYCLE_DELAY);

        addScanner(auto_kill_scanner);
        addScanner(auto_deploy_scanner);
        addScanner(auto_remove_scanner);
        addScanner(status_scanner);
    }

    @Override
    public void deploy(final ApplicationDescriptor descriptor) throws Exception {

        final ApplicationManager manager = descriptor.getApplicationManager();
        final Object application_reference = manager.deploy(descriptor);
        descriptor.setApplicationReference(application_reference);
    }

    @Override
    public void deployAll() throws IOException, InterruptedException, TimeoutException, Exception {

        for (final ApplicationDescriptor applciation_descriptor : this) {

            deploy(applciation_descriptor);
        }
    }

    @Override
    public void killAllOnHost(final Host host) throws Exception {

        for (final ApplicationDescriptor applciation_descriptor : this) {
            if (applciation_descriptor.getHost().equals(host)) {
                kill(applciation_descriptor);
            }
        }
    }

    @Override
    public void killAll() throws Exception {

        for (final ApplicationDescriptor applciation_descriptor : this) {
            kill(applciation_descriptor);
        }
    }

    @Override
    public void kill(final ApplicationDescriptor member) throws Exception {

        member.getApplicationManager().kill(member);

    }

    @Override
    public String getApplicationName() {

        return application_name;
    }

    @Override
    public void awaitAnyOfStates(final ApplicationState... states) throws InterruptedException {

        //TODO tidy this up

        final Iterator<ApplicationDescriptor> application_descriptors = iterator();
        final List<CountDownLatch> latches = new ArrayList<CountDownLatch>();
        while (application_descriptors.hasNext()) {
            final ApplicationDescriptor application_descriptor = application_descriptors.next();
            //Create a latch per listener since we cannot know the number of application descriptors
            final CountDownLatch latch = new CountDownLatch(1);

            if (!application_descriptor.isInState(states)) {
                final PropertyChangeListener state_change = new PropertyChangeListener() {

                    @Override
                    public void propertyChange(final PropertyChangeEvent evt) {

                        if (application_descriptor.isInState(states)) {
                            latch.countDown();
                            application_descriptor.removeStateChangeListener(this);
                        }
                    }
                };
                application_descriptor.addStateChangeListener(state_change);
                latches.add(latch);
            }
        }

        for (final CountDownLatch latch : latches) {
            latch.await();
        }
    }

    public boolean addScanner(final AbstractConcurrentScanner scanner) {

        synchronized (scheduled_scanners) {
            return isAddable(scanner) ? injectExecutorThenAdd(scanner) : false;
        }
    }

    @Override
    public boolean addScanner(final Scanner scanner) {

        synchronized (scheduled_scanners) {
            return isAddable(scanner) ? add(scanner) : false;
        }
    }

    private boolean injectExecutorThenAdd(final AbstractConcurrentScanner scanner) {

        scanner.injectExecutorService(concurrent_scanner_executor);
        return add(scanner);
    }

    private boolean add(final Scanner scanner) {

        final ScheduledFuture<?> scheduled_scanner = scheduleScanner(scanner);
        return scheduled_scanners.put(scanner, scheduled_scanner) == null;
    }

    private boolean isAddable(final Scanner scanner) {

        return !scheduled_scanners.containsKey(scanner);
    }

    @Override
    public boolean removeScanner(final Scanner scanner) {

        final ScheduledFuture<?> scheduled_scanner;
        synchronized (scheduled_scanners) {
            scheduled_scanner = scheduled_scanners.remove(scanner);
        }
        return scheduled_scanner != null && scheduled_scanner.cancel(true);
    }

    @Override
    public void setScanEnabled(final boolean enabled) {

        synchronized (scheduled_scanners) {
            for (final Scanner scanner : scheduled_scanners.keySet()) {
                scanner.setEnabled(enabled);
            }
        }
    }

    @Override
    public void setAutoKillEnabled(final boolean enabled) {

        auto_kill_scanner.setEnabled(enabled);
    }

    @Override
    public void setAutoDeployEnabled(final boolean enabled) {

        auto_deploy_scanner.setEnabled(enabled);
    }

    @Override
    public void setAutoRemoveEnabled(final boolean enabled) {

        auto_remove_scanner.setEnabled(enabled);
    }

    private ScheduledFuture<?> scheduleScanner(final Scanner scanner) {

        final Duration cycle_delay = scanner.getCycleDelay();
        final long cycle_delay_length = cycle_delay.getLength();
        return scanner_executor.scheduleWithFixedDelay(new Runnable() {

            @Override
            public void run() {

                scanner.scan(DefaultApplicationNetwork.this);
            }
        }, cycle_delay_length, cycle_delay_length, cycle_delay.getTimeUnit());
    }

    @Override
    public void shutdown() {

        //TODO tidy up
        scanner_executor.shutdownNow();
        concurrent_scanner_executor.shutdownNow();
        synchronized (scheduled_scanners) {
            for (final ScheduledFuture<?> scheduled_scanner : scheduled_scanners.values()) {
                scheduled_scanner.cancel(true);
            }
        }

        for (final ApplicationDescriptor application_descriptor : this) {
            application_descriptor.kill();
            try {
                application_descriptor.getHost().close();
            }
            catch (final IOException e) {
                LOGGER.log(Level.WARNING, "failed to close host", e);
            }
        }
    }
}
