package com.espressif.ui.models;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface DeviceAPI {
    @POST("/cmd")
    Call<APIResponse> setRequestCommand(@Body API_CommandRequest request);
}
