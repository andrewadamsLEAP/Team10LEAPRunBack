package com.example.instruments;

public class Instrument {
    private String ticker;
    private Double previousClose;
    private Double open;
    private Integer volume;
    private Double avgVolume;
    private String assetType;

    public Instrument(){
    }

    // Getters
    public String getTicker(){
        return ticker;
    }

    public Double getPreviousClose(){
        return previousClose;
    }
    
    public Double getOpen(){
        return open;
    }
    
    public Integer getVolume(){
        return volume;
    }
    
    public Double getAvgVolume(){
        return avgVolume;
    }
    
    public String getAssetType(){
        return assetType;
    }

    // Setters
    public void setTicker(String ticker){
        this.ticker = ticker;
    }

    public void setPreviousClose(Double previousClose){
        this.previousClose = previousClose;
    }

    public void setOpen(Double open){
        this.open = open;
    }

    public void setVolume(Integer volume){
        this.volume = volume;
    }

    public void setAvgVolume(Double avgVolume){
        this.avgVolume = avgVolume;
    }

    public void setAssetType(String assetType){
        this.assetType = assetType;
    }

}
