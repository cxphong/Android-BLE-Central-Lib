package com.bluetooth.le;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

import com.bluetooth.le.exception.BluetoothOffException;
import com.bluetooth.le.scanner.BluetoothLeScannerCompat;
import com.bluetooth.le.scanner.ScanCallback;
import com.bluetooth.le.scanner.ScanFilter;
import com.bluetooth.le.scanner.ScanRecord;
import com.bluetooth.le.scanner.ScanResult;
import com.bluetooth.le.scanner.ScanSettings;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by caoxuanphong on    7/23/16.
 */
public class FioTScanManager {
    private static final String TAG = "FioTScanManager";
    private ScanManagerListener listener;
    private ArrayList<FioTBluetoothDevice> list = new ArrayList<>();
    private BluetoothLeScannerCompat scannerCompat;

    public interface ScanManagerListener {
        void onFoundDevice(FioTBluetoothDevice device, ScanResult result);

        void onScanFailed(int errorCode);
    }

    private ScanSettings getDefaultSetting() {
        return new ScanSettings.Builder()
                .setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH)
                .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
    }

    /**
     * Start scan
     *
     * @param filters
     * @param settings
     * @param listener
     * @throws BluetoothOffException
     */
    public void start(List<ScanFilter> filters,
                      ScanSettings settings,
                      ScanManagerListener listener) {
        this.listener = listener;
        scannerCompat = BluetoothLeScannerCompat.getScanner();

        if (settings == null) {
            settings = getDefaultSetting();
        }

        scannerCompat.startScan(filters, settings, scanCallback);
    }

    /**
     * Stop scan ble device
     */
    public void stop() {
        if (list != null) {
            list.clear();
        }

        if (scannerCompat != null) {
            scannerCompat.stopScan(scanCallback);
        }

    }

    ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            synchronized (this) {
                BluetoothDevice device = result.getDevice();

                Log.i(TAG, "onFoundDevice: " + device.getAddress() + ", " + device.getName());

                FioTBluetoothDevice fioTBluetoothDevice = new FioTBluetoothDevice(device, null);

                if (listener != null) {
                    listener.onFoundDevice(fioTBluetoothDevice, result);
                }
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);

            Log.e(TAG, "onScanFailed: " + errorCode);

            if (listener != null) {
                listener.onScanFailed(errorCode);
            }
        }
    };

}

