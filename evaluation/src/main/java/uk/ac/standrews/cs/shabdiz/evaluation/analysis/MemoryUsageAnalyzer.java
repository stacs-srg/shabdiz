package uk.ac.standrews.cs.shabdiz.evaluation.analysis;

import java.io.File;
import org.supercsv.cellprocessor.ParseDouble;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.util.CsvContext;

import static uk.ac.standrews.cs.shabdiz.evaluation.analysis.AnalyticsUtil.getFilesByName;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class MemoryUsageAnalyzer extends GaugeCsvAnalyzer {

    public static final ConvertByteToMegabyte CONVERT_BYTE_TO_MEGABYTE = new ConvertByteToMegabyte();
    public static final String MEMORY_USAGE = "Memory Usage";
    public static final String NAME = MEMORY_USAGE;
    static final String GAUGE_CSV = "memory_gauge.csv";
    private static final int ONE_MEGABYTE_IN_BYTES = 1000 * 1000;
    public static final String Y_AXIS_LABEL = "Memory Usage (MB)";

    public MemoryUsageAnalyzer(File results_path) {

        super(NAME, getFilesByName(results_path, GAUGE_CSV), Y_AXIS_LABEL, MEMORY_USAGE);
    }

    @Override
    protected CellProcessor getValueCellProcessor() {

        return CONVERT_BYTE_TO_MEGABYTE;
    }

    private static class ConvertByteToMegabyte extends ParseDouble {

        @Override
        public Object execute(final Object value, final CsvContext context) {

            final Double usage_bytes = (Double) super.execute(value, context);
            return usage_bytes / ONE_MEGABYTE_IN_BYTES;
        }
    }
}
