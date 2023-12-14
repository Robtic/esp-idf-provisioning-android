package com.espressif.ui.adapters;

import com.espressif.ui.models.CommandRequest;

public interface DeviceAdapterInterface {

    public abstract void onBtnClick(CommandRequest req);
}
