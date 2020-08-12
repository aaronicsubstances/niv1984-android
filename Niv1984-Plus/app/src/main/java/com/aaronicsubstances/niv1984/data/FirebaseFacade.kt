package com.aaronicsubstances.niv1984.data

import com.aaronicsubstances.niv1984.models.LatestVersionCheckResult
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.slf4j.LoggerFactory
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object FirebaseFacade {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun getConfItems(): LatestVersionCheckResult? =
        suspendCoroutine { cont ->
            val db = Firebase.firestore
            db.collection("conf")
                .get()
                .addOnSuccessListener { result ->
                    for (document in result) {
                        logger.debug("${document.id} => ${document.data}")
                    }
                    val configValues = result.toObjects(ConfigValue::class.java)
                    val converted = LatestVersionCheckResult()
                    try {
                        converted.forceUpgradeMessage = configValues.first {
                            it.key == "version.latest.forceUpgradeMsg"
                        }.value
                        converted.recommendUpgradeMessage = configValues.first {
                            it.key == "version.latest.recommendUpgradeMsg"
                        }.value
                        converted.latestVersion = configValues.first {
                            it.key == "version.latest.name"
                        }.value
                        converted.latestVersionCode = configValues.first {
                            it.key == "version.latest.code"
                        }.value.toLong()
                        cont.resume(converted)
                    }
                    catch (ex: Exception) {
                        logger.error("Error interpreting conf documents.", ex)
                        cont.resume(null)
                    }
                }
                .addOnFailureListener { exception ->
                    logger.warn("Error getting conf documents.", exception)
                    cont.resume(null)
                }
        }

    data class ConfigValue(val key: String = "",
                           val value: String = "")
}