package Ble;

import android.content.Context;

public class pluginCommunicator {
    Context context;
    bleToPlugin pluginInterface;

    pluginCommunicator(Context context, bleToPlugin pluginInterface){
        this.context = context;
        this.pluginInterface = pluginInterface;
    }
}
