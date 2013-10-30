package uk.ac.standrews.cs.shabdiz.evaluation.analysis;

import com.google.visualization.datasource.base.TypeMismatchException;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
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
import uk.ac.standrews.cs.shabdiz.evaluation.Constants;

import static uk.ac.standrews.cs.shabdiz.evaluation.analysis.ChordResurrectionExperimentAnalyzer.Category;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class DeployTimeAnalyzer {

    private static final File COMBINATIONS_HOME = new File("/Users/masih/Desktop/results/ApplicationSizeExperiment");
    private static final Logger LOGGER = LoggerFactory.getLogger(DeployTimeAnalyzer.class);

    public static void main(String[] args) throws IOException, TypeMismatchException {

        final JFreeChart chart = new DeployTimeAnalyzer().analyze();
        GaugeLineChart.saveAsSVG(chart, new Rectangle(new Dimension(600, 400)), new File(COMBINATIONS_HOME, "chart.svg"));
    }

    public JFreeChart analyze() throws IOException, TypeMismatchException {

        final File[] combinations = COMBINATIONS_HOME.listFiles(Analyser.DIRECTORY_FILTER);

        Map<Category, Statistics> availability_statistics = new TreeMap<Category, Statistics>();

        for (File combination : combinations) {

            final List<Properties> properties = Analyser.getAllPropertiesAt(combination);
            final Statistics statistics = new Statistics();

            //TODO assert that network size and manager for properties are the same

            Category category = new Category();
            for (Properties p : properties) {
                final String status = p.getProperty(Constants.EXPERIMENT_STATUS_PROPERTY);
                category.network_size = Integer.parseInt(p.getProperty(Constants.NETWORK_SIZE_PROPERTY));
                category.setManager(p.getProperty(Constants.MANAGER_PROPERTY));

                if (status != null && !status.equalsIgnoreCase("failure")) {

                    final String time_to_reach_auth_string = p.getProperty(Constants.TIME_TO_REACH_RUNNING_DURATION);
                    if (time_to_reach_auth_string != null) {
                        statistics.addSample(Long.parseLong(time_to_reach_auth_string));
                    }
                    else {
                        LOGGER.warn("combination {} does not have {} property", combination, Constants.TIME_TO_REACH_RUNNING_DURATION);
                    }
                }
                else {
                    LOGGER.warn("combination {} does not have success status: {}", combination, status);
                }
            }
            availability_statistics.put(category, statistics);
        }

        DefaultStatisticalCategoryDataset dataset = new DefaultStatisticalCategoryDataset();
        for (Map.Entry<Category, Statistics> s : availability_statistics.entrySet()) {
            final double mean = s.getValue().getMean().doubleValue();
            final double ci = s.getValue().getConfidenceInterval(0.95D).doubleValue();
            dataset.add(HostAvailabilityRecognitionAnalyzer.toSecond(mean), HostAvailabilityRecognitionAnalyzer.toSecond(ci), s.getKey().manager, String.valueOf(s.getKey().network_size));
        }

        JFreeChart jfreechart = ChartFactory.createLineChart("Time to reach RUNNING", "Network Size", "Time to reach Running state", dataset, PlotOrientation.VERTICAL, true, false, false);
        new PlainChartTheme().apply(jfreechart);
        StatisticalBarRenderer statisticalbarrenderer = new StatisticalBarRenderer();
        statisticalbarrenderer.setErrorIndicatorPaint(Color.BLACK);
        statisticalbarrenderer.setItemMargin(0.02);
        jfreechart.getCategoryPlot().setRenderer(statisticalbarrenderer);

        return jfreechart;
    }

}
