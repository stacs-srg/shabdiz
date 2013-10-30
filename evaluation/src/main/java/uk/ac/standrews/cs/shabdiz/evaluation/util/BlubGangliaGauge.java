package uk.ac.standrews.cs.shabdiz.evaluation.util;

import javax.xml.parsers.ParserConfigurationException;
import org.mashti.gauge.Gauge;
import org.mashti.gauge.ganglia.GangliaMetricGauge;
import org.xml.sax.SAXException;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
abstract class BlubGangliaGauge<Value> implements Gauge<Value> {

    static final String BLUB_CLUSTER_NAME = "blub-cs";
    static final String BLUB_HEAD_NODE_NAME = "blub-cs.local";
    protected final GangliaMetricGauge ganglia_metric;

    protected BlubGangliaGauge(final String metric_name) throws ParserConfigurationException, SAXException {

        ganglia_metric = new GangliaMetricGauge(BLUB_CLUSTER_NAME, BLUB_HEAD_NODE_NAME, metric_name);
    }

}
