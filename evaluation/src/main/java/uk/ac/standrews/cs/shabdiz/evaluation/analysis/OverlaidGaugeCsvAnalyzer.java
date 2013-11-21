package uk.ac.standrews.cs.shabdiz.evaluation.analysis;

import java.io.IOException;
import java.util.List;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYErrorRenderer;
import org.jfree.data.xy.YIntervalSeries;
import org.jfree.data.xy.YIntervalSeriesCollection;
import org.mashti.sight.PlainChartTheme;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class OverlaidGaugeCsvAnalyzer implements Analyser {

    private final List<GaugeCsvAnalyzer> analyzers;
    private final String chart_title;
    private final String y_axis_label;
    private final String name;
    private JFreeChart chart;
    private YIntervalSeriesCollection series_collection;

    public OverlaidGaugeCsvAnalyzer(List<GaugeCsvAnalyzer> analyzers) {

        if (analyzers.size() == 0) { throw new IllegalArgumentException("at least one analyser must be given"); }
        this.analyzers = analyzers;
        final GaugeCsvAnalyzer first_analyzer = analyzers.get(0);
        chart_title = first_analyzer.getChartTitle();
        y_axis_label = first_analyzer.getYAxisLabel();
        name = first_analyzer.getName();
    }

    @Override
    public String getName() {

        return "Overlaid " + name;
    }

    @Override
    public JFreeChart getChart() throws IOException {

        if (chart == null) {
            final YIntervalSeriesCollection series_collection = getYIntervalSeriesCollection();

            chart = ChartFactory.createXYLineChart(chart_title, "Time through experiment (s)", y_axis_label, series_collection, PlotOrientation.VERTICAL, true, false, false);
            final XYErrorRenderer error_renderer = new XYErrorRenderer();
            error_renderer.setBaseShapesVisible(false);
            error_renderer.setBaseLinesVisible(true);
            error_renderer.setDrawYError(false);
            final XYPlot plot = chart.getXYPlot();
            plot.getRangeAxis().setLowerBound(0);
            plot.setRenderer(error_renderer);

            PlainChartTheme.applyTheme(chart);
        }
        return chart;
    }

    public synchronized YIntervalSeriesCollection getYIntervalSeriesCollection() throws IOException {

        if (series_collection == null) {
            series_collection = new YIntervalSeriesCollection();
            int repetition = 1;
            for (GaugeCsvAnalyzer analyzer : analyzers) {
                final YIntervalSeries series = analyzer.getYIntervalSeries();
                series.setKey("Repetition " + repetition);
                series_collection.addSeries(series);
                repetition++;
            }
        }
        return series_collection;
    }
}
