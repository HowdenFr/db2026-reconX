package com.dbtraining.reconx.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TradeRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validRequestHasNoViolations() {
        assertThat(validator.validate(validRequest())).isEmpty();
    }

    @Test
    void negativeQuantityProducesPositiveViolation() {
        TradeRequest request = new TradeRequest(
                "ABC-20260727-0001",
                1L,
                2L,
                "EQUITY",
                "BUY",
                new BigDecimal("-1"),
                new BigDecimal("100.00"),
                LocalDate.of(2026, 7, 27));

        Set<ConstraintViolation<TradeRequest>> violations = validator.validate(request);

        assertThat(violations).singleElement().satisfies(violation -> {
            assertThat(violation.getPropertyPath().toString()).isEqualTo("quantity");
            assertThat(violation.getMessage()).isEqualTo("must be greater than 0");
        });
    }

    @Test
    void malformedTradeRefProducesPatternViolation() {
        TradeRequest request = new TradeRequest(
                "bad-ref",
                1L,
                2L,
                "EQUITY",
                "BUY",
                new BigDecimal("10"),
                new BigDecimal("100.00"),
                LocalDate.of(2026, 7, 27));

        Set<ConstraintViolation<TradeRequest>> violations = validator.validate(request);

        assertThat(violations).singleElement().satisfies(violation -> {
            assertThat(violation.getPropertyPath().toString()).isEqualTo("tradeRef");
            assertThat(violation.getMessage()).isEqualTo("tradeRef must match AAA-YYYYMMDD-NNNN");
        });
    }

    @Test
    void futureTradeDateProducesPastOrPresentViolation() {
        TradeRequest request = new TradeRequest(
                "ABC-20260727-0001",
                1L,
                2L,
                "EQUITY",
                "BUY",
                new BigDecimal("10"),
                new BigDecimal("100.00"),
                LocalDate.now().plusDays(1));

        Set<ConstraintViolation<TradeRequest>> violations = validator.validate(request);

        assertThat(violations).singleElement().satisfies(violation -> {
            assertThat(violation.getPropertyPath().toString()).isEqualTo("tradeDate");
            assertThat(violation.getMessage()).isEqualTo("must be a date in the past or in the present");
        });
    }

    private TradeRequest validRequest() {
        return new TradeRequest(
                "ABC-20260727-0001",
                1L,
                2L,
                "EQUITY",
                "BUY",
                new BigDecimal("10"),
                new BigDecimal("100.00"),
                LocalDate.of(2026, 7, 27));
    }
}
