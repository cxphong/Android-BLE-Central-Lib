package com.bluetooth.le;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

/**
 * Created by caoxuanphong on 5/11/17.
 */

public class FiotBluetoothAdapterState {
    private FiotBluetoothAdapterStateListener listener;

    public interface FiotBluetoothAdapterStateListener {
        void onStateChanged(int newState);
    }

    private void startListener(Context context, FiotBluetoothAdapterStateListener listener) {
        try {
            this.listener = listener;
            IntentFilter bluetoothFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            context.registerReceiver(mBluetoothReceiver, bluetoothFilter);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopListener(Context context) {
        try {
            listener = null;
            context.unregisterReceiver(mBluetoothReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);

                if (listener != null) {
                    listener.onStateChanged(state);
                }
            }
        }
    };
}
