package Ble;

public class Constants {
    // string constants
    public final static String Error = "ble-Error";
    public final static String Log = "ble-Log";


    // number constants
    public final static int MessageFromBleUtil = 01;// message from ble util
    public final static int MessageFromOtaUtil = 02;// message from ota util
    public final static int MessageFromBleOperation = 03;// message from ble operation



    // request Constants
    public final static int INITIALIZE_BLE_REQUEST     = 1;
    public final static int DEINITIALIZE_BLE_REQUEST   = 2;
    public final static  int STARTSCAN_REQUEST          = 3;
    public final static  int STOPSCAN_REQUEST           = 4;
    public final static int  LISTDEVICE_REQUEST          = 5;









    // response constants
    final static int INITIALIZE_BLE_RESPONSE   = 51;
    final static int DEINITIALIZE_BLE_RESPONSE = 52;
    final static int STARTSCAN_RESPONSE        = 53;
    final static int STOPSCAN_RESPONSE         = 54;
    final static int LISTDEVICE_RESPONSE       = 55;

    // error constants
    final static int BLE_NOT_ENABLE            = 100;
    final static int BLE_NOT_INITIALIZED       = 101;
    final static int BLE_NO_SCANNING_DATA      = 102;

}
