package com.example.dbclasses;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity

public class Prices {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ticker;

    @JsonProperty("c")

    private BigDecimal price;

    @JsonProperty("d")

    private BigDecimal change_amount;
    
    @JsonProperty("dp")
    
    private BigDecimal percent_change;
    
    @JsonProperty("h")
    
    private BigDecimal high;
    
    @JsonProperty("l")
    
    private BigDecimal low;
    
    @JsonProperty("o")
    
    private BigDecimal open;
    
    @JsonProperty("pc")
    
    private BigDecimal previous_close;
    
    @JsonProperty("t")
    
    private Long timestamp;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getChange_amount() {
        return change_amount;
    }

    public void setChange_amount(BigDecimal change_amount) {
        this.change_amount = change_amount;
    }

    public BigDecimal getPercent_change() {
        return percent_change;
    }

    public void setPercent_change(BigDecimal percent_change) {
        this.percent_change = percent_change;
    }

    public BigDecimal getHigh() {
        return high;
    }

    public void setHigh(BigDecimal high) {
        this.high = high;
    }

    public BigDecimal getLow() {
        return low;
    }

    public void setLow(BigDecimal low) {
        this.low = low;
    }

    public BigDecimal getOpen() {
        return open;
    }

    public void setOpen(BigDecimal open) {
        this.open = open;
    }

    public BigDecimal getPrevious_close() {
        return previous_close;
    }

    public void setPrevious_close(BigDecimal previous_close) {
        this.previous_close = previous_close;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }


}
