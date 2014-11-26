package uk.ac.standrews.cs.shabdiz.evaluation.analysis;

import java.io.IOException;
import java.util.stream.Stream;

import org.jfree.chart.JFreeChart;
import org.jfree.data.general.Dataset;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
interface Analyser {

    String getName();

    JFreeChart getChart() throws IOException;

    Dataset getDataset() throws IOException;

    JSONArray toJSON() throws JSONException, IOException;
}
