package com.aaronicsubstances.niv1984.models

data class LatestVersionCheckResult(var latestVersion: String = "",
                                    var latestVersionCode: Long = 0L,
                                    var forceUpgradeMessage: String = "",
                                    var recommendUpgradeMessage: String = "")