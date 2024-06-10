package Ble;


import static android.bluetooth.le.ScanSettings.MATCH_MODE_AGGRESSIVE;
import static android.content.Context.BLUETOOTH_SERVICE;


import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Build;
import android.os.ParcelUuid;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.content.Context;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// this file do all the ble operations. for every ble operation there will be a callback available to register

@SuppressLint("MissingPermission")
public class bleOperations {
    private static final ParcelUuid SERVICE_UUID = ParcelUuid.fromString("00001190-0000-1000-8000-00805F9B34FB");
    private static final long SCANUPDATEINTERVAL = 2000;

    private BluetoothManager bluetoothManager = null;
    private BluetoothAdapter bluetoothAdapter = null;
    private BluetoothLeAdvertiser bluetoothLeAdvertiser = null;
    private BluetoothLeScanner bluetoothLeScanner = null;
    private final AdvertiseCallback advertiseCallback = null;
    private ScanCallback scanCallback = null;
    private final boolean isFilteringEnabled = false;
    Context ApplicationContext;
    ScanSettings settings = null;
    private static final UUID CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    ScanFilter filter = null;
    List<ScanFilter> scanFilters;
    boolean isRequested = false;
    boolean isremovingOffline = false;
    private ConcurrentHashMap<String,ScanningDevices> devicesList_temp = new ConcurrentHashMap<String,ScanningDevices>();
    private final ConcurrentHashMap<String,ScanningDevices> devicesList = new ConcurrentHashMap<String,ScanningDevices>();
    private final Queue<ScanResult> scanResultQueue = new LinkedList<ScanResult>();
    final bleGattCallbackClass gattCallback = new bleGattCallbackClass();
    Timer scanDataUpdateTimer = new Timer();
    public bleOperations(Context context){
        this.ApplicationContext = context;
        intializeScanCallback();
    }
    void ReleaseUtilSemaphore(){
        // finally release the semaphore on setting up with the message
        if(!isRequested)
            return;
        isRequested = false;
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
        }
        ReleaseUtilSemaphore();
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
        ReleaseUtilSemaphore();
    }
    void addDeviceToList(String deviceAddress, String DeviceName, int rssi, BluetoothDevice bleDevice){
        if(devicesList.containsKey(deviceAddress)){
            devicesList.get(deviceAddress).setDeviceName(DeviceName != null ? DeviceName : "Ble Device");
            devicesList.get(deviceAddress).setRssi(rssi);
        }
        else{
            ScanningDevices device = new ScanningDevices();
            device.setDeviceName(DeviceName != null ? DeviceName : "Ble Device");
            device.setRssi(rssi);
            device.setConnected(false);
            device.setDiscoveredDevice(bleDevice);
            devicesList.put(deviceAddress,device);
        }
        devicesList.get(deviceAddress).setLastScanResultTime(getTimeinMillis());
    }
    void updateDevices(){
        while(scanResultQueue.size()>0){
            ScanResult scanresult = scanResultQueue.remove();
            String deviceAddress = scanresult.getDevice().getAddress();
            ScanRecord scanRecord = scanresult.getScanRecord();
            String DeviceName = scanresult.getDevice().getName();
            int rssi = scanresult.getRssi();
            addDeviceToList(deviceAddress,DeviceName,rssi,scanresult.getDevice());
        }
    }
    void addorUpdateDevice(ScanResult scanresult){
         if(isremovingOffline){
             scanResultQueue.add(scanresult);
         }
         else{
             String deviceAddress = scanresult.getDevice().getAddress();
             ScanRecord scanRecord = scanresult.getScanRecord();
             String DeviceName = scanresult.getDevice().getName();
             int rssi = scanresult.getRssi();
             if(!isremovingOffline && scanResultQueue.size() >0){
                 updateDevices();
             }
             addDeviceToList(deviceAddress,DeviceName,rssi,scanresult.getDevice());
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
    void buildScanSettings(){
        if(settings!=null)
            return;
        if(isBleExtendedSupported()) {
            settings   = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setLegacy(false)
                    .setMatchMode(MATCH_MODE_AGGRESSIVE)
                    .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                    .build();
            filter = new ScanFilter.Builder()
                    .setServiceUuid(SERVICE_UUID)
                    .build();
            scanFilters = new ArrayList<>();
            scanFilters.add(filter);
        }
        else{
            settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setMatchMode(MATCH_MODE_AGGRESSIVE)
                    .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                    .build();
            filter = new ScanFilter.Builder()
                    .setServiceUuid(SERVICE_UUID)
                    .build();
            scanFilters = new ArrayList<>();
            scanFilters.add(filter);

        }
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
        ReleaseUtilSemaphore();
        stopScanUpdateTimer();
    }
    void startScan(int MessageFrom){
        if(!isValid()){
            // bluetooth is not enabled
            SendMessage(Constants.STARTSCAN_RESPONSE,null,0,MessageFrom,Constants.BLE_NOT_ENABLE);
            ReleaseUtilSemaphore();
            return;
        }
        if (bluetoothLeScanner != null) {
            buildScanSettings();
            setCurrentTimelines();
            bluetoothLeScanner.startScan(scanFilters, settings, scanCallback);
            SendMessage(Constants.STARTSCAN_RESPONSE,null,0,MessageFrom);
        }
        else{
            // blutooth is not intialized
            SendMessage(Constants.STARTSCAN_RESPONSE,null,0,MessageFrom,Constants.BLE_NOT_INITIALIZED);
        }
        ReleaseUtilSemaphore();
        if(isFilteringEnabled)
            startScanUpdateTimer();

    }

    void sendScanningdata(int MessageFrom){
         if(devicesList.size() == 0){
            // no scanning data
             SendMessage(Constants.LISTDEVICE_RESPONSE,null,0,MessageFrom,Constants.BLE_NO_SCANNING_DATA);
         }
         else{
             if(isremovingOffline)
                    SendMessage(Constants.LISTDEVICE_RESPONSE,new Object[]{new ConcurrentHashMap<String,ScanningDevices>(devicesList_temp)},1,MessageFrom);
             else
                    SendMessage(Constants.LISTDEVICE_RESPONSE,new Object[]{new ConcurrentHashMap<String,ScanningDevices>(devicesList)},1,MessageFrom);
         }
        ReleaseUtilSemaphore();
    }

    void startScanUpdateTimer(){
         scanDataUpdateTimer.schedule(new TimerTask() {
             @Override
             public void run() {
                 removeOfflineDevices();
                 startScanUpdateTimer();
             }
         },SCANUPDATEINTERVAL);
    }
    long getTimeinMillis(){
        return Calendar.getInstance().getTimeInMillis();
    }
    void setCurrentTimelines(){
        for(ScanningDevices device : devicesList.values()){
            device.setLastScanResultTime(getTimeinMillis());
        }
    }
    void removeOfflineDevices(){
         isremovingOffline = true;
         devicesList_temp = new ConcurrentHashMap<String,ScanningDevices>(devicesList);
         long currentTime = getTimeinMillis();
         ConcurrentHashMap<String,ScanningDevices> temp = new ConcurrentHashMap<String,ScanningDevices>(devicesList);
         for(String key : temp.keySet()){
             if((!temp.get(key).isConnected()) && ((currentTime - temp.get(key).getLastScanResultTime()) > (SCANUPDATEINTERVAL+1000))){
                 devicesList.remove(key);
                 LogUtil.e(Constants.Log,"Device Removed"+key + " "+(currentTime-temp.get(key).getLastScanResultTime())+ " "+temp.get(key).getRssi());
             }
         }
         isremovingOffline = false;
    }
    void stopScanUpdateTimer(){
        scanDataUpdateTimer.cancel();
        scanDataUpdateTimer = new Timer();
    }
    BluetoothDevice checkDeviceExists(String DeviceAddress){
        if(devicesList.containsKey(DeviceAddress))
        {
           return devicesList.get(DeviceAddress).getDiscoveredDevice();
        }
        return null;
    }
    BluetoothGatt getDeviceGatt(String DeviceAddress){
        if(devicesList.containsKey(DeviceAddress) && devicesList.get(DeviceAddress).isConnected())
            return devicesList.get(DeviceAddress).getBleDevice();
        return null;
    }
    class bleGattCallbackClass extends BluetoothGattCallback{
        int messageFrom;
        @Override
        public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            super.onPhyUpdate(gatt, txPhy, rxPhy, status);
            SendMessage(Constants.SET_PHY_RESPONSE,new Object[]{gatt.getDevice().getAddress(),txPhy,rxPhy},3,messageFrom);
            ReleaseUtilSemaphore();

        }

        @Override
        public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            super.onPhyRead(gatt, txPhy, rxPhy, status);
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            String Addresss = gatt.getDevice().getAddress();
            if(newState == BluetoothProfile.STATE_CONNECTED){
                LogUtil.e(Constants.Log,"Device Connected"+Addresss);
                devicesList.get(Addresss).setConnected(true);
                devicesList.get(Addresss).setBleDevice(gatt);
                SendMessage(Constants.CONNECT_RESPONSE,new Object[]{gatt.getDevice().getAddress()},1,Constants.MessageFromBleUtil);
                ReleaseUtilSemaphore();
                gatt.discoverServices();


            }
            else if(newState == BluetoothProfile.STATE_DISCONNECTED){
                LogUtil.e(Constants.Log,"Device Disconnected"+Addresss);
                devicesList.get(Addresss).setConnected(false);
                devicesList.get(Addresss).setBleDevice(null);
                SendMessage(Constants.DISCONNECT_RESPONSE,new Object[]{Addresss},1,Constants.MessageFromBleUtil);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            if(status == BluetoothGatt.GATT_SUCCESS){

                LogUtil.e(Constants.Log,"Services Discovered");
                String Address = gatt.getDevice().getAddress();
                List<BluetoothGattService> services = gatt.getServices();
                for (BluetoothGattService service : services) {
                    List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                    // Process characteristics (read, write, identify)
                    for(BluetoothGattCharacteristic characteristic:characteristics){
                        bleCharacteristic bleCharacteristic = new bleCharacteristic();
                        bleCharacteristic.characteristic = characteristic;
                        bleCharacteristic.messagefrom = Constants.MessageFromBleUtil;
                        devicesList.get(Address).characteristicList.add(bleCharacteristic);
                    }
                }


            }
            else{
                // device service disconvery failed due to some other reason
                LogUtil.e(Constants.Error,"Services Discovered Failed");
            }
            // device got succesfully connected leave the semaphore

        }

        @Override
        public void onCharacteristicRead(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value, int status) {
            super.onCharacteristicRead(gatt, characteristic, value, status);
            int messageFrom = getCharacterMessageFrom(gatt.getDevice().getAddress(),characteristic.getUuid().toString());
            LogUtil.e(Constants.Log,"Characteristic Read");
            if(status == BluetoothGatt.GATT_SUCCESS)
                SendMessage(Constants.READ_CHARACTERISTIC_RESPONSE,new Object[]{gatt.getDevice().getAddress(),characteristic.getUuid().toString(),value},3,messageFrom);
            else{
                // something went failed
                LogUtil.e(Constants.Error,"Characteristic Read Failed");
            }
            ReleaseUtilSemaphore();
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            int messageFrom = getCharacterMessageFrom(gatt.getDevice().getAddress(),characteristic.getUuid().toString());
            LogUtil.e(Constants.Log,"Characteristic Write");
            if(status == BluetoothGatt.GATT_SUCCESS)
                SendMessage(Constants.WRITE_CHARACTERISTIC_RESPONSE,new Object[]{gatt.getDevice().getAddress(),characteristic.getUuid().toString()},2,messageFrom);
            else{
                // something went failed
                LogUtil.e(Constants.Error,"Characteristic Write Failed");
            }
            ReleaseUtilSemaphore();
        }

        @Override
        public void onCharacteristicChanged(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value) {
            super.onCharacteristicChanged(gatt, characteristic, value);
            int messageFrom = getCharacterMessageFrom(gatt.getDevice().getAddress(),characteristic.getUuid().toString());
            SendMessage(Constants.NOTIFY_CHARACTERISTIC_UPDATE_RESPONSE,new Object[]{gatt.getDevice().getAddress(),characteristic.getUuid().toString(),value},3,messageFrom);
        }

        @Override
        public void onDescriptorRead(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattDescriptor descriptor, int status, @NonNull byte[] value) {
            super.onDescriptorRead(gatt, descriptor, status, value);

        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            int messageFrom = getCharacterMessageFrom(gatt.getDevice().getAddress(),descriptor.getCharacteristic().getUuid().toString());
            if(status == BluetoothGatt.GATT_SUCCESS){
                SendMessage(Constants.NOTIFY_CHARACTERISTIC_RESPONSE,new Object[]{gatt.getDevice().getAddress(),descriptor.getCharacteristic().getUuid().toString()},2,messageFrom);
                LogUtil.e(Constants.Log,"Descriptor Write");
            }
            else{
                // descriptor write failed
                LogUtil.e(Constants.Error,"Descriptor Write Failed");
            }
            ReleaseUtilSemaphore();
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            SendMessage(Constants.SET_MTU_RESPONSE,new Object[]{gatt.getDevice().getAddress(),mtu},2,messageFrom);
            ReleaseUtilSemaphore();
        }
    }
    int getCharacterMessageFrom(String Address,String CharacteristicUUid){
         for(bleCharacteristic characteristic:devicesList.get(Address).characteristicList){
             if(characteristic.characteristic.getUuid().toString().equalsIgnoreCase(CharacteristicUUid)){
                 return characteristic.messagefrom;
             }
         }
         return Constants.MessageFromBleUtil;
    }
    void connectToDevice(int MessageFrom,String DeviceAddress){
         BluetoothDevice device  = checkDeviceExists(DeviceAddress);
         gattCallback.messageFrom = MessageFrom;
         if(device != null){
             device.connectGatt(ApplicationContext,false,gattCallback);
         }
         else{
             // null send mssg device not found
             SendMessage(Constants.CONNECT_RESPONSE,new Object[]{DeviceAddress},1,MessageFrom,Constants.BLE_ADDRESS_NOT_FOUND);
             ReleaseUtilSemaphore();
         }
    }
    void DisconnectDevice(int MessageFrom,String DeviceAddress){
         BluetoothGatt gatt = getDeviceGatt(DeviceAddress);
         gattCallback.messageFrom  = MessageFrom;
         if(gatt!=null){
             gatt.disconnect();
             ReleaseUtilSemaphore();
         }
         else{
             LogUtil.e(Constants.Log,"device address not found or not connected");
             SendMessage(Constants.DISCONNECT_RESPONSE,new Object[]{DeviceAddress},1,MessageFrom,Constants.BLE_ADDRESS_NOT_FOUND);
             ReleaseUtilSemaphore();
         }

    }
    bleCharacteristic getCharacter(String uuid,String Address){
          if(devicesList.get(Address).isConnected() == false)
              return null;
          for(bleCharacteristic characteristic:devicesList.get(Address).characteristicList){
                if(characteristic.characteristic.getUuid().toString().equalsIgnoreCase(uuid) == true){
                    return characteristic;
                }
            }
            return null;
    }
    void writeCharacteristic(int MessageFrom,String DeviceAddress,byte[] data,String Characteristicuuid){
        BluetoothGatt gatt = getDeviceGatt(DeviceAddress);
        if(gatt!=null){
            bleCharacteristic characteristic = getCharacter(Characteristicuuid,DeviceAddress);
            characteristic.messagefrom = MessageFrom;
            if(characteristic!=null){

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    gatt.writeCharacteristic(characteristic.characteristic,data,BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                }
                else{
                    characteristic.characteristic.setValue(data);
                    characteristic.characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                    gatt.writeCharacteristic(characteristic.characteristic);
                }
            }
            else {
                SendMessage(Constants.WRITE_CHARACTERISTIC_RESPONSE,new Object[]{DeviceAddress,Characteristicuuid},2,MessageFrom,Constants.BLE_CHARACTER_NOT_FOUND);
                ReleaseUtilSemaphore();
            }
        }
        else{
            SendMessage(Constants.WRITE_CHARACTERISTIC_RESPONSE,new Object[]{DeviceAddress,Characteristicuuid},2,MessageFrom,Constants.BLE_ADDRESS_NOT_FOUND);
            ReleaseUtilSemaphore();
        }
    }
    void readCharacteristic(int MessageFrom,String DeviceAddress,String Characteristicuuid){
        BluetoothGatt gatt = getDeviceGatt(DeviceAddress);
        if(gatt!=null){
            bleCharacteristic characteristic = getCharacter(Characteristicuuid,DeviceAddress);
            if(characteristic!=null){
                characteristic.messagefrom = MessageFrom;
                gatt.readCharacteristic(characteristic.characteristic);
            }
            else{
                SendMessage(Constants.READ_CHARACTERISTIC_RESPONSE,new Object[]{DeviceAddress,Characteristicuuid},2,MessageFrom,Constants.BLE_CHARACTER_NOT_FOUND);
                ReleaseUtilSemaphore();
            }
        }
        else{
            SendMessage(Constants.READ_CHARACTERISTIC_RESPONSE,new Object[]{DeviceAddress,Characteristicuuid},2,MessageFrom,Constants.BLE_ADDRESS_NOT_FOUND);
            ReleaseUtilSemaphore();
        }

    }

    void notifyCharacteristic(int MessageFrom,String DeviceAddress,String Characteristicuuid){
        BluetoothGatt gatt = getDeviceGatt(DeviceAddress);
        if(gatt!=null){
            bleCharacteristic characteristic = getCharacter(Characteristicuuid,DeviceAddress);
            if(characteristic!=null){
                characteristic.messagefrom = MessageFrom;
                gatt.setCharacteristicNotification(characteristic.characteristic,true);
                BluetoothGattDescriptor descriptor = characteristic.characteristic.getDescriptor(CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    gatt.writeDescriptor(descriptor,BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                }
                else{
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    gatt.writeDescriptor(descriptor);
                }

            }
            else{
                SendMessage(Constants.NOTIFY_CHARACTERISTIC_RESPONSE,new Object[]{DeviceAddress,Characteristicuuid},2,MessageFrom,Constants.BLE_CHARACTER_NOT_FOUND);
                ReleaseUtilSemaphore();
            }
        }
        else{
            SendMessage(Constants.NOTIFY_CHARACTERISTIC_RESPONSE,new Object[]{DeviceAddress,Characteristicuuid},2,MessageFrom,Constants.BLE_ADDRESS_NOT_FOUND);
            ReleaseUtilSemaphore();
        }
    }

    void setMtu(int MessageFrom,String DeviceAddress,int mtu){
        BluetoothGatt gatt = getDeviceGatt(DeviceAddress);
        if(gatt!=null){
            gattCallback.messageFrom = MessageFrom;
            gatt.requestMtu(mtu);
        }
        else{
            SendMessage(Constants.SET_MTU_RESPONSE,new Object[]{DeviceAddress,mtu},2,MessageFrom,Constants.BLE_ADDRESS_NOT_FOUND);
            ReleaseUtilSemaphore();
        }
    }

    void setConnectionPriority(int MessageFrom,String DeviceAddress,int priority){
        BluetoothGatt gatt = getDeviceGatt(DeviceAddress);
        if(gatt!=null && (priority>=0 && priority<=3)){
            gattCallback.messageFrom = MessageFrom;
            gatt.requestConnectionPriority(priority);
            SendMessage(Constants.SET_PRIORITY_RESPONSE,new Object[]{DeviceAddress,priority},2,MessageFrom);
        }
        else{
            SendMessage(Constants.SET_PRIORITY_RESPONSE,new Object[]{DeviceAddress,priority},2,MessageFrom,Constants.BLE_ADDRESS_NOT_FOUND);
        }
        ReleaseUtilSemaphore();
    }

    void setPhy(int MessageFrom,String DeviceAddress,int phy) {
        BluetoothGatt gatt = getDeviceGatt(DeviceAddress);

        if (gatt != null && (phy >= BluetoothDevice.PHY_LE_1M && phy <= BluetoothDevice.PHY_LE_CODED) ){
            gattCallback.messageFrom = MessageFrom;
            gatt.setPreferredPhy(phy,phy,BluetoothDevice.PHY_OPTION_S2);
        }
        else{
            SendMessage(Constants.SET_PHY_RESPONSE,new Object[]{DeviceAddress,phy},2,MessageFrom,Constants.BLE_ADDRESS_NOT_FOUND);
            ReleaseUtilSemaphore();
        }
    }
}

