package uk.ac.standrews.cs.shabdiz.evaluation.analysis;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.ConstantCallSite;
import java.util.Comparator;
import java.util.NavigableMap;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.StatisticalBarRenderer;
import org.jfree.data.statistics.DefaultStatisticalCategoryDataset;
import org.json.JSONArray;
import org.json.JSONException;
import org.mashti.sight.PlainChartTheme;
import org.mashti.sina.distribution.statistic.Statistics;
import org.supercsv.cellprocessor.ift.CellProcessor;
import uk.ac.standrews.cs.shabdiz.evaluation.Constants;

import static uk.ac.standrews.cs.shabdiz.evaluation.analysis.AnalyticsUtil.decorateManagerAsApplicationName;
import static uk.ac.standrews.cs.shabdiz.evaluation.analysis.AnalyticsUtil.decorateManagerAsDeploymentStrategy;

/**
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
class AggregatedCrossRepetitionPerPropertyAnalyzer implements Analyser {

    protected final String name;
    protected final String property_key;
    protected final String file_name;
    protected final File results_path;
    protected String x_axis_label;
    protected String y_axis_label;
    protected NavigableMap<String, Statistics> data;
    protected DefaultStatisticalCategoryDataset dataset;

    AggregatedCrossRepetitionPerPropertyAnalyzer(String name, File results_path, String file_name, String property_key) throws IOException {

        this.name = name;
        this.results_path = results_path;
        this.file_name = file_name;
        this.property_key = property_key;
        x_axis_label = "Scan Interval";
        y_axis_label = name;
    }

    @Override
    public String getName() {

        return name;
    }

    @Override
    public JFreeChart getChart() throws IOException {

        final DefaultStatisticalCategoryDataset dataset = getDataset();
        final JFreeChart chart = ChartFactory.createLineChart(getName(), x_axis_label, y_axis_label, dataset, PlotOrientation.VERTICAL, getDataset().getRowCount() > 1, false, false);
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


    protected Comparator<? super String> getComparator() {

        return null;
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
                dataset.add(mean, ci, application, label);
            }
        }
        return dataset;
    }

    @Override
    public JSONArray toJSON() throws IOException, JSONException {

        return DatasetUtils.toJson(getDataset());
    }
}
