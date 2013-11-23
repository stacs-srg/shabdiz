package uk.ac.standrews.cs.shabdiz.evaluation.analysis;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.NavigableMap;
import java.util.Properties;
import java.util.TreeMap;
import org.jfree.chart.JFreeChart;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
abstract class OverlaidCrossRepetitionPerPropertyAnalyzer implements Analyser {

    private final OverlaidGaugeCsvAnalyzer overlaid_analyzer;

    OverlaidCrossRepetitionPerPropertyAnalyzer(File results_path, String property_key) throws IOException {

        overlaid_analyzer = new OverlaidGaugeCsvAnalyzer(getAnalyzers(results_path, property_key));
    }

    @Override
    public String getName() {

        return overlaid_analyzer.getName();
    }

    @Override
    public JFreeChart getChart() throws IOException {

        final JFreeChart chart = overlaid_analyzer.getChart();
        chart.setTitle(getName());
        return chart;
    }

    private NavigableMap<String, GaugeCsvAnalyzer> getAnalyzers(final File results_path, String property_key) throws IOException {

        final File[] files = AnalyticsUtil.listSubDirectoriesExcluding(results_path, Analysis.ANALYSIS_DIR_NAME);
        final TreeMap<String, GaugeCsvAnalyzer> labeled_analyzers = new TreeMap<String, GaugeCsvAnalyzer>(getComparator());
        for (File file : files) {
            final GaugeCsvAnalyzer analyzer = getGaugeCsvAnalyser(file);
            final String label = getLabel(file, property_key);
            labeled_analyzers.put(label, analyzer);
        }

        return labeled_analyzers;
    }

    protected Comparator<String> getComparator() {

        return null;
    }

    protected abstract GaugeCsvAnalyzer getGaugeCsvAnalyser(final File file);

     static String getLabel(final File file, String property_key) throws IOException {

        final Properties[] all_properties = AnalyticsUtil.getAllExperimentPropertiesInPath(file);
        String cached_value = null;
        for (Properties properties : all_properties) {
            final String value = properties.getProperty(property_key);
            if (cached_value == null) {
                cached_value = value;
            }
            else if (!cached_value.equals(value)) { throw new IllegalStateException("expected uniform property value in directory " + file + " for property " + property_key); }
        }
        return cached_value;
    }

}
