package com.ai.plug.test.test;

import java.io.Serializable;

public class PoiData implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String name;
    private double lng;
    private double lat;
    private String poiType;
    private int oreoSales;
    
    // 无参构造函数
    public PoiData() {}
    
    // 有参构造函数
    public PoiData(String name, double lng, double lat, String poiType, int oreoSales) {
        this.name = name;
        this.lng = lng;
        this.lat = lat;
        this.poiType = poiType;
        this.oreoSales = oreoSales;
    }
    
    // Getters and Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public double getLng() {
        return lng;
    }
    
    public void setLng(double lng) {
        this.lng = lng;
    }
    
    public double getLat() {
        return lat;
    }
    
    public void setLat(double lat) {
        this.lat = lat;
    }
    
    public String getPoiType() {
        return poiType;
    }
    
    public void setPoiType(String poiType) {
        this.poiType = poiType;
    }
    
    public int getOreoSales() {
        return oreoSales;
    }
    
    public void setOreoSales(int oreoSales) {
        this.oreoSales = oreoSales;
    }
    
    @Override
    public String toString() {
        return "PoiData{" +
                "name='" + name + '\'' +
                ", lng=" + lng +
                ", lat=" + lat +
                ", poiType='" + poiType + '\'' +
                ", oreoSales=" + oreoSales +
                '}';
    }
}    