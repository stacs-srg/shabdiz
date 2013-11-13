package uk.ac.standrews.cs.shabdiz.evaluation.analysis;

import java.io.File;

import static uk.ac.standrews.cs.shabdiz.evaluation.analysis.AnalyticsUtil.getFilesByName;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class PacketsInPerSecondAnalyzer extends GaugeCsvAnalyzer {

    public static final String PACKETS_RECEIVED_PER_SECOND = "Packets received per second";
    static final String GAUGE_CSV = "ganglia_packets_in.csv";

    public PacketsInPerSecondAnalyzer(File results_path) {

        super(PACKETS_RECEIVED_PER_SECOND, getFilesByName(results_path, GAUGE_CSV), PACKETS_RECEIVED_PER_SECOND, PACKETS_RECEIVED_PER_SECOND);
    }
}
