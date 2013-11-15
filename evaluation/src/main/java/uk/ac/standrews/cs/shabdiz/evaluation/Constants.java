package uk.ac.standrews.cs.shabdiz.evaluation;

import java.util.concurrent.TimeUnit;
import javax.inject.Provider;
import uk.ac.standrews.cs.shabdiz.evaluation.util.BlubHostProvider;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.util.Duration;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public final class Constants {

    public static final int MAX_RETRY_COUNT = 5;
    public static final Integer[] ALL_KILL_PORTIONS = {10, 30, 50, 70, 90}; // percent
    public static final Integer[] KILL_PORTION_50 = {50}; // percent
    public static final Integer[] ALL_SCHEDULER_THREAD_POOL_SIZES = {10, 100};
    public static final Integer[] SCHEDULER_THREAD_POOL_SIZE_10 = {10};
    public static final Integer[] ALL_CONCURRENT_SCANNER_THREAD_POOL_SIZES = {1, 5, 10, 15, 20, Integer.MAX_VALUE};
    public static final Integer[] CONCURRENT_SCANNER_THREAD_POOL_SIZE_5_AND_MAX = {5, Integer.MAX_VALUE};
    public static final Integer[] ALL_NETWORK_SIZES = {10, 20, 30, 40, 48};
    public static final Integer[] NETWORK_SIZE_48 = {48};
    public static final Duration[] SCANNER_INTERVAL_1_SECOND = {new Duration(1, TimeUnit.SECONDS)};
    public static final Duration[] SCANNER_TIMEOUT_5_MINUTE = {new Duration(5, TimeUnit.MINUTES)};
    public static final Duration REPORT_INTERVAL = new Duration(5, TimeUnit.SECONDS);
    public static final ChordManager[] CHORD_APPLICATION_MANAGERS = {ChordManager.FILE_BASED_COLD, ChordManager.FILE_BASED_WARM, ChordManager.URL_BASED, ChordManager.MAVEN_BASED_COLD, ChordManager.MAVEN_BASED_WARM};
    public static final String TIME_TO_REACH_AUTH_START = "time_to_reach_auth.start_nanos";
    public static final String TIME_TO_REACH_AUTH_DURATION = "time_to_reach_auth.duration_nanos";
    public static final String TIME_TO_REACH_RUNNING_START = "time_to_reach_running.start_nanos";
    public static final String TIME_TO_REACH_RUNNING_DURATION = "time_to_reach_running.duration_nanos";
    public static final String TIME_TO_REACH_RUNNING_AFTER_KILL_START = "time_to_reach_running_after_kill.start_nanos";
    public static final String TIME_TO_REACH_RUNNING_AFTER_KILL_DURATION = "time_to_reach_running_after_kill.duration_nanos";
    public static final String TIME_TO_REACH_AUTH_AFTER_KILL_START = "time_to_reach_auth_from_running.start_nanos";
    public static final String TIME_TO_REACH_AUTH_AFTER_KILL_DURATION = "time_to_reach_auth_from_running.duration_nanos";
    public static final String TIME_TO_REACH_STABILIZED_RING_START = "time_to_reach_stabilized_ring.start_nanos";
    public static final String TIME_TO_REACH_STABILIZED_RING_DURATION = "time_to_reach_stabilized_ring.duration_nanos";
    public static final String TIME_TO_REACH_STABILIZED_RING_AFTER_KILL_START = "time_to_reach_stabilized_ring_after_kill.start_nanos";
    public static final String TIME_TO_REACH_STABILIZED_RING_AFTER_KILL_DURATION = "time_to_reach_stabilized_ring_after_kill.duration_nanos";
    public static final String CHORD_JOIN_TIMEOUT = "chord.join.timeout";
    public static final String CHORD_JOIN_RANDOM_SEED = "chord.join.random_seed";
    public static final String CHORD_JOIN_RETRY_INTERVAL = "chord.join.retry_interval";
    public static final String SCANNER_INTERVAL_PROPERTY = "scanner.interval";
    public static final String SCANNER_TIMEOUT_PROPERTY = "scanner.timeout";
    public static final String SCANNER_SCHEDULER_THREAD_POOL_SIZE_PROPERTY = "scanner.scheduler.thread_pool_size";
    public static final String CONCURRENT_SCANNER_THREAD_POOL_SIZE_PROPERTY = "concurrent_scanner.thread_pool_size";
    public static final String KILL_PORTION_RANDOM_SEED = "kill_portion_random.seed";
    public static final String EXPERIMENT_STATUS_PROPERTY = "status";
    public static final String KILL_PORTION_PROPERTY = "kill_portion";
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILURE = "FAILURE";
    public static final String EXPERIMENT_FAILURE_CAUSE = "failure.cause";
    public static final String EXPERIMENT_DURATION_NANOS = "experiment.duration_nanos";
    public static final String EXPERIMENT_START_TIME_NANOS = "experiment.start_time_nanos";
    public static final String USER_PROPERTY = "user";
    public static final String NETWORK_SIZE_PROPERTY = "network_size";
    public static final String MANAGER_PROPERTY = "manager";
    public static final String HOST_PROVIDER_PROPERTY = "host_provider";
    public static final String WORKING_DIRECTORY_PROPERTY = "working_directory";
    public static final String REPORT_INTERVAL_PROPERTY = "report_interval";
    public static final String PROPERTIES_FILE_NAME = "experiment.properties";
    public static final int EXPERIMENT_TIMEOUT = 1000 * 60 * 30; // 30 minutes timeout for an experiment
    public static final int REPETITIONS = 5;
    public static final Provider<Host>[] BLUB_HOST_PROVIDER = new Provider[]{new BlubHostProvider()};
    public static final ExperimentManager[] CHORD_MANAGER_FILE_WARM = {ChordManager.FILE_BASED_WARM};
    public static final ExperimentManager[] ECHO_FILE_WARM_MANAGERS = {EchoManager.FILE_BASED_WARM};
    //@formatter:off
    public static final ExperimentManager[] CHORD_ECHO_HELLO_WORLD_FILE_WARM_MANAGERS = {
            ChordManager.FILE_BASED_WARM,
            EchoManager.FILE_BASED_WARM,
            HelloWorldManager.FILE_BASED_WARM
    }                                    ;
    public static final ExperimentManager[] ALL_MANAGERS_FILE_COLD = {
            ChordManager.FILE_BASED_COLD,
            EchoManager.FILE_BASED_COLD,
            ChordManager.FILE_BASED_COLD,
            HelloWorldManager.FILE_BASED_COLD,
            HelloWorldManager.FILE_BASED_COLD_2M,
            HelloWorldManager.FILE_BASED_COLD_4M,
            HelloWorldManager.FILE_BASED_COLD_8M,
            HelloWorldManager.FILE_BASED_COLD_16M

//            HelloWorldManager.FILE_BASED_COLD_32M,
//            HelloWorldManager.FILE_BASED_COLD_64M,

    };
    public static final ExperimentManager[] ALL_MANAGERS = {

            ChordManager.FILE_BASED_WARM, ChordManager.FILE_BASED_COLD,
            ChordManager.URL_BASED,
            ChordManager.MAVEN_BASED_WARM, ChordManager.MAVEN_BASED_COLD,

            EchoManager.FILE_BASED_WARM, EchoManager.FILE_BASED_COLD,
            EchoManager.URL_BASED,
            EchoManager.MAVEN_BASED_WARM, EchoManager.MAVEN_BASED_COLD,

            HelloWorldManager.FILE_BASED_WARM, HelloWorldManager.FILE_BASED_COLD,
            HelloWorldManager.URL_BASED,
            HelloWorldManager.MAVEN_BASED_WARM, HelloWorldManager.MAVEN_BASED_COLD,

            HelloWorldManager.FILE_BASED_WARM_2M, HelloWorldManager.FILE_BASED_COLD_2M,
            HelloWorldManager.URL_BASED_2M,
            HelloWorldManager.MAVEN_BASED_WARM_2M, HelloWorldManager.MAVEN_BASED_COLD_2M,

            HelloWorldManager.FILE_BASED_WARM_4M, HelloWorldManager.FILE_BASED_COLD_4M,
            HelloWorldManager.URL_BASED_4M,
            HelloWorldManager.MAVEN_BASED_WARM_4M, HelloWorldManager.MAVEN_BASED_COLD_4M,

            HelloWorldManager.FILE_BASED_WARM_8M, HelloWorldManager.FILE_BASED_COLD_8M,
            HelloWorldManager.URL_BASED_8M,
            HelloWorldManager.MAVEN_BASED_WARM_8M, HelloWorldManager.MAVEN_BASED_COLD_8M,

            HelloWorldManager.FILE_BASED_WARM_16M, HelloWorldManager.FILE_BASED_COLD_16M,
            HelloWorldManager.URL_BASED_16M,
            HelloWorldManager.MAVEN_BASED_WARM_16M, HelloWorldManager.MAVEN_BASED_COLD_16M

//            HelloWorldManager.FILE_BASED_WARM_32M, HelloWorldManager.FILE_BASED_COLD_32M,
//            HelloWorldManager.URL_BASED_32M,
//            HelloWorldManager.MAVEN_BASED_WARM_32M, HelloWorldManager.MAVEN_BASED_COLD_32M,
//
//            HelloWorldManager.FILE_BASED_WARM_64M, HelloWorldManager.FILE_BASED_COLD_64M,
//            HelloWorldManager.URL_BASED_64M,
//            HelloWorldManager.MAVEN_BASED_WARM_64M, HelloWorldManager.MAVEN_BASED_COLD_64M

    };
    public static final Duration[] ALL_SCANNER_INTERVALS = {
            new Duration(1, TimeUnit.NANOSECONDS),
            new Duration(1, TimeUnit.SECONDS),
            new Duration(3, TimeUnit.SECONDS),
            new Duration(5, TimeUnit.SECONDS),
            new Duration(7, TimeUnit.SECONDS),
            new Duration(9, TimeUnit.SECONDS),
            new Duration(20, TimeUnit.SECONDS)
    };
    
    public static final Duration[] ALL_SCANNER_TIMEOUTS = {
            new Duration(30, TimeUnit.SECONDS),
            new Duration(1, TimeUnit.MINUTES),
            new Duration(5, TimeUnit.MINUTES),
            new Duration(10, TimeUnit.MINUTES),
    };

    private Constants() {

    }
}
