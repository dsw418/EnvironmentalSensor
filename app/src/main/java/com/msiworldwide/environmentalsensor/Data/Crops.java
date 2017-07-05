package com.msiworldwide.environmentalsensor.Data;

public class Crops {

    private String CropName;
    private String ImageName;
    private String CropBreed;

    public Crops (String CropName, String ImageName, String CropBreed) {
        this.CropName = CropName;
        this.ImageName = ImageName;
        this.CropBreed = CropBreed;
    }

    public void setCropName(String CropName) {
        this.CropName = CropName;
    }

    public void setImageName(String ImageName) {
        this.ImageName = ImageName;
    }

    public void setCropBreed(String CropBreed) {
        this.CropBreed = CropBreed;
    }

    public String getCropName() {
        return CropName;
    }

    public String getImageName() {
        return ImageName;
    }

    public String getCropBreed() {
        return CropBreed;
    }
}
