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
import static uk.ac.standrews.cs.shabdiz.evaluation.analysis.AnalyticsUtil.decorateManagerAsDeploymentStrategy;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
class AggregatedResourceConsumptionPerDeploymentStrategy extends AggregatedCrossRepetitionPerPropertyAnalyzer {

    public static final CellProcessor INTEGER_CELL_PROCESSOR = new CellProcessor() {

        @Override
        public Object execute(final Object value, final CsvContext context) {

            return Integer.valueOf(value.toString());
        }
    };
    private final CellProcessor value_processor;

    private AggregatedResourceConsumptionPerDeploymentStrategy(final String name, final File results_path, final String file_name) throws IOException {

        this(name, results_path, file_name, null);
    }

    private AggregatedResourceConsumptionPerDeploymentStrategy(final String name, final File results_path, final String file_name, CellProcessor value_processor) throws IOException {

        super(name, results_path, file_name, Constants.MANAGER_PROPERTY);
        this.value_processor = value_processor;
        x_axis_label = "Deployment Strategy";
    }

    @Override
    public String getName() {

        return "Aggregated " + super.getName() + " per Deployment Strategy";
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
                final String application = decorateManagerAsApplicationName(label);
                final String strategy = decorateManagerAsDeploymentStrategy(label);
                final double mean = statistics.getMean().doubleValue();
                final double ci = statistics.getConfidenceInterval95Percent().doubleValue();
                dataset.add(mean, ci, application, strategy);
            }
        }
        return dataset;
    }

    static AggregatedResourceConsumptionPerDeploymentStrategy cpuUsage(File results_path) throws IOException {

        return new AggregatedResourceConsumptionPerDeploymentStrategy(CpuUsageAnalyzer.NAME, results_path, CpuUsageAnalyzer.GAUGE_CSV, CpuUsageAnalyzer.PARSE_CPU_USAGE_PERCENT);
    }

    static AggregatedResourceConsumptionPerDeploymentStrategy memoryUsage(File results_path) throws IOException {

        return new AggregatedResourceConsumptionPerDeploymentStrategy(MemoryUsageAnalyzer.NAME, results_path, MemoryUsageAnalyzer.GAUGE_CSV, MemoryUsageAnalyzer.CONVERT_BYTE_TO_MEGABYTE);
    }

    static AggregatedResourceConsumptionPerDeploymentStrategy threadCount(File results_path) throws IOException {

        return new AggregatedResourceConsumptionPerDeploymentStrategy(ThreadCountAnalyzer.NAME, results_path, ThreadCountAnalyzer.GAUGE_CSV, INTEGER_CELL_PROCESSOR);
    }

    static AggregatedResourceConsumptionPerDeploymentStrategy systemLoadAverage(File results_path) throws IOException {

        return new AggregatedResourceConsumptionPerDeploymentStrategy(SystemLoadAverageAnalyzer.NAME, results_path, SystemLoadAverageAnalyzer.GAUGE_CSV);
    }

    static AggregatedResourceConsumptionPerDeploymentStrategy kilobytesOutPerSecond(File results_path) throws IOException {

        return new AggregatedResourceConsumptionPerDeploymentStrategy(KilobytesOutPerSecondAnalyzer.NAME, results_path, KilobytesOutPerSecondAnalyzer.GAUGE_CSV, KilobytesInPerSecondAnalyzer.CONVERT_BYTES_TO_KILO_BYTES);
    }

    static AggregatedResourceConsumptionPerDeploymentStrategy kilobytesInPerSecond(File results_path) throws IOException {

        return new AggregatedResourceConsumptionPerDeploymentStrategy(KilobytesInPerSecondAnalyzer.NAME, results_path, KilobytesInPerSecondAnalyzer.GAUGE_CSV, KilobytesInPerSecondAnalyzer.CONVERT_BYTES_TO_KILO_BYTES);
    }

    static AggregatedResourceConsumptionPerDeploymentStrategy packetsOutPerSecond(File results_path) throws IOException {

        return new AggregatedResourceConsumptionPerDeploymentStrategy(PacketsOutPerSecondAnalyzer.NAME, results_path, PacketsOutPerSecondAnalyzer.GAUGE_CSV);
    }

    static AggregatedResourceConsumptionPerDeploymentStrategy packetsInPerSecond(File results_path) throws IOException {

        return new AggregatedResourceConsumptionPerDeploymentStrategy(PacketsInPerSecondAnalyzer.NAME, results_path, PacketsInPerSecondAnalyzer.GAUGE_CSV);
    }

}
