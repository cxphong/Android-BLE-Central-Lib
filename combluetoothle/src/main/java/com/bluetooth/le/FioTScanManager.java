package com.bluetooth.le;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by caoxuanphong on    7/23/16.
 */
public class FioTScanManager {
    private static final String TAG = "FioTScanManager";
    private String filter = "";
    private ScanManagerListener listener;
    private FioTBluetoothLE ble;
    private volatile boolean ignoreExist;
    private ArrayList<BLEDevice> list = new ArrayList<>();

    public interface ScanManagerListener {
        void onFoundDevice(final String name,
                           final String address,
                           final int type,
                           final int bondState,
                           final int rssi);
    }

    public FioTScanManager(Context context) {
        ble = new FioTBluetoothLE(context);
        ble.setBluetoothLEScanListener(scanListener);
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public void start(ScanManagerListener listener, boolean ignoreExist) {
        this.ignoreExist = ignoreExist;
        this.listener = listener;
        ble.startScanning();
    }

    public void stop() {
        ble.stopScanning();
        list.clear();
    }

    public void end() {
        Log.i(TAG, "Scan manager end");
        ble.end();
    }

    FioTBluetoothLE.BluetoothLEScanListener scanListener = new FioTBluetoothLE.BluetoothLEScanListener() {
        @Override
        public void onFoundDevice(BluetoothDevice device, int rssi) {
            synchronized (this) {
                if (device.getName() == null) return;

                if (device.getName().contains(filter)) {

                    if (!exist(device)) {
                        list.add(new BLEDevice(rssi, device));
                    } else if (ignoreExist) {
                        return;
                    }

                    if (listener != null) {
                        listener.onFoundDevice(device.getName(),
                                device.getAddress(),
                                device.getType(),
                                device.getBondState(),
                                rssi);
                    }
                }
            }
        }
    };

    private boolean exist(BluetoothDevice device) {
        for (BLEDevice d : list) {
            if (d.device.getAddress().equalsIgnoreCase(device.getAddress())) {
                return true;
            }
        }

        return false;
    }

    class BLEDevice {
        BluetoothDevice device;
        int rssi;

        public BLEDevice(int rssi, BluetoothDevice device) {
            this.rssi = rssi;
            this.device = device;
        }
    }

}
