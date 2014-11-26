package uk.ac.standrews.cs.shabdiz.evaluation.analysis;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYErrorRenderer;
import org.jfree.data.xy.YIntervalSeries;
import org.jfree.data.xy.YIntervalSeriesCollection;
import org.json.JSONArray;
import org.json.JSONException;
import org.mashti.sight.PlainChartTheme;

/**
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class OverlaidGaugeCsvAnalyzer implements Analyser {

    private final NavigableMap<String, GaugeCsvAnalyzer> labeled_analyzers;
    private final String chart_title;
    private final String y_axis_label;
    private final String name;
    private JFreeChart chart;
    private YIntervalSeriesCollection series_collection;

    public OverlaidGaugeCsvAnalyzer(List<GaugeCsvAnalyzer> analyzers) {

        this(labelAsRepetitions(analyzers));

    }

    public OverlaidGaugeCsvAnalyzer(NavigableMap<String, GaugeCsvAnalyzer> labeled_analyzers) {

        if (labeled_analyzers.isEmpty()) {
            throw new IllegalArgumentException("at least one analyser must be given");
        }
        this.labeled_analyzers = labeled_analyzers;
        final GaugeCsvAnalyzer first_analyzer = labeled_analyzers.firstEntry().getValue();
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
            final YIntervalSeriesCollection series_collection = getDataset();

            chart = ChartFactory.createXYLineChart(chart_title, "Time through experiment (s)", y_axis_label, series_collection, PlotOrientation.VERTICAL, getDataset().getSeriesCount() > 1, false, false);
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

    @Override
    public synchronized YIntervalSeriesCollection getDataset() throws IOException {

        if (series_collection == null) {
            series_collection = new YIntervalSeriesCollection();
            for (Map.Entry<String, GaugeCsvAnalyzer> labeled_analyzer : labeled_analyzers.entrySet()) {
                final YIntervalSeries series = labeled_analyzer.getValue().getYIntervalSeries();
                series.setKey(labeled_analyzer.getKey());
                series_collection.addSeries(series);
            }
        }
        return series_collection;
    }

    @Override
    public JSONArray toJSON() throws JSONException, IOException {
        return DatasetUtils.toJson(getDataset());
    }

    private static NavigableMap<String, GaugeCsvAnalyzer> labelAsRepetitions(final List<GaugeCsvAnalyzer> analyzers) {

        final TreeMap<String, GaugeCsvAnalyzer> labeled_analyzers = new TreeMap<String, GaugeCsvAnalyzer>();
        int i = 1;
        for (GaugeCsvAnalyzer analyzer : analyzers) {
            labeled_analyzers.put("Repetition " + i, analyzer);
            i++;
        }
        return labeled_analyzers;
    }
}
