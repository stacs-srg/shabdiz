package uk.ac.standrews.cs.shabdiz.evaluation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mashti.gauge.Gauge;
import org.mashti.gauge.Metric;
import org.mashti.gauge.MetricRegistry;
import org.mashti.gauge.reporter.CsvReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.ApplicationNetwork;
import uk.ac.standrews.cs.shabdiz.ApplicationState;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
@RunWith(value = Parameterized.class)
public abstract class Experiment {

    private static final Logger LOGGER = LoggerFactory.getLogger(Experiment.class);
    private final MetricRegistry registry;
    private final CsvReporter reporter;
    private final StateCountGauge auth_state_gauge;
    private final StateCountGauge unknown_state_gauge;
    private final StateCountGauge running_state_gauge;
    private final StateCountGauge other_state_gauge;
    private final File observations_directory;
    private final String name;

    public Experiment(Integer network_size) throws IOException {

        name = getClass().getSimpleName() + "_" + network_size;
        observations_directory = new File(name + "_" + System.currentTimeMillis());
        FileUtils.forceMkdir(observations_directory);
        auth_state_gauge = new StateCountGauge(ApplicationState.AUTH);
        running_state_gauge = new StateCountGauge(ApplicationState.RUNNING);
        unknown_state_gauge = new StateCountGauge(ApplicationState.UNKNOWN);
        other_state_gauge = new StateCountGauge(ApplicationState.DEPLOYED, ApplicationState.INVALID, ApplicationState.KILLED, ApplicationState.LAUNCHED, ApplicationState.NO_AUTH, ApplicationState.UNREACHABLE);

        registry = new MetricRegistry(getClass().getSimpleName());
        reporter = new CsvReporter(registry, observations_directory);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {

        final List<Object[]> parameters = new ArrayList<Object[]>();
        parameters.add(new Object[]{10});
        parameters.add(new Object[]{20});
        parameters.add(new Object[]{30});
        parameters.add(new Object[]{40});
        parameters.add(new Object[]{48});
        return parameters;
    }

    @Before
    public void setUp() throws Exception {

        registerMetric("auth_state_gauge", auth_state_gauge);
        registerMetric("running_state_gauge", running_state_gauge);
        registerMetric("unknown_state_gauge", unknown_state_gauge);
        registerMetric("other_state_gauge", other_state_gauge);

        LOGGER.info("starting experimentation...");
        reporter.start(5, TimeUnit.SECONDS);
    }

    @Test
    @Category(Experiment.class)
    public abstract void doExperiment() throws Exception;

    @After
    public void tearDown() throws Exception {

        reporter.stop();
        getNetwork().shutdown();
        LOGGER.info("done, results are stored at {}", observations_directory);
    }

    protected void registerMetric(final String metric_name, final Metric metric) {

        registry.register(metric_name, metric);
    }

    protected abstract ApplicationNetwork getNetwork();

    protected class StateCountGauge implements Gauge<Integer> {

        private final ApplicationState[] target_states;

        StateCountGauge(ApplicationState... target_states) {

            this.target_states = target_states;
        }

        @Override
        public Integer get() {

            int count = 0;
            for (ApplicationDescriptor descriptor : getNetwork()) {

                if (descriptor.isInAnyOfStates(target_states)) {
                    count++;
                }
            }
            return count;
        }
    }

}
