package com.bluetooth.le;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.UUID;

import static com.bluetooth.le.FioTScanManager.ScanMode.CONTINUOUS;
import static com.bluetooth.le.FioTScanManager.ScanMode.LOW_BATTERY;

/**
 * Created by caoxuanphong on    7/23/16.
 */
public class FioTScanManager {
    private static final String TAG = "FioTScanManager";
    private static final int DURATION_IN_LOW_BATTERY = 30000;
    private static final int SLEEP_TIME_IN_LOW_BATTERY = 30000;
    private String filter = "";
    private ScanManagerListener listener;
    private FioTBluetoothLE ble;
    private volatile boolean ignoreExist;
    private ArrayList<BLEDevice> list = new ArrayList<>();
    private Handler handler1;
    private Handler handler2;

    public enum ScanMode {
        LOW_BATTERY,
        CONTINUOUS,
    }

    private ScanMode scanMode = CONTINUOUS;

    public interface ScanManagerListener {
        void onFoundDevice(BluetoothDevice device,
                           final int rssi);
    }

    public FioTScanManager(Context context) {
        ble = new FioTBluetoothLE(context);
        ble.setBluetoothLEScanListener(scanListener);
    }

    /**
     * Start scan ble
     *
     * @param filter
     * @param ignoreExist
     * @param scanMode
     * @param servicesUUID
     * @param listener
     */
    public void start(final String filter,
                      final boolean ignoreExist,
                      final ScanMode scanMode,
                      final UUID[] servicesUUID,
                      final ScanManagerListener listener) {
        this.scanMode = scanMode;
        this.filter = filter;
        this.ignoreExist = ignoreExist;
        this.listener = listener;

        ble.startScanning(servicesUUID);

        if (scanMode == LOW_BATTERY) {
            if (Looper.myLooper() == null) {
                Looper.prepare();
                Looper.loop();
            } else {
                Log.d(TAG, "start: log is already ok");
            }

            handler1 = new Handler();
            handler1.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG, "run: 1");
                    ble.stopScanning();

                    handler2 = new Handler();
                    handler2.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Log.i(TAG, "run: 2");
                            start(filter,
                                    ignoreExist,
                                    scanMode,
                                    servicesUUID,
                                    listener);
                        }
                    }, SLEEP_TIME_IN_LOW_BATTERY);
                }
            }, DURATION_IN_LOW_BATTERY);
        }
    }

    /**
     * Stop scan ble device
     */
    public void stop() {
        if (scanMode == LOW_BATTERY) {
            Log.d(TAG, "Stop all handler");
            if (handler1 != null) {
                handler1.removeCallbacksAndMessages(null);
            }

            if (handler2 != null) {
                handler2.removeCallbacksAndMessages(null);
            }
        }

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
                        listener.onFoundDevice(device, rssi);
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

