package com.aaronicsubstances.niv1984.data

import com.aaronicsubstances.niv1984.BuildConfig
import com.aaronicsubstances.niv1984.models.LatestVersionCheckResult
import com.aaronicsubstances.niv1984.ui.foreword.ForewordResource
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.slf4j.LoggerFactory
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object FirebaseFacade {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun fetchAboutResource(relativePath: String): ForewordResource? =
        suspendCoroutine { cont ->
            val db = Firebase.firestore
            db.collection("about-${BuildConfig.BUILD_TYPE}")
                .document(relativePath)
                .get()
                .addOnSuccessListener {
                    val result = it.toObject(ForewordResource::class.java)
                    cont.resume(result)
                }
                .addOnFailureListener {ex ->
                    logger.warn("Error getting about resource at $relativePath: ", ex)
                    cont.resume(null)
                }
        }

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
}