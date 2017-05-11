package com.bluetooth.le;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.util.Log;

import com.bluetooth.le.utils.ByteUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

import static android.R.attr.name;
import static com.bluetooth.le.FioTManager.Status.connected;
import static com.bluetooth.le.FioTManager.Status.connecting;
import static com.bluetooth.le.FioTManager.Status.disconnected;

/**
 * Created by caoxuanphong on    7/25/16.
 */

public class FioTManager implements FioTBluetoothLE.BluetoothLEListener, FioTBluetoothLE.BluetoothLEReadListener {
    private static final String TAG = "FioTManager";

    private static final int CONNECT_TIMEOUT_MILLISECOND = 30000;

    private static final int DATA_CHUNK = 20; // 20 bytes

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
    private Timer connectTimeout;
    private FioTScanManager scanManager;
    private Status status;

    /**
     * State callback
     */
    public interface FioTConnectManagerListener {
        void onConnectFail(int error);

        void onConnected();

        void onDisconnected(FioTManager manager);

        void onNotify(FioTBluetoothCharacteristic characteristic);

        void onRead(FioTBluetoothCharacteristic characteristic);
    }

    public FioTManager(Context context, BluetoothDevice device, ArrayList<FioTBluetoothService> services) {
        this.mContext = context;
        this.device = device;
        this.services = services;

        status = disconnected;
        ble = new FioTBluetoothLE(context);
        ble.setBluetoothLEListener(this);
        ble.setBluetoothLEReadListener(this);
        ble.addWorkingService(services);
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

    /**
     * Finish working with ble device
     */
    public void end() {
        Log.i(TAG, "Connect manager end");
        status = disconnected;
        listener = null;

        if (ble != null) {
            ble.disableWrite();
            ble.stopScanning();
            ble.closeConnection();
            ble.end();
            ble.setBluetoothLEListener(null);
            ble.setBluetoothLEReadListener(null);
            ble = null;
        }
    }

    /**
     * Connect to ble device
     */
    public void connect() {
        if (status == disconnected ) {
            status = connecting;
            ble.connect(device.getAddress());
            startConnectTimeout(CONNECT_TIMEOUT_MILLISECOND);
        } else {
            Log.i(TAG, "connect: already connected or connecting");
        }
    }

    private void stopConnectTimeout() {
        if (connectTimeout != null) {
            connectTimeout.cancel();
        }
    }

    private void startConnectTimeout(int timeoutMillisec) {
        if (timeoutMillisec > 0) {
            connectTimeout = new Timer();
            connectTimeout.schedule(new TimerTask() {
                @Override
                public void run() {
                    Log.i(TAG, "Connect time out");
                    if (status != connected) {
                        listener.onConnectFail(-1);
                    }
                    end();
                }
            }, timeoutMillisec);
        }
    }

    public void reConnect(final String oldAddress, final String oldName, final int timeoutmill) {
        scanManager = new FioTScanManager(mContext);
        scanManager.start("", true, new FioTScanManager.ScanManagerListener() {
            @Override
            public void onFoundDevice(BluetoothDevice device, int rssi) {
                if (device.getAddress().equalsIgnoreCase(oldAddress) ||
                        device.getName().equalsIgnoreCase(oldName)) {
                    Log.i(TAG, "onFoundDevice: " + name);

                    if (status == disconnected) {
                        status = connecting;
                        scanManager.stop();
                        scanManager.end();
                        ble.connect(getDevice().getAddress());
                        startConnectTimeout(timeoutmill);
                    } else {
                        Log.i(TAG, "connect: already connected or connecting");
                    }
                }
            }
        });
    }

    public boolean write(String characUUID, byte[] data) {
        if (data == null || characUUID == null) return true;

        FioTBluetoothCharacteristic ch = getCharacteristic(characUUID);
        if (ch == null) return false;
        ch.getCharacteristic().setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);

        Queue<byte[]> queue = ch.getmDataToWriteQueue();
        boolean isQueueEmpty = (queue.size() == 0);

        /* Split data into multiple packet with size equal DATA_CHUNK */
        int index = 0;
        while (index < data.length) {
            byte[] bytes = ByteUtils.subByteArray(data, index, DATA_CHUNK);
            index += bytes.length;
            queue.add(bytes);
        }

        Log.i(TAG, "size: " + queue.size());

        /* If first data */
        if (isQueueEmpty){
            ble.writeDataToCharacteristic(getCharacteristic(characUUID).getCharacteristic(),
                    ch.getmDataToWriteQueue().element());
        }

        return true;
    }

//    public int write(String characUuid, byte[] data) {
//        FioTBluetoothCharacteristic characteristic = getCharacteristic(characUuid);
//        Queue<byte[]> queue = characteristic.getmDataToWriteQueue();
//
//        queue.add(data);
//
//        if (queue)
//        return ble.writeDataToCharacteristic(getCharacteristic(characUuid).getCharacteristic(),
//                characteristic.getmDataToWriteQueue().remove());
//    }
//
//    public int write(FioTBluetoothCharacteristic characteristic, byte[] data) {
//        characteristic.getmDataToWriteQueue().add(data);
//        return ble.writeDataToCharacteristic(getCharacteristic(characteristic.getUuid()).getCharacteristic(), data);
//    }

    public synchronized void write(String characUuid,
                      byte[] data,
                      int delayTimeMilliSec,
                      int blockSize,
                      FioTBluetoothLE.SendListener listener) {
        ble.writeDataToCharacteristic(getCharacteristic(characUuid).getCharacteristic(),
                data,
                delayTimeMilliSec,
                blockSize,
                listener);
    }

    public synchronized void write(String characUuid,
                      byte[] data,
                      int delayTimeMilliSec,
                      int blockSize) {
        ble.writeDataToCharacteristic(getCharacteristic(characUuid).getCharacteristic(),
                data,
                delayTimeMilliSec,
                blockSize);
    }

    public synchronized void write2(String characUuid,
                                     byte[] data,
                                     int delayTimeMilliSec,
                                     int blockSize) {
        ble.writeDataToCharacteristic2(getCharacteristic(characUuid).getCharacteristic(),
                data,
                delayTimeMilliSec,
                blockSize);
    }

    public void read(String characUuid) {
        ble.requestCharacteristicValue(getCharacteristic(characUuid).getCharacteristic());
    }

    public boolean isConnected() {
        return status == connected;
    }

    @Override
    public void onRead(BluetoothGattCharacteristic characteristic) {
        if (listener != null) {
            listener.onRead(getCharacteristic(characteristic));
        }
    }

    @Override
    public void onConnectResult(int result, int error) {
        if (result == FioTBluetoothLE.CONNECT_SUCCESS) {
            /* Wait until search services complete */
            Log.i(TAG, "onConnectResult: success");
        } else if (result == FioTBluetoothLE.CONNECT_FAIL) {
            Log.i(TAG, "onConnectResult: fail");
            if (listener != null) listener.onConnectFail(error);
            stopConnectTimeout();
            end();
        }
    }

    @Override
    public void onGetSupportServiceComplete() {
        Log.i(TAG, "onGetSupportServiceComplete");

        boolean hasCharacteristicsNeedEnableNotify = false;

        for (FioTBluetoothService service : services) {
            for (FioTBluetoothCharacteristic c : service.getCharacteristics()) {
                c.setCharacteristic(ble.getCharacteristic(c.getUuid()));
                if (c.isNotify()) {
                    Log.i(TAG, "onGetSupportServiceComplete: " + c.getUuid());
                    hasCharacteristicsNeedEnableNotify = true;
                    ble.startListenNotification(c.getCharacteristic());
                }
            }
        }

        /* No characteristic need enable notify */
        if (!hasCharacteristicsNeedEnableNotify) {
            status = connected;
            stopConnectTimeout();
            if (listener != null) listener.onConnected();
        }
    }

    @Override
    public void onDisconnected() {
        Log.i(TAG, "onDisconnected");

        if (status == connecting) {
            if (listener != null) listener.onConnectFail(0);
        } else {
            status = disconnected;

            ble.disableWrite();

            if (listener != null) listener.onDisconnected(this);
        }

        stopConnectTimeout();
        end();
    }

    @Override
    public void onReceiveData(BluetoothGatt gatt, BluetoothGattCharacteristic charac, final byte[] data) {
        if (listener != null) listener.onNotify(getCharacteristic(charac));
    }

    @Override
    public void onDidWrite(BluetoothGattCharacteristic cha, int status) {
        FioTBluetoothCharacteristic characteristic = getCharacteristic(cha);
        Queue queue = characteristic.getmDataToWriteQueue();
        queue.remove();
        if (characteristic.getmDataToWriteQueue().size() > 0) {
            ble.writeDataToCharacteristic(cha, (byte[]) queue.element());
        }
    }

    @Override
    public void onReadRemoteRSSI(int rssi, int status) {

    }

    @Override
    public void onStartListenNotificationComplete() {
        Log.i(TAG, "onStartListenNotificationComplete");
        status = connected;

        ble.enableWrite();
        Log.i(TAG, "onStartListenNotificationComplete: " + this);
        if (connectTimeout != null) {
            connectTimeout.cancel();
        }

        if (listener != null) listener.onConnected();
    }

    public void setFioTConnectManagerListener(FioTConnectManagerListener listener) {
        this.listener = listener;
    }

    private FioTBluetoothCharacteristic getCharacteristic(String characUuid) {
        for (FioTBluetoothService service : services) {
            for (FioTBluetoothCharacteristic fioTBluetoothCharacteristic : service.getCharacteristics()) {
                if (fioTBluetoothCharacteristic.getUuid().equalsIgnoreCase(characUuid)) {
                    return fioTBluetoothCharacteristic;
                }
            }
        }

        return null;
    }

    private FioTBluetoothCharacteristic getCharacteristic(BluetoothGattCharacteristic characteristic) {
        FioTBluetoothCharacteristic ch = null;

        for (FioTBluetoothService service : services) {
            for (FioTBluetoothCharacteristic c : service.getCharacteristics()) {
                if (c.getCharacteristic() == characteristic) {
                    ch = c;
                    break;
                }
            }
        }

        return ch;
    }
}
