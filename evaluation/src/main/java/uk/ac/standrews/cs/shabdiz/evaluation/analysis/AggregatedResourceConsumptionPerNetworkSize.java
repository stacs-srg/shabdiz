package uk.ac.standrews.cs.shabdiz.evaluation.analysis;

import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.util.CsvContext;
import uk.ac.standrews.cs.shabdiz.evaluation.Constants;

import java.io.File;
import java.io.IOException;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
class AggregatedResourceConsumptionPerNetworkSize extends AggregatedCrossRepetitionPerPropertyAnalyzer {

    public static final CellProcessor INTEGER_CELL_PROCESSOR = new CellProcessor() {

        @Override
        public Object execute(final Object value, final CsvContext context) {

            return Integer.valueOf(value.toString());
        }
    };
    private final CellProcessor value_processor;

    private AggregatedResourceConsumptionPerNetworkSize(final String name, final File results_path, final String file_name) throws IOException {

        this(name, results_path, file_name, null);
    }

    private AggregatedResourceConsumptionPerNetworkSize(final String name, final File results_path, final String file_name, CellProcessor value_processor) throws IOException {

        super(name, results_path, file_name, Constants.NETWORK_SIZE_PROPERTY);
        this.value_processor = value_processor;
        x_axis_label = "Network Size";
    }

    @Override
    public String getName() {

        return "Aggregated " + super.getName() + " per Network Size";
    }

    @Override
    protected CellProcessor[] getCellProcessors() {

        final CellProcessor[] cellProcessors = super.getCellProcessors();
        if (value_processor != null) {
            cellProcessors[1] = value_processor;
        }
        return cellProcessors;
    }

    static AggregatedResourceConsumptionPerNetworkSize cpuUsage(File results_path) throws IOException {

        return new AggregatedResourceConsumptionPerNetworkSize(CpuUsageAnalyzer.NAME, results_path, CpuUsageAnalyzer.GAUGE_CSV, CpuUsageAnalyzer.PARSE_CPU_USAGE_PERCENT);
    }

    static AggregatedResourceConsumptionPerNetworkSize memoryUsage(File results_path) throws IOException {

        return new AggregatedResourceConsumptionPerNetworkSize(MemoryUsageAnalyzer.NAME, results_path, MemoryUsageAnalyzer.GAUGE_CSV, MemoryUsageAnalyzer.CONVERT_BYTE_TO_MEGABYTE);
    }

    static AggregatedResourceConsumptionPerNetworkSize threadCount(File results_path) throws IOException {

        return new AggregatedResourceConsumptionPerNetworkSize(ThreadCountAnalyzer.NAME, results_path, ThreadCountAnalyzer.GAUGE_CSV, INTEGER_CELL_PROCESSOR);
    }

    static AggregatedResourceConsumptionPerNetworkSize systemLoadAverage(File results_path) throws IOException {

        return new AggregatedResourceConsumptionPerNetworkSize(SystemLoadAverageAnalyzer.NAME, results_path, SystemLoadAverageAnalyzer.GAUGE_CSV);
    }

    static AggregatedResourceConsumptionPerNetworkSize kilobytesOutPerSecond(File results_path) throws IOException {

        return new AggregatedResourceConsumptionPerNetworkSize(KilobytesOutPerSecondAnalyzer.NAME, results_path, KilobytesOutPerSecondAnalyzer.GAUGE_CSV, KilobytesInPerSecondAnalyzer.CONVERT_BYTES_TO_KILO_BYTES);
    }

    static AggregatedResourceConsumptionPerNetworkSize kilobytesInPerSecond(File results_path) throws IOException {

        return new AggregatedResourceConsumptionPerNetworkSize(KilobytesInPerSecondAnalyzer.NAME, results_path, KilobytesInPerSecondAnalyzer.GAUGE_CSV, KilobytesInPerSecondAnalyzer.CONVERT_BYTES_TO_KILO_BYTES);
    }

    static AggregatedResourceConsumptionPerNetworkSize packetsOutPerSecond(File results_path) throws IOException {

        return new AggregatedResourceConsumptionPerNetworkSize(PacketsOutPerSecondAnalyzer.NAME, results_path, PacketsOutPerSecondAnalyzer.GAUGE_CSV);
    }

    static AggregatedResourceConsumptionPerNetworkSize packetsInPerSecond(File results_path) throws IOException {

        return new AggregatedResourceConsumptionPerNetworkSize(PacketsInPerSecondAnalyzer.NAME, results_path, PacketsInPerSecondAnalyzer.GAUGE_CSV);
    }

}
