package uk.ac.standrews.cs.mcjob.interfaces.worker;

import java.io.Serializable;
import java.util.SortedSet;
import java.util.UUID;
import java.util.concurrent.Future;

public interface IWorkerNode {

    SortedSet<UUID> getJobIds();

    Future<? extends Serializable> getFutureById(UUID job_id);

    void handleJobCompletion(UUID job_id, Serializable result);

    void handleJobException(UUID job_id, Exception exception);
}
