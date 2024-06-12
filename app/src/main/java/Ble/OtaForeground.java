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

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

public class OtaForeground extends Service {
    public boolean IsForegroundServicerunning = false;
    private final IBinder mBinder = new BinderServicClass();
    bleOperations bleOperations;
    otaToPlugin otaToPlugin;
    Semaphore semaphore;
    private int notificationId = 1190;
    private NotificationManager notificationManager;
    public static final String CHANNEL_ID = "ota_progress_channel";
    byte []fileContent;
    String bleAddress;

    private final String IMAGE_IDENTIFY = "f000ffc1-0451-4000-b000-000000000000";
    private final String IMAGE_BLOCK = "f000ffc2-0451-4000-b000-000000000000";
    private final String OAD_EXT_CTRL = "f000ffc5-0451-4000-b000-000000000000";
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
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
        remoteViews.setProgressBar(R.id.progressBar,100,Progress,false);
        remoteViews.setTextViewText(R.id.progress,Progress+"%");
        if(IsForegroundServicerunning)
            notificationManager.notify(notificationId,builder.build());
    }
    public void doOtaFor(String deviceAddress,byte[] fileContent){
        bleAddress = deviceAddress;
        this.fileContent = fileContent;
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
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        updateColor(remoteViews);
        notificationManager.notify(notificationId,notificationBuilder.build());
        Intent cancel = new Intent(this,CancelReceiver.class);
        cancel.putExtra("id",notificationId);
        PendingIntent cancelPendingIntent = PendingIntent.getBroadcast(this,0,cancel,PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,new IntentFilter("clickCancel"));
        remoteViews.setOnClickPendingIntent(R.id.Cancel,cancelPendingIntent);
        startForeground(notificationId,notificationBuilder.build());

        // ota code
        bleOperations.notifyCharacteristic(Constants.MessageFromOtaUtil,bleAddress,);
        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        int count = 0;
                        while(IsForegroundServicerunning){

                            try {
                                Communication communication = Common.otaCommunication_Queue.take();

                            } catch (InterruptedException e) {
                                e.printStackTrace();
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
