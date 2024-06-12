package Ble;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;

public class Common {

       // queue Declaration
      static BlockingQueue<Communication> bleUtil_Queue;
      static BlockingQueue<Communication> pluginCommunicator_Queue;
      static  Semaphore bleOperationSemaphore;
      static bleOperations bleOperationsObject;
      static BlockingQueue<Communication> otaCommunication_Queue;
}
