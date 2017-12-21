package com.aaronicsubstances.niv1984.apis;

import android.content.Context;

import com.aaronicsubstances.niv1984.etc.SharedPrefsManager;
import com.aaronicsubstances.niv1984.etc.Utils;

/**
 * Created by Aaron on 9/9/2017.
 */

public class DefaultApiRequestModel {
    private DeviceProfile device;
    private String uid;
    private String mobileAppVersion;
    private int mobileAppVersionCode;
    private String mobileDataVersion;
    private int mobileDataVersionCode;

    public DefaultApiRequestModel(Context context) {
        device = new DeviceProfile(context);

        mobileAppVersion = Utils.getAppVersion(context);
        mobileAppVersionCode = Utils.getAppVersionCode(context);

        SharedPrefsManager prefsManager =  new SharedPrefsManager(context);
        uid = prefsManager.getUserUid();
    }

    public DeviceProfile getDevice() {
        return device;
    }

    public void setDevice(DeviceProfile device) {
        this.device = device;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getMobileAppVersion() {
        return mobileAppVersion;
    }

    public void setMobileAppVersion(String mobileAppVersion) {
        this.mobileAppVersion = mobileAppVersion;
    }

    public int getMobileAppVersionCode() {
        return mobileAppVersionCode;
    }

    public void setMobileAppVersionCode(int mobileAppVersionCode) {
        this.mobileAppVersionCode = mobileAppVersionCode;
    }

    public int getMobileDataVersionCode() {
        return mobileDataVersionCode;
    }

    public void setMobileDataVersionCode(int mobileDataVersionCode) {
        this.mobileDataVersionCode = mobileDataVersionCode;
    }

    public String getMobileDataVersion() {
        return mobileDataVersion;
    }

    public void setMobileDataVersion(String mobileDataVersion) {
        this.mobileDataVersion = mobileDataVersion;
    }
}
