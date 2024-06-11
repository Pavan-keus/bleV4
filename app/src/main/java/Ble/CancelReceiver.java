package Ble;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class CancelReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationManager manger = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manger.cancel(intent.getExtras().getInt("id"));
        Log.e("appOutput", "canceled");

        Intent i = new Intent("clickCancel");
        i.putExtra("data", "additional data");
        LocalBroadcastManager.getInstance(context).sendBroadcast(i);
    }
}
