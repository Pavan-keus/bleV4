package Ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import java.util.ArrayList;
import java.util.List;
class bleCharacteristic{
      BluetoothGattCharacteristic characteristic;
      int messagefrom;
}
public class ScanningDevices {
      String deviceName;
      int rssi;
      byte[] manufactureData;
      int manufactureDataSize;
      long lastScanResultTime;
      boolean isConnected;
      boolean isTriggeredForPair;

      BluetoothDevice discoveredDevice;
      BluetoothGatt bleDevice;
      List<bleCharacteristic> characteristicList = new ArrayList<>();
      public BluetoothGatt getBleDevice() {
            return bleDevice;
      }

      public void setBleDevice(BluetoothGatt bleDevice) {
            this.bleDevice = bleDevice;
      }

      public BluetoothDevice getDiscoveredDevice() {
            return discoveredDevice;
      }

      public void setDiscoveredDevice(BluetoothDevice device) {
            this.discoveredDevice = device;
      }

      public boolean isTriggeredForPair() {
            return isTriggeredForPair;
      }

      public void setTriggeredForPair(boolean triggeredForPair) {
            isTriggeredForPair = triggeredForPair;
      }

      public boolean isConnected() {
            return isConnected;
      }

      public void setConnected(boolean connected) {
            isConnected = connected;
      }


      public String getDeviceName() {
            return deviceName;
      }

      public long getLastScanResultTime() {
            return lastScanResultTime;
      }

      public void setLastScanResultTime(long lastScanResultTime) {
            this.lastScanResultTime = lastScanResultTime;
      }

      public int getRssi() {
            return rssi;
      }

      public byte[] getManufactureData() {
            return manufactureData;
      }

      public int getManufactureDataSize() {
            return manufactureDataSize;
      }

      public void setDeviceName(String deviceName) {
            this.deviceName = deviceName;
      }

      public void setRssi(int rssi) {
            this.rssi = rssi;
      }

      public void setManufactureData(byte[] manufactureData) {
            this.manufactureData = manufactureData;
      }

      public void setManufactureDataSize(int manufactureDataSize) {
            this.manufactureDataSize = manufactureDataSize;
      }
}
