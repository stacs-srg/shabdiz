package uk.ac.standrews.cs.shabdiz.evaluation;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.LoggerFactory;
import uk.ac.standrews.cs.shabdiz.util.ProcessUtil;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class ExperiementRunner {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ExperiementRunner.class);
    private static final Map<Class, Collection<Object[]>> EXPERIMENTS = new HashMap<Class, Collection<Object[]>>();
    static {

        EXPERIMENTS.put(ChordRunningToRunningAfterKillExperiment.class, ChordRunningToRunningAfterKillExperiment.getParameters());
        EXPERIMENTS.put(RunningToAuthAfterKillExperiment.class, RunningToAuthAfterKillExperiment.getParameters());
        EXPERIMENTS.put(RunningToRunningAfterKillExperiment.class, RunningToRunningAfterKillExperiment.getParameters());
        EXPERIMENTS.put(RunningToRunningAfterResetExperiment.class, RunningToRunningAfterResetExperiment.getParameters());
    }

    public static void main(String[] args) throws Exception {

        int experiment = 0;
        int j = 0;
        if (args.length == 0) {
            for (Map.Entry<Class, Collection<Object[]>> entry : EXPERIMENTS.entrySet()) {

                final Class exp_class = entry.getKey();

                for (Object[] o : entry.getValue()) {
                    System.out.println();
                    System.out.println("executing " + exp_class + " param: " + j);
                    System.out.println();
                    ProcessBuilder builder = new ProcessBuilder("bash", "-c", "java -jar experiments.jar " + exp_class.getName() + " " + j);
                    builder.redirectErrorStream(true);
                    final Process start = builder.start();
                    final int exit_value = start.waitFor();
                    start.destroy();
                    System.out.println();
                    if (exit_value == 0) {
                        System.out.println("NORMAL");
                    }
                    else {
                        System.out.println("FAILED ");
                    }
                    System.out.println("DONE executing " + entry.getKey() + " param: " + j);
                    System.out.println();

                    System.out.println("Cleanining up");
                    cleanup();

                    j++;
                    System.out.println("experiment " + experiment);
                    experiment++;
                }

                j = 0;

            }
        }
        else if (args.length == 2) {
            String class_name = args[0];
            int param_index = Integer.parseInt(args[1]);
            try {
                final Class<?> experiment_class = Class.forName(class_name);
                final Collection<Object[]> exp_args = EXPERIMENTS.get(experiment_class);
                for (final Object[] constructor_args : exp_args) {
                    if (j == param_index) {

                        final ExecutorService executorService = Executors.newSingleThreadExecutor();

                        try {
                            executorService.submit(new Callable<Object>() {

                                @Override
                                public Object call() throws Exception {

                                    final Constructor constructor = experiment_class.getDeclaredConstructors()[0];
                                    final Experiment experiment1 = (Experiment) constructor.newInstance(constructor_args);
                                    experiment1.setUp();
                                    experiment1.doExperiment();
                                    experiment1.tearDown();
                                    return null;
                                }
                            }).get(15, TimeUnit.MINUTES);
                        }
                        catch (TimeoutException e) {
                            LOGGER.error(" time out " + class_name + " " + param_index, e);
                            System.out.println(">>>>> time out " + class_name + " " + param_index);
                        }
                        finally {
                            executorService.shutdownNow();
                        }
                        System.exit(0);
                    }
                    j++;
                }
            }
            catch (Throwable e) {
                LOGGER.error("failed to execute " + class_name + "   " + param_index, e);
                System.exit(1);
            }
        }
    }

    private static void cleanup() throws IOException, InterruptedException {

        ProcessBuilder builder = new ProcessBuilder("bash", "-c", "rocks run host \"killall java\";");
        builder.redirectErrorStream(true);
        final Process start = builder.start();
        ProcessUtil.awaitNormalTerminationAndGetOutput(start);
        start.waitFor();
        start.destroy();
    }
}
