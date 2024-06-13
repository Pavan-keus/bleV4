package Ble;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.blev4.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

public class OtaForeground extends Service {
    public boolean IsForegroundServicerunning = false;
    private final IBinder mBinder = new BinderServicClass();
    bleOperations bleOperations;
    otaToPlugin otaToPlugin;
    boolean isActivityBind = false;
    Semaphore semaphore;
    private int notificationId = 1190;
    private NotificationManager notificationManager;
    public static final String CHANNEL_ID = "ota_progress_channel";
    byte []fileContent;
    String bleAddress;
    int TotalBlocks;
    private final String IMAGE_IDENTIFY = "f000ffc1-0451-4000-b000-000000000000";
    private final String IMAGE_BLOCK = "f000ffc2-0451-4000-b000-000000000000";
    private final String OAD_EXT_CTRL = "f000ffc5-0451-4000-b000-000000000000";
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        isActivityBind = true;
        return mBinder;
    }
    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            destroyService();
        }
    };
    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel(getApplicationContext());
        Common.otaCommunication_Queue = new LinkedBlockingQueue<Communication>(50);
    }
    public void createNotificationChannel(Context context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "OTA Progress Channel";
            String description = "Channel for OTA progress notifications";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    void updateNotification(RemoteViews remoteViews,NotificationCompat.Builder builder, int Progress){
        updateColor(remoteViews);
        remoteViews.setProgressBar(R.id.progressBar,100,Progress,false);
        remoteViews.setTextViewText(R.id.progress,Progress+"%");
        if(IsForegroundServicerunning)
            notificationManager.notify(notificationId,builder.build());
    }
    public void doOtaFor(String deviceAddress,byte[] fileContent){
        bleAddress = deviceAddress;
        this.fileContent = fileContent;
    }
    void acquireSemaphore(){
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    void releaseSemaphore(){
        try{
            semaphore.release();
        }
        catch (Exception e){
            throw new RuntimeException(e);
        }
    }
    int build_uint32(byte b3, byte b2, byte b1, byte b0) {
        return ((b3 & 0xff) << 24) | ((b2 & 0xff) << 16) | ((b1 & 0xff) << 8) | (b0 & 0xff);
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
         super.onStartCommand(intent, flags, startId);
         IsForegroundServicerunning = true;
         LogUtil.d(Constants.Log,"Service Started");
         RemoteViews remoteViews;
         // notification code
        remoteViews = new RemoteViews(getPackageName(),R.layout.notification);
        remoteViews.setTextViewText(R.id.deviceName,"KZirb");
        remoteViews.setTextViewText(R.id.progress,"0%");
        remoteViews.setTextViewText(R.id.updating,"3.52");
        remoteViews.setProgressBar(R.id.progressBar,100,0,false);
        remoteViews.setImageViewResource(R.id.Cancel_Icon,R.drawable.cancel_24px);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this,CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setCustomBigContentView(remoteViews)
                .setAutoCancel(false)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW);
        updateColor(remoteViews);
        notificationManager.notify(notificationId,notificationBuilder.build());
        Intent cancel = new Intent(this,CancelReceiver.class);
        cancel.putExtra("id",notificationId);
        PendingIntent cancelPendingIntent = PendingIntent.getBroadcast(this,0,cancel,PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,new IntentFilter("clickCancel"));
        remoteViews.setOnClickPendingIntent(R.id.Cancel,cancelPendingIntent);
        startForeground(notificationId,notificationBuilder.build());
        TotalBlocks = (int)Math.floor(((double) fileContent.length)/240);

        // ota code

        new Thread(
                new Runnable() {
                    int previousProgress = 0;
                    int currentProgress = 0;
                    @Override
                    public void run() {
                        int count = 0;
                        Communication sendrequest = new Communication();
                        sendrequest.messageType = Constants.OTA_REQUEST;
                        sendrequest.data = null;
                        sendrequest.Error = 0;
                        Common.otaCommunication_Queue.add(sendrequest);
                        LogUtil.d(Constants.Log,"Sent OTA Request with file content size"+ fileContent.length);
                        while(IsForegroundServicerunning){
                            try {

                                Communication communication = Common.otaCommunication_Queue.take();
                                Thread.sleep(10);

                                LogUtil.d(Constants.Log,"Received Message in ota queue"+communication.messageType);
                                LogUtil.d(Constants.Log,"Received Data"+Arrays.deepToString(communication.data));
                                switch (communication.messageType){
                                    case Constants.OTA_REQUEST:
                                        bleOperations.notifyCharacteristic(Constants.MessageFromOtaUtil, bleAddress, IMAGE_IDENTIFY);
                                        break;
                                    case Constants.NOTIFY_CHARACTERISTIC_RESPONSE:

                                        if(communication.Error != 0){
                                            // cancel the ota
                                            LogUtil.d(Constants.Log,"Error in OTA failed to notify characteristics");
                                            throw new RuntimeException("Error in OTA");


                                        }
                                        else {
                                            if (((String) communication.data[1]).compareToIgnoreCase(IMAGE_IDENTIFY) == 0) {
                                                bleOperations.notifyCharacteristic(Constants.MessageFromOtaUtil, bleAddress, OAD_EXT_CTRL);
                                            } else if (((String) communication.data[1]).compareToIgnoreCase(OAD_EXT_CTRL) == 0) {
                                                // start the ota Step
                                                bleOperations.writeCharacteristic(Constants.MessageFromOtaUtil, bleAddress, new byte[]{0x01}, OAD_EXT_CTRL);
                                            }
                                        }
                                        break;

                                    case Constants.NOTIFY_CHARACTERISTIC_UPDATE_RESPONSE:
                                    {
                                        byte[] value = (byte[])communication.data[2];
                                        switch(((String)communication.data[1]).toLowerCase()){
                                            case IMAGE_IDENTIFY:
                                                if(value[0] == 0x00){
                                                    bleOperations.writeCharacteristic(Constants.MessageFromOtaUtil,bleAddress,new byte[]{0x03},OAD_EXT_CTRL);
                                                }
                                                break;
                                            case IMAGE_BLOCK:
                                                LogUtil.e(Constants.Log,"Received image block"+ Arrays.toString(value));
                                                break;
                                            case OAD_EXT_CTRL:
                                            {
                                                switch(value[0]){
                                                    case 0x01:{
                                                         LogUtil.e(Constants.Log,"OTA Block size"+((value[1] & 0xff) | (value[2] & 0xff)>>8));
                                                         byte data[] = new byte[22];
                                                         int datacounter =0;
                                                         for(int i=0;i<36;i++){
                                                             if((i>=0 && i<8) || (i>=12 && i<14) || (i>=16 && i<20) || (i>=24 && i<28) || (i>=32 && i<36)){
                                                                 data[datacounter++] = fileContent[i];
                                                             }
                                                         }
                                                         LogUtil.e(Constants.Log,"OTA Data"+datacounter);
                                                         bleOperations.writeCharacteristic(Constants.MessageFromOtaUtil,bleAddress,data,IMAGE_IDENTIFY);
                                                    }
                                                    break;
                                                    case 0x04:{
                                                        LogUtil.e(Constants.Log,"OTA Completed");
                                                        destroyService();
                                                    }
                                                    break;
                                                    case 0x12:{
                                                        if(value[1] == 14){
                                                            bleOperations.writeCharacteristic(Constants.MessageFromOtaUtil,bleAddress,new byte[]{0x04},OAD_EXT_CTRL);
                                                        }
                                                        else if(value[1] == 0){
                                                            int blockId = build_uint32(value[5],value[4],value[3],value[2]);
                                                            LogUtil.e(Constants.Log, "Block Id"+blockId);
                                                            int blocklen = 240;
                                                            int blockStart = blocklen * blockId;
                                                            int blockend = blockStart + blocklen;
                                                            blockend = blockend > fileContent.length ? fileContent.length : blockend;
                                                            byte otaData[] = new byte[((blockend-blockStart)+4)];
                                                            int index = 0;
                                                            otaData[index++] = value[2];
                                                            otaData[index++] = value[3];
                                                            otaData[index++] = value[4];
                                                            otaData[index++] = value[5];
                                                            for(int i=blockStart;i<blockend;i++){
                                                                otaData[index++] = fileContent[i];
                                                            }
                                                            bleOperations.writeCharacteristic(Constants.MessageFromOtaUtil,bleAddress,otaData,IMAGE_BLOCK);
                                                            currentProgress = (int)Math.floor(((double)(blockId)/TotalBlocks)*100);
                                                            if(previousProgress!=currentProgress){
                                                                previousProgress = currentProgress;
                                                                updateNotification(remoteViews,notificationBuilder,currentProgress);
                                                                if(true){

                                                                    JSONObject jsonObject = new JSONObject();
                                                                    JSONObject data = new JSONObject();
                                                                    try {
                                                                        jsonObject.put("Type",Constants.OTA_RESPONSE);
                                                                        data.put("Progress",currentProgress);
                                                                        data.put("bleAddress",bleAddress);
                                                                        jsonObject.put("Data",data);
                                                                        otaToPlugin.otaProgress(jsonObject);
                                                                    } catch (JSONException e) {
                                                                        throw new RuntimeException(e);
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                    break;
                                                }
                                                break;
                                            }
                                            default:
                                                LogUtil.d(Constants.Log,"Unknown Message in ota queue"+communication.messageType);
                                                break;
                                        }
                                        break;
                                    }
                                    case Constants.WRITE_CHARACTERISTIC_RESPONSE:
                                        if(communication.Error != 0) {
                                            // cancel the ota
                                            LogUtil.d(Constants.Log, "Error in OTA failed to write characteristics");
                                            throw new RuntimeException("Error in OTA");
                                        }
                                        break;
                                    default:
                                        LogUtil.d(Constants.Log,"Unknown Message in ota queue"+communication.messageType);
                                        break;


                                }

                            } catch (InterruptedException e) {
                                LogUtil.d(Constants.Log,"Exception in ota queue");
                                releaseSemaphore();
                            }
                        }
                    }
                }
        ).start();
         return START_NOT_STICKY;
    }
    void updateColor(RemoteViews remoteView){
        if (isDarkMode()) {
            remoteView.setTextColor(R.id.deviceName,getResources().getColor(R.color.white));
            remoteView.setTextColor(R.id.progress,getResources().getColor(R.color.white));
            remoteView.setTextColor(R.id.updating,getResources().getColor(R.color.white));
            remoteView.setTextColor(R.id.NameHeading,getResources().getColor(R.color.white));
            remoteView.setTextColor(R.id.progressHeading,getResources().getColor(R.color.white));
            remoteView.setTextColor(R.id.versionHeading,getResources().getColor(R.color.white));
            remoteView.setImageViewResource(R.id.Cancel_Icon,R.drawable.cancel_24px);
        } else {
            remoteView.setTextColor(R.id.deviceName,getResources().getColor(R.color.black));
            remoteView.setTextColor(R.id.progress,getResources().getColor(R.color.black));
            remoteView.setTextColor(R.id.updating,getResources().getColor(R.color.black));
            remoteView.setTextColor(R.id.NameHeading,getResources().getColor(R.color.black));
            remoteView.setTextColor(R.id.progressHeading,getResources().getColor(R.color.black));
            remoteView.setTextColor(R.id.versionHeading,getResources().getColor(R.color.black));
            remoteView.setImageViewResource(R.id.Cancel_Icon,R.drawable.cancel_24px_black);
        }

    }
    private boolean isDarkMode() {
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        IsForegroundServicerunning = false;
        Log.d(Constants.Log,"Service Destroyed");
    }
    public void destroyService(){
        IsForegroundServicerunning = false;
        stopForeground(true);
        stopSelf();

    }
    @Override
    public boolean onUnbind(Intent intent) {
        isActivityBind = false;
        return super.onUnbind(intent);
    }
    public void setBleOperations(bleOperations bleOperations) {
        this.bleOperations = bleOperations;
    }
    public void setOtaToPlugin(otaToPlugin otaToPlugin) {
        this.otaToPlugin = otaToPlugin;
    }
    public bleOperations getBleOperations() {
        return bleOperations;
    }
    public boolean getIsForegroundServicerunning() {
        return IsForegroundServicerunning;
    }
    public Semaphore getSemaphore() {
        return semaphore;
    }
    public void setSemaphore(Semaphore semaphore) {
        this.semaphore = semaphore;
    }
    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }
    public class BinderServicClass extends Binder{
        public OtaForeground getService(){
            return OtaForeground.this;
        }

    }
}
