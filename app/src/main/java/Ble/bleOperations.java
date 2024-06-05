package Ble;


import static android.bluetooth.le.ScanSettings.MATCH_MODE_AGGRESSIVE;
import static android.content.Context.BLUETOOTH_SERVICE;


import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.ParcelUuid;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.content.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

// this file do all the ble operations. for every ble operation there will be a callback available to register
@SuppressLint("MissingPermission")
public class bleOperations {
    private static final ParcelUuid SERVICE_UUID = ParcelUuid.fromString("00001190-0000-1000-8000-00805F9B34FB");

    private BluetoothManager bluetoothManager = null;
    private BluetoothAdapter bluetoothAdapter = null;
    private BluetoothLeAdvertiser bluetoothLeAdvertiser = null;
    private BluetoothLeScanner bluetoothLeScanner = null;
    private final AdvertiseCallback advertiseCallback = null;
    private ScanCallback scanCallback = null;
    Context ApplicationContext;

    private final HashMap<String,ScanningDevices>  devicesList = new HashMap<String,ScanningDevices>();

    public bleOperations(Context context){
        this.ApplicationContext = context;
        intializeScanCallback();
    }
    void ReleaseUtilSemaphore(){
        // finally release the semaphore on setting up with the message
        Common.bleOperationSemaphore.release();
    }
    // reply back to the messages
    void SendMessage(int MessageType, Object[] Messagedata, int Messagesize, int FromMessage){
         Communication Message = new Communication();
         Message.fromMessage = Constants.MessageFromBleOperation;
         Message.messageType = MessageType;
         Message.data = Messagedata;
         Message.messageSize = Messagesize;
         if(FromMessage == Constants.MessageFromBleUtil){
             try {
                 LogUtil.e(Constants.Log,"Message send from operations");
                 Common.pluginCommunicator_Queue.put(Message);

             } catch (InterruptedException e) {
                 LogUtil.e(Constants.Error, "SendMessage Failed: "+e.getMessage());
             }
         }
         else{
             LogUtil.e(Constants.Log,"Message not send"+FromMessage);
         }

        ReleaseUtilSemaphore();

    }
    void SendMessage(int MessageType, Object[] Messagedata, int Messagesize, int FromMessage,int Error){
        Communication Message = new Communication();
        Message.fromMessage = Constants.MessageFromBleOperation;
        Message.messageType = MessageType;
        Message.data = Messagedata;
        Message.messageSize = Messagesize;
        Message.Error = Error;
        if(FromMessage == Constants.MessageFromBleUtil){
            try {
                LogUtil.e(Constants.Log,"Message send from operations");
                Common.pluginCommunicator_Queue.put(Message);

            } catch (InterruptedException e) {
                LogUtil.e(Constants.Error, "SendMessage Failed: "+e.getMessage());
            }
        }
        else{
            LogUtil.e(Constants.Log,"Message not send"+FromMessage);
        }

        ReleaseUtilSemaphore();

    }
    void InitializeBle(int MessageFrom){
        if(bluetoothManager == null){
            bluetoothManager = (BluetoothManager) ApplicationContext.getSystemService(BLUETOOTH_SERVICE);
            bluetoothAdapter = bluetoothManager.getAdapter();
            bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
            SendMessage(Constants.INITIALIZE_BLE_RESPONSE,null,0,MessageFrom);
        }
        else{
            SendMessage(Constants.INITIALIZE_BLE_RESPONSE,null,0,MessageFrom);
            ReleaseUtilSemaphore();
        }
    }
    boolean isValid(){
        if(!bluetoothAdapter.isEnabled()){
            return false;
        }
        return bluetoothAdapter != null && bluetoothLeAdvertiser != null && bluetoothLeScanner != null;
    }
    boolean isBleExtendedSupported(){
        return bluetoothAdapter.isLeExtendedAdvertisingSupported();
    }


    void DeInitializeBle(int MessageFrom){
        if (bluetoothLeAdvertiser != null && advertiseCallback!=null) {
            // Stop BLE advertising if it is ongoing
            bluetoothLeAdvertiser.stopAdvertising(advertiseCallback);
            bluetoothLeAdvertiser = null;
        }

        if (bluetoothLeScanner != null && scanCallback!=null) {
            // Stop BLE scanning if it is ongoing
            bluetoothLeScanner.stopScan(scanCallback);
            bluetoothLeScanner = null;
        }
        bluetoothManager = null;
        bluetoothAdapter = null;
        bluetoothLeAdvertiser = null;
        bluetoothLeScanner = null;
        SendMessage(Constants.DEINITIALIZE_BLE_RESPONSE,null,0,MessageFrom);
    }
    void addorUpdateDevice(ScanResult scanresult){
         String deviceAddress = scanresult.getDevice().getAddress();
         ScanRecord scanRecord = scanresult.getScanRecord();
         String DeviceName = scanresult.getDevice().getName();
         int rssi = scanresult.getRssi();
         if(devicesList.containsKey(deviceAddress)){
            devicesList.get(deviceAddress).setDeviceName(DeviceName != null ? DeviceName : "Ble Device");
            devicesList.get(deviceAddress).setRssi(rssi);
         }
         else{
             ScanningDevices device = new ScanningDevices();
             device.setDeviceName(DeviceName);
             device.setRssi(rssi);
             devicesList.put(deviceAddress,device);
         }
    }
    void intializeScanCallback(){
        scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                addorUpdateDevice(result);
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);
                for (ScanResult result : results) {
                    addorUpdateDevice(result);
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                LogUtil.e(Constants.Error,"Scan Failed"+errorCode);
            }
        };
    }
    void stopScan(int MessageFrom){
        if(bluetoothLeScanner!=null){
            bluetoothLeScanner.flushPendingScanResults(scanCallback);
            bluetoothLeScanner.stopScan(scanCallback);
            SendMessage(Constants.STOPSCAN_RESPONSE,null,0,MessageFrom);
        }
        else{
            // bluetooth is not initialized
            SendMessage(Constants.STOPSCAN_RESPONSE,null,0,MessageFrom,Constants.BLE_NOT_INITIALIZED);
        }
    }
    void startScan(int MessageFrom){
        if(!isValid()){
            // bluetooth is not enabled
            SendMessage(Constants.STARTSCAN_RESPONSE,null,0,MessageFrom,Constants.BLE_NOT_ENABLE);
            return;
        }
        if (bluetoothLeScanner != null) {
            if(isBleExtendedSupported()) {
                ScanSettings settings = new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .setLegacy(false)
                        .setMatchMode(MATCH_MODE_AGGRESSIVE)
                        .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                        .build();
                ScanFilter filter = new ScanFilter.Builder()
                        .setServiceUuid(SERVICE_UUID)
                        .build();
                List<ScanFilter> scanFilters = new ArrayList<>();
                scanFilters.add(filter);
                bluetoothLeScanner.startScan(scanFilters, settings, scanCallback);
            }
            else{
                ScanSettings settings = new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .setMatchMode(MATCH_MODE_AGGRESSIVE)
                        .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                        .build();
                ScanFilter filter = new ScanFilter.Builder()
                        .setServiceUuid(SERVICE_UUID)
                        .build();
                List<ScanFilter> scanFilters = new ArrayList<>();
                scanFilters.add(filter);
                bluetoothLeScanner.startScan(scanFilters, settings, scanCallback);
            }
            SendMessage(Constants.STARTSCAN_RESPONSE,null,0,MessageFrom);
        }
        else{
            // blutooth is not intialized
            SendMessage(Constants.STARTSCAN_RESPONSE,null,0,MessageFrom,Constants.BLE_NOT_INITIALIZED);
        }

    }

    void sendScanningdata(int MessageFrom){
         if(devicesList.size() == 0){
            // no scanning data
             SendMessage(Constants.LISTDEVICE_RESPONSE,null,0,MessageFrom,Constants.BLE_NO_SCANNING_DATA);
         }
         else{
            SendMessage(Constants.LISTDEVICE_RESPONSE,new Object[]{devicesList.clone()},1,MessageFrom);
         }
    }
}
