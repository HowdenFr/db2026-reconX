package com.dbtraining.reconx.service;

//Import Recon Results( Of Trades) | Then Current Status of Trades
import com.dbtraining.reconx.dto.ReconResult;
import com.dbtraining.reconx.dto.ReconResult.Status;

//Recon Summary -- Holds total Info On Total | Matched Trades | Broken Trades
public record ReconSummary(
        long total,
        long matched,
        long broken) {

    // Static Constructor( Returns Total Results)

    public static ReconSummary empty() {
        return new ReconSummary(0, 0, 0);
    }

    // Builder For All the Information/Once we get the Information We have a Final
    // Class
    public static final class Builder {

        long total;
        long matched;
        long broken;

        // Process one ReconResult
        public void add(ReconResult result) {
            total++;

            if (result.status() == Status.MATCHED) {
                matched++;
            } else {
                broken++;
            }
        }

        // Merge another Builder into this one
        public Builder merge(Builder other) {
            total += other.total;
            matched += other.matched;
            broken += other.broken;
            return this;
        }

        // Build immutable ReconSummary
        public ReconSummary build() {
            return new ReconSummary(total, matched, broken);
        }
    }
}