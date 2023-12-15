package com.espressif.ui.adapters;

import com.espressif.ui.models.APIResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class APICallback implements Callback<APIResponse>{
    private Response<APIResponse> resp;
    @Override
    public void onResponse(Call<APIResponse> call, Response<APIResponse> response) {
        this.resp = response;
    }

    @Override
    public void onFailure(Call call, Throwable t) {
        call.cancel();
        this.resp = null;
    }

    public Response<APIResponse> getLastResponse()
    {
        return resp;
    }
}
