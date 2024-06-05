package Ble;

import android.util.Log;

public class LogUtil {
    private static final boolean ENABLE_LOG = true;

    public static void d(String tag, String message) {
        if (ENABLE_LOG) {
            Log.d(tag, message);
        }
    }

    public static void e(String tag, String message) {
        if (ENABLE_LOG) {
            Log.e(tag, message);
        }
    }
}
