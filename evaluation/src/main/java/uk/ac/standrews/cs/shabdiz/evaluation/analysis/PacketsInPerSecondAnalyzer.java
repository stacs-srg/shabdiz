package uk.ac.standrews.cs.shabdiz.evaluation.analysis;

import java.io.File;

import static uk.ac.standrews.cs.shabdiz.evaluation.analysis.AnalyticsUtil.getFilesByName;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class PacketsInPerSecondAnalyzer extends GaugeCsvAnalyzer {

    public static final String NAME = "Packets received per second";
    static final String GAUGE_CSV = "ganglia_packets_in.csv";

    public PacketsInPerSecondAnalyzer(File results_path) {

        super(NAME, getFilesByName(results_path, GAUGE_CSV), NAME, NAME);
    }
}
