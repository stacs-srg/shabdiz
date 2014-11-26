package uk.ac.standrews.cs.shabdiz.evaluation.analysis;

import org.jfree.data.statistics.DefaultStatisticalCategoryDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.YIntervalSeries;
import org.jfree.data.xy.YIntervalSeriesCollection;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;

/**
 * @author masih
 */
public final class DatasetUtils {

    private DatasetUtils() {
    }


    public static JSONArray toJson(DefaultStatisticalCategoryDataset dataset) throws JSONException {

        final JSONArray json = new JSONArray();

        for (Object row : dataset.getRowKeys()) {

            final String row_st = row.toString();
            for (Object col : dataset.getColumnKeys()) {

                final String col_st = col.toString();
                final double mean = dataset.getMeanValue(row_st, col_st).doubleValue();
                final double ci = dataset.getStdDevValue(row_st, col_st).doubleValue();

                final JSONObject col_value = new JSONObject();
                col_value.put("row", row_st);
                col_value.put("col", col_st);
                col_value.put("mean", format(mean));
                col_value.put("lower", format(mean - ci));
                col_value.put("upper", format(mean + ci));

                json.put(col_value);
            }
        }

        return json;
    }

    public static JSONArray toJson(YIntervalSeriesCollection dataset) throws JSONException {

        final JSONArray json = new JSONArray();

        for (int series_index = 0; series_index < dataset.getSeriesCount(); series_index++) {

            final YIntervalSeries series = dataset.getSeries(series_index);
            final String series_key = dataset.getSeriesKey(series_index).toString();

            for (int item_index = 0; item_index < series.getItemCount(); item_index++) {
                final JSONObject item_value = new JSONObject();
                item_value.put("series_key", series_key);
                item_value.put("x", format(series.getX(item_index)));
                item_value.put("y", format(series.getYValue(item_index)));
                item_value.put("y_low", format(series.getYLowValue(item_index)));
                item_value.put("y_high", format(series.getYHighValue(item_index)));

                json.put(item_value);
            }
        }

        return json;
    }

    private static double format(Number value) {
        return Double.valueOf(String.format("%.2f", value.doubleValue()));
    }
}
