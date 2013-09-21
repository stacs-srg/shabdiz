package uk.ac.standrews.cs.shabdiz.evaluation;

import java.io.File;
import javax.inject.Provider;
import org.mashti.gauge.MetricRegistry;
import org.mashti.gauge.reporter.ScheduledReporter;
import uk.ac.standrews.cs.shabdiz.ApplicationNetwork;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.util.Duration;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class ExperimentConfiguration {

    private String name;
    private int networkSize;
    private Provider<Host> hostProvider;
    private ExperimentManager manager;
    private ApplicationNetwork network;
    private boolean cold;
    private MetricRegistry metricRegistry;
    private ScheduledReporter reporter;
    private File resultsHome;
    private int repetitions;
    private Duration reportInterval;

    public ExperimentConfiguration() {

    }
}
