package com.bluetooth.le;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.bluetooth.le.FioTManager.Status.connected;
import static com.bluetooth.le.FioTManager.Status.connecting;
import static com.bluetooth.le.FioTManager.Status.disconnected;

/**
 * Created by caoxuanphong on    7/25/16.
 */

public class FioTManager implements FioTBluetoothLE.BluetoothLEListener {
    private static final String TAG = "FioTManager";

    public enum Status {
        disconnected,
        connecting,
        connected
    }

    private BluetoothDevice device;
    private List<FioTBluetoothService> services = new ArrayList<>();
    private FioTBluetoothLE ble;
    private FioTConnectManagerListener listener;
    private Context mContext;
    private ScheduledFuture connectionSchedule;
    private FioTScanManager scanManager;
    private Status status;

    public interface FioTConnectManagerListener {
        void onConnectFail(int error);
        void onConnected();
        void onDisconnected();
        void onHasData(byte[] data, FioTBluetoothCharacteristic characteristic);
    }

    public FioTManager(Context context, BluetoothDevice device, List<FioTBluetoothService> services) {
        this.mContext = context;
        this.device = device;
        this.services = services;

        status = disconnected;
        ble = new FioTBluetoothLE(context);
        ble.setBluetoothLEListener(this);
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    public List<FioTBluetoothService> getServices() {
        return services;
    }

    public FioTBluetoothLE getBle() {
        return ble;
    }

    public FioTScanManager getScanManager() {
        return scanManager;
    }

    public Status getStatus() {
        return status;
    }

    public void end() {
        Log.i(TAG, "Connect manager end");
        status = disconnected;
        ble.disableWrite();
        ble.stopScanning();
        ble.closeConnection();
        ble.end();
        ble.setBluetoothLEListener(null);
    }

    public void connect(String address, int timeoutMillisec) {
        Log.i(TAG, "connect: " + timeoutMillisec);
        if (status == disconnected) {
            status = connecting;
            ble.connect(address);
            startConnectTimeout(timeoutMillisec);
        } else {
            Log.i(TAG, "connect: already connected or connecting");
        }
    }

    private void startConnectTimeout(int timeoutMillisec) {
        if (timeoutMillisec > 0) {
            ScheduledExecutorService s =
                    Executors.newScheduledThreadPool(1);
            connectionSchedule = s.schedule(new Callable() {
                @Override
                public Object call() throws Exception {
                    Log.i(TAG, "call: check");
                    if (status == connected) {
                        listener.onConnectFail(-1);
                    }
                    return null;
                }
            }, timeoutMillisec, TimeUnit.MILLISECONDS);
        }
    }

    public void reConnect(final String oldAddress, final String oldName, final int timeoutmill) {
        scanManager = new FioTScanManager(mContext);
        scanManager.start(true, new FioTScanManager.ScanManagerListener() {
            @Override
            public void onFoundDevice(String name, String address, int type, int bondState, int rssi) {
                if (address.equalsIgnoreCase(oldAddress) ||
                        name.equalsIgnoreCase(oldName)) {
                    Log.i(TAG, "onFoundDevice: " + name);

                    if (status == disconnected) {
                        status = connecting;
                        scanManager.stop();
                        scanManager.end();
                        ble.connect(address);
                        startConnectTimeout(timeoutmill);
                    } else {
                        Log.i(TAG, "connect: already connected or connecting");
                    }
                }
            }
        });
    }

    public int write(FioTBluetoothCharacteristic c,
                      byte[] data) {
        return ble.writeDataToCharacteristic(c.getCharacteristic(), data);
    }

    public void write(FioTBluetoothCharacteristic c,
                      byte[] data,
                      int delayTimeMilliSec,
                      int blockSize,
                      FioTBluetoothLE.SendListener listener) {
        ble.writeDataToCharacteristic(c.getCharacteristic(),
                data,
                delayTimeMilliSec,
                blockSize,
                listener);
    }

    public void write(FioTBluetoothCharacteristic c,
                      byte[] data,
                      int delayTimeMilliSec,
                      int blockSize) {
        ble.writeDataToCharacteristic(c.getCharacteristic(),
                data,
                delayTimeMilliSec,
                blockSize);
    }

    public void write2(FioTBluetoothCharacteristic c,
                      byte[] data,
                      int delayTimeMilliSec,
                      int blockSize) {
        ble.writeDataToCharacteristic2(c.getCharacteristic(),
                data,
                delayTimeMilliSec,
                blockSize);
    }

    public void read(BluetoothGattCharacteristic characteristic) {
        ble.requestCharacteristicValue(characteristic);
    }

    public boolean isConnected() {
        return status == connected;
    }
    @Override
    public void onConnectResult(int result, int error) {
        if (result == FioTBluetoothLE.CONNECT_SUCCESS) {
            Log.i(TAG, "onConnectResult: success");
        } else if (result == FioTBluetoothLE.CONNECT_FAIL) {
            Log.i(TAG, "onConnectResult: fail");
            if (listener != null) listener.onConnectFail(error);
        }
    }

    @Override
    public void onGetSupportServiceComplete() {
        Log.i(TAG, "onGetSupportServiceComplete");

        boolean hasNotify = false;

        for (FioTBluetoothService service : services) {
            for (FioTBluetoothCharacteristic c : service.getCharacteristics()) {
                if (c.isNotify()) {
                    Log.i(TAG, "onGetSupportServiceComplete: " + c.getUuid());
                    hasNotify = true;
                    ble.startListenNotification(c.getCharacteristic());
                }
            }
        }

        if (!hasNotify) {
            status = connected;

            if (connectionSchedule != null) {
                if (!connectionSchedule.isCancelled() ||
                        !connectionSchedule.isDone()) {
                    connectionSchedule.cancel(false);
                }
            }

            if (listener != null) listener.onConnected();
        }
    }

    @Override
    public void onDisconnected() {
        Log.i(TAG, "onDisconnected");
        status = disconnected;

        ble.disableWrite();

        try {
            connectionSchedule.cancel(true);
        } catch (Exception e) {

        }

        if (listener != null) listener.onDisconnected();
    }

    @Override
    public void onReceiveData(BluetoothGatt gatt, BluetoothGattCharacteristic charac, final byte[] data) {
        FioTBluetoothCharacteristic ch = null;

        for (FioTBluetoothService service : services) {
            for (FioTBluetoothCharacteristic c : service.getCharacteristics()) {
                if (c.getCharacteristic() == charac) {
                    ch = c;
                    break;
                }
            }
        }

        if (listener != null) listener.onHasData(data, ch);
    }

    @Override
    public void onDidWrite(BluetoothGattCharacteristic cha, int status) {

    }

    @Override
    public void onReadRemoteRSSI(int rssi, int status) {

    }

    @Override
    public void onStartListenNotificationComplete() {
        Log.i(TAG, "onStartListenNotificationComplete");
        status = connected;

        ble.enableWrite();
        Log.i(TAG, "onStartListenNotificationComplete: " + connectionSchedule);
        if (connectionSchedule != null) {
            if (!connectionSchedule.isCancelled() ||
                    !connectionSchedule.isDone()) {
                connectionSchedule.cancel(false);
            }
        }

        if (listener != null) listener.onConnected();
    }

    public void setFioTConnectManagerListener(FioTConnectManagerListener listener) {
        this.listener = listener;
    }

}
