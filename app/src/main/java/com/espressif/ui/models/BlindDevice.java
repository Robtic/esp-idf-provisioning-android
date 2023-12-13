package com.espressif.ui.models;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.espressif.provisioning.WiFiAccessPoint;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class BlindDevice implements Parcelable {

    private InetAddress address;
    private int port;

    public BlindDevice()
    {

    }

    public BlindDevice(InetAddress addr, int port)
    {
        this.address = addr;
        this.port = port;
    }

    private BlindDevice(Parcel in) {

        address = InetSocketAddress.createUnresolved(in.readString(),in.readInt()).getAddress();
        port = in.readInt();
    }

    public InetAddress getAddress()
    {
        return address;
    }

    public int getPort()
    {
        return port;
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
        parcel.writeInt(port);
        parcel.writeString(address.toString());
    }
}
