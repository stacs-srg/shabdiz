package uk.ac.standrews.cs.shabdiz.evaluation.analysis;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Rectangle;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYErrorRenderer;
import org.jfree.data.xy.YIntervalSeries;
import org.jfree.data.xy.YIntervalSeriesCollection;
import org.mashti.gauge.util.GaugeCsvCombiner;
import org.mashti.gauge.util.GaugeLineChart;
import org.mashti.jetson.util.CloseableUtil;
import org.mashti.sight.PlainChartTheme;
import uk.ac.standrews.cs.shabdiz.ApplicationState;
import uk.ac.standrews.cs.shabdiz.evaluation.Constants;
import uk.ac.standrews.cs.shabdiz.evaluation.ExperiementRunner;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class Analyser {

    public static final FileFilter DIRECTORY_FILTER = new FileFilter() {

        @Override
        public boolean accept(final File file) {

            return file.isDirectory();
        }
    };
    public static final Rectangle BOUNDS = new Rectangle(0, 0, 600, 400);
    private static final File RESULTS_HOME = new File("/Users/masih/Desktop/results/");
    private static final String[] GAUGE_FILES = {"auth_state_counter.csv", "cpu_gauge.csv", "deployed_state_counter.csv", "invalid_state_counter.csv", "killed_state_counter.csv", "launched_state_counter.csv", "memory_gauge.csv", "no_auth_state_counter.csv", "running_state_counter.csv",
                    "thread_count_gauge.csv", "unknown_state_counter.csv", "unreachable_state_counter.csv", "system_load_average_gauge.csv"};
    private static final boolean SKIP_IF_ANALYSIS_EXISTS = false;
    private final File observations_directory;

    public Analyser(File observations_directory) {

        this.observations_directory = observations_directory;
    }

    public static List<Properties> getAllPropertiesAt(File directory) throws IOException {

        final Collection<File> properties_files = FileUtils.listFiles(directory, new String[]{"properties"}, true);
        final List<Properties> properties = new ArrayList<Properties>();

        for (File file : properties_files) {

            if (file.getName().equals(Constants.PROPERTIES_FILE_NAME)) {

                final Properties p = new Properties();
                FileReader reader = null;
                try {
                    reader = new FileReader(file);
                    p.load(reader);
                }
                finally {
                    IOUtils.closeQuietly(reader);
                }
                properties.add(p);
            }
        }

        return properties;

    }

    public static void main(String[] args) throws IOException {

        final File[] experiments = RESULTS_HOME.listFiles(DIRECTORY_FILTER);

        for (File experiment : experiments) {

            final File[] combinations = experiment.listFiles(DIRECTORY_FILTER);

            for (File combination : combinations) {

                final File repetitions = new File(combination, ExperiementRunner.REPETITIONS_HOME_NAME);
                final File analysis = new File(combination, "analysis");

                if (SKIP_IF_ANALYSIS_EXISTS && analysis.isDirectory()) {
                    continue;
                }
                else {
                    FileUtils.deleteQuietly(analysis);
                }

                final YIntervalSeriesCollection series_collection = new YIntervalSeriesCollection();

                for (ApplicationState state : ApplicationState.values()) {

                    String state_counter_file_name = getFileNameByState(state);

                    final GaugeCsvCombiner combiner = new GaugeCsvCombiner();
                    final File csv_home = new File(analysis, "data");
                    final File svg_home = new File(analysis, "svg");
                    final File jfc_home = new File(analysis, "jfc");
                    final File combined = new File(csv_home, "combined_" + state_counter_file_name);
                    FileUtils.forceMkdir(csv_home);
                    FileUtils.forceMkdir(svg_home);
                    FileUtils.forceMkdir(jfc_home);

                    combiner.addRepetitions(repetitions, state_counter_file_name);
                    combiner.combine(combined, Constants.REPORT_INTERVAL.getLength(), Constants.REPORT_INTERVAL.getTimeUnit());

                    final GaugeLineChart chart_generator = new GaugeLineChart(state_counter_file_name, combined);
                    final JFreeChart chart = chart_generator.getChart();
                    final YIntervalSeries series = chart_generator.getSeries();
                    series.setKey(state.name());
                    final String name = FilenameUtils.getBaseName(combined.getName());
                    GaugeLineChart.saveAsJFC(chart, new File(jfc_home, name + ".jfc"));
                    GaugeLineChart.saveAsSVG(chart, BOUNDS, new File(svg_home, name + ".svg"));

                    series_collection.addSeries(series);
                }

                JFreeChart chart = ChartFactory.createXYLineChart("Change of state over time", "Time through experiment (s)", "Number of instances", series_collection, PlotOrientation.VERTICAL, true, false, false);
                new PlainChartTheme().apply(chart);
                XYErrorRenderer xyerrorrenderer = new XYErrorRenderer();
                xyerrorrenderer.setBaseLinesVisible(true);
                xyerrorrenderer.setBaseShapesVisible(false);
                xyerrorrenderer.setDrawYError(true);
                xyerrorrenderer.setErrorPaint(Color.BLACK);
                xyerrorrenderer.setErrorStroke(new BasicStroke(1));

                final XYPlot xyPlot = chart.getXYPlot();
                xyPlot.setRenderer(xyerrorrenderer);
                xyPlot.getRangeAxis().setLowerBound(0);

                GaugeLineChart.saveAsSVG(chart, BOUNDS, new File(analysis, "status_change.svg"));
            }
        }
    }

    public Properties loadExperimentProperties() throws IOException {

        final Properties properties = new Properties();
        BufferedInputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(new File(observations_directory, Constants.PROPERTIES_FILE_NAME)));
            properties.load(in);
        }
        finally {
            CloseableUtil.closeQuietly(in);
        }
        return properties;
    }

    private static String getFileNameByState(final ApplicationState state) {

        for (String g : GAUGE_FILES) {
            if (g.startsWith(state.name().toLowerCase())) { return g; }
        }

        throw new RuntimeException("no file with state " + state);
    }
}
