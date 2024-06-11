package Ble;

import static android.app.Notification.FLAG_NO_CLEAR;

import android.app.Notification;
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

public class OtaForeground extends Service {
    public boolean IsForegroundServicerunning = false;
    private final IBinder mBinder = new BinderServicClass();
    bleOperations bleOperations;
    otaToPlugin otaToPlugin;
    private int notificationId = 1190;
    private NotificationManager notificationManager;
    public static final String CHANNEL_ID = "ota_progress_channel";
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
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
         super.onStartCommand(intent, flags, startId);
         IsForegroundServicerunning = true;
         LogUtil.d(Constants.Log,"Service Started");
         RemoteViews remoteViews;
        remoteViews = new RemoteViews(getPackageName(),R.layout.notification);
        remoteViews.setTextViewText(R.id.deviceName,"Name : custom name");
        remoteViews.setTextViewText(R.id.progress,"Progress : 0%");
        remoteViews.setTextViewText(R.id.updating,"version: 3.52");
        remoteViews.setProgressBar(R.id.progressBar,100,0,false);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this,CHANNEL_ID)
                .setContentTitle("OTA Progress")
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
        remoteViews.setOnClickPendingIntent(R.id.Cancel,cancelPendingIntent);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,new IntentFilter("clickCancel"));
        startForeground(notificationId,notificationBuilder.build());
        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        int count = 0;
                        while(IsForegroundServicerunning){

                            try {
                                Thread.sleep(700);
                                Log.e("output","threa is running "+count);
                                updateColor(remoteViews);
                                remoteViews.setProgressBar(R.id.progressBar,100,count,false);
                                remoteViews.setTextViewText(R.id.progress,"Progress : "+count+"%");
                                if(IsForegroundServicerunning)
                                    notificationManager.notify(notificationId,notificationBuilder.build());
                                if(count == 100){
                                    destroyService();
                                }
                                count++;
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
            remoteView.setTextColor(R.id.Cancel,getResources().getColor(R.color.white));
        } else {
            remoteView.setTextColor(R.id.deviceName,getResources().getColor(R.color.black));
            remoteView.setTextColor(R.id.progress,getResources().getColor(R.color.black));
            remoteView.setTextColor(R.id.updating,getResources().getColor(R.color.black));
            remoteView.setTextColor(R.id.Cancel,getResources().getColor(R.color.white));
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
