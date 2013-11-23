package uk.ac.standrews.cs.shabdiz.evaluation.analysis;

import java.io.File;
import org.supercsv.cellprocessor.ParseDouble;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.util.CsvContext;

import static uk.ac.standrews.cs.shabdiz.evaluation.analysis.AnalyticsUtil.getFilesByName;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class KilobytesInPerSecondAnalyzer extends GaugeCsvAnalyzer {

     static final String NAME = "Kilobytes received per second";
    static final String GAUGE_CSV = "ganglia_bytes_in.csv";
    static final ConvertBytesToKiloBytes CONVERT_BYTES_TO_KILO_BYTES = new ConvertBytesToKiloBytes();

    public KilobytesInPerSecondAnalyzer(File results_path) {

        super(NAME, getFilesByName(results_path, GAUGE_CSV), NAME, NAME);
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
