package com.example.blev4;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import Ble.Constants;
import Ble.bleToPlugin;
import Ble.pluginCommunicator;

public class MainActivity extends AppCompatActivity implements bleToPlugin {
    private static final int REQUEST_PERMISSIONS_CODE = 1;

    private final List<String> permissionsToRequest = new ArrayList<>();
    Button button;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        button = findViewById(R.id.button);
        pluginCommunicator communicator = new pluginCommunicator(getApplicationContext(),this);
        communicator.start();

        // ble Initialziation
        JSONObject bleIntialiation = new JSONObject();
        try {
            bleIntialiation.put("Type",Constants.INITIALIZE_BLE_REQUEST);
            bleIntialiation.put("Data",new Object[0]);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        JSONObject bleDeIntialiation = new JSONObject();
        try {
            bleDeIntialiation.put("Type",Constants.DEINITIALIZE_BLE_REQUEST);
            bleDeIntialiation.put("Data",new Object[0]);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        JSONObject bleStartScan = new JSONObject();
        try {
            bleStartScan.put("Type",Constants.STARTSCAN_REQUEST);
            bleStartScan.put("Data",new Object[0]);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        communicator.sendMessageToBle(bleIntialiation);
        JSONObject bleStopScan = new JSONObject();
        try {
            bleStopScan.put("Type",Constants.STOPSCAN_REQUEST);
            bleStopScan.put("Data",new Object[0]);
        } catch (JSONException e) {
            throw new RuntimeException(e);
            }
        JSONObject blegetDevices = new JSONObject();
        try {
            blegetDevices.put("Type",Constants.LISTDEVICE_REQUEST);
            blegetDevices.put("Data",new Object[0]);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        button.setOnClickListener(new View.OnClickListener() {
            int count = 0;
            @Override
            public void onClick(View v) {
                 if(count % 2 ==0)
                     communicator.sendMessageToBle(bleStartScan);
                 else
                     communicator.sendMessageToBle(bleStopScan);
                 count++;
            }
        });
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    try {
                        Thread.sleep(1000);
                        communicator.sendMessageToBle(blegetDevices);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }).start();
        checkAndRequestPermissions();
    }
    @SuppressLint("BatteryLife")
    private void checkAndRequestPermissions() {
        boolean permissionsGranted = true;
        permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissionsToRequest.add(Manifest.permission.BLUETOOTH_ADMIN);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN);
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_ADVERTISE);
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT);
        }
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        String []requiredPermissions = new String[permissionsToRequest.size()];
        permissionsToRequest.toArray(requiredPermissions);
        // Check if all required permissions are granted
        for (String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsGranted = false;
                break;
            }
        }
        if (!permissionsGranted) {
            // Request permissions
            ActivityCompat.requestPermissions(this, requiredPermissions, REQUEST_PERMISSIONS_CODE);
        }
        else{
            Intent intent = new Intent();
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                startActivity(intent);
            }

            // can do what ever we want
        }
    }

    @SuppressLint("BatteryLife")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSIONS_CODE) {
            boolean allPermissionsGranted = true;

            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (!allPermissionsGranted) {
                finish(); // Close the app
            }

            Intent intent = new Intent();
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                startActivity(intent);
            }

            // can do what ever we want
        }
    }

    @Override
    public void bleInitialization(JSONObject response) {
        Log.e(Constants.Log,response.toString());
    }

    @Override
    public void bleDeInitialization(JSONObject response) {
        Log.e(Constants.Log,response.toString());
    }

    @Override
    public void bleStartScan(JSONObject response) {
        Log.e(Constants.Log,response.toString());
    }

    @Override
    public void bleStopScan(JSONObject response) {
        Log.e(Constants.Log,response.toString());
    }

    @Override
    public void blegetDevices(JSONObject response) {
        try {
            Log.e(Constants.Log,response.getJSONObject("Data").getJSONArray("Devices").length()+"");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}