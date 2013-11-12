package uk.ac.standrews.cs.shabdiz.evaluation.analysis;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYErrorRenderer;
import org.jfree.data.xy.YIntervalSeriesCollection;
import org.mashti.sight.PlainChartTheme;
import uk.ac.standrews.cs.shabdiz.ApplicationState;
import uk.ac.standrews.cs.shabdiz.evaluation.util.ApplicationStateCounters;

import static uk.ac.standrews.cs.shabdiz.evaluation.analysis.AnalyticsUtil.getFilesByName;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class StateChangeAnalyzer implements Analyser {

    private final List<GaugeCsvAnalyzer> state_analysers;
    private JFreeChart chart;
    private YIntervalSeriesCollection series_collection;

    public StateChangeAnalyzer(File results_path) {

        state_analysers = new ArrayList<GaugeCsvAnalyzer>();
        InitializeStateAnalyzers(results_path);
    }

    @Override
    public String getName() {

        return "State change over time";
    }

    @Override
    public JFreeChart getChart() throws IOException {

        if (chart == null) {
            final YIntervalSeriesCollection series_collection = getYIntervalSeriesCollection();

            chart = ChartFactory.createXYLineChart("Change of state over time", "Time through experiment (s)", "Number of instances", series_collection, PlotOrientation.VERTICAL, true, false, false);
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
            for (GaugeCsvAnalyzer analyzer : state_analysers) {
                series_collection.addSeries(analyzer.getYIntervalSeries());
            }
        }
        return series_collection;
    }

    private void InitializeStateAnalyzers(final File results_path) {

        for (ApplicationState state : ApplicationState.values()) {
            GaugeCsvAnalyzer analyzer = getStateAnalyzer(results_path, state);
            state_analysers.add(analyzer);
        }
    }

    private GaugeCsvAnalyzer getStateAnalyzer(File results_path, final ApplicationState state) {

        final String state_name = state.name();
        final String csv_file_name = getStateGaugeCsvFileName(state);
        final String y_axis_label = "Number of instances in state " + state_name;
        return new GaugeCsvAnalyzer(state_name, getFilesByName(results_path, csv_file_name), y_axis_label, y_axis_label + " over time");
    }

    private String getStateGaugeCsvFileName(final ApplicationState state) {

        return ApplicationStateCounters.getNameByState(state) + ".csv";
    }
}
