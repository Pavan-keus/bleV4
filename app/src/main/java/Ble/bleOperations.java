package Ble;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
// this file do all the ble operations. for every ble operation there will be a callback available to register
public class bleOperations {
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeAdvertiser bluetoothLeAdvertiser;
    private BluetoothLeScanner bluetoothLeScanner;
    Context ApplicationContext;
    public bleOperations(Context context){
        super();
        this.ApplicationContext = context;
    }
}
