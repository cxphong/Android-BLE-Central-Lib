package com.bluetooth.le;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.bluetooth.le.exception.BluetoothOffException;

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
    private ArrayList<FioTBluetoothDevice> list = new ArrayList<>();
    private Handler handler1;
    private Handler handler2;

    public enum ScanMode {
        LOW_BATTERY,
        CONTINUOUS,
    }

    private ScanMode scanMode = CONTINUOUS;

    public interface ScanManagerListener {
        void onFoundDevice(FioTBluetoothDevice device,
                           final int rssi);
    }

    public FioTScanManager(Context context) {
        ble = new FioTBluetoothLE(context);
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
                      final ScanManagerListener listener) throws BluetoothOffException {
        this.scanMode = scanMode;
        this.filter = filter;
        this.ignoreExist = ignoreExist;
        this.listener = listener;
        ble.setBluetoothLEScanListener(scanListener);

        ble.startScanning(servicesUUID);

        if (scanMode == LOW_BATTERY) {
            if (Looper.myLooper() == null) {
                Looper.prepare();
                Looper.loop();
            } else {
                Log.d(TAG, "start: loop is already ok");
            }

            handler1 = new Handler();
            handler1.postDelayed(new Runnable() {
                @Override
                public void run() {
                    ble.stopScanning();

                    handler2 = new Handler();
                    handler2.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                start(filter,
                                        ignoreExist,
                                        scanMode,
                                        servicesUUID,
                                        listener);
                            } catch (BluetoothOffException e) {
                                e.printStackTrace();
                            }
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

        ble.setBluetoothLEScanListener(null);
        ble.stopScanning();
        list.clear();
    }

    public void end() {
        Log.i(TAG, "Scan manager end");
        ble.end();
        list.clear();
    }

    public void removeDevice(String mac) {
        FioTBluetoothDevice device = null;

        for (FioTBluetoothDevice d : list) {
            if (d.getBluetoothDevice().getAddress().equalsIgnoreCase(mac)) {
                device = d;
            }
        }

        if (device != null) {
            list.remove(device);
        }
    }

    FioTBluetoothLE.BluetoothLEScanListener scanListener = new FioTBluetoothLE.BluetoothLEScanListener() {
        @Override
        public void onFoundDevice(BluetoothDevice device, int rssi) {
            synchronized (this) {
                if (device.getName() == null) return;

                Log.i(TAG, "onFoundDevice: " + this);

                FioTBluetoothDevice fioTBluetoothDevice = null;
                if (device.getName().contains(filter)) {
                    if (!exist(device)) {
                        fioTBluetoothDevice = new FioTBluetoothDevice(device, null);
                        list.add(fioTBluetoothDevice);
                    } else if (ignoreExist) {
                        return;
                    }

                    if (listener != null) {
                        listener.onFoundDevice(fioTBluetoothDevice, rssi);
                    }
                }
            }
        }
    };

    private boolean exist(BluetoothDevice device) {
        try {
            for (FioTBluetoothDevice d : list) {
                if (d.getBluetoothDevice().getAddress().equalsIgnoreCase(device.getAddress())) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

}

