package com.espressif.ui.models;

import com.google.gson.annotations.SerializedName;

public enum CommandActionEnum
{
    @SerializedName("RAISE")
    MOVE_UP,
    @SerializedName("LOWER")
    MOVE_DOWN,
    @SerializedName("STOP")
    MOVE_STOP
}
