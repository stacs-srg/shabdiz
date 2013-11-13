package uk.ac.standrews.cs.shabdiz.evaluation.analysis;

import java.io.File;

import static uk.ac.standrews.cs.shabdiz.evaluation.analysis.AnalyticsUtil.getFilesByName;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class PacketsOutPerSecondAnalyzer extends GaugeCsvAnalyzer {

    public static final String PACKETS_SENT_PER_SECOND = "Packets sent per second";
    static final String GAUGE_CSV = "ganglia_packets_out.csv";

    public PacketsOutPerSecondAnalyzer(File results_path) {

        super(PACKETS_SENT_PER_SECOND, getFilesByName(results_path, GAUGE_CSV), PACKETS_SENT_PER_SECOND, PACKETS_SENT_PER_SECOND);
    }
}
