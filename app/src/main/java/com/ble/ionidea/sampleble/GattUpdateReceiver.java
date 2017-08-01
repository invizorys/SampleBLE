package com.ble.ionidea.sampleble;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

/**
 * Created by user on 6/26/2017.
 */

public class GattUpdateReceiver extends BroadcastReceiver {
    private final String TAG = "GattUpdateReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (BLEService.ACTION_GATT_CONNECTED.equals(action)) {
            EventBus.getDefault().post(new BleStateEvent(BleState.CONNECTED));
        } else if (BLEService.ACTION_GATT_DISCONNECTED.equals(action)) {
            EventBus.getDefault().post(new BleStateEvent(BleState.DISCONNECTED));
        } else if (BLEService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
            EventBus.getDefault().post(new BleStateEvent(BleState.DISCOVERED));
        } else if (BLEService.ACTION_DATA_AVAILABLE.equals(action)) {
            String wearableData = intent.getStringExtra(BLEService.EXTRA_DATA);
            Log.d(TAG, "wearable data: " + wearableData);
            EventBus.getDefault().post(new BleStateEvent(BleState.DATA_AVAILABLE, wearableData));
        }
    }
}
