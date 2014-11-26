package uk.ac.standrews.cs.shabdiz.evaluation.analysis;

import org.jfree.data.statistics.DefaultStatisticalCategoryDataset;
import org.mashti.sina.distribution.statistic.Statistics;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.util.CsvContext;
import uk.ac.standrews.cs.shabdiz.evaluation.Constants;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;

import static uk.ac.standrews.cs.shabdiz.evaluation.analysis.AnalyticsUtil.decorateManagerAsApplicationName;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
class AggregatedResourceConsumptionPerThreadPoolSize extends AggregatedCrossRepetitionPerPropertyAnalyzer {

    public static final CellProcessor INTEGER_CELL_PROCESSOR = new CellProcessor() {

        @Override
        public Object execute(final Object value, final CsvContext context) {

            return Integer.valueOf(value.toString());
        }
    };
    private final CellProcessor value_processor;

    private AggregatedResourceConsumptionPerThreadPoolSize(final String name, final File results_path, final String file_name) throws IOException {

        this(name, results_path, file_name, null);
    }

    private AggregatedResourceConsumptionPerThreadPoolSize(final String name, final File results_path, final String file_name, CellProcessor value_processor) throws IOException {

        super(name, results_path, file_name, Constants.CONCURRENT_SCANNER_THREAD_POOL_SIZE_PROPERTY);
        this.value_processor = value_processor;
        x_axis_label = "Thread Pool Size";
    }

    @Override
    public String getName() {

        return "Aggregated " + super.getName() + " per Thread Pool Size";
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

    @Override
    public DefaultStatisticalCategoryDataset getDataset() throws IOException {

        if (dataset == null) {
            dataset = new DefaultStatisticalCategoryDataset();
            final File[] files = AnalyticsUtil.listSubDirectoriesExcluding(results_path, Analysis.ANALYSIS_DIR_NAME);
            for (File file : files) {
                final Statistics statistics = AnalyticsUtil.getAggregatedCstStatistic(AnalyticsUtil.getFilesByName(file, file_name), getCellProcessors(), 1, true);
                final String label = OverlaidCrossRepetitionPerPropertyAnalyzer.getLabel(file, property_key);
                final String application = decorateManagerAsApplicationName(OverlaidCrossRepetitionPerPropertyAnalyzer.getLabel(file, Constants.MANAGER_PROPERTY));
                final double mean = statistics.getMean().doubleValue();
                final double ci = statistics.getConfidenceInterval95Percent().doubleValue();
                dataset.add(mean, ci, application, AnalyticsUtil.decoratePoolSize(label));
            }
        }
        return dataset;
    }

    static AggregatedResourceConsumptionPerThreadPoolSize cpuUsage(File results_path) throws IOException {

        return new AggregatedResourceConsumptionPerThreadPoolSize(CpuUsageAnalyzer.NAME, results_path, CpuUsageAnalyzer.GAUGE_CSV, CpuUsageAnalyzer.PARSE_CPU_USAGE_PERCENT);
    }

    static AggregatedResourceConsumptionPerThreadPoolSize memoryUsage(File results_path) throws IOException {

        return new AggregatedResourceConsumptionPerThreadPoolSize(MemoryUsageAnalyzer.NAME, results_path, MemoryUsageAnalyzer.GAUGE_CSV, MemoryUsageAnalyzer.CONVERT_BYTE_TO_MEGABYTE);
    }

    static AggregatedResourceConsumptionPerThreadPoolSize threadCount(File results_path) throws IOException {

        return new AggregatedResourceConsumptionPerThreadPoolSize(ThreadCountAnalyzer.NAME, results_path, ThreadCountAnalyzer.GAUGE_CSV, INTEGER_CELL_PROCESSOR);
    }

    static AggregatedResourceConsumptionPerThreadPoolSize systemLoadAverage(File results_path) throws IOException {

        return new AggregatedResourceConsumptionPerThreadPoolSize(SystemLoadAverageAnalyzer.NAME, results_path, SystemLoadAverageAnalyzer.GAUGE_CSV);
    }

    static AggregatedResourceConsumptionPerThreadPoolSize kilobytesOutPerSecond(File results_path) throws IOException {

        return new AggregatedResourceConsumptionPerThreadPoolSize(KilobytesOutPerSecondAnalyzer.NAME, results_path, KilobytesOutPerSecondAnalyzer.GAUGE_CSV, KilobytesInPerSecondAnalyzer.CONVERT_BYTES_TO_KILO_BYTES);
    }

    static AggregatedResourceConsumptionPerThreadPoolSize kilobytesInPerSecond(File results_path) throws IOException {

        return new AggregatedResourceConsumptionPerThreadPoolSize(KilobytesInPerSecondAnalyzer.NAME, results_path, KilobytesInPerSecondAnalyzer.GAUGE_CSV, KilobytesInPerSecondAnalyzer.CONVERT_BYTES_TO_KILO_BYTES);
    }

    static AggregatedResourceConsumptionPerThreadPoolSize packetsOutPerSecond(File results_path) throws IOException {

        return new AggregatedResourceConsumptionPerThreadPoolSize(PacketsOutPerSecondAnalyzer.NAME, results_path, PacketsOutPerSecondAnalyzer.GAUGE_CSV);
    }

    static AggregatedResourceConsumptionPerThreadPoolSize packetsInPerSecond(File results_path) throws IOException {

        return new AggregatedResourceConsumptionPerThreadPoolSize(PacketsInPerSecondAnalyzer.NAME, results_path, PacketsInPerSecondAnalyzer.GAUGE_CSV);
    }

}
