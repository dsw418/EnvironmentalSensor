package com.msiworldwide.environmentalsensor.AzureServicesEs;

/**
 * Created by Qiyamuddin Ahmadzai on 9/10/2017.
 */

public class EnvSensorData {


    @com.google.gson.annotations.SerializedName("id")
    private String mId;
    public String getId() { return mId; }
    public final void setId(String id) { mId = id; }


    @com.google.gson.annotations.SerializedName("canalname")
    private String mCanalname;
    public String getCanalname() { return mCanalname; }
    public final void setCanalname(String text) { mCanalname = text; }

    @com.google.gson.annotations.SerializedName("fieldname")
    private String mFieldname;
    public String getFieldname() { return mFieldname; }
    public final void setFieldname(String text) { mFieldname = text; }

    @com.google.gson.annotations.SerializedName("date")
    private String mDate;
    public String getDate() { return mDate; }
    public final void setDate(String text) { mDate = text; }

    @com.google.gson.annotations.SerializedName("time")
    private String mTime;
    public String getTime() { return mTime; }
    public final void setTime(String text) { mTime = text; }

    @com.google.gson.annotations.SerializedName("latitude")
    private String mLatitude;
    public String getLatitude() { return mLatitude; }
    public final void setLatitude(String text) { mLatitude = text; }

    @com.google.gson.annotations.SerializedName("logitude")
    private String mLogitude;
    public String getLogitude() { return mLogitude; }
    public final void setLogitude(String text) { mLogitude = text; }

    @com.google.gson.annotations.SerializedName("nutration")
    private String mNutration;
    public String getNutration() { return mNutration; }
    public final void setNutration(String text) { mNutration = text; }

    @com.google.gson.annotations.SerializedName("moisture")
    private String mMoisture;
    public String getMoisture() { return mMoisture; }
    public final void setMoisture(String text) { mMoisture = text; }

    @com.google.gson.annotations.SerializedName("sunlight")
    private String mSunlight;
    public String getSunlight() { return mSunlight; }
    public final void setSunlight(String text) { mSunlight = text; }

    @com.google.gson.annotations.SerializedName("humidity")
    private String mHumidity;
    public String getHumidity() { return mHumidity; }
    public final void setHumidity(String text) { mHumidity = text; }


    @com.google.gson.annotations.SerializedName("temp")
    private String mTemp;
    public String getTemp() { return mTemp; }
    public final void setTemp(String text) { mTemp = text; }




}
