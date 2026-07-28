package com.dbtraining.reconx.service;

//ImportStatemnets: ReconResult OBJ | BiConsumer | BinaryOperator | Function| Supplier | Collector
import com.dbtraining.reconx.dto.ReconResult;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

//Final Class For ReconSummary Collector 
public final class ReconSummaryCollector
        implements Collector<ReconResult, ReconSummary.Builder, ReconSummary> {
    // Reference Method to Build a Brand New Object
    @Override
    public Supplier<ReconSummary.Builder> supplier() {
        return ReconSummary.Builder::new;
    }

    // Updates Builder witha a ReconResult | Reference Method
    @Override
    public BiConsumer<ReconSummary.Builder, ReconResult> accumulator() {
        return ReconSummary.Builder::add;
    }

    // Combiner Method Which Takes in Builder Object( Taking Left Recon Result -->
    // Built into Right)
    @Override
    public BinaryOperator<ReconSummary.Builder> combiner() {
        return (left, right) -> left.merge(right);
    }

    @Override
    public Function<ReconSummary.Builder, ReconSummary> finisher() {
        return ReconSummary.Builder::build;
    }

    @Override
    public Set<Characteristics> characteristics() {
        return EnumSet.of(Characteristics.UNORDERED);
    }
}