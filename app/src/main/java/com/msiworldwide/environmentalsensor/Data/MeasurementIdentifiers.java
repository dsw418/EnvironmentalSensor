package com.msiworldwide.environmentalsensor.Data;


public class MeasurementIdentifiers {
    public static final String TAG = MeasurementIdentifiers.class.getSimpleName();

    int measurement_number_id;
    String FieldId;
    String Date;

    public MeasurementIdentifiers() {}

    public MeasurementIdentifiers(int measurement_number_id, String FieldId, String Date) {
        this.measurement_number_id = measurement_number_id;
        this.FieldId = FieldId;
        this.Date = Date;
    }

    public void setMeasurementNumberId(int measurement_number_id) {
        this.measurement_number_id = measurement_number_id;
    }

    public void setFieldId(String FieldId) {
        this.FieldId = FieldId;
    }

    public void setDate(String Date) {
        this.Date = Date;
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
}
