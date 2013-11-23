package uk.ac.standrews.cs.shabdiz.evaluation.analysis;

import java.io.File;
import org.supercsv.cellprocessor.ift.CellProcessor;

import static uk.ac.standrews.cs.shabdiz.evaluation.analysis.AnalyticsUtil.getFilesByName;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class KilobytesOutPerSecondAnalyzer extends GaugeCsvAnalyzer {

     static final String NAME = "Kilobytes sent per second";
    static final String GAUGE_CSV = "ganglia_bytes_out.csv";

    public KilobytesOutPerSecondAnalyzer(File results_path) {

        super(NAME, getFilesByName(results_path, GAUGE_CSV), NAME, NAME);
    }

    @Override
    protected CellProcessor getValueCellProcessor() {

        return KilobytesInPerSecondAnalyzer.CONVERT_BYTES_TO_KILO_BYTES;
    }
}
