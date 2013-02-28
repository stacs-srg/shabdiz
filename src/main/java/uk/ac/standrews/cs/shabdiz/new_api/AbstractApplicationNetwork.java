package uk.ac.standrews.cs.shabdiz.new_api;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
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

import uk.ac.standrews.cs.nds.util.Duration;

public abstract class AbstractApplicationNetwork<T extends SimpleApplicationDescriptor> extends ConcurrentSkipListSet<T> implements ApplicationNetwork<T> {

    private static final int DEFAULT_SCANNER_EXECUTOR_THREAD_POOL_SIZE = 5;
    private final String application_name;
    private final Map<Scanner<T>, ScheduledFuture<?>> scheduled_scanners;
    private final ScheduledThreadPoolExecutor scanner_executor;
    private final ExecutorService concurrent_scanner_executor;

    public AbstractApplicationNetwork(final String application_name) {

        this.application_name = application_name;
        scheduled_scanners = new HashMap<Scanner<T>, ScheduledFuture<?>>();
        scanner_executor = new ScheduledThreadPoolExecutor(DEFAULT_SCANNER_EXECUTOR_THREAD_POOL_SIZE);
        concurrent_scanner_executor = Executors.newCachedThreadPool();
        addScanner(new StatusScanner(new Duration(1, TimeUnit.SECONDS)));
    }

    @Override
    public void kill(final SimpleApplicationDescriptor member) {

        final Iterator<Process> process_iterator = member.getProcesses().iterator();
        while (process_iterator.hasNext()) {
            final Process process = process_iterator.next();
            process.destroy();
        }
    }

    @Override
    public String getApplicationName() {

        return application_name;
    }

    @Override
    public void awaitUniformState(final State... states) throws InterruptedException {

        //TODO tidy this up

        final Iterator<T> application_descriptors = iterator();
        final List<CountDownLatch> latches = new ArrayList<CountDownLatch>();
        while (application_descriptors.hasNext()) {
            final SimpleApplicationDescriptor application_descriptor = application_descriptors.next();
            //Create a latch per listener since we cannot know the number of application descriptors
            final CountDownLatch latch = new CountDownLatch(1);

            if (!application_descriptor.isInState(states)) {
                final PropertyChangeListener state_change = new PropertyChangeListener() {

                    @Override
                    public void propertyChange(final PropertyChangeEvent evt) {

                        if (evt.getPropertyName().equals(SimpleApplicationDescriptor.STATE_PROPERTY_NAME)) {
                            if (application_descriptor.isInState(states)) {
                                latch.countDown();
                                application_descriptor.removePropertyChangeListener(SimpleApplicationDescriptor.STATE_PROPERTY_NAME, this);
                            }
                        }
                    }
                };
                application_descriptor.addPropertyChangeListener(SimpleApplicationDescriptor.STATE_PROPERTY_NAME, state_change);
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
    public boolean addScanner(final Scanner<T> scanner) {

        synchronized (scheduled_scanners) {
            return isAddable(scanner) ? add(scanner) : false;
        }
    }

    private boolean injectSelfAndExecutorThenAdd(final AbstractConcurrentScanner<T> scanner) {

        scanner.injectExecutorService(concurrent_scanner_executor);
        return injectSelfThenAdd(scanner);
    }

    private boolean injectSelfThenAdd(final AbstractScanner<T> scanner) {

        scanner.injectApplicationNetwork(this);
        return add(scanner);
    }

    private boolean add(final Scanner<T> scanner) {

        final ScheduledFuture<?> scheduled_scanner = scheduleScanner(scanner);
        return scheduled_scanners.put(scanner, scheduled_scanner) == null;
    }

    private boolean isAddable(final Scanner<T> scanner) {

        return !scheduled_scanners.containsKey(scanner);
    }

    @Override
    public boolean removeScanner(final Scanner<T> scanner) {

        final ScheduledFuture<?> scheduled_scanner;
        synchronized (scheduled_scanners) {
            scheduled_scanner = scheduled_scanners.remove(scanner);
        }
        return scheduled_scanner != null && scheduled_scanner.cancel(true);
    }

    @Override
    public void setScanEnabled(final boolean enabled) {

        synchronized (scheduled_scanners) {
            for (final Scanner<T> scanner : scheduled_scanners.keySet()) {
                scanner.setEnabled(enabled);
            }
        }
    }

    private ScheduledFuture<?> scheduleScanner(final Scanner<T> scanner) {

        final Duration cycle_delay = scanner.getCycleDelay();
        final long cycle_delay_length = cycle_delay.getLength();
        return scanner_executor.scheduleWithFixedDelay(new Runnable() {

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
            for (final Process p : application_descriptor.getProcesses()) {
                p.destroy();
            }
            application_descriptor.getHost().shutdown();
        }

    }
}
