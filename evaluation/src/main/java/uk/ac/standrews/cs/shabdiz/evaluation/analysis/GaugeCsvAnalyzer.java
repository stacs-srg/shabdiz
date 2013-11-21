package uk.ac.standrews.cs.shabdiz.evaluation.analysis;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.TickUnits;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYErrorRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.YIntervalSeries;
import org.jfree.data.xy.YIntervalSeriesCollection;
import org.mashti.sight.PlainChartTheme;
import org.mashti.sina.distribution.statistic.Statistics;
import org.supercsv.cellprocessor.ift.CellProcessor;
import uk.ac.standrews.cs.shabdiz.evaluation.Constants;
import uk.ac.standrews.cs.shabdiz.util.Duration;

import static java.lang.Double.NaN;
import static uk.ac.standrews.cs.shabdiz.evaluation.analysis.AnalyticsUtil.RELATIVE_TIME_IN_SECONDS_PROCESSOR;
import static uk.ac.standrews.cs.shabdiz.evaluation.analysis.AnalyticsUtil.getCombinedGaugeCsvStatistics;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
class GaugeCsvAnalyzer implements Analyser {

    public static final String X_AXIS_LABEL = "Time Through Experiment (s)";
    private final String name;
    private final Duration report_interval;
    private final Collection<File> csv_files;
    private final String y_axis_label;
    private final String chart_title;
    private boolean show_error_bars;
    private XYPlot plot;
    private YIntervalSeriesCollection xy_dataset;
    private YIntervalSeries series;
    private Collection<Statistics[]> rows_statistics;

    protected GaugeCsvAnalyzer(String name, Collection<File> csv_files, String y_axis_label, String chart_title) {

        this(name, Constants.REPORT_INTERVAL, csv_files, y_axis_label, chart_title);
    }

    protected GaugeCsvAnalyzer(String name, Duration report_interval, Collection<File> csv_files, String y_axis_label, String chart_title) {

        this.name = name;
        this.report_interval = report_interval;
        this.csv_files = csv_files;
        this.y_axis_label = y_axis_label;
        this.chart_title = chart_title;
        show_error_bars = true;
    }

    @Override
    public String getName() {

        return name;
    }

    @Override
    public JFreeChart getChart() throws IOException {

        final JFreeChart chart = new JFreeChart(chart_title, getPlot());
        PlainChartTheme.applyTheme(chart);
        return chart;
    }

    protected synchronized XYPlot getPlot() throws IOException {

        if (plot == null) {
            final ValueAxis x_axis = getXAxis();
            final ValueAxis y_axis = getYAxis();
            final XYItemRenderer error_renderer = getXYItemRenderer();
            final XYDataset dataset = getXYDataset();
            plot = new XYPlot(dataset, x_axis, y_axis, error_renderer);
            plot.getRangeAxis().setLowerBound(0);
            final TickUnits tickUnits = new TickUnits();
            tickUnits.add(new NumberTickUnit(5));
            plot.getDomainAxis().setStandardTickUnits(tickUnits);
        }
        return plot;
    }

    private XYDataset getXYDataset() throws IOException {

        if (xy_dataset == null) {
            xy_dataset = new YIntervalSeriesCollection();
            xy_dataset.addSeries(getYIntervalSeries());
        }
        return xy_dataset;
    }

    private ValueAxis getYAxis() {

        final NumberAxis axis = new NumberAxis(y_axis_label);
        return axis;
    }

    private ValueAxis getXAxis() {

        final NumberAxis axis = new NumberAxis(X_AXIS_LABEL);
        return axis;
    }

    private synchronized XYItemRenderer getXYItemRenderer() {

        if (show_error_bars) {
            final XYErrorRenderer error_renderer = new XYErrorRenderer();
            error_renderer.setBaseLinesVisible(true);
            error_renderer.setBaseShapesVisible(false);
            error_renderer.setDrawYError(true);
            return error_renderer;
        }
        else {
            return null;
        }
    }

    protected boolean showErrorBars() {

        return show_error_bars;
    }

    protected synchronized void setShowErrorBars(boolean show_error_bars) {

        if (this.show_error_bars != show_error_bars) {
            this.show_error_bars = show_error_bars;
            plot = null;
        }
    }

    protected CellProcessor getValueCellProcessor() {

        return AnalyticsUtil.DOUBLE_PROCESSOR;
    }

    protected CellProcessor getTimeCellProcessor() {

        return RELATIVE_TIME_IN_SECONDS_PROCESSOR;
    }

    protected synchronized Collection<Statistics[]> getStatistics() throws IOException {

        if (rows_statistics == null) {
            final CellProcessor[] processors = {getTimeCellProcessor(), getValueCellProcessor()};
            rows_statistics = getCombinedGaugeCsvStatistics(csv_files, processors);
        }
        return rows_statistics;
    }

    protected synchronized YIntervalSeries getYIntervalSeries() throws IOException {

        if (series == null) {
            series = new YIntervalSeries(getName());
            final Collection<Statistics[]> rows_statistics = getStatistics();
            final long bucket_size = report_interval.getLength(TimeUnit.SECONDS);
            long time_bucket = 0;
            for (Statistics[] row_statistics : rows_statistics) {

                final double mean = row_statistics[1].getMean().doubleValue();
                final double ci = row_statistics[1].getConfidenceInterval95Percent().doubleValue();
                final double low = mean - ci;
                final double high = mean + ci;
                series.add(time_bucket, mean == NaN ? 0 : mean, low == NaN ? 0 : low, high == NaN ? 0 : high);
                time_bucket += bucket_size;
            }
        }
        return series;
    }
}
