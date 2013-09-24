package uk.ac.standrews.cs.shabdiz.evaluation;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class ExperiementRunner {

    private static final Map<Class, Collection<Object[]>> EXPERIMENTS = new HashMap<Class, Collection<Object[]>>();
    static {

        EXPERIMENTS.put(ChordRunningToRunningAfterKillExperiment.class, ChordRunningToRunningAfterKillExperiment.getParameters());
        EXPERIMENTS.put(RunningToAuthAfterKillExperiment.class, RunningToAuthAfterKillExperiment.getParameters());
        EXPERIMENTS.put(RunningToRunningAfterKillExperiment.class, RunningToRunningAfterKillExperiment.getParameters());
        EXPERIMENTS.put(RunningToRunningAfterResetExperiment.class, RunningToRunningAfterResetExperiment.getParameters());

    }

    public static void main(String[] args) throws Exception {

        int experiment = 0;
        int i = 0, j = 0;
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

                i++;
            }
        }
        else if (args.length == 2) {
            try {
                String class_name = args[0];
                int param_index = Integer.parseInt(args[1]);
                final Class<?> experiment_class = Class.forName(class_name);
                final Collection<Object[]> exp_args = EXPERIMENTS.get(experiment_class);
                for (Object[] constructor_args : exp_args) {
                    if (j == param_index) {
                        final Constructor constructor = experiment_class.getDeclaredConstructors()[0];
                        final Experiment experiment1 = (Experiment) constructor.newInstance(constructor_args);
                        experiment1.setUp();
                        experiment1.doExperiment();
                        experiment1.tearDown();
                        System.exit(0);
                    }
                    j++;
                }
            }
            catch (Throwable e) {
                System.exit(1);
            }
        }
    }

    private static void cleanup() throws IOException, InterruptedException {

        ProcessBuilder builder = new ProcessBuilder("bash", "-c", "rocks run host \"killall java\";");
        builder.redirectErrorStream(true);
        final Process start = builder.start();
        start.waitFor();
        start.destroy();
    }
}
