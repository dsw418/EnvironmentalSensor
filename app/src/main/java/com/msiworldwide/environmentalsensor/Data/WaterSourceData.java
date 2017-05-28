package com.msiworldwide.environmentalsensor.Data;


public class WaterSourceData {
    public static final String TAG = WaterSourceData.class.getSimpleName();

    String WaterSourceId;
    String Coordinates;

    public WaterSourceData() {}

    public WaterSourceData(String WaterSourceId, String Coordinates) {
        this.WaterSourceId = WaterSourceId;
        this.Coordinates = Coordinates;
    }

    // setters
    public void setWaterSourceId(String WaterSourceId) {
        this.WaterSourceId = WaterSourceId;
    }

    public void setCoordinates(String Coordinates) {
        this.Coordinates = Coordinates;
    }

    // getters
    public String getWaterSourceId() {
        return this.WaterSourceId;
    }

    public String getCoordinates() {
        return this.Coordinates;
    }
}
