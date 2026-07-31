package com.dbtraining.reconx.repository.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.HashMap;
import java.util.Map;

/**
 * TICKET-ADV051 — JPA entity Instrument. metadata uses Hibernate 6's native
 * @JdbcTypeCode(SqlTypes.JSON), which resolves to the right physical column
 * type per dialect (jsonb on Postgres, a CLOB-equivalent on H2) instead of
 * a hardcoded columnDefinition — the previous hardcoded "jsonb" string made
 * ddl-auto=validate impossible to satisfy on H2, since it always expected
 * a literal jsonb type regardless of dialect.
 */
@Entity
@Table(name = "instruments")
public class Instrument {

    public enum AssetClass { EQUITY, FIXED_INCOME, FX }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String symbol;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_class", nullable = false, length = 20)
    private AssetClass assetClass;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(length = 12)
    private String isin;

    /**
     * Metadata: tick size, lot size, exchange code, etc. True JSONB on
     * Postgres, queryable via the @> operator (see TICKET-ADV009).
     */
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> metadata = new HashMap<>();

    public Instrument() {}

    public Long getId()              { return id; }
    public String getSymbol()        { return symbol; }
    public String getName()          { return name; }
    public AssetClass getAssetClass(){ return assetClass; }
    public String getCurrency()      { return currency; }
    public String getIsin()          { return isin; }
    public Map<String, Object> getMetadata() { return metadata; }

    public void setSymbol(String v)         { this.symbol = v; }
    public void setName(String v)           { this.name = v; }
    public void setAssetClass(AssetClass v) { this.assetClass = v; }
    public void setCurrency(String v)       { this.currency = v; }
    public void setIsin(String v)           { this.isin = v; }
    public void setMetadata(Map<String, Object> v) { this.metadata = v; }
}
