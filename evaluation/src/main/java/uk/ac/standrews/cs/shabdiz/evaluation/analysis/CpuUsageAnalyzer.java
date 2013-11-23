package uk.ac.standrews.cs.shabdiz.evaluation.analysis;

import java.io.File;
import org.supercsv.cellprocessor.ParseDouble;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.util.CsvContext;

import static uk.ac.standrews.cs.shabdiz.evaluation.analysis.AnalyticsUtil.getFilesByName;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class CpuUsageAnalyzer extends GaugeCsvAnalyzer {

    static final ParseCpuUsagePercent PARSE_CPU_USAGE_PERCENT = new ParseCpuUsagePercent();
    static final String GAUGE_CSV = "cpu_gauge.csv";
    public static final String NAME = "CPU Usage";
    public static final String Y_AXIS_LABEL = "CPU Usage (%)";
    public static final String CHART_TITLE = "CPU Usage Percentage";

    public CpuUsageAnalyzer(File results_path) {

        super(NAME, getFilesByName(results_path, GAUGE_CSV), Y_AXIS_LABEL, CHART_TITLE);
        
    }

    @Override
    protected CellProcessor getValueCellProcessor() {

        return PARSE_CPU_USAGE_PERCENT;
    }

    private static class ParseCpuUsagePercent extends ParseDouble {

        @Override
        public Object execute(final Object value, final CsvContext context) {

            final Double usage = (Double) super.execute(value, context);
            return usage * 100;
        }
    }
}
