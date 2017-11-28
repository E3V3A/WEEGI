package com.senordesign.weegi;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnItemSelected;
import io.resourcepool.jarpic.client.SsdpClient;
import io.resourcepool.jarpic.client.SsdpClientImpl;
import io.resourcepool.jarpic.model.DiscoveryListener;
import io.resourcepool.jarpic.model.DiscoveryRequest;
import io.resourcepool.jarpic.model.SsdpService;
import io.resourcepool.jarpic.model.SsdpServiceAnnouncement;
import retrofit2.Retrofit;


import com.senordesign.weegi.web.services.CytonService;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Thread.sleep;

public class MainActivity extends AppCompatActivity {

    private static final String OPENBCI_DEVICE_TYPE = "urn:schemas-upnp-org:device:Basic:1";
    private static final String TAG = "com.seniordesign.weegi";
    private Retrofit mRetrofit;
    private CytonService mCytonService;

    private Spinner mDeviceSpinner;
    private List<String> deviceList;
    private ArrayAdapter<String> deviceListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        deviceList = new ArrayList<>();
        deviceList.add(getString(R.string.searching));
        deviceListAdapter = new ArrayAdapter<>(
                getApplicationContext(), android.R.layout.simple_spinner_item, deviceList);
        deviceListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mDeviceSpinner = findViewById(R.id.device_spinner);
        mDeviceSpinner.setAdapter(deviceListAdapter);

        updateDeviceList();
    }

    @OnItemSelected(R.id.device_spinner)
    public void deviceSelected(Spinner spinner, int position) {
        Log.d(TAG, "device_spinner");
        checkRetrofitClient();
        // TODO
        // try connecting
        // if failure, then show toast and refresh device list
    }

    @OnClick(R.id.device_refresh_button)
    public void onDeviceRefreshClick() {
        Log.d(TAG, "device_refresh_button");
        updateDeviceList();
    }

    @OnClick(R.id.start_recording_btn)
    public void onStartRecordingClick() {
        Log.d(TAG, "start_recording_btn");
        checkRetrofitClient();
    }

    @OnClick(R.id.stop_recording_btn)
    public void onStopRecordingClick() {
        Log.d(TAG, "stop_recording_btn");
        checkRetrofitClient();
    }

    private void updateDeviceList() {
        deviceList.clear();
        deviceListAdapter.clear();

        SsdpClient client = new SsdpClientImpl();
        DiscoveryRequest networkStorageDevice = DiscoveryRequest.builder()
                .serviceType(OPENBCI_DEVICE_TYPE)
                .build();
        client.discoverServices(networkStorageDevice, new DiscoveryListener() {
            @Override
            public void onServiceDiscovered(SsdpService service) {
                deviceList.add(service.getRemoteIp().toString());
                Log.i(TAG, "Service discovered: getLocation = " + service.getLocation());
                Log.i(TAG, "Service discovered: getSerialNumber = " + service.getSerialNumber());
                Log.i(TAG, "Service discovered: getServiceType = " + service.getServiceType());
                Log.i(TAG, "Service discovered: getRemoteIp = " + service.getRemoteIp().toString());
                Log.i(TAG, "Service discovered: isExpired = " + service.isExpired());
                Log.i(TAG, "Service discovered: " + service);
            }

            @Override
            public void onServiceAnnouncement(SsdpServiceAnnouncement announcement) {
                Log.i(TAG, "Service announced something: " + announcement);
            }

            @Override
            public void onFailed(Exception ex) {
                Log.i(TAG, "Service discovery failed: " + ex.getMessage());
            }
        });

        try {
            sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        client.stopDiscovery();
        deviceListAdapter = new ArrayAdapter<>(
                getApplicationContext(), android.R.layout.simple_spinner_item, deviceList);
        deviceListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        deviceListAdapter.notifyDataSetChanged();
    }

    private void checkRetrofitClient() {
        if (mRetrofit == null)
            initializeRetrofitClient();

        if (!mRetrofit.baseUrl().toString().contains(mDeviceSpinner.getSelectedItem().toString()))  // TODO crashes
            initializeRetrofitClient();
    }

    private void initializeRetrofitClient() {
        mRetrofit = new Retrofit.Builder()
                .baseUrl("http://" + (mDeviceSpinner.getSelectedItem() != null
                        ? mDeviceSpinner.getSelectedItem().toString()
                        : "0.0.0.0") + "/")
                .build();

        mCytonService = mRetrofit.create(CytonService.class);
    }
}
