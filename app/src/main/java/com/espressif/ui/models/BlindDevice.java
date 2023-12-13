package com.espressif.ui.models;

import android.net.nsd.NsdServiceInfo;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.espressif.provisioning.WiFiAccessPoint;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class BlindDevice implements Parcelable {

    private NsdServiceInfo serviceInfo;

    public BlindDevice()
    {

    }

    public BlindDevice(NsdServiceInfo serviceInfo)
    {
        this.serviceInfo = serviceInfo;
    }

    private BlindDevice(Parcel in) {

        serviceInfo.setHost(InetSocketAddress.createUnresolved(in.readString(),in.readInt()).getAddress());
        serviceInfo.setPort(in.readInt());
    }

    public InetAddress getAddress()
    {
        return serviceInfo.getHost();
    }

    public int getPort()
    {
        return serviceInfo.getPort();
    }

    public String getServiceName()
    {
        return serviceInfo.getServiceName();
    }

    public static final Creator<BlindDevice> CREATOR = new Creator<BlindDevice>() {
        @Override
        public BlindDevice createFromParcel(Parcel in) {
            return new BlindDevice(in);
        }

        @Override
        public BlindDevice[] newArray(int size) {
            return new BlindDevice[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
        parcel.writeInt(serviceInfo.getPort());
        parcel.writeString(serviceInfo.getHost().getHostAddress());
    }
}
