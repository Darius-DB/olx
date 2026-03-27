package com.olxanalyzer.output;

import com.olxanalyzer.analysis.PriceAnalyzer.CityStats;

import java.util.List;

/**
 * Formats a list of {@link CityStats} as a Unicode box-drawing table for stdout.
 */
public class TableFormatter {

    private static final String H = "═";
    private static final String V = "║";
    private static final String TL = "╔", TM = "╦", TR = "╗";
    private static final String ML = "╠", MM = "╬", MR = "╣";
    private static final String BL = "╚", BM = "╩", BR = "╝";

    // Column widths (min)
    private static final int W_CITY      = 18;
    private static final int W_LISTINGS  = 9;
    private static final int W_AVG       = 15;
    private static final int W_MEDIAN    = 16;
    private static final int W_MIN       = 14;
    private static final int W_MAX       = 14;

    /**
     * Format the stats as a multi-line string.
     */
    public String format(List<CityStats> stats) {
        if (stats == null || stats.isEmpty()) {
            return "(no data)";
        }

        StringBuilder sb = new StringBuilder();

        // top border
        sb.append(buildBorder(TL, TM, TR)).append('\n');

        // header
        sb.append(buildRow("City", "Listings", "Avg Price/m²", "Median Price/m²", "Min Price/m²", "Max Price/m²")).append('\n');

        // separator
        sb.append(buildBorder(ML, MM, MR)).append('\n');

        // data rows
        for (CityStats s : stats) {
            String city     = s.getCity();
            String listings = String.valueOf(s.getValidListings());
            String avg      = formatValue(s.getAvg(),    s.getCurrency());
            String median   = formatValue(s.getMedian(), s.getCurrency());
            String min      = formatValue(s.getMin(),    s.getCurrency());
            String max      = formatValue(s.getMax(),    s.getCurrency());
            sb.append(buildRow(city, listings, avg, median, min, max)).append('\n');
        }

        // bottom border
        sb.append(buildBorder(BL, BM, BR)).append('\n');

        return sb.toString();
    }

    // -------------------------------------------------------------------------

    private String buildBorder(String left, String mid, String right) {
        return left
                + H.repeat(W_CITY + 2)      + mid
                + H.repeat(W_LISTINGS + 2)  + mid
                + H.repeat(W_AVG + 2)       + mid
                + H.repeat(W_MEDIAN + 2)    + mid
                + H.repeat(W_MIN + 2)       + mid
                + H.repeat(W_MAX + 2)       + right;
    }

    private String buildRow(String city, String listings, String avg, String median, String min, String max) {
        return V + " " + pad(city,     W_CITY)     + " "
             + V + " " + center(listings, W_LISTINGS) + " "
             + V + " " + pad(avg,     W_AVG)       + " "
             + V + " " + pad(median,  W_MEDIAN)    + " "
             + V + " " + pad(min,     W_MIN)        + " "
             + V + " " + pad(max,     W_MAX)        + " "
             + V;
    }

    /** Left-pad a string to the given width. */
    private String pad(String s, int width) {
        if (s.length() >= width) return s.substring(0, width);
        return s + " ".repeat(width - s.length());
    }

    /** Centre a string in a field of the given width. */
    private String center(String s, int width) {
        if (s.length() >= width) return s.substring(0, width);
        int totalPad = width - s.length();
        int left  = totalPad / 2;
        int right = totalPad - left;
        return " ".repeat(left) + s + " ".repeat(right);
    }

    private String formatValue(double value, String currency) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "N/A";
        }
        String cur = (currency != null && !currency.isBlank()) ? " " + currency : "";
        return String.format("%,.0f%s", value, cur);
    }
}
