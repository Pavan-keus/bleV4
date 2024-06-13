package Ble;



import android.content.Context;
import android.util.Log;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

public class bleUtil extends Thread{
    private Context context;


    bleUtil(Context context){
        this.context = context;
        Common.bleUtil_Queue = new LinkedBlockingQueue<Communication>();


    }

    @Override
    public void run(){
        LogUtil.e(Constants.Log,"ble Util Thread has been started");
        while(true){

            try {
                Communication Message = Common.bleUtil_Queue.take();

                LogUtil.e(Constants.Log,"Message Received in Util "+Message.messageType);

                Common.bleOperationSemaphore.acquire();
                LogUtil.e(Constants.Log,"Semaphore Acquired");
                Common.bleOperationsObject.isRequested = true;
                switch (Message.messageType){
                    case Constants.INITIALIZE_BLE_REQUEST:
                        Common.bleOperationsObject.InitializeBle(Message.fromMessage);
                        break;
                    case Constants.DEINITIALIZE_BLE_REQUEST:
                        Common.bleOperationsObject.DeInitializeBle(Message.fromMessage);
                        break;
                    case Constants.STARTSCAN_REQUEST:
                        Common.bleOperationsObject.startScan(Message.fromMessage);
                        break;
                    case Constants.STOPSCAN_REQUEST:
                        Common.bleOperationsObject.stopScan(Message.fromMessage);
                        break;
                    case Constants.LISTDEVICE_REQUEST:
                        Common.bleOperationsObject.sendScanningdata(Message.fromMessage);
                        break;
                    case Constants.CONNECT_REQUEST:
                        Common.bleOperationsObject.connectToDevice(Message.fromMessage,(String)Message.data[0]);
                        break;
                    case Constants.DISCONNECT_REQUEST:
                        Common.bleOperationsObject.DisconnectDevice(Message.fromMessage,(String)Message.data[0]);
                        break;
                    case Constants.WRITE_CHARACTERISTIC_REQUEST:
                        Common.bleOperationsObject.writeCharacteristic(Message.fromMessage,(String)Message.data[0],(byte[])Message.data[1],(String)Message.data[2]);
                        break;
                    case Constants.READ_CHARACTERISTIC_REQUEST:
                        Common.bleOperationsObject.readCharacteristic(Message.fromMessage,(String)Message.data[0],(String)Message.data[1]);
                        break;
                    case Constants.NOTIFY_CHARACTERISTIC_REQUEST:
                        Common.bleOperationsObject.notifyCharacteristic(Message.fromMessage,(String)Message.data[0],(String)Message.data[1]);
                        break;
                    case Constants.SET_MTU_REQUEST:
                        Common.bleOperationsObject.setMtu(Message.fromMessage,(String)Message.data[0],(Integer) Message.data[1]);
                        break;
                    case Constants.SET_PHY_REQUEST:
                        Common.bleOperationsObject.setPhy(Message.fromMessage,(String)Message.data[0],(Integer)Message.data[1]);
                        break;
                    case Constants.SET_PRIORITY_REQUEST:
                        Common.bleOperationsObject.setConnectionPriority(Message.fromMessage,(String)Message.data[0],(Integer) Message.data[1]);
                        break;
                }
            } catch (InterruptedException e) {
                LogUtil.e(Constants.Error,"error caught in util Queue");
                Common.bleOperationSemaphore.release();
                Common.bleOperationsObject.isRequested = false;
            }
        }

    }
}
