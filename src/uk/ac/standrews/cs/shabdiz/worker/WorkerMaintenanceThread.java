package uk.ac.standrews.cs.shabdiz.worker;

import java.io.Serializable;
import java.util.SortedSet;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import uk.ac.standrews.cs.nds.util.Duration;

class WorkerMaintenanceThread extends Thread {

    private static final Duration MAINTENANCE_LOOP_DELAY = new Duration(5, TimeUnit.SECONDS);

    private final WorkerImpl node;

    WorkerMaintenanceThread(final WorkerImpl node) {

        this.node = node;
    }

    @Override
    public void run() {

        while (!Thread.currentThread().isInterrupted()) {

            maintainFutureResults();

            try {
                MAINTENANCE_LOOP_DELAY.sleep();
            }
            catch (final InterruptedException e) {
                interrupt();
            }
        }
    }

    public void shutdown() {

        interrupt();
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    private void maintainFutureResults() {

        final SortedSet<UUID> job_ids = node.getJobIds();

        for (final UUID job_id : job_ids) {

            final Future<? extends Serializable> future_result = node.getFutureById(job_id);
            maintainFutureResult(job_id, future_result);
        }
    }

    private void maintainFutureResult(final UUID job_id, final Future<? extends Serializable> future_result) {

        if (future_result == null) { return; }

        if (future_result.isDone()) {

            try {

                final Serializable result = future_result.get();
                node.handleCompletion(job_id, result);
            }
            catch (final Exception e) {

                node.handleException(job_id, e);
            }
        }
    }
}
