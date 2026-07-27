package com.dbtraining.reconx.model;

import java.util.Objects;
import java.util.regex.Pattern;



/**

 * WHAT:

 * Represents the unique business identifier assigned to a trade.

 *

 * HOW:

 * Implemented as an immutable record that validates the supplied reference

 * against the platform's required format during construction.

 *

 * WHY:

 * Encapsulating the trade reference in a dedicated value object prevents

 * arbitrary strings from being used where a valid trade identifier is required.

 */

public record TradeRef(String value) {

    private static final Pattern PATTERN = Pattern.compile("^[A-Z]{3}-\\d{8}-\\d{4}$");

    public TradeRef {
        Objects.requireNonNull(value, "tradeRef value");
        if (!PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Invalid tradeRef format '%s' — expected AAA-YYYYMMDD-NNNN".formatted(value));
        }
    }

    public static TradeRef of(String value) {
        return new TradeRef(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
