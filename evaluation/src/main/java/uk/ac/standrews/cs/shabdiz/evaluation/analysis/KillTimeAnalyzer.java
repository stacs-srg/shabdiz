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
import uk.ac.standrews.cs.shabdiz.evaluation.Experiment;
import uk.ac.standrews.cs.shabdiz.evaluation.KillTimeExperiment;

import static uk.ac.standrews.cs.shabdiz.evaluation.analysis.HostAvailabilityRecognitionAnalyzer.toSecond;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class KillTimeAnalyzer {

    private static final File COMBINATIONS_HOME = new File("/Users/masih/Desktop/results/KillTimeExperiment");
    private static final Logger LOGGER = LoggerFactory.getLogger(KillTimeAnalyzer.class);

    public static void main(String[] args) throws IOException, TypeMismatchException {

        final JFreeChart chart = new KillTimeAnalyzer().analyze();
        GaugeLineChart.saveAsSVG(chart, new Rectangle(new Dimension(600, 400)), new File(COMBINATIONS_HOME, "chart.svg"));
    }

    public JFreeChart analyze() throws IOException, TypeMismatchException {

        final File[] combinations = COMBINATIONS_HOME.listFiles(Analyser.DIRECTORY_FILTER);

        Map<RunningApplicationRecognitionAnalyzer.Category, Statistics> availability_statistics = new TreeMap<RunningApplicationRecognitionAnalyzer.Category, Statistics>();

        for (File combination : combinations) {

            final List<Properties> properties = Analyser.getAllPropertiesAt(combination);
            final Statistics statistics = new Statistics();

            //TODO assert that network size and manager for properties are the same

            RunningApplicationRecognitionAnalyzer.Category category = new RunningApplicationRecognitionAnalyzer.Category();
            for (Properties p : properties) {
                final String status = p.getProperty(Experiment.EXPERIMENT_STATUS);
                category.network_size = Integer.parseInt(p.getProperty(Experiment.NETWORK_SIZE_PROPERTY));
                category.setManager(p.getProperty(Experiment.MANAGER_PROPERTY));

                if (status != null && !status.equalsIgnoreCase("failure")) {

                    final String time_to_reach_auth_string = p.getProperty(KillTimeExperiment.TIME_TO_REACH_AUTH_FROM_RUNNING);
                    if (time_to_reach_auth_string != null) {
                        statistics.addSample(Long.parseLong(time_to_reach_auth_string));
                    }
                    else {
                        LOGGER.warn("combination {} does not have {} property", combination, KillTimeExperiment.TIME_TO_REACH_AUTH_FROM_RUNNING);
                    }
                }
                else {
                    LOGGER.warn("combination {} does not have success status: {}", combination, status);
                }
            }
            availability_statistics.put(category, statistics);
        }

        DefaultStatisticalCategoryDataset dataset = new DefaultStatisticalCategoryDataset();
        for (Map.Entry<RunningApplicationRecognitionAnalyzer.Category, Statistics> s : availability_statistics.entrySet()) {
            final double mean = s.getValue().getMean().doubleValue();
            final double ci = s.getValue().getConfidenceInterval(0.95D).doubleValue();
            dataset.add(toSecond(mean), toSecond(ci), s.getKey().manager, String.valueOf(s.getKey().network_size));
        }

        JFreeChart jfreechart = ChartFactory.createLineChart("Time to kill running applications", "Network Sizes", "Time to reach AUTH state after kill (s)", dataset, PlotOrientation.VERTICAL, true, false, false);
        new PlainChartTheme().apply(jfreechart);
        StatisticalBarRenderer statisticalbarrenderer = new StatisticalBarRenderer();
        statisticalbarrenderer.setErrorIndicatorPaint(Color.BLACK);
        statisticalbarrenderer.setItemMargin(0.02);
        jfreechart.getCategoryPlot().setRenderer(statisticalbarrenderer);

        return jfreechart;
    }

}
