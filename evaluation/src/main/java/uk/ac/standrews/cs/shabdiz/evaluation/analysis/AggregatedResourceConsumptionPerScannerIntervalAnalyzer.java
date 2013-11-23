package uk.ac.standrews.cs.shabdiz.evaluation.analysis;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.util.CsvContext;
import uk.ac.standrews.cs.shabdiz.evaluation.Constants;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
class AggregatedResourceConsumptionPerScannerIntervalAnalyzer extends AggregatedCrossRepetitionPerPropertyAnalyzer {

    public static final CellProcessor INTEGER_CELL_PROCESSOR = new CellProcessor() {

        @Override
        public Object execute(final Object value, final CsvContext context) {

            return Integer.valueOf(value.toString());
        }
    };
    private final CellProcessor value_processor;

    private AggregatedResourceConsumptionPerScannerIntervalAnalyzer(final String name, final File results_path, final String file_name) throws IOException {

        this(name, results_path, file_name, null);
    }

    private AggregatedResourceConsumptionPerScannerIntervalAnalyzer(final String name, final File results_path, final String file_name, CellProcessor value_processor) throws IOException {

        super(name, results_path, file_name, Constants.SCANNER_INTERVAL_PROPERTY);
        this.value_processor = value_processor;
    }

    @Override
    public String getName() {

        return "Aggregated " + super.getName() + " per Scanner Interval";
    }

    @Override
    protected Comparator<? super String> getComparator() {

        return OverlaidResourceConsumptionPerScannerInterval.STRING_DURATION_COMPARATOR;
    }

    @Override
    protected CellProcessor[] getCellProcessors() {

        final CellProcessor[] cellProcessors = super.getCellProcessors();
        if (value_processor != null) {
            cellProcessors[1] = value_processor;
        }
        return cellProcessors;
    }

    static AggregatedResourceConsumptionPerScannerIntervalAnalyzer cpuUsage(File results_path) throws IOException {

        return new AggregatedResourceConsumptionPerScannerIntervalAnalyzer(CpuUsageAnalyzer.NAME, results_path, CpuUsageAnalyzer.GAUGE_CSV, CpuUsageAnalyzer.PARSE_CPU_USAGE_PERCENT);
    }

    static AggregatedResourceConsumptionPerScannerIntervalAnalyzer memoryUsage(File results_path) throws IOException {

        return new AggregatedResourceConsumptionPerScannerIntervalAnalyzer(MemoryUsageAnalyzer.NAME, results_path, MemoryUsageAnalyzer.GAUGE_CSV, MemoryUsageAnalyzer.CONVERT_BYTE_TO_MEGABYTE);
    }

    static AggregatedResourceConsumptionPerScannerIntervalAnalyzer threadCount(File results_path) throws IOException {

        return new AggregatedResourceConsumptionPerScannerIntervalAnalyzer(ThreadCountAnalyzer.NAME, results_path, ThreadCountAnalyzer.GAUGE_CSV, INTEGER_CELL_PROCESSOR);
    }

    static AggregatedResourceConsumptionPerScannerIntervalAnalyzer systemLoadAverage(File results_path) throws IOException {

        return new AggregatedResourceConsumptionPerScannerIntervalAnalyzer(SystemLoadAverageAnalyzer.NAME, results_path, SystemLoadAverageAnalyzer.GAUGE_CSV);
    }

    static AggregatedResourceConsumptionPerScannerIntervalAnalyzer kilobytesOutPerSecond(File results_path) throws IOException {

        return new AggregatedResourceConsumptionPerScannerIntervalAnalyzer(KilobytesOutPerSecondAnalyzer.NAME, results_path, KilobytesOutPerSecondAnalyzer.GAUGE_CSV, KilobytesInPerSecondAnalyzer.CONVERT_BYTES_TO_KILO_BYTES);
    }

    static AggregatedResourceConsumptionPerScannerIntervalAnalyzer kilobytesInPerSecond(File results_path) throws IOException {

        return new AggregatedResourceConsumptionPerScannerIntervalAnalyzer(KilobytesInPerSecondAnalyzer.NAME, results_path, KilobytesInPerSecondAnalyzer.GAUGE_CSV, KilobytesInPerSecondAnalyzer.CONVERT_BYTES_TO_KILO_BYTES);
    }

    static AggregatedResourceConsumptionPerScannerIntervalAnalyzer packetsOutPerSecond(File results_path) throws IOException {

        return new AggregatedResourceConsumptionPerScannerIntervalAnalyzer(PacketsOutPerSecondAnalyzer.NAME, results_path, PacketsOutPerSecondAnalyzer.GAUGE_CSV);
    }

    static AggregatedResourceConsumptionPerScannerIntervalAnalyzer packetsInPerSecond(File results_path) throws IOException {

        return new AggregatedResourceConsumptionPerScannerIntervalAnalyzer(PacketsInPerSecondAnalyzer.NAME, results_path, PacketsInPerSecondAnalyzer.GAUGE_CSV);
    }

}
