package com.olxanalyzer.scraper;

import com.olxanalyzer.model.ApartmentListing;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OlxScraperTest {

    private OlxScraper scraper;

    @BeforeEach
    void setUp() {
        scraper = new OlxScraper(null, 0);
    }

    // -------------------------------------------------------------------------
    // parsePrice
    // -------------------------------------------------------------------------

    @Test
    void parsePrice_eurWithSpaceThousands() {
        OlxScraper.PriceResult r = scraper.parsePrice("75 000 EUR");
        assertEquals(75_000.0, r.amount, 0.01);
        assertEquals("EUR", r.currency);
    }

    @Test
    void parsePrice_ronLei() {
        OlxScraper.PriceResult r = scraper.parsePrice("250000 lei");
        assertEquals(250_000.0, r.amount, 0.01);
        assertEquals("RON", r.currency);
    }

    @Test
    void parsePrice_dotThousandSeparator() {
        OlxScraper.PriceResult r = scraper.parsePrice("1.500 EUR");
        assertEquals(1_500.0, r.amount, 0.01);
        assertEquals("EUR", r.currency);
    }

    @Test
    void parsePrice_commaDecimal() {
        OlxScraper.PriceResult r = scraper.parsePrice("75,50 EUR");
        assertEquals(75.5, r.amount, 0.01);
        assertEquals("EUR", r.currency);
    }

    @Test
    void parsePrice_noCurrency() {
        OlxScraper.PriceResult r = scraper.parsePrice("100000");
        assertEquals(100_000.0, r.amount, 0.01);
        assertNull(r.currency);
    }

    @Test
    void parsePrice_nullInput() {
        OlxScraper.PriceResult r = scraper.parsePrice(null);
        assertNull(r.amount);
        assertNull(r.currency);
    }

    @Test
    void parsePrice_emptyString() {
        OlxScraper.PriceResult r = scraper.parsePrice("");
        assertNull(r.amount);
    }

    @Test
    void parsePrice_nonNumeric() {
        OlxScraper.PriceResult r = scraper.parsePrice("Negociabil");
        assertNull(r.amount);
    }

    // -------------------------------------------------------------------------
    // extractArea
    // -------------------------------------------------------------------------

    @Test
    void extractArea_mp() {
        assertEquals(75.0, scraper.extractArea("Apartament 2 camere, 75 mp, Sector 1"), 0.001);
    }

    @Test
    void extractArea_m2WithSpace() {
        assertEquals(120.0, scraper.extractArea("Penthouse 120 m², vedere panoramica"), 0.001);
    }

    @Test
    void extractArea_mpNoSpace() {
        assertEquals(50.0, scraper.extractArea("Garsoniera, 50mp, zona centrala"), 0.001);
    }

    @Test
    void extractArea_decimalArea() {
        assertEquals(63.5, scraper.extractArea("Apartament 63,5 mp utili"), 0.001);
    }

    @Test
    void extractArea_noAreaInText() {
        assertNull(scraper.extractArea("Apartament frumos, pret negociabil"));
    }

    @Test
    void extractArea_nullInput() {
        assertNull(scraper.extractArea(null));
    }

    // -------------------------------------------------------------------------
    // parsePage – mock HTML fixture
    // -------------------------------------------------------------------------

    @Test
    void parsePage_fixture_correctNumberOfListings() throws IOException {
        Document doc = loadFixture("fixtures/listing_page.html");
        List<ApartmentListing> listings = scraper.parsePage(doc, "bucuresti");

        // fixture has 5 cards
        assertEquals(5, listings.size());
    }

    @Test
    void parsePage_fixture_correctValidCount() throws IOException {
        Document doc = loadFixture("fixtures/listing_page.html");
        List<ApartmentListing> listings = scraper.parsePage(doc, "bucuresti");

        long valid = listings.stream().filter(ApartmentListing::isValid).count();
        // Cards 1, 2, 5 are valid; card 3 has no area; card 4 has no price
        assertEquals(3, valid);
    }

    @Test
    void parsePage_fixture_firstListingValues() throws IOException {
        Document doc = loadFixture("fixtures/listing_page.html");
        List<ApartmentListing> listings = scraper.parsePage(doc, "bucuresti");

        ApartmentListing first = listings.get(0);
        assertTrue(first.isValid());
        assertEquals(75_000.0, first.getPrice(), 0.01);
        assertEquals("EUR", first.getCurrency());
        assertEquals(75.0, first.getAreaSqm(), 0.01);
        assertEquals(1000.0, first.getPricePerSqm(), 0.01);
        assertEquals("bucuresti", first.getCity());
    }

    @Test
    void parsePage_fixture_listingWithoutArea_isInvalid() throws IOException {
        Document doc = loadFixture("fixtures/listing_page.html");
        List<ApartmentListing> listings = scraper.parsePage(doc, "bucuresti");

        // Card 3: no area -> invalid
        ApartmentListing noArea = listings.get(2);
        assertFalse(noArea.isValid());
        assertNull(noArea.getAreaSqm());
    }

    @Test
    void parsePage_fixture_listingWithoutPrice_isInvalid() throws IOException {
        Document doc = loadFixture("fixtures/listing_page.html");
        List<ApartmentListing> listings = scraper.parsePage(doc, "bucuresti");

        // Card 4: no price -> invalid
        ApartmentListing noPrice = listings.get(3);
        assertFalse(noPrice.isValid());
        assertNull(noPrice.getPrice());
    }

    @Test
    void parsePage_emptyPage_returnsEmptyList() {
        Document doc = Jsoup.parse("<html><body></body></html>");
        List<ApartmentListing> listings = scraper.parsePage(doc, "bucuresti");
        assertTrue(listings.isEmpty());
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private Document loadFixture(String resourcePath) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            assertNotNull(is, "Fixture not found: " + resourcePath);
            String html = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return Jsoup.parse(html, "https://www.olx.ro/");
        }
    }
}
