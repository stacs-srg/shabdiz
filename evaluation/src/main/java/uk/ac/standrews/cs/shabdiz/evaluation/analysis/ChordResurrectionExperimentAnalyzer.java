package uk.ac.standrews.cs.shabdiz.evaluation.analysis;

import com.google.visualization.datasource.base.TypeMismatchException;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.StatisticalBarRenderer;
import org.jfree.data.statistics.DefaultStatisticalCategoryDataset;
import org.mashti.gauge.util.GaugeLineChart;
import org.mashti.sight.PlainChartTheme;
import org.mashti.sina.distribution.statistic.Statistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.standrews.cs.shabdiz.evaluation.ChordResurrectionExperiment;
import uk.ac.standrews.cs.shabdiz.evaluation.Experiment;
import uk.ac.standrews.cs.shabdiz.evaluation.ResurrectionExperiment;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class ChordResurrectionExperimentAnalyzer {

    private static final File COMBINATIONS_HOME = new File("/Users/masih/Desktop/results/ChordResurrectionExperiment");
    private static final Logger LOGGER = LoggerFactory.getLogger(ChordResurrectionExperimentAnalyzer.class);

    public static void main(String[] args) throws IOException, TypeMismatchException {

        new ChordResurrectionExperimentAnalyzer().analyze(ChordResurrectionExperiment.TIME_TO_REACH_RUNNING_AFTER_KILL);
        new ChordResurrectionExperimentAnalyzer().analyze(ChordResurrectionExperiment.TIME_TO_REACH_STABILIZED_RING);
        new ChordResurrectionExperimentAnalyzer().analyze(ChordResurrectionExperiment.TIME_TO_REACH_STABILIZED_RING_AFTER_KILL);

    }

    public void analyze(final String property_name) throws IOException, TypeMismatchException {

        final File[] combinations = COMBINATIONS_HOME.listFiles(Analyser.DIRECTORY_FILTER);

        Map<Category2, Statistics> availability_statistics = new TreeMap<Category2, Statistics>();

        for (File combination : combinations) {

            final List<Properties> properties = Analyser.getAllPropertiesAt(combination);
            final Statistics statistics = new Statistics();

            //TODO assert that network size and manager for properties are the same

            Category2 category = new Category2();
            for (Properties p : properties) {
                final String status = p.getProperty(Experiment.EXPERIMENT_STATUS);
                category.network_size = Integer.parseInt(p.getProperty(Experiment.NETWORK_SIZE_PROPERTY));
                category.manager = p.getProperty(Experiment.MANAGER_PROPERTY).replace("EchoManager.", "");
                category.kill_portion = p.getProperty(ResurrectionExperiment.KILL_PORTION);

                if (status != null && !status.equalsIgnoreCase("failure")) {

                    final String time_to_reach_auth_string = p.getProperty(property_name);
                    if (time_to_reach_auth_string != null) {
                        statistics.addSample(Long.parseLong(time_to_reach_auth_string));
                    }
                    else {
                        LOGGER.warn("combination {} does not have {} property", combination, property_name);
                    }
                }
                else {
                    LOGGER.warn("combination {} does not have success status: {}", combination, status);
                }
            }
            availability_statistics.put(category, statistics);
        }

        for (Integer network_size : ResurrectionExperiment.NETWORK_SIZES) {

            DefaultStatisticalCategoryDataset dataset = new DefaultStatisticalCategoryDataset();
            for (Map.Entry<Category2, Statistics> s : availability_statistics.entrySet()) {
                final Category2 category = s.getKey();

                if (category.network_size == network_size) {
                    final double mean = s.getValue().getMean().doubleValue();
                    final double ci = s.getValue().getConfidenceInterval(0.95D).doubleValue();
                    dataset.add(HostAvailabilityRecognitionAnalyzer.toSecond(mean), HostAvailabilityRecognitionAnalyzer.toSecond(ci), category.manager, decorateKillPortion(category.kill_portion));
                    //                    dataset.add(toSecond(mean), toSecond(ci), category.kill_portion, category.manager);
                }
            }

            JFreeChart chart = ChartFactory.createLineChart(property_name.replace("_", " ").replace(" after kill", "") + " after killing portions of network size " + network_size, "Percentage of killed instances", "Time to reach RUNNING state after kill (s)", dataset, PlotOrientation.VERTICAL, true, false, false);
            new PlainChartTheme().apply(chart);
            StatisticalBarRenderer statisticalbarrenderer = new StatisticalBarRenderer();
            statisticalbarrenderer.setErrorIndicatorPaint(Color.BLACK);
            statisticalbarrenderer.setItemMargin(0.02);
            chart.getCategoryPlot().setRenderer(statisticalbarrenderer);
            chart.getCategoryPlot().getRangeAxis().setLowerBound(0);
            chart.getCategoryPlot().getRangeAxis().setUpperBound(350);
            GaugeLineChart.saveAsSVG(chart, Analyser.BOUNDS, new File(COMBINATIONS_HOME, property_name + "_by_network_size_" + network_size + ".svg"));
        }

        for (Float kill_portion : ResurrectionExperiment.KILL_PORTIONS) {

            DefaultStatisticalCategoryDataset dataset = new DefaultStatisticalCategoryDataset();
            for (Map.Entry<Category2, Statistics> s : availability_statistics.entrySet()) {
                final Category2 category = s.getKey();

                if (category.kill_portion.equals(String.valueOf(kill_portion))) {
                    final double mean = s.getValue().getMean().doubleValue();
                    final double ci = s.getValue().getConfidenceInterval(0.95D).doubleValue();
                    dataset.add(HostAvailabilityRecognitionAnalyzer.toSecond(mean), HostAvailabilityRecognitionAnalyzer.toSecond(ci), category.manager, String.valueOf(category.network_size));
                }
            }

            JFreeChart chart = ChartFactory.createLineChart(property_name.replace("_", " ").replace(" after kill", "") + " after killing " + decorateKillPortion(kill_portion) + " of instances", "Network Size", "Time to reach RUNNING state after kill (s)", dataset, PlotOrientation.VERTICAL, true, false, false);
            new PlainChartTheme().apply(chart);
            StatisticalBarRenderer statisticalbarrenderer = new StatisticalBarRenderer();
            statisticalbarrenderer.setErrorIndicatorPaint(Color.BLACK);
            statisticalbarrenderer.setItemMargin(0.02);
            chart.getCategoryPlot().setRenderer(statisticalbarrenderer);
            chart.getCategoryPlot().getRangeAxis().setLowerBound(0);
            chart.getCategoryPlot().getRangeAxis().setUpperBound(350);
            GaugeLineChart.saveAsSVG(chart, Analyser.BOUNDS, new File(COMBINATIONS_HOME, property_name + "_by_kill_portion_" + kill_portion + ".svg"));
        }

    }

    static String decorateKillPortion(String kill_portion) {

        return decorateKillPortion(Float.valueOf(kill_portion));
    }

    static String decorateKillPortion(Float kill_portion) {

        return (int) (kill_portion * 100) + "%";
    }

    static class Category2 extends RunningApplicationRecognitionAnalyzer.Category {

        String kill_portion;

        @Override
        public String toString() {

            return super.toString() + " " + kill_portion;
        }
    }

}
