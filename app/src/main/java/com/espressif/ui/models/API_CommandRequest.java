package com.espressif.ui.models;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.net.InetAddress;

public class API_CommandRequest {

    @SerializedName("action")
    private CommandActionEnum action;

    public API_CommandRequest()
    {

    }

    public CommandActionEnum getAction()
    {
        return action;
    }

    public void setAction(CommandActionEnum action)
    {
        this.action = action;
    }


}
