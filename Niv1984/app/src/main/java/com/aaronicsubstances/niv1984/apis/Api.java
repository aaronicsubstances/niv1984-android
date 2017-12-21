package com.aaronicsubstances.niv1984.apis;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

/**
 * Created by Aaron on 8/21/2017.
 */

public interface Api {

    @POST("mobile/versions/latest")
    Call<VersionCheckResponse> checkLatestVersion(@Header("Authorization") String authorization,
                                                  @Body DefaultApiRequestModel model);
}
