package com.bluetooth.le;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.bluetooth.le.exception.BluetoothOffException;

import java.util.ArrayList;
import java.util.UUID;

import static com.bluetooth.le.FioTScanManager.ScanMode.FAST;
import static com.bluetooth.le.FioTScanManager.ScanMode.LOW_BATTERY;

/**
 * Created by caoxuanphong on    7/23/16.
 */
public class FioTScanManager {
    private static final String TAG = "FioTScanManager";
    private static final int DURATION_IN_LOW_BATTERY = 30000;
    private static final int SLEEP_TIME_IN_LOW_BATTERY = 30000;
    private String nameFilter = "";
    private ScanManagerListener listener;
    private FioTBluetoothLE ble;
    private volatile boolean ignoreDuplicate;
    private ArrayList<FioTBluetoothDevice> list = new ArrayList<>();
    private Handler handler1;
    private Handler handler2;

    public enum ScanMode {
        LOW_BATTERY,
        FAST,
    }

    private ScanMode scanMode = FAST;

    public interface ScanManagerListener {
        void onFoundDevice(FioTBluetoothDevice device, final int rssi, byte[] scanRecord);
    }

    public FioTScanManager(Context context) {
        ble = new FioTBluetoothLE(context);
    }

    /**
     * Start scan ble
     *
     * @param uuid
     * @param ignoreDuplicate
     * @param scanMode
     * @param
     * @param listener
     */
    public void startWithUUIDFilter(final UUID[] uuid,
                                    final boolean ignoreDuplicate,
                                    final ScanMode scanMode,
                                    final ScanManagerListener listener) throws BluetoothOffException {
        this.scanMode = scanMode;
        this.ignoreDuplicate = ignoreDuplicate;
        this.listener = listener;
        ble.setBluetoothLEScanListener(scanUUIDFilterListener);

        ble.startScanning(uuid);

        if (scanMode == LOW_BATTERY) {
            if (Looper.myLooper() == null) {
                Looper.prepare();
                Looper.loop();
            } else {
                Log.d(TAG, "startWithNameFilter: loop is already ok");
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
                                startWithUUIDFilter(uuid,
                                        ignoreDuplicate,
                                        scanMode,
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
     * Start scan ble
     *
     * @param nameFilter
     * @param ignoreDuplicate
     * @param scanMode
     * @param
     * @param listener
     */
    public void startWithNameFilter(final String nameFilter,
                                    final boolean ignoreDuplicate,
                                    final ScanMode scanMode,
                                    final ScanManagerListener listener) throws BluetoothOffException {
        this.scanMode = scanMode;
        this.nameFilter = nameFilter;
        this.ignoreDuplicate = ignoreDuplicate;
        this.listener = listener;
        ble.setBluetoothLEScanListener(scanNameFilterListener);

        ble.startScanning(null);

        if (scanMode == LOW_BATTERY) {
            if (Looper.myLooper() == null) {
                Looper.prepare();
                Looper.loop();
            } else {
                Log.d(TAG, "startWithNameFilter: loop is already ok");
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
                                startWithNameFilter(nameFilter,
                                        ignoreDuplicate,
                                        scanMode,
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

    FioTBluetoothLE.BluetoothLEScanListener scanNameFilterListener = new FioTBluetoothLE.BluetoothLEScanListener() {
        @Override
        public void onFoundDevice(BluetoothDevice device, int rssi, byte[] scanRecord) {
            synchronized (this) {
                Log.i(TAG, "onFoundDevice: " + this);

                if (device == null) {
                    return;
                }

                if (nameFilter != null && nameFilter.length() > 0) {// Has filter name
                    if (device.getName() == null || device.getName().length() == 0) {
                        return;
                    }
                }

                FioTBluetoothDevice fioTBluetoothDevice = new FioTBluetoothDevice(device, null);

                if (ignoreDuplicate) {
                    if (!exist(device)) {
                        list.add(fioTBluetoothDevice);
                    } else {
                        return;
                    }
                }

                if (listener != null) {
                    listener.onFoundDevice(fioTBluetoothDevice, rssi, scanRecord);
                }
            }
        }
    };

    FioTBluetoothLE.BluetoothLEScanListener scanUUIDFilterListener = new FioTBluetoothLE.BluetoothLEScanListener() {
        @Override
        public void onFoundDevice(BluetoothDevice device, int rssi, byte[] scanRecord) {
            synchronized (this) {
                Log.i(TAG, "onFoundDevice: " + this);

                if (device == null) {
                    return;
                }

                FioTBluetoothDevice fioTBluetoothDevice = new FioTBluetoothDevice(device, null);

                if (ignoreDuplicate) {
                    if (!exist(device)) {
                        list.add(fioTBluetoothDevice);
                    } else {
                        return;
                    }
                }

                if (listener != null) {
                    listener.onFoundDevice(fioTBluetoothDevice, rssi, scanRecord);
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

