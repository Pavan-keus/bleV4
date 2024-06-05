package Ble;

public class ScanningDevices {
      String deviceName;
      int rssi;
      byte[] manufactureData;
      int manufactureDataSize;
      int lastScanResultTime;
      public String getDeviceName() {
            return deviceName;
      }

      public int getLastScanResultTime() {
            return lastScanResultTime;
      }

      public void setLastScanResultTime(int lastScanResultTime) {
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
