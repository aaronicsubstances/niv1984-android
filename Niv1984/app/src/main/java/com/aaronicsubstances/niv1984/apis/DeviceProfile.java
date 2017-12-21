/*
 *  (c) Aaronic Substances.
 */
package com.aaronicsubstances.niv1984.apis;

import android.content.Context;
import android.os.Build;
import android.util.DisplayMetrics;

/**
 *
 * @author Aaron
 */
public class DeviceProfile {
    private String buildVersion;
    private int buildApiLevel;
    private String model;
    private String manufacturer;
    private float densityDpi;
    private float xdpi;
    private float ydpi;
    private int widthPixels;
    private int heightPixels;

    public DeviceProfile(Context context) {
        manufacturer = Build.MANUFACTURER;
        model = Build.MODEL;
        buildVersion = Build.VERSION.RELEASE;
        buildApiLevel = Build.VERSION.SDK_INT;

        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        densityDpi = dm.densityDpi;
        xdpi = dm.xdpi;
        ydpi = dm.ydpi;
        widthPixels = dm.widthPixels;
        heightPixels = dm.heightPixels;
    }

    public String getBuildVersion() {
        return buildVersion;
    }

    public void setBuildVersion(String buildVersion) {
        this.buildVersion = buildVersion;
    }

    public int getBuildApiLevel() {
        return buildApiLevel;
    }

    public void setBuildApiLevel(int buildApiLevel) {
        this.buildApiLevel = buildApiLevel;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public float getDensityDpi() {
        return densityDpi;
    }

    public void setDensityDpi(float densityDpi) {
        this.densityDpi = densityDpi;
    }

    public float getXdpi() {
        return xdpi;
    }

    public void setXdpi(float xdpi) {
        this.xdpi = xdpi;
    }

    public float getYdpi() {
        return ydpi;
    }

    public void setYdpi(float ydpi) {
        this.ydpi = ydpi;
    }

    public int getWidthPixels() {
        return widthPixels;
    }

    public void setWidthPixels(int widthPixels) {
        this.widthPixels = widthPixels;
    }

    public int getHeightPixels() {
        return heightPixels;
    }

    public void setHeightPixels(int heightPixels) {
        this.heightPixels = heightPixels;
    }
}
