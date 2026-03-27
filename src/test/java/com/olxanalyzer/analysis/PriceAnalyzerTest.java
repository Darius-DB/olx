package com.olxanalyzer.analysis;

import com.olxanalyzer.model.ApartmentListing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PriceAnalyzerTest {

    private PriceAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new PriceAnalyzer();
    }

    // -------------------------------------------------------------------------
    // computeMedian
    // -------------------------------------------------------------------------

    @Test
    void computeMedian_null_returnsNaN() {
        assertTrue(Double.isNaN(analyzer.computeMedian(null)));
    }

    @Test
    void computeMedian_empty_returnsNaN() {
        assertTrue(Double.isNaN(analyzer.computeMedian(Collections.emptyList())));
    }

    @Test
    void computeMedian_singleElement() {
        assertEquals(42.0, analyzer.computeMedian(List.of(42.0)));
    }

    @Test
    void computeMedian_oddCount() {
        // sorted: [1, 2, 3, 4, 5] -> median = 3
        assertEquals(3.0, analyzer.computeMedian(List.of(1.0, 2.0, 3.0, 4.0, 5.0)));
    }

    @Test
    void computeMedian_evenCount() {
        // sorted: [1, 2, 3, 4] -> median = (2+3)/2 = 2.5
        assertEquals(2.5, analyzer.computeMedian(List.of(1.0, 2.0, 3.0, 4.0)));
    }

    @Test
    void computeMedian_twoElements() {
        assertEquals(1500.0, analyzer.computeMedian(List.of(1000.0, 2000.0)));
    }

    @Test
    void computeMedian_allSame() {
        assertEquals(1000.0, analyzer.computeMedian(List.of(1000.0, 1000.0, 1000.0)));
    }

    // -------------------------------------------------------------------------
    // analyze
    // -------------------------------------------------------------------------

    @Test
    void analyze_noListings_returnsZeroValidWithNaN() {
        PriceAnalyzer.CityStats stats = analyzer.analyze("test", Collections.emptyList());
        assertEquals("test", stats.getCity());
        assertEquals(0, stats.getValidListings());
        assertTrue(Double.isNaN(stats.getAvg()));
        assertTrue(Double.isNaN(stats.getMedian()));
    }

    @Test
    void analyze_onlyInvalidListings_returnsZeroValid() {
        // Invalid = no price or no area
        ApartmentListing noArea = ApartmentListing.builder()
                .city("test").price(100_000.0).currency("EUR").areaSqm(null).build();
        ApartmentListing noPrice = ApartmentListing.builder()
                .city("test").price(null).currency("EUR").areaSqm(75.0).build();

        PriceAnalyzer.CityStats stats = analyzer.analyze("test", List.of(noArea, noPrice));
        assertEquals(0, stats.getValidListings());
    }

    @Test
    void analyze_threeValidListings_correctStats() {
        // price/sqm: 1000, 2000, 3000 -> avg=2000, median=2000, min=1000, max=3000
        ApartmentListing l1 = ApartmentListing.builder()
                .city("test").price(100_000.0).currency("EUR").areaSqm(100.0).build();
        ApartmentListing l2 = ApartmentListing.builder()
                .city("test").price(120_000.0).currency("EUR").areaSqm(60.0).build();
        ApartmentListing l3 = ApartmentListing.builder()
                .city("test").price(90_000.0).currency("EUR").areaSqm(30.0).build();

        PriceAnalyzer.CityStats stats = analyzer.analyze("test", List.of(l1, l2, l3));

        assertEquals(3, stats.getValidListings());
        assertEquals(2000.0, stats.getAvg(), 0.01);
        assertEquals(2000.0, stats.getMedian(), 0.01);
        assertEquals(1000.0, stats.getMin(), 0.01);
        assertEquals(3000.0, stats.getMax(), 0.01);
        assertEquals("EUR", stats.getCurrency());
    }

    @Test
    void analyze_fourValidListings_evenMedian() {
        // price/sqm: 1000, 1500, 2500, 3000 -> median = (1500+2500)/2 = 2000
        ApartmentListing l1 = listing(100_000.0, 100.0, "EUR");  // 1000/m²
        ApartmentListing l2 = listing(75_000.0,  50.0, "EUR");   // 1500/m²
        ApartmentListing l3 = listing(125_000.0, 50.0, "EUR");   // 2500/m²
        ApartmentListing l4 = listing(150_000.0, 50.0, "EUR");   // 3000/m²

        PriceAnalyzer.CityStats stats = analyzer.analyze("test", List.of(l1, l2, l3, l4));

        assertEquals(4, stats.getValidListings());
        assertEquals(2000.0, stats.getMedian(), 0.01);
        assertEquals(2000.0, stats.getAvg(), 0.01);
    }

    @Test
    void analyze_mixedCurrencies_mostCommonCurrencySelected() {
        ApartmentListing eur1 = listing(100_000.0, 100.0, "EUR");
        ApartmentListing eur2 = listing(100_000.0, 100.0, "EUR");
        ApartmentListing ron1 = listing(500_000.0, 100.0, "RON");

        PriceAnalyzer.CityStats stats = analyzer.analyze("test", List.of(eur1, eur2, ron1));
        assertEquals("EUR", stats.getCurrency());
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private ApartmentListing listing(double price, double area, String currency) {
        return ApartmentListing.builder()
                .city("test").price(price).currency(currency).areaSqm(area).build();
    }
}
