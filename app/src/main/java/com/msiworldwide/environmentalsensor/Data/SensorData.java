package com.msiworldwide.environmentalsensor.Data;

public class SensorData {
    public static final String TAG = SensorData.class.getSimpleName();

    int measurement_number_id;
    String FieldId;
    String Date;
    String Time;
    double lat;
    double lng;
    int moisture;
    int sunlight;
    double temperature;
    double humidity;
    int status;

    public SensorData() {}

    public SensorData(int measurement_number_id, String FieldId, String Date, String Time, double lat, double lng,
                      int moisture, int sunlight, double temperature, double humidity) {
        this.measurement_number_id = measurement_number_id;
        this.FieldId = FieldId;
        this.Date = Date;
        this.Time = Time;
        this.lat = lat;
        this.lng = lng;
        this.moisture = moisture;
        this.sunlight = sunlight;
        this.temperature = temperature;
        this.humidity = humidity;
    }



    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }


    // setters
    public void setMeasurementNumberId(int measurement_number_id) {
        this.measurement_number_id = measurement_number_id;
    }

    public void setFieldId(String FieldId) {
        this.FieldId = FieldId;
    }

    public void setDate(String Date) {
        this.Date = Date;
    }

    public void setTime(String Time) {
        this.Time = Time;
    }

    public void setlat(double lat) {
        this.lat = lat;
    }

    public void setlng(double lng) {
        this.lng = lng;
    }

    public void setmoisture(int moisture) {
        this.moisture = moisture;
    }

    public void setsunlight(int sunlight){
        this.sunlight = sunlight;
    }

    public void settemperature(double temperature) {
        this.temperature = temperature;
    }

    public void sethumidity(double humidity) {
        this.humidity = humidity;
    }

    public int getMeasurement_number_id() {
        return this.measurement_number_id;
    }

    public String getFieldId() {
        return this.FieldId;
    }

    public String getDate() {
        return this.Date;
    }

    public String getTime() {
        return this.Time;
    }

    public double getLat() {
        return this.lat;
    }

    public double getLng() {
        return this.lng;
    }

    public int getmoisture() {
        return this.moisture;
    }

    public int getsunlight() {
        return this.sunlight;
    }

    public double gettemperature() {
        return this.temperature;
    }

    public double gethumidity() {
        return this.humidity;
    }
}
