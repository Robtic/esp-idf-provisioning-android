package com.espressif.ui.models;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import java.net.InetAddress;

public class CommandRequest {

    private API_CommandRequest request;
    private InetAddress targetIP;

    private int item_pos;

    private int targetPort;
    public CommandRequest()
    {

    }

    public void setRequestBody(API_CommandRequest req)
    {
        this.request = req;
    }

    public API_CommandRequest getRequestBody()
    {
        return request;
    }

    public InetAddress getAddress()
    {
        return targetIP;
    }
    public void setAddress(InetAddress address)
    {
        this.targetIP = address;
    }

    public void setPort(int port)
    {
        this.targetPort = port;
    }

    public int getPort()
    {
        return this.targetPort;
    }

    public int getItemPosition()
    {
        return item_pos;
    }

    public void setItemPosition(int position)
    {
        this.item_pos = position;
    }
    @NonNull
    @Override
    public String toString()
    {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
