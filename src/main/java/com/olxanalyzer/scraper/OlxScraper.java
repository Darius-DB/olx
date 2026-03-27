package com.olxanalyzer.scraper;

import com.olxanalyzer.model.ApartmentListing;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scrapes apartment-for-sale listings from OLX.ro for a given city.
 *
 * <p>URL template used:
 * {@code https://www.olx.ro/imobiliare/apartamente-garsoniere-de-vanzare/{city}/?page={n}}
 *
 * <p><strong>Selector notes</strong> – OLX.ro markup is subject to change.
 * All CSS selectors are centralised here so they can be updated in one place.
 */
public class OlxScraper {

    private static final Logger LOG = Logger.getLogger(OlxScraper.class.getName());

    // ---- configurable constants -----------------------------------------

    private static final String BASE_URL =
            "https://www.olx.ro/imobiliare/apartamente-garsoniere-de-vanzare/%s/?page=%d";

    private static final String DEFAULT_USER_AGENT =
            "Mozilla/5.0 (compatible; OlxPriceAnalyzer/1.0; +https://github.com/Darius-DB/olx)";

    private static final int CONNECT_TIMEOUT_MS = 10_000;

    // ---- CSS selectors (update here if OLX changes markup) ---------------

    /** Selector for individual listing cards on a search-results page. */
    private static final String SEL_LISTING_CARD = "[data-cy='l-card']";

    /** Selector for the anchor tag containing the listing URL. */
    private static final String SEL_LISTING_LINK = "a[href]";

    /** Selector for the listing title. */
    private static final String SEL_TITLE = "h6, [data-testid='ad-title']";

    /** Selector for the price element. */
    private static final String SEL_PRICE = "[data-testid='ad-price'], .price";

    /** Selector for the location/date element that shows the city of the listing. */
    private static final String SEL_LOCATION = "[data-testid='location-date'], [data-testid='location']";

    // ---- regex patterns --------------------------------------------------

    /** Matches a numeric value (with optional thousand-separator) inside a string. */
    private static final Pattern PRICE_PATTERN =
            Pattern.compile("([\\d\\s\\.]+(?:[,\\.]\\d{1,2})?)\\s*(EUR|RON|lei|€)?",
                    Pattern.CASE_INSENSITIVE);

    /** Matches area in m²: e.g. "75 mp", "75 m²", "75mp". */
    private static final Pattern AREA_PATTERN =
            Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*m[²p2]", Pattern.CASE_INSENSITIVE);

    // ---- instance fields -------------------------------------------------

    private final String userAgent;
    private final long delayMs;

    public OlxScraper(String userAgent, long delayMs) {
        this.userAgent = userAgent == null || userAgent.isBlank() ? DEFAULT_USER_AGENT : userAgent;
        this.delayMs = Math.max(0, delayMs);
    }

    public OlxScraper() {
        this(DEFAULT_USER_AGENT, 1_000);
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Fetch up to {@code maxPages} pages of listings for the given city slug
     * and return all parsed listings (both valid and invalid).
     *
     * @param citySlug  OLX.ro city URL slug (e.g. "bucuresti", "cluj-napoca")
     * @param maxPages  maximum pages to fetch (1-based)
     * @return list of {@link ApartmentListing}
     */
    public List<ApartmentListing> scrape(String citySlug, int maxPages) {
        List<ApartmentListing> all = new ArrayList<>();
        Set<String> seenUrls = new HashSet<>();

        for (int page = 1; page <= maxPages; page++) {
            String url = String.format(BASE_URL, citySlug, page);
            LOG.info(String.format("[%s] Fetching page %d/%d: %s", citySlug, page, maxPages, url));

            List<ApartmentListing> pageListings;
            try {
                Document doc = fetchDocument(url);
                pageListings = parsePage(doc, citySlug);
            } catch (IOException e) {
                LOG.warning(String.format("[%s] Failed to fetch page %d: %s", citySlug, page, e.getMessage()));
                break;
            }

            if (pageListings.isEmpty()) {
                LOG.info(String.format("[%s] No listings on page %d, stopping early.", citySlug, page));
                break;
            }

            int added = 0;
            int duplicates = 0;
            for (ApartmentListing listing : pageListings) {
                String listingUrl = listing.getUrl();
                if (listingUrl != null && !listingUrl.isBlank() && !seenUrls.add(listingUrl)) {
                    LOG.fine(String.format("[%s] Duplicate URL skipped: %s", citySlug, listingUrl));
                    duplicates++;
                } else {
                    all.add(listing);
                    added++;
                }
            }

            LOG.info(String.format("[%s] page %d/%d ... %d listing(s) added, %d duplicate(s) skipped (%d valid)",
                    citySlug, page, maxPages, added, duplicates,
                    pageListings.stream().filter(ApartmentListing::isValid).count()));

            throttle();
        }

        return all;
    }

    // -------------------------------------------------------------------------
    // Package-accessible for testing
    // -------------------------------------------------------------------------

    /**
     * Parse a pre-loaded jsoup {@link Document} into a list of listings.
     * Exposed package-private so tests can inject fixture HTML.
     */
    List<ApartmentListing> parsePage(Document doc, String citySlug) {
        List<ApartmentListing> listings = new ArrayList<>();
        Elements cards = doc.select(SEL_LISTING_CARD);

        for (Element card : cards) {
            try {
                ApartmentListing listing = parseCard(card, citySlug);
                if (listing == null) {
                    continue; // city mismatch – already logged in parseCard
                }
                if (!listing.isValid()) {
                    LOG.fine(String.format("Skipped invalid listing: %s", listing));
                }
                listings.add(listing);
            } catch (Exception e) {
                LOG.log(Level.FINE, "Failed to parse card, skipping.", e);
            }
        }
        return listings;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private ApartmentListing parseCard(Element card, String citySlug) {
        // URL
        String url = "";
        Element linkEl = card.selectFirst(SEL_LISTING_LINK);
        if (linkEl != null) {
            url = linkEl.absUrl("href");
            if (url.isBlank()) {
                url = linkEl.attr("href");
            }
        }

        // Title
        String title = "";
        Element titleEl = card.selectFirst(SEL_TITLE);
        if (titleEl != null) {
            title = titleEl.text().trim();
        }

        // Price
        Double price = null;
        String currency = null;
        Element priceEl = card.selectFirst(SEL_PRICE);
        if (priceEl != null) {
            String priceText = priceEl.text();
            PriceResult pr = parsePrice(priceText);
            price = pr.amount;
            currency = pr.currency;
        }

        // Area – try to find in title first, then in card full text
        Double area = extractArea(title);
        if (area == null) {
            area = extractArea(card.text());
        }

        if (area == null) {
            LOG.fine(String.format("No area found for listing '%s' [%s]", title, url));
        }

        // City mismatch check: if a location element is present and the location does not
        // mention the expected city, the listing was promoted from another city – skip it.
        Element locationEl = card.selectFirst(SEL_LOCATION);
        if (locationEl != null) {
            String locationText = locationEl.text();
            if (!locationText.isBlank() && !isLocationMatchingCity(locationText, citySlug)) {
                LOG.fine(String.format("[%s] City mismatch: location '%s', skipping listing: %s",
                        citySlug, locationText, url));
                return null;
            }
        }

        return ApartmentListing.builder()
                .url(url)
                .title(title)
                .city(citySlug)
                .price(price)
                .currency(currency)
                .areaSqm(area)
                .build();
    }

    /**
     * Fetch an OLX.ro page as a jsoup Document.
     */
    Document fetchDocument(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent(userAgent)
                .timeout(CONNECT_TIMEOUT_MS)
                .get();
    }

    /**
     * Parse a price string like "75 000 EUR", "1.500 EUR", "250000 lei".
     */
    PriceResult parsePrice(String text) {
        if (text == null || text.isBlank()) {
            return new PriceResult(null, null);
        }

        Matcher m = PRICE_PATTERN.matcher(text);
        if (!m.find()) {
            return new PriceResult(null, null);
        }

        String numStr = m.group(1).replaceAll("[\\s\\.]", "").replace(',', '.');
        Double amount;
        try {
            amount = Double.parseDouble(numStr);
        } catch (NumberFormatException e) {
            LOG.fine("Could not parse price number: " + numStr);
            return new PriceResult(null, null);
        }

        String rawCurrency = m.group(2);
        String currency = normalizeCurrency(rawCurrency);

        return new PriceResult(amount, currency);
    }

    /**
     * Extract area in m² from arbitrary text.
     * Returns null if no area found.
     */
    Double extractArea(String text) {
        if (text == null || text.isBlank()) return null;
        Matcher m = AREA_PATTERN.matcher(text);
        if (!m.find()) return null;
        String numStr = m.group(1).replace(',', '.');
        try {
            return Double.parseDouble(numStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String normalizeCurrency(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        return switch (trimmed.toLowerCase()) {
            case "eur", "€" -> "EUR";
            case "ron", "lei" -> "RON";
            default -> trimmed.toUpperCase();
        };
    }

    /**
     * Returns {@code true} when the human-readable {@code locationText} from an OLX listing card
     * can be attributed to the given {@code citySlug}.  Both the location text and the slug are
     * normalised (diacritics stripped, lower-cased, hyphens replaced with spaces) before
     * comparison so that e.g. "Târgu Mureș" matches the slug "targu-mures".
     * A whole-word boundary check prevents short slugs (e.g. "brad") from matching longer
     * location names that merely contain them as a substring (e.g. "Brădet").
     */
    private boolean isLocationMatchingCity(String locationText, String citySlug) {
        String normalizedLocation = removeDiacritics(locationText.toLowerCase(Locale.ROOT)).replace("-", " ");
        String normalizedSlug    = removeDiacritics(citySlug.toLowerCase(Locale.ROOT)).replace("-", " ");

        int idx = normalizedLocation.indexOf(normalizedSlug);
        if (idx < 0) return false;

        // Verify word boundaries so that e.g. "brad" does not match "bradesti"
        boolean startOk = (idx == 0)
                || !Character.isLetterOrDigit(normalizedLocation.charAt(idx - 1));
        boolean endOk   = (idx + normalizedSlug.length() >= normalizedLocation.length())
                || !Character.isLetterOrDigit(normalizedLocation.charAt(idx + normalizedSlug.length()));
        return startOk && endOk;
    }

    /** Strips Unicode combining diacritical marks (e.g. ș→s, ț→t, ă→a, â→a, î→i). */
    private static String removeDiacritics(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                         .replaceAll("\\p{Mn}", "");
    }

    private void throttle() {
        if (delayMs <= 0) return;
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // -------------------------------------------------------------------------
    // Inner helper for price parsing result
    // -------------------------------------------------------------------------

    static final class PriceResult {
        final Double amount;
        final String currency;

        PriceResult(Double amount, String currency) {
            this.amount = amount;
            this.currency = currency;
        }
    }
}
