// Copyright 2020 Espressif Systems (Shanghai) PTE LTD
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.espressif.ui.activities;

import static com.espressif.ui.models.CommandActionEnum.MOVE_STOP;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.espressif.AppConstants;
import com.espressif.provisioning.ESPConstants;
import com.espressif.provisioning.ESPProvisionManager;
import com.espressif.ui.adapters.APICallback;
import com.espressif.ui.adapters.DeviceAdapterInterface;
import com.espressif.ui.adapters.DeviceCardAdapter;
import com.espressif.ui.adapters.WiFiListAdapter;
import com.espressif.ui.models.APIResponse;
import com.espressif.ui.models.API_CommandRequest;
import com.espressif.ui.models.BlindDevice;
import com.espressif.ui.models.CommandActionEnum;
import com.espressif.ui.models.CommandRequest;
import com.espressif.ui.models.DeviceAPI;
import com.espressif.ui.models.DeviceAPIClient;
import com.espressif.wifi_provisioning.BuildConfig;
import com.espressif.wifi_provisioning.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class EspMainActivity extends AppCompatActivity {

    private static final String TAG = EspMainActivity.class.getSimpleName();

    private DeviceAPI apiInterface;

    // Request codes
    private static final int REQUEST_LOCATION = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    private ESPProvisionManager provisionManager;
    private FloatingActionButton btnAddDevice;

    private FloatingActionButton btnRefreshDevices;
    private ImageView ivEsp;
    private SharedPreferences sharedPreferences;
    private String deviceType;

    private NsdManager nsdManager;
    private NsdManager.DiscoveryListener discoveryListener;
    private final String SERVICE_TYPE = "_http._tcp";

    private ListView foundDeviceListView;
    private DeviceCardAdapter foundDevicesAdapter;

    private ArrayList<BlindDevice> foundDevicesList;
    private Executor background_ex = Executors.newSingleThreadExecutor();

    private final NsdManager.ResolveListener resolveListener = new NsdManager.ResolveListener() {
        @Override
        public void onResolveFailed(NsdServiceInfo nsdServiceInfo, int i) {
            Log.e(TAG, "Resolve Failed. " + nsdServiceInfo);
            Log.e(TAG, "Resolve Failed " + i);
        }

        @Override
        public void onServiceResolved(NsdServiceInfo nsdServiceInfo) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Map<String, byte[]> attrs;
                    attrs = nsdServiceInfo.getAttributes();

                    if(!checkServiceKnown(nsdServiceInfo.getServiceName()))
                    {
                        foundDevicesList.add(new BlindDevice(nsdServiceInfo));
                        foundDevicesAdapter.notifyDataSetChanged();
                    }

                    Log.e(TAG, "Device count: "+foundDevicesList.size());
                }
            });

            Log.e(TAG, "Resolve Succeeded. " + nsdServiceInfo);
            Log.e(TAG, "IP " + nsdServiceInfo.getHost().getHostAddress()+" Port "+nsdServiceInfo.getPort());
        }
    };

    private boolean checkServiceKnown(String serviceName)
    {
        BlindDevice dev_found= foundDevicesList.stream().filter(c->c.getServiceName().equals(serviceName)).findFirst().orElse(null);

        return dev_found != null;
    }

    public void initializeDiscoveryListener() {
        discoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onDiscoveryStarted(String s) {
                Log.i(TAG, "onDiscoveryStarted: " + s);
            }

            @Override
            public void onServiceFound(NsdServiceInfo nsdServiceInfo) {
                Log.i(TAG, "onServiceFound: " + nsdServiceInfo.toString());
                if(nsdServiceInfo.getServiceName().startsWith("PROV"))
                {
                    nsdManager.resolveService(nsdServiceInfo,resolveListener);
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo nsdServiceInfo) {
                Log.i(TAG, "onServiceLost: " + nsdServiceInfo.toString());
            }

            @Override
            public void onDiscoveryStopped(String s) {
                Log.i(TAG, "onDiscoveryStopped: " + s);
            }

            @Override
            public void onStartDiscoveryFailed(String s, int i) {
                Log.i(TAG, "onStartDiscoveryFailed: " + s);
            }

            @Override
            public void onStopDiscoveryFailed(String s, int i) {
                Log.i(TAG, "onStopDiscoveryFailed: " + s);
            }
        };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_esp_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        initViews();

        sharedPreferences = getSharedPreferences(AppConstants.ESP_PREFERENCES, Context.MODE_PRIVATE);
        provisionManager = ESPProvisionManager.getInstance(getApplicationContext());

        nsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);
        discoverServices();
    }

    @Override
    protected void onResume() {
        super.onResume();

        deviceType = sharedPreferences.getString(AppConstants.KEY_DEVICE_TYPES, AppConstants.DEVICE_TYPE_DEFAULT);
        if (deviceType.equals("wifi")) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(AppConstants.KEY_DEVICE_TYPES, AppConstants.DEVICE_TYPE_DEFAULT);
            editor.apply();
        }

        deviceType = sharedPreferences.getString(AppConstants.KEY_DEVICE_TYPES, AppConstants.DEVICE_TYPE_DEFAULT);
        if (deviceType.equals(AppConstants.DEVICE_TYPE_BLE)) {
            ivEsp.setImageResource(R.drawable.ic_esp_ble);
        } else if (deviceType.equals(AppConstants.DEVICE_TYPE_SOFTAP)) {
            ivEsp.setImageResource(R.drawable.ic_esp_softap);
        } else {
            ivEsp.setImageResource(R.drawable.ic_esp);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        if (BuildConfig.isSettingsAllowed) {
            // Inflate the menu; this adds items to the action bar if it is present.
            getMenuInflater().inflate(R.menu.menu_settings, menu);
            return true;
        } else {
            menu.clear();
            return true;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_LOCATION) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {

                if (isLocationEnabled()) {
                    addDeviceClick();
                }
            }
        }

        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            Toast.makeText(this, "Bluetooth is turned ON, you can provision device now.", Toast.LENGTH_LONG).show();
        }
    }

    private void initViews() {

        ivEsp = findViewById(R.id.iv_esp);
        btnAddDevice = findViewById(R.id.btn_provision_device);
        btnAddDevice.setOnClickListener(addDeviceBtnClickListener);

        btnRefreshDevices = findViewById(R.id.btn_refresh_devices);
        btnRefreshDevices.setOnClickListener(refreshDevicesBtnClickListener);

        foundDeviceListView = findViewById(R.id.found_device_list);
        foundDevicesList = new ArrayList<>();
        foundDevicesAdapter = new DeviceCardAdapter(this, R.id.device_ip, foundDevicesList, new DeviceAdapterInterface() {
            @Override
            public void onBtnClick(CommandRequest req) {
                sendRequest(req);
            }
        });

        foundDeviceListView.setAdapter(foundDevicesAdapter);
        foundDeviceListView.setVisibility(View.VISIBLE);

        foundDeviceListView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {

            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
            }
        });

        TextView tvAppVersion = findViewById(R.id.tv_app_version);

        String version = "";
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        String appVersion = getString(R.string.app_version) + " - v" + version;
        tvAppVersion.setText(appVersion);
    }

    View.OnClickListener addDeviceBtnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {

                if (!isLocationEnabled()) {
                    askForLocation();
                    return;
                }
            }
            addDeviceClick();
        }
    };

    View.OnClickListener refreshDevicesBtnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            discoverServices();
        }
    };

    private void sendRequest(CommandRequest req) {
        APICallback api_callback = new APICallback();
        apiInterface = DeviceAPIClient.getClient(req.getAddress().getHostAddress(),req.getPort()).create(DeviceAPI.class);
        Call<APIResponse> apiResponseCall = apiInterface.setRequestCommand(req.getRequestBody());
        Log.i(TAG,"Sending Request: "+req.toString());
        apiResponseCall.enqueue(new Callback<APIResponse>() {
            @Override
            public void onResponse(Call<APIResponse> call, Response<APIResponse> response) {

                if(req.getRequestBody().getAction() != MOVE_STOP && response.isSuccessful())
                {
                    foundDevicesAdapter.showStop(req.getItemPosition());
                }
                else if(req.getRequestBody().getAction() == MOVE_STOP && response.isSuccessful())
                {
                    foundDevicesAdapter.showControls(req.getItemPosition());
                }
                else
                {
                    //Could not complete the request, don't change the UI
                }
            }

            @Override
            public void onFailure(Call<APIResponse> call, Throwable t) {
                call.cancel();
                Toast.makeText(EspMainActivity.this, "Unable to reach device. Please refresh list!", Toast.LENGTH_SHORT).show();
                Log.i(TAG,"Request failed. Cancelling...");
            }
        });
    }

    public void discoverServices() {
        stopDiscovery();  // Cancel any existing discovery request
        initializeDiscoveryListener();
        foundDevicesList.clear();
        foundDevicesAdapter.notifyDataSetChanged();
        nsdManager.discoverServices(
                SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
    }

    public void stopDiscovery() {
        if (discoveryListener != null) {
            try {
                nsdManager.stopServiceDiscovery(discoveryListener);
            } finally {
            }
            discoveryListener = null;
        }
    }

    private void addDeviceClick() {

        if (BuildConfig.isQrCodeSupported) {

            gotoQrCodeActivity();

        } else {

            if (deviceType.equals(AppConstants.DEVICE_TYPE_BLE) || deviceType.equals(AppConstants.DEVICE_TYPE_BOTH)) {

                final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                BluetoothAdapter bleAdapter = bluetoothManager.getAdapter();

                if (!bleAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                } else {
                    startProvisioningFlow();
                }
            } else {
                startProvisioningFlow();
            }
        }
    }

    private void startProvisioningFlow() {

        deviceType = sharedPreferences.getString(AppConstants.KEY_DEVICE_TYPES, AppConstants.DEVICE_TYPE_DEFAULT);
        final boolean isSec1 = sharedPreferences.getBoolean(AppConstants.KEY_SECURITY_TYPE, true);
        Log.d(TAG, "Device Types : " + deviceType);
        Log.d(TAG, "isSec1 : " + isSec1);
        int securityType = 0;
        if (isSec1) {
            securityType = 1;
        }

        if (deviceType.equals(AppConstants.DEVICE_TYPE_BLE)) {

            if (isSec1) {
                provisionManager.createESPDevice(ESPConstants.TransportType.TRANSPORT_BLE, ESPConstants.SecurityType.SECURITY_1);
            } else {
                provisionManager.createESPDevice(ESPConstants.TransportType.TRANSPORT_BLE, ESPConstants.SecurityType.SECURITY_0);
            }
            goToBLEProvisionLandingActivity(securityType);

        } else if (deviceType.equals(AppConstants.DEVICE_TYPE_SOFTAP)) {

            if (isSec1) {
                provisionManager.createESPDevice(ESPConstants.TransportType.TRANSPORT_SOFTAP, ESPConstants.SecurityType.SECURITY_1);
            } else {
                provisionManager.createESPDevice(ESPConstants.TransportType.TRANSPORT_SOFTAP, ESPConstants.SecurityType.SECURITY_0);
            }
            goToWiFiProvisionLandingActivity(securityType);

        } else {

            final String[] deviceTypes = {"BLE", "SoftAP"};
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(true);
            builder.setTitle(R.string.dialog_msg_device_selection);
            final int finalSecurityType = securityType;
            builder.setItems(deviceTypes, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int position) {

                    switch (position) {
                        case 0:

                            if (isSec1) {
                                provisionManager.createESPDevice(ESPConstants.TransportType.TRANSPORT_BLE, ESPConstants.SecurityType.SECURITY_1);
                            } else {
                                provisionManager.createESPDevice(ESPConstants.TransportType.TRANSPORT_BLE, ESPConstants.SecurityType.SECURITY_0);
                            }
                            goToBLEProvisionLandingActivity(finalSecurityType);
                            break;

                        case 1:

                            if (isSec1) {
                                provisionManager.createESPDevice(ESPConstants.TransportType.TRANSPORT_SOFTAP, ESPConstants.SecurityType.SECURITY_1);
                            } else {
                                provisionManager.createESPDevice(ESPConstants.TransportType.TRANSPORT_SOFTAP, ESPConstants.SecurityType.SECURITY_0);
                            }
                            goToWiFiProvisionLandingActivity(finalSecurityType);
                            break;
                    }
                    dialog.dismiss();
                }
            });
            builder.show();
        }
    }

    private void askForLocation() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setMessage(R.string.dialog_msg_gps);

        // Set up the buttons
        builder.setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), REQUEST_LOCATION);
            }
        });

        builder.setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private boolean isLocationEnabled() {

        boolean gps_enabled = false;
        boolean network_enabled = false;
        LocationManager lm = (LocationManager) getApplicationContext().getSystemService(Activity.LOCATION_SERVICE);

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
        }

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {
        }

        Log.d(TAG, "GPS Enabled : " + gps_enabled + " , Network Enabled : " + network_enabled);

        boolean result = gps_enabled || network_enabled;
        return result;
    }

    private void gotoQrCodeActivity() {
        Intent intent = new Intent(EspMainActivity.this, AddDeviceActivity.class);
        startActivity(intent);
    }

    private void goToBLEProvisionLandingActivity(int securityType) {

        Intent intent = new Intent(EspMainActivity.this, BLEProvisionLanding.class);
        intent.putExtra(AppConstants.KEY_SECURITY_TYPE, securityType);
        startActivity(intent);
    }

    private void goToWiFiProvisionLandingActivity(int securityType) {

        Intent intent = new Intent(EspMainActivity.this, ProvisionLanding.class);
        intent.putExtra(AppConstants.KEY_SECURITY_TYPE, securityType);
        startActivity(intent);
    }
}
