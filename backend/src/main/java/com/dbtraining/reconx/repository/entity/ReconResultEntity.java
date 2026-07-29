package com.dbtraining.reconx.repository.entity;

import jakarta.persistence.*;

/**
 * TICKET-ADV045 — Persistent representation of a ReconResult so the
 * insert-recon-verify integration test can round-trip through real Postgres.
 */
@Entity
@Table(name = "recon_results")
public class ReconResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trade_ref", nullable = false, length = 30)
    private String tradeRef;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "discrepancy_type", length = 30)
    private String discrepancyType;

    @Column(length = 500)
    private String details;

    public ReconResultEntity() {}

    public Long getId()                { return id; }
    public String getTradeRef()        { return tradeRef; }
    public String getStatus()          { return status; }
    public String getDiscrepancyType() { return discrepancyType; }
    public String getDetails()         { return details; }

    public void setTradeRef(String v)        { this.tradeRef = v; }
    public void setStatus(String v)          { this.status = v; }
    public void setDiscrepancyType(String v) { this.discrepancyType = v; }
    public void setDetails(String v)         { this.details = v; }
}
