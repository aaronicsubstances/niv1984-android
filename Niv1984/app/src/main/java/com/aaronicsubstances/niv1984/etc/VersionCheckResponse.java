/*
 *  (c) Aaronic Substances.
 */
package com.aaronicsubstances.niv1984.etc;

/**
 *
 * @author Aaron
 */
public class VersionCheckResponse {
    private String versionName;
    private int versionCode;
    private String forceUpgrade;
    private String recommendUpgrade;

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public int getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(int versionCode) {
        this.versionCode = versionCode;
    }

    public String getForceUpgrade() {
        return forceUpgrade;
    }

    public void setForceUpgrade(String forceUpgrade) {
        this.forceUpgrade = forceUpgrade;
    }

    public String getRecommendUpgrade() {
        return recommendUpgrade;
    }

    public void setRecommendUpgrade(String recommendUpgrade) {
        this.recommendUpgrade = recommendUpgrade;
    }

    @Override
    public String toString() {
        return "VersionCheckResponse{" +
                "versionName='" + versionName + '\'' +
                ", versionCode=" + versionCode +
                ", forceUpgrade='" + forceUpgrade + '\'' +
                ", recommendUpgrade='" + recommendUpgrade + '\'' +
                '}';
    }
}
