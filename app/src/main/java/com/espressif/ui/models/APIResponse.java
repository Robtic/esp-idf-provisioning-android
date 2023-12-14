package com.espressif.ui.models;

import com.google.gson.annotations.SerializedName;

public class APIResponse {

    @SerializedName("message")
    private String message;

    public APIResponse()
    {

    }

    public String getMessage()
    {
        return message;
    }
}
