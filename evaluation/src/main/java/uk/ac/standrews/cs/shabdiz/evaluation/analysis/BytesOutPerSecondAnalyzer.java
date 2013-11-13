package uk.ac.standrews.cs.shabdiz.evaluation.analysis;

import java.io.File;
import org.supercsv.cellprocessor.ift.CellProcessor;

import static uk.ac.standrews.cs.shabdiz.evaluation.analysis.AnalyticsUtil.getFilesByName;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class BytesOutPerSecondAnalyzer extends GaugeCsvAnalyzer {

    public static final String BYTES_SENT_PER_SECOND = "Kilobytes sent per second";
    static final String GAUGE_CSV = "ganglia_bytes_out.csv";

    public BytesOutPerSecondAnalyzer(File results_path) {

        super(BYTES_SENT_PER_SECOND, getFilesByName(results_path, GAUGE_CSV), BYTES_SENT_PER_SECOND, BYTES_SENT_PER_SECOND);
    }

    @Override
    protected CellProcessor getValueCellProcessor() {

        return BytesInPerSecondAnalyzer.CONVERT_BYTES_TO_KILO_BYTES;
    }
}
