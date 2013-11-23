package uk.ac.standrews.cs.shabdiz.evaluation.analysis;

import java.io.File;

import static uk.ac.standrews.cs.shabdiz.evaluation.analysis.AnalyticsUtil.getFilesByName;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class SystemLoadAverageAnalyzer extends GaugeCsvAnalyzer {

    public static final String NAME = "One Minute System Load Average";
    static final String GAUGE_CSV = "system_load_average_gauge.csv";

    public SystemLoadAverageAnalyzer(File results_path) {

        super(NAME, getFilesByName(results_path, GAUGE_CSV), NAME, NAME);
    }
}
