package uk.ac.standrews.cs.shabdiz.jobs;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class WorkerJsonFactory extends JsonFactory {

    private static final long serialVersionUID = 15401382709188608L;
    private static ObjectMapper worker_object_mapper;
    static {
        worker_object_mapper = new ObjectMapper();
        final Module worker_module = new WorkerModule();
        worker_object_mapper.registerModule(worker_module);
    }

    private static WorkerJsonFactory WORKER_JSON_FACTORY_INSTANCE = new WorkerJsonFactory();

    private WorkerJsonFactory() {

        super(worker_object_mapper);
    }

    public static WorkerJsonFactory getInstance() {

        return WORKER_JSON_FACTORY_INSTANCE;
    }

}
