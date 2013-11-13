package uk.ac.standrews.cs.shabdiz.evaluation.analysis;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.StatisticalBarRenderer;
import org.jfree.data.statistics.DefaultStatisticalCategoryDataset;
import org.mashti.sight.PlainChartTheme;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
abstract class TimeToReachStateAnalyzer implements Analyser {

    protected final Properties[] experiment_properties;
    protected final String name;
    protected final String duration_property;
    protected boolean showLegend;
    protected String x_axis_label;

    protected TimeToReachStateAnalyzer(String name, File results_path, String duration_property) throws IOException {

        this.name = name;
        this.duration_property = duration_property;
        experiment_properties = AnalyticsUtil.getAllExperimentPropertiesInPath(results_path);
        x_axis_label = "Application";
    }

    @Override
    public JFreeChart getChart() throws IOException {

        final DefaultStatisticalCategoryDataset dataset = getStatisticalCategoryDataset();
        final JFreeChart chart = ChartFactory.createLineChart(getName(), x_axis_label, "Time to reach " + name + " (s)", dataset, PlotOrientation.VERTICAL, showLegend, false, false);
        final StatisticalBarRenderer renderer = new StatisticalBarRenderer();
        chart.getCategoryPlot().setRenderer(renderer);
        chart.getCategoryPlot().getRangeAxis().setLowerBound(0);
        PlainChartTheme.applyTheme(chart);
        return chart;
    }

    protected abstract DefaultStatisticalCategoryDataset getStatisticalCategoryDataset();
}
