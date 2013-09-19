package uk.ac.standrews.cs.shabdiz.evaluation;

import java.util.NoSuchElementException;
import java.util.Properties;
import org.junit.internal.runners.model.EachTestNotifier;
import org.junit.runner.Result;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.Parameterized;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.host.LocalHost;
import uk.ac.standrews.cs.shabdiz.host.exec.AgentBasedJavaProcessBuilder;
import uk.ac.standrews.cs.shabdiz.util.Duration;

public class Parallelized extends Parameterized {

    static final String TEST_PARAM_INDEX = "param.indecies";
    private final Integer target_parameters_index;

    /** Only called reflectively. Do not use programmatically. */
    public Parallelized(final Class<?> klass) throws Throwable {

        super(klass);
        final String index_as_string = System.getProperty(TEST_PARAM_INDEX);
        target_parameters_index = index_as_string != null ? Integer.valueOf(index_as_string) : null;
    }

    @Override
    protected void runChild(final Runner runner, final RunNotifier notifier) {

        final int index = getRunnerIndex(runner);
        if (target_parameters_index != null) {
            if (target_parameters_index == index) {
                super.runChild(runner, notifier);
            }
        }
        else {
            EachTestNotifier eachNotifier = new EachTestNotifier(notifier, runner.getDescription());
            AgentBasedJavaProcessBuilder builder = new AgentBasedJavaProcessBuilder();
            builder.setMainClass(JUnitBootstrapCore.class);
            builder.addCurrentJVMClasspath();
            //                builder.addMavenDependency(Constants.CS_GROUP_ID, "shabdiz-evaluation", Constants.SHABDIZ_VERSION);
            builder.addJVMArgument("-D" + TEST_PARAM_INDEX + "=" + index);
            builder.addJVMArgument("-Xmx1024m");
            builder.addJVMArgument("-XX:MaxPermSize=256m");
            //            builder.setWorkingDirectory(System.getProperty("user.dir", "."));
            builder.setDeleteWorkingDirectoryOnExit(true);
            final Host host;
            try {
                host = new LocalHost();
                eachNotifier.fireTestStarted();
                final Process start = builder.start(host, getTestClass().getJavaClass().getName());
                final Properties properties = JUnitBootstrapCore.readProperties(JUnitBootstrapCore.class, start, Duration.MAX_DURATION);
                final Result result = JUnitBootstrapCore.getResultProperty(properties);
                if (result.wasSuccessful()) {
                    eachNotifier.fireTestFinished();
                }
                else {
                    for (Failure failure : result.getFailures()) {
                        eachNotifier.addFailure(failure.getException());
                    }
                }
            }
            catch (Exception e) {
                eachNotifier.addFailure(e);
                e.printStackTrace();
            }
        }
    }

    private int getRunnerIndex(final Runner runner) {

        int i = 0;
        for (Runner r : getChildren()) {
            if (runner.equals(r)) { return i; }
            i++;
        }
        throw new NoSuchElementException();
    }
}
