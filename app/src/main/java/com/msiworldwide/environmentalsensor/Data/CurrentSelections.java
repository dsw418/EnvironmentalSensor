package com.msiworldwide.environmentalsensor.Data;

public class CurrentSelections {
    public static final String TAG = CurrentSelections.class.getSimpleName();

    long field_id;
    long water_id;
    String crop_id;

    public CurrentSelections() {}

    // Setters
    public void setField_id(long FieldId) {
        this.field_id = FieldId;
    }

    public void setWater_id(long WaterId) {
        this.water_id = WaterId;
    }

    public void setCrop_id(String CropId) {
        this.crop_id = CropId;
    }

    // Getters
    public long getField_id() {
        return this.field_id;
    }

    public long getWater_id() {
        return this.water_id;
    }

    public String getCrop_id() {
        return this.crop_id;
    }
}
