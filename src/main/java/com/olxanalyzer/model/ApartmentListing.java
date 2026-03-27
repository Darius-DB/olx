package com.olxanalyzer.model;

/**
 * Represents a single apartment-for-sale listing scraped from OLX.ro.
 */
public class ApartmentListing {

    private final String url;
    private final String title;
    private final String city;
    private final Double price;
    private final String currency;
    private final Double areaSqm;
    private final Double pricePerSqm;

    private ApartmentListing(Builder builder) {
        this.url = builder.url;
        this.title = builder.title;
        this.city = builder.city;
        this.price = builder.price;
        this.currency = builder.currency;
        this.areaSqm = builder.areaSqm;
        this.pricePerSqm = (builder.price != null && builder.areaSqm != null && builder.areaSqm > 0)
                ? builder.price / builder.areaSqm
                : null;
    }

    public String getUrl()          { return url; }
    public String getTitle()        { return title; }
    public String getCity()         { return city; }
    public Double getPrice()        { return price; }
    public String getCurrency()     { return currency; }
    public Double getAreaSqm()      { return areaSqm; }
    public Double getPricePerSqm()  { return pricePerSqm; }

    /** Returns true when this listing has a usable price-per-m² value. */
    public boolean isValid() {
        return pricePerSqm != null && pricePerSqm > 0;
    }

    @Override
    public String toString() {
        return String.format("ApartmentListing{city='%s', price=%s %s, area=%s m², pricePerSqm=%s, title='%s'}",
                city, price, currency, areaSqm, pricePerSqm, title);
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String url;
        private String title;
        private String city;
        private Double price;
        private String currency;
        private Double areaSqm;

        public Builder url(String url)             { this.url = url;           return this; }
        public Builder title(String title)         { this.title = title;       return this; }
        public Builder city(String city)           { this.city = city;         return this; }
        public Builder price(Double price)         { this.price = price;       return this; }
        public Builder currency(String currency)   { this.currency = currency; return this; }
        public Builder areaSqm(Double areaSqm)     { this.areaSqm = areaSqm;  return this; }

        public ApartmentListing build() {
            return new ApartmentListing(this);
        }
    }
}
