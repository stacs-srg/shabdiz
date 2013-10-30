package uk.ac.standrews.cs.shabdiz.evaluation.util;

import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class BlubPacketsOutGangliaGauge extends BlubGangliaGauge<Float> {

    private static final String PACKETS_OUT_METRIC_NAME = "pkts_out";

    public BlubPacketsOutGangliaGauge() throws ParserConfigurationException, SAXException {

        super(PACKETS_OUT_METRIC_NAME);
    }

    @Override
    public Float get() {

        return Float.valueOf(ganglia_metric.get());
    }
}
