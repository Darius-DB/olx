# OLX.ro Apartment Price Analyzer

A Java 17 CLI tool that fetches apartment-for-sale listings from **OLX.ro** for multiple Romanian cities, computes **price per square meter** per listing, and outputs **average** and **median** price/m² per city to enable easy comparison.

---

## Table of Contents
- [Requirements](#requirements)
- [Build](#build)
- [Usage](#usage)
- [CLI Options](#cli-options)
- [Example Output](#example-output)
- [CSV Export](#csv-export)
- [Adding New Cities](#adding-new-cities)
- [Limitations](#limitations)
- [Legal / ToS](#legal--tos)

---

## Requirements

| Tool | Version |
|------|---------|
| Java | 17+ |
| Maven | 3.8+ |

---

## Build

```bash
mvn clean package -q
```

This produces a fat JAR at `target/olx-price-analyzer-1.0.0.jar`.

---

## Usage

```bash
java -jar target/olx-price-analyzer-1.0.0.jar [OPTIONS]
```

### Quick start – compare 3 cities (3 pages each):

```bash
java -jar target/olx-price-analyzer-1.0.0.jar \
  --cities bucuresti,cluj-napoca,timisoara \
  --pages 3
```

### Full example with CSV export and custom delay:

```bash
java -jar target/olx-price-analyzer-1.0.0.jar \
  --cities bucuresti,cluj-napoca,timisoara,iasi,brasov,constanta \
  --pages 5 \
  --delay-ms 1500 \
  --out results.csv
```

---

## CLI Options

| Option | Default | Description |
|--------|---------|-------------|
| `--cities` | `bucuresti,cluj-napoca,timisoara` | Comma-separated list of OLX.ro city URL slugs |
| `--pages` | `3` | Maximum listing pages to fetch per city |
| `--delay-ms` | `1000` | Milliseconds to sleep between HTTP requests |
| `--out` | *(none)* | If provided, save summary CSV to this file path |
| `--currency` | *(info only)* | Shown in output; currency normalisation is not performed (see Limitations) |

---

## Example Output

```
Fetching data for 3 cities...
[bucuresti] page 1/3 ... 32 listings found (18 valid)
[bucuresti] page 2/3 ... 30 listings found (17 valid)
[bucuresti] page 3/3 ... 28 listings found (16 valid)
[cluj-napoca] page 1/3 ... 25 listings found (14 valid)
...

╔══════════════════╦═══════════╦═══════════════╦════════════════╦══════════════╦══════════════╗
║ City             ║ Listings  ║ Avg Price/m²  ║ Median Price/m²║ Min Price/m² ║ Max Price/m² ║
╠══════════════════╬═══════════╬═══════════════╬════════════════╬══════════════╬══════════════╣
║ bucuresti        ║     51    ║  1 820 EUR    ║   1 750 EUR    ║   800 EUR    ║  4 200 EUR   ║
║ cluj-napoca      ║     42    ║  2 050 EUR    ║   1 980 EUR    ║   950 EUR    ║  4 800 EUR   ║
║ timisoara        ║     38    ║  1 430 EUR    ║   1 370 EUR    ║   700 EUR    ║  3 100 EUR   ║
╚══════════════════╩═══════════╩═══════════════╩════════════════╩══════════════╩══════════════╝
```

---

## CSV Export

When `--out results.csv` is specified the tool writes a summary CSV:

```
city,validListings,avgPricePerSqm,medianPricePerSqm,minPricePerSqm,maxPricePerSqm,currency
bucuresti,51,1820.45,1750.00,800.00,4200.00,EUR
cluj-napoca,42,2050.12,1980.00,950.00,4800.00,EUR
timisoara,38,1430.78,1370.00,700.00,3100.00,EUR
```

---

## Adding New Cities

OLX.ro city slugs are used directly in the URL:

```
https://www.olx.ro/imobiliare/apartamente-garsoniere-de-vanzare/{CITY-SLUG}/
```

Examples of valid slugs:

| City | Slug |
|------|------|
| București | `bucuresti` |
| Cluj-Napoca | `cluj-napoca` |
| Timișoara | `timisoara` |
| Iași | `iasi` |
| Brașov | `brasov` |
| Constanța | `constanta` |
| Craiova | `craiova` |
| Galați | `galati` |
| Ploiești | `ploiesti` |
| Oradea | `oradea` |

Just pass any slug via `--cities`. Unknown slugs will produce 0 valid listings and a warning.

---

## Limitations

1. **OLX markup may change** – The tool uses CSS selectors and text patterns that are current as of early 2025. If OLX.ro redesigns its listing pages, the selectors must be updated in `OlxScraper.java`.
2. **Area not always present** – Many listings do not include a surface area in the title or detail summary; these listings are skipped (logged at DEBUG level). This biases results toward listings that do include area.
3. **Currency** – OLX.ro listings use EUR and RON. The tool reports prices in the currency shown on the listing and does not convert between currencies. Mixing currencies within the same city produces misleading statistics. The `currency` column in the output shows the most common currency for the city's valid listings; the raw CSV contains per-listing currency.
4. **Pagination** – OLX.ro paginates results at ~40 listings per page. Fetching many pages for popular cities may take time; keep `--pages` reasonable (≤10 suggested).
5. **Rate limiting / blocking** – Excessive requests may result in OLX.ro returning empty pages or HTTP 429/403. The `--delay-ms` option helps; default is 1 000 ms. Do not set it below ~500 ms.
6. **No JavaScript rendering** – The tool uses HTTP + jsoup; dynamically loaded content (if any) is not captured.

---

## Legal / ToS

Scraping OLX.ro may be subject to OLX's Terms of Service. This tool is provided for **personal, non-commercial analysis only**. By using it you accept full responsibility for compliance with OLX's ToS and applicable laws. The author provides no warranties.

---

## Project Structure

```
src/
└── main/java/com/olxanalyzer/
    ├── App.java                      # CLI entry point (picocli)
    ├── cli/
    │   └── AnalyzeCommand.java       # picocli command definition
    ├── model/
    │   └── ApartmentListing.java     # Data model for a single listing
    ├── scraper/
    │   └── OlxScraper.java           # Fetches & parses OLX.ro pages
    ├── analysis/
    │   └── PriceAnalyzer.java        # Statistical analysis (avg, median, min, max)
    └── output/
        ├── TableFormatter.java       # Formats results as a CLI table
        └── CsvExporter.java          # Writes summary CSV
src/
└── test/java/com/olxanalyzer/
    ├── analysis/
    │   └── PriceAnalyzerTest.java    # Unit tests for median/avg computation
    ├── scraper/
    │   └── OlxScraperTest.java       # Unit tests with mock HTML fixtures
    └── output/
        └── CsvExporterTest.java      # Unit tests for CSV output
```
