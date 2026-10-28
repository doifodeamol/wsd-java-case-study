package com.solvians.showcase;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * One immutable update of a certificate on the data feed.
 *
 * <p>Prices are {@link BigDecimal}s with a fixed scale of 2 - never use {@code double} for money:
 * {@code 0.1 + 0.2 != 0.3} in binary floating point, and a value like {@code 100.00} would be printed as
 * {@code 100.0}.
 */
public final class CertificateUpdate {

    private final long timestamp;
    private final String isin;
    private final BigDecimal bidPrice;
    private final int bidSize;
    private final BigDecimal askPrice;
    private final int askSize;

    public CertificateUpdate(long timestamp, String isin, BigDecimal bidPrice, int bidSize,
                             BigDecimal askPrice, int askSize) {
        this.timestamp = timestamp;
        this.isin = Objects.requireNonNull(isin, "isin");
        this.bidPrice = Objects.requireNonNull(bidPrice, "bidPrice");
        this.bidSize = bidSize;
        this.askPrice = Objects.requireNonNull(askPrice, "askPrice");
        this.askSize = askSize;
    }

    /** Milliseconds since 1970-01-01T00:00:00Z. */
    public long getTimestamp() {
        return timestamp;
    }

    public String getIsin() {
        return isin;
    }

    public BigDecimal getBidPrice() {
        return bidPrice;
    }

    public int getBidSize() {
        return bidSize;
    }

    public BigDecimal getAskPrice() {
        return askPrice;
    }

    public int getAskSize() {
        return askSize;
    }

    /** @return the feed line, e.g. {@code 1352122280502,DE1234567896,101.23,1000,103.45,1000}. */
    public String toLine() {
        return timestamp + "," + isin + ","
                + bidPrice.toPlainString() + "," + bidSize + ","
                + askPrice.toPlainString() + "," + askSize;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CertificateUpdate)) {
            return false;
        }
        CertificateUpdate that = (CertificateUpdate) o;
        return timestamp == that.timestamp
                && bidSize == that.bidSize
                && askSize == that.askSize
                && isin.equals(that.isin)
                && bidPrice.equals(that.bidPrice)
                && askPrice.equals(that.askPrice);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, isin, bidPrice, bidSize, askPrice, askSize);
    }

    @Override
    public String toString() {
        return toLine();
    }
}
