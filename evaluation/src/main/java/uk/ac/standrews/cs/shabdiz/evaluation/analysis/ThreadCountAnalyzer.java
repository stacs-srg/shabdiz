package uk.ac.standrews.cs.shabdiz.evaluation.analysis;

import java.io.File;

import static uk.ac.standrews.cs.shabdiz.evaluation.analysis.AnalyticsUtil.getFilesByName;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class ThreadCountAnalyzer extends GaugeCsvAnalyzer {

    public static final String NAME = "Number of Threads";
    static final String GAUGE_CSV = "thread_count_gauge.csv";

    public ThreadCountAnalyzer(File results_path) {

        super(NAME, getFilesByName(results_path, GAUGE_CSV), NAME, NAME);
    }
}
