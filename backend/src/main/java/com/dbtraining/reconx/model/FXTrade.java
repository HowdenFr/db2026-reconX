package com.dbtraining.reconx.model;

/**
 * WHAT:
 * Represents a foreign exchange trade between two currencies.
 *
 * HOW:
 * Constructed through the {@link Builder}, which validates required fields and
 * business rules before creating an immutable trade instance.
 *
 * WHY:
 * Provides a type-safe representation of FX transactions for reconciliation
 * while preventing invalid currency combinations and exchange rates.
 */

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Objects;

public final class FXTrade implements TradeType {

    private final TradeRef tradeRef;
    private final Currency ccy1;
    private final Currency ccy2;
    private final BigDecimal notionalCcy1;
    private final BigDecimal fxRate;
    private final Side side;
    private final LocalDate tradeDate;
    private final long counterpartyId;

    private FXTrade(Builder b) {
        this.tradeRef     = b.tradeRef;
        this.ccy1         = b.ccy1;
        this.ccy2         = b.ccy2;
        this.notionalCcy1 = b.notionalCcy1;
        this.fxRate       = b.fxRate;
        this.side         = b.side;
        this.tradeDate    = b.tradeDate;
        this.counterpartyId = b.counterpartyId;
    }

/**
 * Creates a new builder for constructing an {@code FXTrade}.
 *
 * @return a new builder instance
 */

    public static Builder builder() {
        return new Builder();
    }


/**
 * Returns the unique business reference for this trade.
 *
 * @return the immutable trade reference
 */
    @Override
    public TradeRef tradeRef() {
        return tradeRef;
    }

/**
 * Returns the execution date of the trade.
 *
 * @return the date on which the trade occurred
 */

    @Override
    public LocalDate tradeDate() {
        return tradeDate;
    }
/**
 * Returns the asset class represented by this trade.
 *
 * @return {@link AssetClass#FX}
 */
    @Override
    public AssetClass assetClass() {
        return AssetClass.FX;
    }

    /**
     * Notional in quote currency ccy2 = notionalCcy1 * fxRate.
     */
    @Override
    public Money notional() {
        return new Money(notionalCcy1.multiply(fxRate), ccy2);
    }

/**
 * Returns the base currency.
 *
 * @return the base currency of the FX trade
 */

    public Currency ccy1() {
        return ccy1;
    }

/**
 * Returns the quote currency.
 *
 * @return the quote currency of the FX trade
 */

    public Currency ccy2() {
        return ccy2;
    }

/**
 * Calculates the trade notional in the quote currency.
 *
 * <p>The returned value is calculated as
 * {@code notionalCcy1 × fxRate} and is expressed in {@code ccy2}.</p>
 *
 * @return the calculated notional in the quote currency
 */

    public BigDecimal notionalCcy1() {
        return notionalCcy1;
    }

/**
 * Returns the exchange rate used by this trade.
 *
 * @return the FX conversion rate
 */

    public BigDecimal fxRate() {
        return fxRate;
    }

/**
 * Returns whether the trade is a buy or sell.
 *
 * @return the trade side
 */

    public Side side() {
        return side;
    }

/**
 * Returns the counterparty identifier.
 *
 * @return the internal counterparty identifier
 */

    public long counterpartyId() {
        return counterpartyId;
    }

    public static final class Builder {
        private TradeRef tradeRef;
        private Currency ccy1;
        private Currency ccy2;
        private BigDecimal notionalCcy1;
        private BigDecimal fxRate;
        private Side side;
        private LocalDate tradeDate;
        private long counterpartyId;

        public Builder tradeRef(TradeRef v) {
            this.tradeRef = v;
            return this;
        }

        public Builder ccy1(String code) {
            this.ccy1 = Currency.getInstance(code);
            return this;
        }

        public Builder ccy2(String code) {
            this.ccy2 = Currency.getInstance(code);
            return this;
        }

        public Builder notionalCcy1(BigDecimal v) {
            this.notionalCcy1 = v;
            return this;
        }

        public Builder fxRate(BigDecimal v) {
            this.fxRate = v;
            return this;
        }

        public Builder side(Side v) {
            this.side = v;
            return this;
        }

        public Builder tradeDate(LocalDate v) {
            this.tradeDate = v;
            return this;
        }

        public Builder counterpartyId(long v) {
            this.counterpartyId = v;
            return this;
        }

        public FXTrade build() {
            Objects.requireNonNull(tradeRef, "tradeRef");
            Objects.requireNonNull(ccy1, "ccy1");
            Objects.requireNonNull(ccy2, "ccy2");
            Objects.requireNonNull(notionalCcy1, "notionalCcy1");
            Objects.requireNonNull(fxRate, "fxRate");
            Objects.requireNonNull(side, "side");
            Objects.requireNonNull(tradeDate, "tradeDate");
            if (ccy1.equals(ccy2)) {
                throw new IllegalStateException("ccy1 and ccy2 must differ");
            }
            if (notionalCcy1.signum() <= 0) {
                throw new IllegalStateException("notionalCcy1 must be > 0");
            }
            if (fxRate.signum() <= 0) {
                throw new IllegalStateException("fxRate must be > 0");
            }
            return new FXTrade(this);
        }
    }
}
