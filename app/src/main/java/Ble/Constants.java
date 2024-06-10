package Ble;

public class Constants {
    // string constants
    public final static String Error = "ble-Error";
    public final static String Log = "ble-Log";


    // number constants
    public final static int MessageFromBleUtil = 01;// message from ble util
    public final static int MessageFromOtaUtil = 02;// message from ota util
    public final static int MessageFromBleOperation = 03;// message from ble operation
    public final static int MessageFromInternal     = 04; // message within the class

    // request Constants
    public final static int INITIALIZE_BLE_REQUEST     = 1;
    public final static int DEINITIALIZE_BLE_REQUEST   = 2;
    public final static  int STARTSCAN_REQUEST          = 3;
    public final static  int STOPSCAN_REQUEST           = 4;
    public final static int  LISTDEVICE_REQUEST          = 5;
    public final static int  CONNECT_REQUEST             = 6;
    public final static int  DISCONNECT_REQUEST          = 7;
    public final static int  WRITE_CHARACTERISTIC_REQUEST               = 8;
    public final static int  READ_CHARACTERISTIC_REQUEST                = 9;
    public final static int  NOTIFY_CHARACTERISTIC_REQUEST              = 10;
    public final static int  SET_PRIORITY_REQUEST           = 11;
    public final static int  SET_MTU_REQUEST                = 12;







    // response constants
    final static int INITIALIZE_BLE_RESPONSE   = 51;
    final static int DEINITIALIZE_BLE_RESPONSE = 52;
    final static int STARTSCAN_RESPONSE        = 53;
    final static int STOPSCAN_RESPONSE         = 54;
    final static int LISTDEVICE_RESPONSE       = 55;
    final static int CONNECT_RESPONSE          = 56;
    final static int DISCONNECT_RESPONSE       = 57;
    final static int WRITE_CHARACTERISTIC_RESPONSE               = 58;
    final static int READ_CHARACTERISTIC_RESPONSE                = 59;
    final static int NOTIFY_CHARACTERISTIC_RESPONSE              = 60;
    final static int SET_PRIORITY_RESPONSE       = 61;
    final static int SET_MTU_RESPONSE            = 62;

    // error constants
    final static int BLE_NOT_ENABLE            = 100;
    final static int BLE_NOT_INITIALIZED       = 101;
    final static int BLE_NO_SCANNING_DATA      = 102;
    final static int BLE_ADDRESS_NOT_FOUND     = 103;
    final static int BLE_CHARACTER_NOT_FOUND   = 104;

}
