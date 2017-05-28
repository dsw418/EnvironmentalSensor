package com.msiworldwide.environmentalsensor.Data;

public class FieldData {
    public static final String TAG = FieldData.class.getSimpleName();

    String FieldId;
    String Coordinates;

    public FieldData() {}

    public FieldData(String FieldId, String Coordinates) {
        this.FieldId = FieldId;
        this.Coordinates = Coordinates;
    }

    // setters
    public void setFieldId(String FieldId) {
        this.FieldId = FieldId;
    }

    public void setCoordinates(String Coordinates) {
        this.Coordinates = Coordinates;
    }

    // getters
    public String getFieldId() {
        return this.FieldId;
    }

    public String getCoordinates() {
        return this.Coordinates;
    }
}
