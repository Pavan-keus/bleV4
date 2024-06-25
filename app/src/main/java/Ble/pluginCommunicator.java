package Ble;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

public class pluginCommunicator extends Thread {
    Context context;
    bleToPlugin pluginInterface;
    otaToPlugin otaPluginInterface;

    bleUtil bleUtilThread;
    private OtaForeground otaForegroundService;
    private boolean isBound = true;
    Intent serviceIntent;
    OtaForeground.BinderServicClass binder;
    public pluginCommunicator(Context context, bleToPlugin pluginInterface,otaToPlugin otaPluginInterface){
        this.context = context;
        this.pluginInterface = pluginInterface;
        this.otaPluginInterface = otaPluginInterface;
        bleUtilThread = new bleUtil(context);

        serviceIntent = new Intent(context,OtaForeground.class);
        context.bindService(serviceIntent,connection,Context.BIND_AUTO_CREATE);


        Common.pluginCommunicator_Queue = new LinkedBlockingQueue<Communication>(50);
    }
    // check valid message or not if not don't send to ble util
    public void sendMessageToBle(JSONObject message){

        Communication bleMessage = new Communication();
        bleMessage.fromMessage = Constants.MessageFromBleUtil;
        if(message.has("type") && message.has("data")){
            LogUtil.e(Constants.Log,"Message Received in Plugin");
            try{
                switch (message.getInt("type")){
                    // message for ble Initialization
                    case Constants.INITIALIZE_BLE_REQUEST:
                        bleMessage.messageType = Constants.INITIALIZE_BLE_REQUEST;
                        bleMessage.data = null;
                        bleMessage.messageSize = 0;
                        Common.bleUtil_Queue.put(bleMessage);
                        break;
                    case Constants.DEINITIALIZE_BLE_REQUEST:
                        bleMessage.messageType = Constants.DEINITIALIZE_BLE_REQUEST;
                        bleMessage.data = null;
                        bleMessage.messageSize = 0;
                        Common.bleUtil_Queue.put(bleMessage);
                        break;
                    case Constants.STARTSCAN_REQUEST:
                        bleMessage.messageType = Constants.STARTSCAN_REQUEST;
                        bleMessage.data = null;
                        bleMessage.messageSize = 0;
                        Common.bleUtil_Queue.put(bleMessage);
                        break;
                    case Constants.STOPSCAN_REQUEST:
                        bleMessage.messageType = Constants.STOPSCAN_REQUEST;
                        bleMessage.data = null;
                        bleMessage.messageSize = 0;
                        Common.bleUtil_Queue.put(bleMessage);
                        break;
                    case Constants.LISTDEVICE_REQUEST:
                        bleMessage.messageType = Constants.LISTDEVICE_REQUEST;
                        bleMessage.data = null;
                        bleMessage.messageSize = 0;
                        Common.bleUtil_Queue.put(bleMessage);
                        break;
                    case Constants.CONNECT_REQUEST:
                        bleMessage.messageType = Constants.CONNECT_REQUEST;
                        bleMessage.data = new Object[]{message.getJSONObject("data").getString("deviceId")};
                        bleMessage.messageSize = 1;
                        Common.bleUtil_Queue.put(bleMessage);
                        break;
                    case Constants.DISCONNECT_REQUEST:
                        bleMessage.messageType = Constants.DISCONNECT_REQUEST;
                        bleMessage.data = new Object[]{message.getJSONObject("data").getString("deviceId")};
                        bleMessage.messageSize = 1;
                        Common.bleUtil_Queue.put(bleMessage);
                        break;
                    case Constants.WRITE_CHARACTERISTIC_REQUEST:
                        bleMessage.messageType = Constants.WRITE_CHARACTERISTIC_REQUEST;
                        JSONArray valueArray = message.getJSONObject("data").getJSONArray("value");
                        byte[] data = new byte[valueArray.length()];
                        for(int i=0;i<valueArray.length();i++){
                            data[i] = (byte)valueArray.getInt(i);
                        }
                        bleMessage.data = new Object[]{message.getJSONObject("data").getString("deviceId"),data,message.getJSONObject("data").getString("characteristic")};
                        bleMessage.messageSize = 3;
                        Common.bleUtil_Queue.put(bleMessage);
                        break;
                    case Constants.READ_CHARACTERISTIC_REQUEST:
                        bleMessage.messageType = Constants.READ_CHARACTERISTIC_REQUEST;
                        bleMessage.data = new Object[]{message.getJSONObject("data").getString("deviceId"),message.getJSONObject("data").getString("characteristic")};
                        bleMessage.messageSize = 2;
                        Common.bleUtil_Queue.put(bleMessage);
                        break;
                    case Constants.NOTIFY_CHARACTERISTIC_REQUEST:
                        bleMessage.messageType = Constants.NOTIFY_CHARACTERISTIC_REQUEST;
                        bleMessage.data = new Object[]{message.getJSONObject("data").getString("deviceId"),message.getJSONObject("data").getString("characteristic")};
                        bleMessage.messageSize = 2;
                        Common.bleUtil_Queue.put(bleMessage);
                        break;
                    case Constants.SET_MTU_REQUEST:
                        bleMessage.messageType = Constants.SET_MTU_REQUEST;
                        bleMessage.data = new Object[]{message.getJSONObject("data").getString("deviceId"),message.getJSONObject("data").getInt("mtu")};
                        bleMessage.messageSize = 2;
                        Common.bleUtil_Queue.put(bleMessage);
                        break;
                    case Constants.SET_PRIORITY_REQUEST:
                        bleMessage.messageType = Constants.SET_PRIORITY_REQUEST;
                        bleMessage.data = new Object[]{message.getJSONObject("data").getString("deviceId"),message.getJSONObject("data").getInt("priority")};
                        bleMessage.messageSize = 2;
                        Common.bleUtil_Queue.put(bleMessage);
                        break;
                    case Constants.SET_PHY_REQUEST:
                        bleMessage.messageType = Constants.SET_PHY_REQUEST;
                        bleMessage.data = new Object[]{message.getJSONObject("data").getString("deviceId"),message.getJSONObject("data").getInt("phy")};
                        bleMessage.messageSize = 2;
                        Common.bleUtil_Queue.put(bleMessage);
                        break;
                    case Constants.DISCOVER_SERVICE_REQUEST:
                        bleMessage.messageType = Constants.DISCOVER_SERVICE_REQUEST;
                        bleMessage.data = new Object[]{message.getJSONObject("data").getString("deviceId")};
                        bleMessage.messageSize = 1;
                        Common.bleUtil_Queue.put(bleMessage);
                }
            }catch (Exception e){
                LogUtil.e(Constants.Error,"error in processing Communication"+e.getMessage());
            }

        }
        else{
            LogUtil.e(Constants.Error,"Insufficient Fields for processing Message");
        }
    }
    private final ServiceConnection  connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            binder = (OtaForeground.BinderServicClass)service;
            otaForegroundService = binder.getService();
            isBound = true;
            if(otaForegroundService.getIsForegroundServicerunning())
            {
                Common.bleOperationsObject = otaForegroundService.getBleOperations();
                Common.bleOperationSemaphore = otaForegroundService.getSemaphore();
                LogUtil.e(Constants.Log,"service got rebind with previous data");
            }
            else{
                Common.bleOperationsObject  = new bleOperations(context);
                Common.bleOperationSemaphore = new Semaphore(1);
                LogUtil.e(Constants.Log,"service got rebind with new data");
                //startForegroundService();
            }
            bleUtilThread.start();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };
    public void startForegroundService(){
        binder.getService().setBleOperations(Common.bleOperationsObject);
        binder.getService().setOtaToPlugin(otaPluginInterface);
        binder.getService().setSemaphore(Common.bleOperationSemaphore);
        context.startForegroundService(serviceIntent);
    }
    public void ondestroyCallback(){
        if(isBound){
            LogUtil.e(Constants.Log,"Unbinding Service");
            context.unbindService(connection);
            isBound = false;
        }
    }
    // functions to send response to bletoplugin interface
    void sendInitializationResponse(){
        try{
            JSONObject response = new JSONObject();
            JSONObject Data = new JSONObject();
            response.put("type",Constants.INITIALIZE_BLE_RESPONSE);
            Data.put("success",true);
            response.put("data",Data);
            pluginInterface.bleInitialization(response);
        }
        catch (Exception e){
            LogUtil.e(Constants.Error,"Error in sending Initialization Response"+e.getMessage());
        }

    }
    void sendDeInitializationResponse(){
        JSONObject response = new JSONObject();
        JSONObject Data = new JSONObject();
        try{
            response.put("type",Constants.DEINITIALIZE_BLE_RESPONSE);
            Data.put("success",true);
            response.put("data",Data);
            pluginInterface.bleDeInitialization(response);
        }
        catch (Exception e){
            LogUtil.e(Constants.Error,"Error in sending DeInitialization Response"+e.getMessage());
        }
    }
    void sendStartScanResponse(){
        JSONObject response = new JSONObject();
        try{
            response.put("type",Constants.STARTSCAN_RESPONSE);
            response.put("data",true);
            pluginInterface.bleStartScan(response);
        }
        catch (Exception e){
            LogUtil.e(Constants.Error,"Error in sending start scan Response"+e.getMessage());
        }
    }
    void sendStartScanResponse(int error){
        JSONObject response = new JSONObject();
        try{
            response.put("type",Constants.STARTSCAN_RESPONSE);
            response.put("data",error);
            pluginInterface.bleStartScan(response);
        }
        catch (Exception e){
            LogUtil.e(Constants.Error,"Error in sending start scan with error Response"+e.getMessage());
        }
    }
    void sendStopScanResponse(){
        JSONObject response = new JSONObject();
        try{
            response.put("type",Constants.STOPSCAN_RESPONSE);
            response.put("data",true);
            pluginInterface.bleStopScan(response);
        }
        catch (Exception e){
            LogUtil.e(Constants.Error,"Error in sending stop scan Response"+e.getMessage());
        }
    }
    void sendStopScanResponse(int error){
        JSONObject response = new JSONObject();
        try{
            response.put("type",Constants.STOPSCAN_RESPONSE);
            response.put("data",error);
            pluginInterface.bleStopScan(response);
        }
        catch (Exception e){
            LogUtil.e(Constants.Error,"Error in sending stop scan with Response"+e.getMessage());
        }
    }

    void sendListDeviceResponse(ConcurrentHashMap<String,ScanningDevices> devicesList){
        try{
            if(devicesList == null){
                JSONObject response = new JSONObject();
                response.put("type",Constants.LISTDEVICE_RESPONSE);
                JSONObject devices = new JSONObject();
                devices.put("devices",null);
                response.put("data",devices);
                pluginInterface.blegetDevices(response);
            }
            else{
                JSONObject response = new JSONObject();
                response.put("type",Constants.LISTDEVICE_RESPONSE);
                JSONObject deviceData = new JSONObject();
                JSONArray devices = new JSONArray();
                for(String key: devicesList.keySet()){
                    ScanningDevices device = devicesList.get(key);
                    JSONObject deviceProperties = new JSONObject();
                    deviceProperties.put("name",device.deviceName);
                    deviceProperties.put("address",key);
                    deviceProperties.put("rssi",device.rssi);
                    devices.put(deviceProperties);
                }
                deviceData.put("devices",devices);
                response.put("data",deviceData);
                pluginInterface.blegetDevices(response);
            }
        }
        catch (Exception e){
            LogUtil.e(Constants.Error,"Error in sending List Device Response"+e.getMessage());
        }
    }
    void sendConnectResponse(String bleAddress){
         JSONObject response = new JSONObject();
         JSONObject data = new JSONObject();
         try{
             response.put("type",Constants.CONNECT_RESPONSE);
             data.put("success",true);
             data.put("deviceId",bleAddress);
             response.put("data",data);
             pluginInterface.bleConnectDevice(response);
         }
         catch(Exception e){
             LogUtil.e(Constants.Error,"Error in sending Connect Response"+e.getMessage());
         }
    }
    void sendConnectResponse(String bleAddress, int error){
        JSONObject response = new JSONObject();
        JSONObject data = new JSONObject();
        try{
            response.put("type",Constants.CONNECT_RESPONSE);
            data.put("success",false);
            data.put("deviceId",bleAddress);
            data.put("reason",error);
            response.put("data",data);
            pluginInterface.bleConnectDevice(response);
        }
        catch(Exception e){
            LogUtil.e(Constants.Error,"Error in sending Connect Response"+e.getMessage());
        }
    }
    void sendDisconnectResponse(String bleAddress){
        JSONObject response = new JSONObject();
        JSONObject data = new JSONObject();
        try{
            response.put("type",Constants.DISCONNECT_RESPONSE);
            data.put("deviceId",bleAddress);
            data.put("success",true);
            response.put("data",data);
            pluginInterface.bleDisconnectDevice(response);
        }
        catch(Exception e){
            LogUtil.e(Constants.Error,"Error in sending disConnect Response"+e.getMessage());
        }
    }
    void sendWriteCharacteristicResponse(String bleAddress, String characteristic){
        JSONObject response = new JSONObject();
        JSONObject data = new JSONObject();
        try{
            response.put("type",Constants.WRITE_CHARACTERISTIC_RESPONSE);
            data.put("deviceId",bleAddress);
            data.put("success",true);
            data.put("characteristic",characteristic);
            response.put("data",data);
            pluginInterface.bleWriteCharacteristic(response);
        }
        catch(Exception e){
            LogUtil.e(Constants.Error,"Error in sending write Response"+e.getMessage());
        }
    }
    void sendReadCharacteristicResponse(String bleAddress, String characteristic, byte[] value){
        JSONObject response = new JSONObject();
        JSONObject data = new JSONObject();
        JSONArray array = new JSONArray();
        for(int arrayvalue:value){
            array.put(arrayvalue);
        }
        try {
            response.put("type", Constants.READ_CHARACTERISTIC_RESPONSE);
            data.put("deviceId", bleAddress);
            data.put("success", true);
            data.put("characteristic", characteristic);
            data.put("value", array);
            response.put("data", data);
            pluginInterface.bleReadCharacteristic(response);
        }
        catch (Exception e){
            LogUtil.e(Constants.Error,"Error in sending read Response"+e.getMessage());
        }
    }
    void sendNotifyCharacteristicResponse(String bleAddress, String characteristic){
        JSONObject response = new JSONObject();
        JSONObject data = new JSONObject();
        try{
            response.put("type",Constants.NOTIFY_CHARACTERISTIC_RESPONSE);
            data.put("deviceId",bleAddress);
            data.put("success",true);
            data.put("characteristic",characteristic);
            response.put("data",data);
            pluginInterface.bleNotifyCharacteristic(response);
        }
        catch(Exception e){
            LogUtil.e(Constants.Error,"Error in sending notify Response"+e.getMessage());
        }
    }
    void sendSetMtuResponse(String bleAddress, int mtu){
        JSONObject response = new JSONObject();
        JSONObject data = new JSONObject();
        try{
            response.put("type",Constants.SET_MTU_RESPONSE);
            data.put("deviceId",bleAddress);
            data.put("success",true);
            data.put("mtu",mtu);
            response.put("data",data);
            pluginInterface.blesetMtu(response);
        }
        catch(Exception e){
            LogUtil.e(Constants.Error,"Error in sending mtu Response"+e.getMessage());
        }
    }
    void sendSetPriorityResponse(String bleAddress, int priority){
        JSONObject response = new JSONObject();
        JSONObject data = new JSONObject();
        try{
            response.put("type",Constants.SET_PRIORITY_RESPONSE);
            data.put("deviceId",bleAddress);
            data.put("success",true);
            data.put("priority",priority);
            response.put("data",data);
            pluginInterface.blesetPriority(response);
        }
        catch(Exception e){
            LogUtil.e(Constants.Error,"Error in sending Priority Response"+e.getMessage());
        }
    }
    void setSetPhyResponse(String bleAddress, int phy){
        JSONObject response = new JSONObject();
        JSONObject data = new JSONObject();
        try{
            response.put("type",Constants.SET_PHY_RESPONSE);
            data.put("deviceId",bleAddress);
            data.put("success",true);
            data.put("phy",phy);
            response.put("data",data);
            pluginInterface.blesetPhy(response);
        }
        catch(Exception e){
            LogUtil.e(Constants.Error,"Error in sending Phy Response"+e.getMessage());
        }
    }
    void sendDisconnectResponse(String bleAddress,int Error){
        JSONObject response = new JSONObject();
        JSONObject data = new JSONObject();
        try{
            response.put("type",Constants.DISCONNECT_RESPONSE);
            data.put("deviceId",bleAddress);
            data.put("success",false);
            data.put("reason",Error);
            response.put("data",data);
            pluginInterface.bleDisconnectDevice(response);
        }
        catch(Exception e){
            LogUtil.e(Constants.Error,"Error in sending disConnect Response"+e.getMessage());
        }
    }
    void sendWriteCharacteristicResponse(String bleAddress, String characteristic,int Error){
        JSONObject response = new JSONObject();
        JSONObject data = new JSONObject();
        try{
            response.put("type",Constants.WRITE_CHARACTERISTIC_RESPONSE);
            data.put("deviceId",bleAddress);
            data.put("success",false);
            data.put("reason",Error);
            data.put("characteristic",characteristic);
            response.put("data",data);
            pluginInterface.bleWriteCharacteristic(response);
        }
        catch(Exception e){
            LogUtil.e(Constants.Error,"Error in sending write Response"+e.getMessage());
        }
    }
    void sendReadCharacteristicResponse(String bleAddress, String characteristic,int Error){
        JSONObject response = new JSONObject();
        JSONObject data = new JSONObject();
        try {
            response.put("type", Constants.READ_CHARACTERISTIC_RESPONSE);
            data.put("deviceId", bleAddress);
            data.put("success", false);
            data.put("reason",Error);
            data.put("characteristic", characteristic);
            response.put("data", data);
            pluginInterface.bleReadCharacteristic(response);
        }
        catch (Exception e){
            LogUtil.e(Constants.Error,"Error in sending read Response"+e.getMessage());
        }
    }
    void sendNotifyCharacteristicResponse(String bleAddress, String characteristic, int Error){
        JSONObject response = new JSONObject();
        JSONObject data = new JSONObject();
        try{
            response.put("type",Constants.NOTIFY_CHARACTERISTIC_RESPONSE);
            data.put("deviceId",bleAddress);
            data.put("success",false);
            data.put("reason",Error);
            data.put("characteristic",characteristic);
            response.put("data",data);
            pluginInterface.bleNotifyCharacteristic(response);
        }
        catch(Exception e){
            LogUtil.e(Constants.Error,"Error in sending notify Response"+e.getMessage());
        }
    }
    void sendSetMtuResponse(String bleAddress, int mtu,int Error){
        JSONObject response = new JSONObject();
        JSONObject data = new JSONObject();
        try{
            response.put("type",Constants.SET_MTU_RESPONSE);
            data.put("deviceId",bleAddress);
            data.put("success",false);
            data.put("mtu",mtu);
            data.put("reason",Error);
            response.put("data",data);
            pluginInterface.blesetMtu(response);
        }
        catch(Exception e){
            LogUtil.e(Constants.Error,"Error in sending mtu Response"+e.getMessage());
        }
    }
    void sendSetPriorityResponse(String bleAddress, int priority,int Error){
        JSONObject response = new JSONObject();
        JSONObject data = new JSONObject();
        try{
            response.put("type",Constants.SET_PRIORITY_RESPONSE);
            data.put("deviceId",bleAddress);
            data.put("success",false);
            data.put("reason",Error);
            data.put("priority",priority);
            response.put("data",data);
            pluginInterface.blesetPriority(response);
        }
        catch(Exception e){
            LogUtil.e(Constants.Error,"Error in sending Priority Response"+e.getMessage());
        }
    }
    void setSetPhyResponse(String bleAddress, int phy,int Error){
        JSONObject response = new JSONObject();
        JSONObject data = new JSONObject();
        try{
            response.put("type",Constants.SET_PHY_RESPONSE);
            data.put("deviceId",bleAddress);
            data.put("success",false);
            data.put("phy",phy);
            response.put("data",data);
            pluginInterface.blesetPhy(response);
        }
        catch(Exception e){
            LogUtil.e(Constants.Error,"Error in sending Phy Response"+e.getMessage());
        }
    }
    void sendNotifyCharacterResponseData(String bleAddress, String characteristic, byte[] value){
        JSONObject response = new JSONObject();
        JSONObject data = new JSONObject();
        JSONArray array = new JSONArray();
        for(int arrayvalue:value){
            array.put(arrayvalue);
        }
        try {
            response.put("type", Constants.NOTIFY_CHARACTERISTIC_UPDATE_RESPONSE);
            data.put("deviceId", bleAddress);
            data.put("success", true);
            data.put("characteristic", characteristic);
            data.put("value", array);
            response.put("data", data);
            pluginInterface.bleNotifyCharacteristicData(response);
        }
        catch (Exception e){
            LogUtil.e(Constants.Error,"Error in sending notify characteristic data Response"+e.getMessage());
        }
    }
    private void sendMessagetoPluginCommunicator(int Messagetype,int fromMessage,int messageSize,Object[] data){
        Communication Message = new Communication();
        Message.messageType = Messagetype;
        Message.fromMessage = fromMessage;
        Message.messageSize = messageSize;
        Message.data = data;
        try{
            Common.pluginCommunicator_Queue.put(Message);
        }
        catch (Exception e){
            LogUtil.e(Constants.Error,"Error in sending Message to Plugin Communicator"+e.getMessage());
        }
    }
    private void sendMessagetoPluginCommunicator(int Messagetype,int fromMessage,int messageSize,Object[] data,int Error){
        Communication Message = new Communication();
        Message.messageType = Messagetype;
        Message.fromMessage = fromMessage;
        Message.messageSize = messageSize;
        Message.data = data;
        Message.Error = Error;
        try{
            Common.pluginCommunicator_Queue.put(Message);
        }
        catch (Exception e){
            LogUtil.e(Constants.Error,"Error in sending Message to Plugin Communicator"+e.getMessage());
        }
    }
    byte[] getFilecontent(String requestUrl,String queryParams){
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(requestUrl + "?" + queryParams);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoOutput(true);  // This is optional as you're using GET parameters
            // For a POST request, you don't necessarily need to send a body if you're sending parameters in URL
            try (OutputStream os = urlConnection.getOutputStream()) {
                byte[] input = queryParams.getBytes("utf-8");
                os.write(input, 0, input.length);
            }
            int responseCode = urlConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) { // success
                BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                JSONObject object = new JSONObject(response.toString());
                JSONArray data = object.getJSONObject("data").getJSONArray("data");
                byte binarydata[]= new byte[data.length()];

                for(int i=0;i<data.length();i++)
                    binarydata[i] = (byte)data.getInt(i);
                return  binarydata;
            } else {
                System.out.println("POST request did not work.");
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }
    public void sendMessageToOta(String deviceAddress, String deviceType,String deviceCategory, String version, String token, String branch){
        if(Common.bleOperationsObject.isdeviceExists(deviceAddress) && Common.bleOperationsObject.isConnected(deviceAddress)){
            String requestUrl = "http://iot-operations.keus.in:6060/api/getOtaBinFile";
            String queryParams = "deviceCategory="+deviceCategory+"&deviceType="+deviceType+"&token="+token+"&version="+version+"&branch="+branch;
            byte[] filecontent = getFilecontent(requestUrl,queryParams);
            if(filecontent != null){
                binder.getService().doOtaFor(deviceAddress,filecontent);
                startForegroundService();
            }
            else{
                sendMessagetoPluginCommunicator(Constants.OTA_RESPONSE,Constants.MessageFromOtaUtil,1,new Object[]{deviceAddress},Constants.FAILED_TO_FETCH_FILE_CONTENT);
            }
        }
        else{
            sendMessagetoPluginCommunicator(Constants.OTA_RESPONSE,Constants.MessageFromOtaUtil,1,new Object[]{deviceAddress},Constants.BLE_ADDRESS_NOT_FOUND);
        }
    }

    void sendServiceDiscoveryResponse(String deviceAddress){
         try{
             JSONObject response = new JSONObject();
             JSONObject data = new JSONObject();
             response.put("type",Constants.DISCOVER_SERVICE_RESPONSE);
             data.put("success",true);
             data.put("deviceId",deviceAddress);
             response.put("data",data);
             pluginInterface.bleServiceDiscoveryData(response);
         }
         catch(Exception e){
             LogUtil.e(Constants.Error,"Error in sending Service Discovery Response"+e.getMessage());
         }
    }

    void sendServiceDiscoveryResponse(String deviceAddress , int error){
         try{
            JSONObject response = new JSONObject();
            JSONObject data = new JSONObject();
            response.put("type",Constants.DISCOVER_SERVICE_RESPONSE);
            data.put("success",false);
            data.put("deviceId",deviceAddress);
            response.put("data",data);
            pluginInterface.bleServiceDiscoveryData(response);
         }
         catch(Exception e){
             LogUtil.e(Constants.Error,"Error in sending Service Discovery Response"+e.getMessage());
         }
    }

    @Override
    public void run() {
        LogUtil.e(Constants.Log,"plugin Communicator Thread Started");
        while(true){
            try {
                Communication message = Common.pluginCommunicator_Queue.take();
                LogUtil.e(Constants.Log,"Message Received in Communicator thread"+message.messageType);
                switch(message.messageType){
                    case Constants.INITIALIZE_BLE_RESPONSE:
                        sendInitializationResponse();
                        break;
                    case Constants.DEINITIALIZE_BLE_RESPONSE:
                        sendDeInitializationResponse();
                        break;
                    case Constants.STARTSCAN_RESPONSE:
                        if(message.Error ==0)
                            sendStartScanResponse();
                        else
                            sendStartScanResponse(message.Error);
                        break;
                    case Constants.STOPSCAN_RESPONSE:
                        if(message.Error ==0 ){
                            sendStopScanResponse();
                        }
                        else{
                            sendStopScanResponse(message.Error);
                        }
                        break;
                    case Constants.LISTDEVICE_RESPONSE:
                        if(message.messageSize > 0)
                            sendListDeviceResponse((ConcurrentHashMap<String,ScanningDevices>)message.data[0]);
                        else
                            sendListDeviceResponse(null);
                        break;
                    case Constants.CONNECT_RESPONSE:
                        if(message.Error == 0)
                            sendConnectResponse((String)message.data[0]);
                        else
                            sendConnectResponse((String)message.data[0],message.Error);
                        break;
                    case Constants.DISCONNECT_RESPONSE:
                        if(message.Error == 0)
                            sendDisconnectResponse((String)message.data[0]);
                        else
                            sendDisconnectResponse((String)message.data[0],message.Error);
                        break;
                    case Constants.WRITE_CHARACTERISTIC_RESPONSE:
                        if(message.Error == 0)
                            sendWriteCharacteristicResponse((String)message.data[0],(String)message.data[1]);
                        else
                            sendWriteCharacteristicResponse((String)message.data[0],(String)message.data[1],message.Error);
                        break;
                    case Constants.READ_CHARACTERISTIC_RESPONSE:
                        if(message.Error == 0)
                            sendReadCharacteristicResponse((String)message.data[0],(String)message.data[1],(byte[])message.data[2]);
                        else
                            sendReadCharacteristicResponse((String)message.data[0],(String)message.data[1],message.Error);
                        break;
                    case Constants.NOTIFY_CHARACTERISTIC_RESPONSE:
                        if(message.Error == 0)
                            sendNotifyCharacteristicResponse((String)message.data[0],(String)message.data[1]);
                        else
                            sendNotifyCharacteristicResponse((String)message.data[0],(String)message.data[1],message.Error);
                        break;
                    case Constants.NOTIFY_CHARACTERISTIC_UPDATE_RESPONSE:
                        sendNotifyCharacterResponseData((String)message.data[0],(String)message.data[1],(byte[]) message.data[2]);
                        break;
                    case Constants.SET_MTU_RESPONSE:
                        if(message.Error == 0)
                            sendSetMtuResponse((String)message.data[0],(int)message.data[1]);
                        else
                            sendSetMtuResponse((String)message.data[0],(int)message.data[1],message.Error);
                        break;
                    case Constants.SET_PRIORITY_RESPONSE:
                        if(message.Error == 0)
                            sendSetPriorityResponse((String)message.data[0],(int)message.data[1]);
                        else
                            sendSetPriorityResponse((String)message.data[0],(int)message.data[1],message.Error);
                        break;
                    case Constants.SET_PHY_RESPONSE:
                        if(message.Error == 0)
                            setSetPhyResponse((String)message.data[0],(int)message.data[1]);
                        else
                            setSetPhyResponse((String)message.data[0],(int)message.data[1],message.Error);
                        break;
                    case Constants.DISCOVER_SERVICE_RESPONSE:
                        if(message.Error == 0)
                            sendServiceDiscoveryResponse((String)message.data[0]);
                        else
                            sendServiceDiscoveryResponse((String)message.data[0],message.Error);

                }
            } catch (Exception e) {
                LogUtil.e(Constants.Error,"Error in plugin Communicator thread"+e.getMessage());
            }
        }
    }
}
