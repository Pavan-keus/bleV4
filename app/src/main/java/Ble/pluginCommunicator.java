package Ble;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class pluginCommunicator extends Thread {
    Context context;
    bleToPlugin pluginInterface;

    bleUtil bleUtilThread;


    public pluginCommunicator(Context context, bleToPlugin pluginInterface){
        this.context = context;
        this.pluginInterface = pluginInterface;
        bleUtilThread = new bleUtil(context);
        bleUtilThread.start();
        Common.pluginCommunicator_Queue = new LinkedBlockingQueue<Communication>(50);
    }
    // check valid message or not if not don't send to ble util
    public void sendMessageToBle(JSONObject message){

        Communication bleMessage = new Communication();
        bleMessage.fromMessage = Constants.MessageFromBleUtil;
        if(message.has("Type") && message.has("Data")){
            LogUtil.e(Constants.Log,"Message Received in Plugin");
            try{
                switch (message.getInt("Type")){
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
                }
            }catch (Exception e){
                LogUtil.e(Constants.Error,"error in processing Communication"+e.getMessage());
            }

        }
        else{
            LogUtil.e(Constants.Error,"Insufficient Fields for processing Message");
        }
    }

    // functions to send response to bletoplugin interface
    void sendInitializationResponse(){
        try{
            JSONObject response = new JSONObject();
            response.put("Type",Constants.INITIALIZE_BLE_RESPONSE);
            response.put("Data",true);
            pluginInterface.bleInitialization(response);
        }
        catch (Exception e){
            LogUtil.e(Constants.Error,"Error in sending Initialization Response"+e.getMessage());
        }

    }
    void sendDeInitializationResponse(){
        JSONObject response = new JSONObject();
        try{
            response.put("Type",Constants.DEINITIALIZE_BLE_RESPONSE);
            response.put("Data",true);
            pluginInterface.bleDeInitialization(response);
        }
        catch (Exception e){
            LogUtil.e(Constants.Error,"Error in sending DeInitialization Response"+e.getMessage());
        }
    }
    void sendStartScanResponse(){
        JSONObject response = new JSONObject();
        try{
            response.put("Type",Constants.STARTSCAN_RESPONSE);
            response.put("Data",true);
            pluginInterface.bleStartScan(response);
        }
        catch (Exception e){
            LogUtil.e(Constants.Error,"Error in sending start scan Response"+e.getMessage());
        }
    }
    void sendStartScanResponse(int error){
        JSONObject response = new JSONObject();
        try{
            response.put("Type",Constants.STARTSCAN_RESPONSE);
            response.put("Data",error);
            pluginInterface.bleStartScan(response);
        }
        catch (Exception e){
            LogUtil.e(Constants.Error,"Error in sending start scan with error Response"+e.getMessage());
        }
    }
    void sendStopScanResponse(){
        JSONObject response = new JSONObject();
        try{
            response.put("Type",Constants.STOPSCAN_RESPONSE);
            response.put("Data",true);
            pluginInterface.bleStopScan(response);
        }
        catch (Exception e){
            LogUtil.e(Constants.Error,"Error in sending stop scan Response"+e.getMessage());
        }
    }
    void sendStopScanResponse(int error){
        JSONObject response = new JSONObject();
        try{
            response.put("Type",Constants.STOPSCAN_RESPONSE);
            response.put("Data",error);
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
                response.put("Type",Constants.LISTDEVICE_RESPONSE);
                JSONObject devices = new JSONObject();
                devices.put("Devices",null);
                response.put("Data",devices);
                pluginInterface.blegetDevices(response);
            }
            else{
                JSONObject response = new JSONObject();
                response.put("Type",Constants.LISTDEVICE_RESPONSE);
                JSONObject deviceData = new JSONObject();
                JSONArray devices = new JSONArray();
                for(String key: devicesList.keySet()){
                    ScanningDevices device = devicesList.get(key);
                    JSONObject deviceProperties = new JSONObject();
                    deviceProperties.put("Name",device.deviceName);
                    deviceProperties.put("Address",key);
                    deviceProperties.put("RSSI",device.rssi);
                    devices.put(deviceProperties);
                }
                deviceData.put("Devices",devices);
                response.put("Data",deviceData);
                pluginInterface.blegetDevices(response);
            }
        }
        catch (Exception e){
            LogUtil.e(Constants.Error,"Error in sending List Device Response"+e.getMessage());
        }
    }
    @Override
    public void run() {
        LogUtil.e(Constants.Log,"plugin Communicator Thread Started");
        while(true){
            try {
                Communication message = Common.pluginCommunicator_Queue.take();
                LogUtil.e(Constants.Log,"Message Received in Communicator thread");
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
                }
            } catch (Exception e) {
                LogUtil.e(Constants.Error,"Error in plugin Communicator thread"+e.getMessage());
            }
        }
    }
}
