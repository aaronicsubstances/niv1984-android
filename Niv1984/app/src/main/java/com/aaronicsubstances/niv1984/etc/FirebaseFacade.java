package com.aaronicsubstances.niv1984.etc;

import com.aaronicsubstances.niv1984.BuildConfig;
import com.google.firebase.firestore.FirebaseFirestore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FirebaseFacade {
    private static final Logger LOGGER = LoggerFactory.getLogger(FirebaseFacade.class);

    public static void getConfItems(VersionCheckCallback cb) {
        Utils.EXECUTOR_INSTANCE.execute(() -> {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("conf-" + BuildConfig.BUILD_TYPE)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(result -> {
                        try {
                            VersionCheckResponse configValue = result.getDocuments().get(0).toObject(VersionCheckResponse.class);
                            LOGGER.debug("Successfully parsed conf document: {}", configValue);
                            Utils.HANDLER_INSTANCE.post(() -> cb.accept(configValue));
                        }
                        catch (Exception ex) {
                            LOGGER.warn("Error interpreting conf document.", ex);
                            Utils.HANDLER_INSTANCE.post(() -> cb.accept(null));
                        }
                    })
                    .addOnFailureListener( exception -> {
                        LOGGER.warn("Error getting conf document.", exception);
                        Utils.HANDLER_INSTANCE.post(() -> cb.accept(null));
                    });
        });
    }
}
