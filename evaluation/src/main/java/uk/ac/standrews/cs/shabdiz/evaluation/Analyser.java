package uk.ac.standrews.cs.shabdiz.evaluation;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.mashti.gauge.util.GaugeCsvCombiner;
import org.mashti.gauge.util.GaugeLineChart;
import org.mashti.jetson.util.CloseableUtil;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class Analyser {

    private static final File RESULTS_HOME = new File("/Users/masih/Documents/PhD/Code/P2P Workspace/shabdiz/evaluation/results/");
    private static final String[] GAUGE_FILES = {"auth_state_gauge.csv", "other_state_gauge.csv", "running_state_gauge.csv", "unknown_state_gauge.csv"};
    private final File observations_directory;

    public Analyser(File observations_directory) {

        this.observations_directory = observations_directory;
    }

    public static void main(String[] args) throws IOException {

        final File[] experiments = RESULTS_HOME.listFiles(new FileFilter() {

            @Override
            public boolean accept(final File file) {

                return file.isDirectory();
            }
        });

        for (File experiment : experiments) {

            for (String g : GAUGE_FILES) {

                final GaugeCsvCombiner combiner = new GaugeCsvCombiner();
                combiner.addRepetitions(new File(experiment, "repetitions"), g);
                final File analysis_home = new File(experiment, "analysis");
                final File csv_home = new File(analysis_home, "csv");
                FileUtils.forceMkdir(csv_home);
                final File combined = new File(csv_home, "combined_" + g);
                combiner.combine(combined);

                final GaugeLineChart chart_generator = new GaugeLineChart(g, combined);

                File svg_home = new File(analysis_home, "svg");
                FileUtils.forceMkdir(svg_home);
                File jfc_home = new File(analysis_home, "jfc");
                FileUtils.forceMkdir(jfc_home);

                final String name = FilenameUtils.getBaseName(combined.getName());
                chart_generator.saveAsJFC(new File(jfc_home, name + ".jfc"));
                chart_generator.saveAsSVG(new File(svg_home, name + ".svg"));
            }
        }

    }

    public Properties loadExperimentProperties() throws IOException {

        final Properties properties = new Properties();
        BufferedInputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(new File(observations_directory, Experiment.PROPERTOES_FILE_NAME)));
            properties.load(in);
        }
        finally {
            CloseableUtil.closeQuietly(in);
        }
        return properties;
    }
}
