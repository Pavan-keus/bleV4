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

import org.json.JSONArray;
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
    JSONObject write(String bleaddress,String characteristic,byte[] data){
        JSONObject object = new JSONObject();
        JSONArray array = new JSONArray();
        for(byte dataByte : data)
            array.put(dataByte);
        try {
            object.put("Type",Constants.WRITE_CHARACTERISTIC_REQUEST);
            JSONObject dataObject = new JSONObject();
            dataObject.put("deviceId",bleaddress);
            dataObject.put("characteristic",characteristic);
            dataObject.put("value",array);
            object.put("Data",dataObject);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        };
        return object;
    }
    JSONObject read(String bleaddress,String characteristic) {
        JSONObject object = new JSONObject();
        try {
            object.put("Type",Constants.READ_CHARACTERISTIC_REQUEST);
            JSONObject dataObject = new JSONObject();
            dataObject.put("deviceId",bleaddress);
            dataObject.put("characteristic",characteristic);
            object.put("Data",dataObject);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        };
        return object;
    }
    JSONObject notify(String bleaddress,String characteristic) {
        JSONObject object = new JSONObject();

        try {
            object.put("Type",Constants.NOTIFY_CHARACTERISTIC_REQUEST);
            JSONObject dataObject = new JSONObject();
            dataObject.put("deviceId",bleaddress);
            dataObject.put("characteristic",characteristic);
            object.put("Data",dataObject);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        };
        return object;
    }
    JSONObject setMtu(String bleAddress , int mtu){
        JSONObject object = new JSONObject();
        try {
            object.put("Type",Constants.SET_MTU_REQUEST);
            JSONObject dataObject = new JSONObject();
            dataObject.put("deviceId",bleAddress);
            dataObject.put("mtu",mtu);
            object.put("Data",dataObject);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        };
        return object;
    }
    JSONObject setPriority(String bleAddress , int Priority){
        JSONObject object = new JSONObject();
        try {
            object.put("Type",Constants.SET_PRIORITY_REQUEST);
            JSONObject dataObject = new JSONObject();
            dataObject.put("deviceId",bleAddress);
            dataObject.put("priority",Priority);
            object.put("Data",dataObject);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        };
        return object;
    }

    JSONObject setPhy(String bleAddress , int phy){
        JSONObject object = new JSONObject();
        try {
            object.put("Type",Constants.SET_PHY_REQUEST);
            JSONObject dataObject = new JSONObject();
            dataObject.put("deviceId",bleAddress);
            dataObject.put("phy",phy);
            object.put("Data",dataObject);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        };
        return object;
    }


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
        JSONObject bleStartScan = new JSONObject();
        try {
            bleStartScan.put("Type",Constants.STARTSCAN_REQUEST);
            bleStartScan.put("Data",new Object[0]);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        String channel = "f000a005-0451-4000-b000-000000000000";
        String sendbeacon = "f000a002-0451-4000-b000-000000000000";
        String ntwkState = "f000a001-0451-4000-b000-000000000000";
        JSONObject channel_read = read("0C:EC:80:95:AA:D8",channel);
        JSONObject channel_write = write("0C:EC:80:95:AA:D8",channel,new byte[]{0x26});
        JSONObject sendbeacon_write = write("0C:EC:80:95:AA:D8",sendbeacon,new byte[]{0x01});
        JSONObject nwkstate_notify =  notify("0C:EC:80:95:AA:D8",ntwkState);
        JSONObject mtu = setMtu("0C:EC:80:95:AA:D8",256);
        JSONObject phy = setPhy("0C:EC:80:95:AA:D8",Constants.PHY_LE_2M);
        JSONObject priority = setPriority("0C:EC:80:95:AA:D8",Constants.CONNECTION_PRIORITY_HIGH);
        communicator.sendMessageToBle(bleIntialiation);
        communicator.sendMessageToBle(bleStartScan);

        JSONObject connectDevice = new JSONObject();
        try {
            connectDevice.put("Type",Constants.CONNECT_REQUEST);
            JSONObject data = new JSONObject();
            data.put("deviceId","0C:EC:80:95:AA:D8");
            connectDevice.put("Data",data);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        JSONObject disconnectDevice = new JSONObject();
        try{
            disconnectDevice.put("Type",Constants.DISCONNECT_REQUEST);
            JSONObject data = new JSONObject();
            data.put("deviceId","0C:EC:80:95:AA:D8");
            disconnectDevice.put("Data",data);
        }
        catch (Exception e){
            e.printStackTrace();
        }
        button.setOnClickListener(new View.OnClickListener() {
            int count = 0;
            @Override
            public void onClick(View v) {
                if(count %2 == 0)
                 communicator.sendMessageToBle(connectDevice);
                else
                    communicator.sendMessageToBle(disconnectDevice);
                count++;
            }
        });
        new Thread(new Runnable() {
            @Override
            public void run() {

                    try {
                        Thread.sleep(2000 );
                        communicator.sendMessageToBle(connectDevice);
                        Thread.sleep(5000);
                        communicator.sendMessageToBle(mtu);
                        Thread.sleep(2000);
                        communicator.sendMessageToBle(phy);
                        Thread.sleep(2000);
                        communicator.sendMessageToBle(priority);
                        Thread.sleep(3000);

                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
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

    @Override
    public void bleConnectDevice(JSONObject response) {
        Log.e(Constants.Log,response.toString());
    }

    @Override
    public void bleDisconnectDevice(JSONObject response) {
        Log.e(Constants.Log,response.toString());
    }

    @Override
    public void bleWriteCharacteristic(JSONObject response) {
        Log.e(Constants.Log,response.toString());
    }

    @Override
    public void bleReadCharacteristic(JSONObject response) {
        Log.e(Constants.Log,response.toString());
    }

    @Override
    public void bleNotifyCharacteristic(JSONObject response) {
        Log.e(Constants.Log,response.toString());
    }

    @Override
    public void blesetMtu(JSONObject response) {
        Log.e(Constants.Log,response.toString());
    }

    @Override
    public void blesetPhy(JSONObject response) {
        Log.e(Constants.Log,response.toString());
    }

    @Override
    public void bleNotifyCharacteristicData(JSONObject response) {
        Log.e(Constants.Log,response.toString());
    }

    @Override
    public void blesetPriority(JSONObject response) {
        Log.e(Constants.Log,response.toString());
    }
}