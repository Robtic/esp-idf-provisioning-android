package com.espressif.ui.adapters;

import static com.espressif.ui.models.CommandActionEnum.MOVE_DOWN;
import static com.espressif.ui.models.CommandActionEnum.MOVE_STOP;
import static com.espressif.ui.models.CommandActionEnum.MOVE_UP;

import android.app.Activity;
import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.espressif.provisioning.ESPConstants;
import com.espressif.provisioning.WiFiAccessPoint;
import com.espressif.ui.models.API_CommandRequest;
import com.espressif.ui.models.BlindDevice;
import com.espressif.ui.models.CommandActionEnum;
import com.espressif.ui.models.CommandRequest;
import com.espressif.wifi_provisioning.R;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class DeviceCardAdapter extends ArrayAdapter<BlindDevice> {

    private Context context;
    private ArrayList<BlindDevice> deviceList;

    private DeviceAdapterInterface mClickListener = null;

    public DeviceCardAdapter(Context context, int resource, ArrayList<BlindDevice> deviceList,DeviceAdapterInterface listener){
        super(context, resource, deviceList);
        this.deviceList = deviceList;
        this.context = context;
        this.mClickListener = listener;
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        BlindDevice blindDevice = deviceList.get(position);

        //get the inflater and inflate the XML layout for each item
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.device_row, null);
        Button btn_raise = view.findViewById(R.id.btn_raise);
        Button btn_lower = view.findViewById(R.id.btn_lower);
        Button btn_stop = view.findViewById(R.id.btn_stop);

        TextView ip_text = view.findViewById(R.id.device_ip);
        ip_text.setText(blindDevice.getAddress().getHostAddress());
        ip_text.setVisibility(View.VISIBLE);

        btn_raise.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mClickListener != null)
                {
                    API_CommandRequest req_body = new API_CommandRequest();
                    req_body.setAction(MOVE_UP);
                    CommandRequest req = new CommandRequest();
                    req.setAddress(blindDevice.getAddress());
                    req.setPort(blindDevice.getPort());
                    req.setRequestBody(req_body);
                    mClickListener.onBtnClick(req);

                    btn_raise.setVisibility(View.INVISIBLE);
                    btn_lower.setVisibility(View.INVISIBLE);
                    btn_stop.setVisibility(View.VISIBLE);
                }
            }
        });

        btn_lower.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mClickListener != null)
                {
                    API_CommandRequest req_body = new API_CommandRequest();
                    req_body.setAction(MOVE_DOWN);
                    CommandRequest req = new CommandRequest();
                    req.setAddress(blindDevice.getAddress());
                    req.setPort(blindDevice.getPort());
                    req.setRequestBody(req_body);
                    mClickListener.onBtnClick(req);

                    btn_raise.setVisibility(View.INVISIBLE);
                    btn_lower.setVisibility(View.INVISIBLE);
                    btn_stop.setVisibility(View.VISIBLE);
                }
            }
        });

        btn_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mClickListener != null)
                {
                    API_CommandRequest req_body = new API_CommandRequest();
                    req_body.setAction(MOVE_STOP);
                    CommandRequest req = new CommandRequest();
                    req.setAddress(blindDevice.getAddress());
                    req.setPort(blindDevice.getPort());
                    req.setRequestBody(req_body);
                    mClickListener.onBtnClick(req);

                    btn_raise.setVisibility(View.VISIBLE);
                    btn_lower.setVisibility(View.VISIBLE);
                    btn_stop.setVisibility(View.INVISIBLE);
                }
            }
        });

        return view;
    }

    public int getItemCount() {
        return deviceList.size();
    }

}
