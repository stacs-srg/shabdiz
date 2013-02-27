package uk.ac.standrews.cs.shabdiz.api;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import uk.ac.standrews.cs.nds.util.Duration;

public abstract class AbstractApplicationNetwork extends ConcurrentSkipListSet<SimpleApplicationDescriptor> implements ApplicationNetwork<SimpleApplicationDescriptor> {

    private final String application_name;
    private final ConcurrentSkipListSet<Scanner> scanners;
    private final ConcurrentSkipListMap<Scanner, ScheduledFuture<?>> scheduled_scanners;
    private final ScheduledThreadPoolExecutor scanner_executor;

    public AbstractApplicationNetwork(final String application_name) {

        this.application_name = application_name;
        scanners = new ConcurrentSkipListSet<Scanner>();
        scheduled_scanners = new ConcurrentSkipListMap<Scanner, ScheduledFuture<?>>();
        scanner_executor = new ScheduledThreadPoolExecutor(5);
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
    public void awaitAnyState(final State... states) throws InterruptedException {

        //TODO tidy this up

        final Iterator<SimpleApplicationDescriptor> application_descriptors = iterator();
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

    @Override
    public boolean addScanner(final Scanner scanner) {

        if (!scheduled_scanners.containsKey(scanner)) {
            final Duration cycle_delay = scanner.getCycleDelay();
            final long cycle_delay_length = cycle_delay.getLength();
            final ScheduledFuture<?> scheduled_scanner = scanner_executor.scheduleWithFixedDelay(new Runnable() {

                @Override
                public void run() {

                    //                    scanner.scan(iterator());
                }
            }, cycle_delay_length, cycle_delay_length, cycle_delay.getTimeUnit());

            scheduled_scanners.put(scanner, scheduled_scanner);
        }
        return scanners.add(scanner);
    }

    @Override
    public boolean removeScanner(final Scanner scanner) {

        return scanners.remove(scanner);
    }

    @Override
    public void setScanEnabled(final boolean enabled) {

        for (final Scanner scanner : scanners) {
            scanner.setEnabled(enabled);
        }
    }
}
