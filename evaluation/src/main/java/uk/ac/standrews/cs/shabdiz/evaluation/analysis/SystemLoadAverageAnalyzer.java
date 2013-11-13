package uk.ac.standrews.cs.shabdiz.evaluation.analysis;

import java.io.File;

import static uk.ac.standrews.cs.shabdiz.evaluation.analysis.AnalyticsUtil.getFilesByName;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class SystemLoadAverageAnalyzer extends GaugeCsvAnalyzer {

    public static final String ONE_MINUTE_SYSTEM_LOAD_AVERAGE = "One Minute System Load Average";
    static final String GAUGE_CSV = "system_load_average_gauge.csv";

    public SystemLoadAverageAnalyzer(File results_path) {

        super(ONE_MINUTE_SYSTEM_LOAD_AVERAGE, getFilesByName(results_path, GAUGE_CSV), ONE_MINUTE_SYSTEM_LOAD_AVERAGE, ONE_MINUTE_SYSTEM_LOAD_AVERAGE);
    }
}
