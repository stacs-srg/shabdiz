package uk.ac.standrews.cs.shabdiz.evaluation.analysis;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.StatisticalBarRenderer;
import org.jfree.data.statistics.DefaultStatisticalCategoryDataset;
import org.mashti.sight.PlainChartTheme;
import org.mashti.sina.distribution.statistic.Statistics;
import org.supercsv.cellprocessor.ift.CellProcessor;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
class AggregatedCrossRepetitionPerPropertyAnalyzer implements Analyser {

    protected final String name;
    protected final String property_key;
    private final String file_name;
    private final File results_path;
    protected String x_axis_label;
    protected String y_axis_label;
    protected NavigableMap<String, Statistics> data;
    private TreeMap<String, Statistics> labeled_statistics;

    AggregatedCrossRepetitionPerPropertyAnalyzer(String name, File results_path, String file_name, String property_key) throws IOException {

        this.name = name;
        this.results_path = results_path;
        this.file_name = file_name;
        this.property_key = property_key;
        x_axis_label = "Application";
        y_axis_label = name;
    }

    @Override
    public String getName() {

        return name;
    }

    @Override
    public JFreeChart getChart() throws IOException {

        final DefaultStatisticalCategoryDataset dataset = getStatisticalCategoryDataset();
        final JFreeChart chart = ChartFactory.createLineChart(getName(), x_axis_label, y_axis_label, dataset, PlotOrientation.VERTICAL, true, false, false);
        final StatisticalBarRenderer renderer = new StatisticalBarRenderer();
        final CategoryPlot category_plot = chart.getCategoryPlot();
        category_plot.setRenderer(renderer);

        final ValueAxis range_axis = category_plot.getRangeAxis();
        range_axis.setLowerBound(0);

        PlainChartTheme.applyTheme(chart);
        return chart;
    }

    protected CellProcessor[] getCellProcessors() {

        return AnalyticsUtil.DEFAULT_GAUGE_CSV_PROCESSORS;
    }

    protected synchronized NavigableMap<String, Statistics> getLabeledStatistics() throws IOException {

        if (labeled_statistics == null) {
            final File[] files = AnalyticsUtil.listSubDirectoriesExcluding(results_path, Analysis.ANALYSIS_DIR_NAME);
            labeled_statistics = new TreeMap<String, Statistics>(getComparator());
            for (File file : files) {
                final Statistics statistics = AnalyticsUtil.getAgregatedCsvStatistic(AnalyticsUtil.getFilesByName(file, file_name), getCellProcessors(), 1, true);
                final String label = OverlaidCrossRepetitionPerPropertyAnalyzer.getLabel(file, property_key);
                labeled_statistics.put(label, statistics);
            }
        }
        return labeled_statistics;
    }

    protected Comparator<? super String> getComparator() {

        return null;
    }

    protected DefaultStatisticalCategoryDataset getStatisticalCategoryDataset() throws IOException {

        final Map<String, Statistics> labeled_statistics = getLabeledStatistics();
        final DefaultStatisticalCategoryDataset dataset = new DefaultStatisticalCategoryDataset();
        for (Map.Entry<String, Statistics> entry : labeled_statistics.entrySet()) {
            final Statistics statistics = entry.getValue();
            final double mean = statistics.getMean().doubleValue();
            final double ci = statistics.getConfidenceInterval95Percent().doubleValue();
            dataset.add(mean, ci, "Application", entry.getKey());
        }
        return dataset;
    };
}
