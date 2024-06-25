package Ble;

import org.json.JSONObject;

public interface bleToPlugin {
    void bleInitialization(JSONObject response);
    void bleDeInitialization(JSONObject response);
    void bleStartScan(JSONObject response);
    void bleStopScan(JSONObject response);
    void blegetDevices(JSONObject response);
    void bleConnectDevice(JSONObject response);
    void bleDisconnectDevice(JSONObject response);
    void bleWriteCharacteristic(JSONObject response);
    void bleReadCharacteristic(JSONObject response);
    void bleNotifyCharacteristic(JSONObject response);
    void blesetMtu(JSONObject response);
    void blesetPhy(JSONObject response);
    void blesetPriority(JSONObject response);
    void bleNotifyCharacteristicData(JSONObject response);
    void bleServiceDiscoveryData(JSONObject response);
    void bleAdvertiseData(JSONObject response);
}
