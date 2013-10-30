package uk.ac.standrews.cs.shabdiz.evaluation.util;

import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class BlubBytesInGangliaGauge extends BlubGangliaGauge<Float> {

    private static final String BYTES_IN_METRIC_NAME = "bytes_in";

    public BlubBytesInGangliaGauge() throws ParserConfigurationException, SAXException {

        super(BYTES_IN_METRIC_NAME);
    }

    @Override
    public Float get() {

        return Float.valueOf(ganglia_metric.get());
    }
}
