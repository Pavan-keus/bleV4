package Ble;

import org.json.JSONObject;

public interface bleToPlugin {
    void bleInitialization(JSONObject response);
    void bleDeInitialization(JSONObject response);
    void bleStartScan(JSONObject response);
    void bleStopScan(JSONObject response);
    void blegetDevices(JSONObject response);
}
