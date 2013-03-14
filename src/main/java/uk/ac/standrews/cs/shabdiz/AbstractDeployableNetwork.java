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
import uk.ac.standrews.cs.shabdiz.api.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.api.ApplicationNetwork;
import uk.ac.standrews.cs.shabdiz.api.ApplicationState;
import uk.ac.standrews.cs.shabdiz.api.Host;
import uk.ac.standrews.cs.shabdiz.api.Scanner;

public abstract class AbstractDeployableNetwork<T extends DefaultApplicationDescriptor> extends ConcurrentSkipListSet<T> implements ApplicationNetwork<T> {

    private static final Logger LOGGER = Logger.getLogger(AbstractDeployableNetwork.class.getName());

    private static final int DEFAULT_SCANNER_EXECUTOR_THREAD_POOL_SIZE = 5;
    private static final Duration DEFAULT_SCANNER_CYCLE_DELAY = new Duration(2, TimeUnit.SECONDS);
    private static final Duration DEFAULT_SCANNER_CYCLE_TIMEOUT = new Duration(15, TimeUnit.SECONDS);
    private final String application_name;
    private final Map<Scanner<? extends T>, ScheduledFuture<?>> scheduled_scanners;
    private final ScheduledThreadPoolExecutor scanner_executor;
    private final ExecutorService concurrent_scanner_executor;

    private final AutoKillScanner<T> auto_kill_scanner;
    private final AutoDeployScanner<T> auto_deploy_scanner;
    private final AutoRemoveScanner<T> auto_remove_scanner;
    private final StatusScanner<T> status_scanner;

    public AbstractDeployableNetwork(final String application_name) {

        this.application_name = application_name;
        scheduled_scanners = new HashMap<Scanner<? extends T>, ScheduledFuture<?>>();
        scanner_executor = new ScheduledThreadPoolExecutor(DEFAULT_SCANNER_EXECUTOR_THREAD_POOL_SIZE);
        concurrent_scanner_executor = Executors.newCachedThreadPool();

        auto_kill_scanner = new AutoKillScanner<T>(DEFAULT_SCANNER_CYCLE_DELAY, DEFAULT_SCANNER_CYCLE_TIMEOUT);
        auto_deploy_scanner = new AutoDeployScanner<T>(DEFAULT_SCANNER_CYCLE_DELAY);
        auto_remove_scanner = new AutoRemoveScanner<T>(DEFAULT_SCANNER_CYCLE_DELAY);
        status_scanner = new StatusScanner<T>(DEFAULT_SCANNER_CYCLE_DELAY);

        addScanner(auto_kill_scanner);
        addScanner(auto_deploy_scanner);
        addScanner(auto_remove_scanner);
        addScanner(status_scanner);
    }

    @Override
    public void deployAll() {

        for (final T applciation_descriptor : this) {

            try {
                deploy(applciation_descriptor);
            }
            catch (final IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            catch (final TimeoutException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    @Override
    public void killAllOnHost(final Host host) {

        for (final T applciation_descriptor : this) {
            if (applciation_descriptor.getHost().equals(host)) {
                kill(applciation_descriptor);
            }
        }
    }

    @Override
    public void killAll() {

        for (final T applciation_descriptor : this) {
            kill(applciation_descriptor);
        }
    }

    @Override
    public void kill(final T member) {

        final Iterator<Process> process_iterator = member.getProcesses().iterator();
        while (process_iterator.hasNext()) {
            final Process process = process_iterator.next();
            process.destroy();
        }
    }

    @Override
    public String getName() {

        return application_name;
    }

    @Override
    public void awaitAnyOfStates(final ApplicationState... states) throws InterruptedException {

        //TODO tidy this up

        final Iterator<T> application_descriptors = iterator();
        final List<CountDownLatch> latches = new ArrayList<CountDownLatch>();
        while (application_descriptors.hasNext()) {
            final DefaultApplicationDescriptor application_descriptor = application_descriptors.next();
            //Create a latch per listener since we cannot know the number of application descriptors
            final CountDownLatch latch = new CountDownLatch(1);

            if (!application_descriptor.isInState(states)) {
                final PropertyChangeListener state_change = new PropertyChangeListener() {

                    @Override
                    public void propertyChange(final PropertyChangeEvent evt) {

                        if (evt.getPropertyName().equals(DefaultApplicationDescriptor.STATE_PROPERTY_NAME)) {
                            if (application_descriptor.isInState(states)) {
                                latch.countDown();
                                application_descriptor.removePropertyChangeListener(DefaultApplicationDescriptor.STATE_PROPERTY_NAME, this);
                            }
                        }
                    }
                };
                application_descriptor.addPropertyChangeListener(DefaultApplicationDescriptor.STATE_PROPERTY_NAME, state_change);
                latches.add(latch);
            }
        }

        for (final CountDownLatch latch : latches) {
            latch.await();
        }
    }

    public boolean addScanner(final AbstractScanner<T> scanner) {

        synchronized (scheduled_scanners) {
            return isAddable(scanner) ? injectSelfThenAdd(scanner) : false;
        }
    }

    public boolean addScanner(final AbstractConcurrentScanner<T> scanner) {

        synchronized (scheduled_scanners) {
            return isAddable(scanner) ? injectSelfAndExecutorThenAdd(scanner) : false;
        }
    }

    @Override
    public boolean addScanner(final Scanner<? extends T> scanner) {

        synchronized (scheduled_scanners) {
            return isAddable(scanner) ? add(scanner) : false;
        }
    }

    private boolean injectSelfAndExecutorThenAdd(final AbstractConcurrentScanner<T> scanner) {

        scanner.injectExecutorService(concurrent_scanner_executor);
        return injectSelfThenAdd(scanner);
    }

    private boolean injectSelfThenAdd(final AbstractScanner<T> scanner) {

        scanner.setNetwork(this);
        return add(scanner);
    }

    private boolean add(final Scanner<? extends T> scanner) {

        final ScheduledFuture<?> scheduled_scanner = scheduleScanner(scanner);
        return scheduled_scanners.put(scanner, scheduled_scanner) == null;
    }

    private boolean isAddable(final Scanner<? extends ApplicationDescriptor> scanner) {

        return !scheduled_scanners.containsKey(scanner);
    }

    @Override
    public boolean removeScanner(final Scanner<? extends T> scanner) {

        final ScheduledFuture<?> scheduled_scanner;
        synchronized (scheduled_scanners) {
            scheduled_scanner = scheduled_scanners.remove(scanner);
        }
        return scheduled_scanner != null && scheduled_scanner.cancel(true);
    }

    @Override
    public void setScanEnabled(final boolean enabled) {

        synchronized (scheduled_scanners) {
            for (final Scanner<? extends T> scanner : scheduled_scanners.keySet()) {
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

    private ScheduledFuture<?> scheduleScanner(@SuppressWarnings("rawtypes") final Scanner scanner) {

        final Duration cycle_delay = scanner.getCycleDelay();
        final long cycle_delay_length = cycle_delay.getLength();
        return scanner_executor.scheduleWithFixedDelay(new Runnable() {

            @SuppressWarnings("unchecked")
            @Override
            public void run() {

                scanner.scan();
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

        for (final T application_descriptor : this) {
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
