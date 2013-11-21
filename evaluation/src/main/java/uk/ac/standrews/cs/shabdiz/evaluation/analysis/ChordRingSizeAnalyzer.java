package uk.ac.standrews.cs.shabdiz.evaluation.analysis;

import java.io.File;

import static uk.ac.standrews.cs.shabdiz.evaluation.analysis.AnalyticsUtil.getFilesByName;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class ChordRingSizeAnalyzer extends GaugeCsvAnalyzer {

    public static final String RING_SIZE = "Ring Size";
    static final String GAUGE_CSV = "ring_size_gauge.csv";

    public ChordRingSizeAnalyzer(File results_path) {

        super(RING_SIZE, getFilesByName(results_path, GAUGE_CSV), RING_SIZE, RING_SIZE);
    }
}
