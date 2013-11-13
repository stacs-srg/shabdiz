package uk.ac.standrews.cs.shabdiz.evaluation.analysis;

import java.io.File;

import static uk.ac.standrews.cs.shabdiz.evaluation.analysis.AnalyticsUtil.getFilesByName;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class ThreadCountAnalyzer extends GaugeCsvAnalyzer {

    public static final String NUMBER_OF_THREADS = "Number of Threads";
    static final String GAUGE_CSV = "thread_count_gauge.csv";

    public ThreadCountAnalyzer(File results_path) {

        super(NUMBER_OF_THREADS, getFilesByName(results_path, GAUGE_CSV), NUMBER_OF_THREADS, NUMBER_OF_THREADS);
    }
}
