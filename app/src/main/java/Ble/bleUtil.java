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
        Common.bleOperationSemaphore = new Semaphore(1);
        Common.bleOperationsObject = new bleOperations(context);
    }

    @Override
    public void run(){
        LogUtil.e(Constants.Log,"ble Util Thread has been started");
        while(true){

            try {
                Communication Message = Common.bleUtil_Queue.take();
                Common.bleOperationsObject.isRequested = true;
                LogUtil.e(Constants.Log,"Message Received in Util "+Message.messageType);
                Common.bleOperationSemaphore.acquire();
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
                }
            } catch (InterruptedException e) {
                LogUtil.e(Constants.Error,"error caught in util Queue");
                Common.bleOperationSemaphore.release();
                Common.bleOperationsObject.isRequested = false;
            }
        }

    }
}
