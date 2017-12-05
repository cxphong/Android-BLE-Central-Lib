package com.bluetooth.le;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.util.Log;

import com.bluetooth.le.exception.BluetoothOffException;
import com.bluetooth.le.exception.CharacteristicNotFound;
import com.bluetooth.le.exception.IncorrectState;
import com.bluetooth.le.utils.ByteUtils;
import com.example.com.bluetooth.le.R;

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

    /* After timeout @onConnectFail() is called */
    private static final int CONNECT_TIMEOUT_MILLISECOND = 30000;

    /* Number of bytes send to characteristic a time, limit 20 bytes, more cause error */
    private static final int DATA_CHUNK = 20;

    public enum Status {
        disconnected,
        connecting,
        connected
    }

    private BluetoothDevice device;
    private ArrayList<FioTBluetoothService> services = new ArrayList<>();
    private FioTBluetoothLE ble;
    private FioTConnectManagerListener listener;
    private Context mContext;
    private Timer connectTimeout;
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

        void onReadRSSI(int rssi);
    }

    public FioTManager(Context context, FioTBluetoothDevice device, ArrayList<FioTBluetoothService> services) {
        this.mContext = context;
        this.device = device.getBluetoothDevice();
        this.services = services;
        status = disconnected;
        initLE();
    }

    /**
     * Init FiotBluetoothLE
     */
    private void initLE() {
        ble = new FioTBluetoothLE(mContext);
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

    public Status getStatus() {
        return status;
    }

    /**
     * Finish working with ble device
     */
    public void end() {
        Log.i(TAG, "Connect manager end");
        status = disconnected;
        services.clear();
        device = null;

        if (connectTimeout != null) {
            connectTimeout.cancel();
        }

        if (ble != null) {
            ble.end();
            ble = null;
        }
    }

    /**
     * Connect to ble device
     */
    public int connect() {
        if (status == disconnected) {
            status = connecting;
            ble.connect(device.getAddress());
            startConnectTimeout(CONNECT_TIMEOUT_MILLISECOND);
            return 0;
        } else {
            Log.i(TAG, "connect: already connected or connecting");
            return 1;
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

    /**
     * Write data to characteristic with any size
     * If size > DATA_CHUNK, it is cut into small chunks
     */
    public boolean writeWithQueue(String characUUID, byte[] data) throws CharacteristicNotFound {
        if (data == null || characUUID == null || ble == null) return true;

        FioTBluetoothCharacteristic ch = getCharacteristic(characUUID);

        if (ch == null) {
            throw new CharacteristicNotFound(mContext.getResources().
                    getString(R.string.exception_characteristic_not_found) +
                    characUUID);
        }

        Queue<byte[]> queue = ch.getmDataToWriteQueue();
        boolean isQueueEmpty = (queue.size() == 0);

        /* Split data into multiple packet with size equal DATA_CHUNK */
        int index = 0;
        while (index < data.length) {
            byte[] bytes = ByteUtils.subByteArray(data, index, DATA_CHUNK);
            index += bytes.length;
            queue.add(bytes);
        }

        /* If first data */
        if (isQueueEmpty && ble != null) {
            ble.writeToCharacteristic(getCharacteristic(characUUID).getCharacteristic(),
                    ch.getmDataToWriteQueue().element());
        } else {
            Log.i(TAG, "not writeWithQueue, queue's size " + queue.size());

            for (byte[] bytes : queue) {
                Log.i(TAG, "writeWithQueue: " + ByteUtils.toHexString(bytes));
            }
        }

        return true;
    }

    public void writeWithoutQueue(String characUUID, byte[] data) throws CharacteristicNotFound {
        if (data == null || characUUID == null || ble == null) return;

        FioTBluetoothCharacteristic ch = getCharacteristic(characUUID);

        if (ch == null) {
            throw new CharacteristicNotFound(mContext.getResources().
                    getString(R.string.exception_characteristic_not_found) +
                    characUUID);
        }

        ble.writeToCharacteristic(getCharacteristic(characUUID).getCharacteristic(), data);

    }

    public void clearCharacteristicQueue(String uuid) {
        FioTBluetoothCharacteristic ch = getCharacteristic(uuid);

        if (ch != null) {
            Queue<byte[]> queue = ch.getmDataToWriteQueue();

            if (queue != null) {
                if (queue.size() > 0) {
                    queue.clear();
                }
            }
        }
    }

    public synchronized void writeLargeSafe(String characUuid,
                                            byte[] data,
                                            int delayTimeMilliSec,
                                            int blockSize,
                                            FioTBluetoothLE.SendListener listener) {
        ble.writeWithReadBack(getCharacteristic(characUuid).getCharacteristic(),
                data,
                delayTimeMilliSec,
                blockSize,
                listener);
    }

    public synchronized void writeLargeFast(String characUuid,
                                            byte[] data,
                                            int delayTimeMilliSec,
                                            int blockSize) {
        ble.writeWithoutReadBack(getCharacteristic(characUuid).getCharacteristic(),
                data,
                delayTimeMilliSec,
                blockSize,
                null);
    }

    public void read(String characUuid) {
        ble.requestCharacteristicValue(getCharacteristic(characUuid).getCharacteristic());
    }

    public boolean isConnected() {
        return status == connected;
    }

    public void setFioTConnectManagerListener(FioTConnectManagerListener listener) {
        this.listener = listener;
    }

    public FioTBluetoothCharacteristic getCharacteristic(String characUuid) {
        for (FioTBluetoothService service : services) {
            for (FioTBluetoothCharacteristic fioTBluetoothCharacteristic : service.getCharacteristics()) {
                if (fioTBluetoothCharacteristic.getUuid().equalsIgnoreCase(characUuid)) {
                    return fioTBluetoothCharacteristic;
                }
            }
        }

        return null;
    }

    public FioTBluetoothCharacteristic getCharacteristic(BluetoothGattCharacteristic characteristic) {
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

    public void enableNotification(String characteristicUUID) throws IncorrectState, CharacteristicNotFound {
        if (ble == null) {
            throw new IncorrectState(mContext.getResources().getString(R.string.exception_icorrect_state));
        }

        FioTBluetoothCharacteristic characteristic = getCharacteristic(characteristicUUID);

        if (characteristic == null) {
            throw new CharacteristicNotFound(mContext.getResources().
                    getString(R.string.exception_characteristic_not_found) +
                    characteristicUUID);
        }

        characteristic.setNotify(true);
        ble.setNotification(getCharacteristic(characteristicUUID));
    }

    public void disableNotification(String characteristicUUID) throws IncorrectState, CharacteristicNotFound {
        if (ble == null) {
            throw new IncorrectState(mContext.getResources().getString(R.string.exception_icorrect_state));
        }

        FioTBluetoothCharacteristic characteristic = getCharacteristic(characteristicUUID);

        if (characteristic == null) {
            throw new CharacteristicNotFound(mContext.getResources().
                    getString(R.string.exception_characteristic_not_found) +
                    characteristicUUID);
        }

        characteristic.setNotify(false);
        ble.setNotification(getCharacteristic(characteristicUUID));
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
                    try {
                        enableNotification(c.getUuid());
                    } catch (IncorrectState incorrectState) {
                        incorrectState.printStackTrace();
                    } catch (CharacteristicNotFound characteristicNotFound) {
                        characteristicNotFound.printStackTrace();
                    }
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
        Log.i(TAG, "onDidWrite: " + status);
        FioTBluetoothCharacteristic characteristic = getCharacteristic(cha);
        Queue queue = characteristic.getmDataToWriteQueue();

        if (queue != null && queue.size() > 0) {
            queue.remove();
            if (characteristic.getmDataToWriteQueue().size() > 0) {
                ble.writeToCharacteristic(cha, (byte[]) queue.element());
            }
        }
    }

    @Override
    public void onReadRemoteRSSI(int rssi, int status) {
        if (listener != null) {
            listener.onReadRSSI(rssi);
        }
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

}