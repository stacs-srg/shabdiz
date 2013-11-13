package uk.ac.standrews.cs.shabdiz.evaluation.analysis;

import java.io.File;
import org.supercsv.cellprocessor.ParseDouble;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.util.CsvContext;

import static uk.ac.standrews.cs.shabdiz.evaluation.analysis.AnalyticsUtil.getFilesByName;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class BytesInPerSecondAnalyzer extends GaugeCsvAnalyzer {

    private static final String BYTES_RECEIVED_PER_SECOND = "Kilobytes received per second";
    static final String GAUGE_CSV = "ganglia_bytes_in.csv";
    static final ConvertBytesToKiloBytes CONVERT_BYTES_TO_KILO_BYTES = new ConvertBytesToKiloBytes();

    public BytesInPerSecondAnalyzer(File results_path) {

        super(BYTES_RECEIVED_PER_SECOND, getFilesByName(results_path, GAUGE_CSV), BYTES_RECEIVED_PER_SECOND, BYTES_RECEIVED_PER_SECOND);
    }

    @Override
    protected CellProcessor getValueCellProcessor() {

        return CONVERT_BYTES_TO_KILO_BYTES;
    }

    private static class ConvertBytesToKiloBytes extends ParseDouble {

        @Override
        public Object execute(final Object value, final CsvContext context) {

            final Double bytes = (Double) super.execute(value, context);
            return bytes / 1000;
        }
    }
}
