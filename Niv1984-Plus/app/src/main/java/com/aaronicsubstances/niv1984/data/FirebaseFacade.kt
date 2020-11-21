package com.aaronicsubstances.niv1984.data

import com.aaronicsubstances.niv1984.BuildConfig
import com.aaronicsubstances.niv1984.models.LatestVersionCheckResult
//import com.aaronicsubstances.niv1984.utils.AppUtils
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
//import com.google.firebase.storage.ktx.storage
import org.slf4j.LoggerFactory
//import java.nio.charset.Charset
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object FirebaseFacade {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun getConfItems(): LatestVersionCheckResult? =
        suspendCoroutine { cont ->
            val db = Firebase.firestore
            db.collection("conf-${BuildConfig.BUILD_TYPE}")
                .limit(1)
                .get()
                .addOnSuccessListener { result ->
                    try {
                        val configValue = result.documents[0].toObject(LatestVersionCheckResult::class.java)
                        logger.info("Successfully parsed conf document: $configValue")
                        cont.resume(configValue)
                    }
                    catch (ex: Exception) {
                        logger.warn("Error interpreting conf document.", ex)
                        cont.resume(null)
                    }
                }
                .addOnFailureListener { exception ->
                    logger.warn("Error getting conf document.", exception)
                    cont.resume(null)
                }
        }

    /*suspend fun getConfItems(): LatestVersionCheckResult? =
        suspendCoroutine { cont ->
            val storage = Firebase.storage

            // Create a storage reference from our app
            val storageRef = storage.reference

            storageRef.child("conf-${BuildConfig.BUILD_TYPE}.json")
                    //storage.getReferenceFromUrl("gs://niv84-272d6.appspot.com/conf-debug.json")
                .getBytes(1_048_576L) //1MB
                .addOnSuccessListener {
                    try {
                        val config = AppUtils.deserializeFromJson(
                            it.toString(
                                Charset.forName(AppUtils.DEFAULT_CHARSET)
                            ),
                            ConfigValue::class.java
                        )
                        logger.info("Obtained configuration successfully: $config")
                        cont.resume(config.latestVersionInfo)
                    }
                    catch (ex: Exception) {
                        logger.error("Error interpreting configuration. ", ex)
                        cont.resume(null)
                    }
                }.addOnFailureListener { exception ->
                    logger.warn("Error getting configuration. ", exception)
                    cont.resume(null)
                }
        }

    data class ConfigValue(var latestVersionInfo: LatestVersionCheckResult)*/
}