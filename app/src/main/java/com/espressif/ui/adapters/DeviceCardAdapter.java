package com.espressif.ui.adapters;

import android.app.Activity;
import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.espressif.provisioning.ESPConstants;
import com.espressif.provisioning.WiFiAccessPoint;
import com.espressif.ui.models.BlindDevice;
import com.espressif.wifi_provisioning.R;

import java.util.ArrayList;
import java.util.List;

public class DeviceCardAdapter extends ArrayAdapter<BlindDevice> {

    private Context context;
    private ArrayList<BlindDevice> deviceList;

    public DeviceCardAdapter(Context context, int resource, ArrayList<BlindDevice> deviceList){
        super(context, resource, deviceList);
        this.deviceList = deviceList;
        this.context = context;
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        BlindDevice blindDevice = deviceList.get(position);

        //get the inflater and inflate the XML layout for each item
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.device_row, null);

        TextView ip_text = view.findViewById(R.id.device_ip);

        ip_text.setText(blindDevice.getAddress().getHostAddress());

        ip_text.setVisibility(View.VISIBLE);

        return view;
    }

    public int getItemCount() {
        return deviceList.size();
    }

}
