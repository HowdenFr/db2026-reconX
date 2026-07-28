package com.dbtraining.reconx.service;

import com.dbtraining.reconx.model.BondTrade;
import com.dbtraining.reconx.model.DerivativeTrade;
import com.dbtraining.reconx.model.EquityTrade;
import com.dbtraining.reconx.model.FXTrade;
import com.dbtraining.reconx.model.TradeType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collector.Characteristics;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * TICKET-ADV034 — Trade analytics with Collectors (groupingBy + summarizing)
 * TICKET-ADV035 — VWAP calculator using Streams + custom collector
 * TICKET-ADV036 — P&L per instrument: stream reduction
 * ============================================================================
 */
@Service
public class TradeAnalyticsService {

    /** TICKET-ADV034 — count + sum + min + max + average of notional per counterparty. */
    public Map<String, NotionalSummary> notionalByCounterparty(List<? extends TradeType> trades) {
        return trades.stream().collect(Collectors.groupingBy(
                t -> String.valueOf(counterpartyIdOf(t)),
                Collectors.collectingAndThen(
                        Collectors.toList(),
                        list -> {
                            long count = list.size();
                            BigDecimal total = list.stream()
                                    .map(t -> t.notional().amount())
                                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                            BigDecimal min = list.stream()
                                    .map(t -> t.notional().amount())
                                    .min(BigDecimal::compareTo)
                                    .orElse(BigDecimal.ZERO);
                            BigDecimal max = list.stream()
                                    .map(t -> t.notional().amount())
                                    .max(BigDecimal::compareTo)
                                    .orElse(BigDecimal.ZERO);
                            BigDecimal average = count == 0
                                    ? BigDecimal.ZERO
                                    : total.divide(BigDecimal.valueOf(count), 8, RoundingMode.HALF_UP);
                            return new NotionalSummary(count, total, min, max, average);
                        })));
    }

    /**
     * TICKET-ADV035 — VWAP = SUM(price * qty) / SUM(qty). Equity-only — only
     * EquityTrade has a meaningful price-volume pair.
     */
    public static Collector<EquityTrade, VwapAccumulator, BigDecimal> vwapCollector() {
        return new VwapCollector();
    }

    public Map<String, BigDecimal> vwapByInstrument(List<EquityTrade> equityTrades) {
        if (equityTrades == null) return Map.of();
        return equityTrades.stream().collect(Collectors.groupingBy(
                EquityTrade::instrumentSymbol,
                new VwapCollector()
        ));
    }

    static final class VwapAccumulator {
        BigDecimal sumPriceQty = BigDecimal.ZERO;
        BigDecimal sumQty = BigDecimal.ZERO;
    }

    static final class VwapCollector implements Collector<EquityTrade, VwapAccumulator, BigDecimal> {

        @Override
        public Supplier<VwapAccumulator> supplier() {
            return VwapAccumulator::new;
        }

        @Override
        public BiConsumer<VwapAccumulator, EquityTrade> accumulator() {
            return (acc, t) -> {
                acc.sumPriceQty = acc.sumPriceQty.add(t.price().multiply(t.quantity()));
                acc.sumQty = acc.sumQty.add(t.quantity());
            };
        }

        @Override
        public BinaryOperator<VwapAccumulator> combiner() {
            return (a, b) -> {
                VwapAccumulator res = new VwapAccumulator();
                res.sumPriceQty = a.sumPriceQty.add(b.sumPriceQty);
                res.sumQty = a.sumQty.add(b.sumQty);
                return res;
            };
        }

        @Override
        public Function<VwapAccumulator, BigDecimal> finisher() {
            return acc -> {
                if (acc.sumQty.signum() == 0) return BigDecimal.ZERO;
                return acc.sumPriceQty.divide(acc.sumQty, 6, RoundingMode.HALF_UP);
            };
        }

        @Override
        public Set<Characteristics> characteristics() {
            return Set.of(Characteristics.UNORDERED);
        }
    }

    /** TICKET-ADV036 — P&L per instrument symbol (sign by Side). */
    public Map<String, BigDecimal> pnlByInstrument(List<EquityTrade> equityTrades) {
        // TODO(TICKET-ADV036): groupingBy(EquityTrade::instrumentSymbol,
        //   mapping(this::pnl, reducing(BigDecimal.ZERO, BigDecimal::add))).
        //   Side.SELL contributes positively; Side.BUY contributes negatively.
        throw new UnsupportedOperationException("TICKET-ADV036");
    }

    private BigDecimal pnl(EquityTrade t) {
        // TODO(TICKET-ADV036): BigDecimal abs = price * qty; SELL -> abs, BUY -> abs.negate().
        throw new UnsupportedOperationException("TICKET-ADV036");
    }

    private long counterpartyIdOf(TradeType t) {
        return switch (t) {
            case EquityTrade e                                 -> e.counterpartyId();
            case FXTrade fx                                   -> fx.counterpartyId();
            case BondTrade b                                  -> b.counterpartyId();
            case DerivativeTrade d                            -> d.counterpartyId();
        };
    }

    public record NotionalSummary(long count,
                                   BigDecimal total,
                                   BigDecimal min,
                                   BigDecimal max,
                                   BigDecimal average) {}
}
