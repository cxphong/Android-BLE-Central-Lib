package com.bluetooth.le;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.util.Log;

import com.bluetooth.le.exception.CharacteristicNotFound;
import com.bluetooth.le.exception.IncorrectState;
import com.bluetooth.le.request.Request;
import com.bluetooth.le.request.RequestCmd;
import com.bluetooth.le.request.RequestData;
import com.bluetooth.le.request.RequestHandler;
import com.bluetooth.le.utils.ByteUtils;
import com.example.com.bluetooth.le.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

import static com.bluetooth.le.FioTManager.ConnectionStatus.Connected;
import static com.bluetooth.le.FioTManager.ConnectionStatus.Connecting;
import static com.bluetooth.le.FioTManager.ConnectionStatus.Disconnected;

/**
 * Created by caoxuanphong on    7/25/16.
 */
public class FioTManager implements FioTBluetoothLE.BluetoothLEListener, FioTBluetoothLE.BluetoothLEReadListener {
    private static final String TAG = "FioTManager";

    /* After timeout @onConnectFail() is called */
    private static final int CONNECT_TIMEOUT_MILLISECOND = 30000;

    /* Number of bytes send to characteristic a time, limit 20 bytes, more cause error */
    private static final int DATA_CHUNK = 20;

    public enum ConnectionStatus {
        Disconnected,
        Connecting,
        Connected
    }

    private BluetoothDevice device;
    private ArrayList<FioTBluetoothService> services;
    private FioTBluetoothLE ble;
    private FioTManagerConnectionListener connectionListener;
    private FioTManagerDataListener dataListener;
    private Context mContext;
    private Timer connectionTimeout;
    private ConnectionStatus connectionStatus;
    private RequestHandler requestHandler;

    /**
     * State callback
     */
    public interface FioTManagerConnectionListener {
        void onConnectFail(int error);

        void onConnected();

        void onDisconnected(FioTManager manager);

        void onStatusChange(int interval);
    }

    public interface FioTManagerDataListener {
        void onNotify(FioTBluetoothCharacteristic characteristic);

        void onRead(FioTBluetoothCharacteristic characteristic);

        void onReadRSSI(int rssi);
    }

    public FioTManager(Context context, FioTBluetoothDevice device, ArrayList<FioTBluetoothService> services) {
        this.mContext = context;
        this.device = device.getBluetoothDevice();
        this.services = services;
        this.requestHandler = new RequestHandler();
        connectionStatus = Disconnected;
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

    public ConnectionStatus getConnectionStatus() {
        return connectionStatus;
    }

    /**
     * Finish working with ble device
     */
    public void end() {
        Log.i(TAG, "Connect manager end");
        connectionStatus = Disconnected;
        services.clear();
        device = null;

        stopConnectTimeout();

        if (ble != null) {
            ble.end();
            ble = null;
        }
    }

    /**
     * Connect to ble device
     */
    public int connect() {
        synchronized (connectionStatus) {
            if (connectionStatus == Disconnected) {
                connectionStatus = Connecting;
                ble.connect(device.getAddress());
                startConnectTimeout(CONNECT_TIMEOUT_MILLISECOND);
                return 0;
            } else {
                Log.i(TAG, "connect: already Connected or Connecting");
                return 1;
            }
        }
    }

    private void stopConnectTimeout() {
        if (connectionTimeout != null) {
            connectionTimeout.cancel();
        }
    }

    private void startConnectTimeout(int timeoutMillisec) {
        if (timeoutMillisec > 0) {
            connectionTimeout = new Timer();
            connectionTimeout.schedule(new TimerTask() {
                @Override
                public void run() {
                    Log.i(TAG, "Connect time out");
                    synchronized (connectionStatus) {
                        if (connectionStatus != Connected) {
                            connectionListener.onConnectFail(-1);
                        }
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
        if (!ch.isWriteable()) {
            throw new CharacteristicNotFound(mContext.getResources().
                    getString(R.string.exception_characteristic_do_not_have_property_write) +
                    characUUID);
        }
        /* Split data into multiple packet with size equal DATA_CHUNK */
        int index = 0;
        while (index < data.length) {
            byte[] bytes = ByteUtils.subByteArray(data, index, DATA_CHUNK);
            index += bytes.length;

            RequestData requestData = new RequestData(ch.getCharacteristic(), bytes);
            Request request = new Request(RequestCmd.WRITE, requestData);
            requestHandler.enqueue(request);
            requestHandler.implRightNow(ble);
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
        if (!ch.isWriteable()) {
            throw new CharacteristicNotFound(mContext.getResources().
                    getString(R.string.exception_characteristic_do_not_have_property_write) +
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
        Log.d(TAG, "read: ");
        BluetoothGattCharacteristic characteristic = getCharacteristic(characUuid).getCharacteristic();
        FioTBluetoothCharacteristic ch = getCharacteristic(characUuid);

        if (characteristic != null) {
            if (ch.isReadable()) {
                if (requestHandler != null) {
                    Log.d(TAG, "read: ");
                    RequestData requestData = new RequestData(characteristic, null);
                    Request request = new Request(RequestCmd.READ, requestData);
                    requestHandler.enqueue(request);
                    requestHandler.implRightNow(ble);
                } else {
                    Log.e(TAG, "read: request handler is null");
                }
            } else {
                Log.e(TAG, "read: characteristic don't have property read" + characUuid);
            }
        } else {
            Log.e(TAG, "read: no exist characteristics");
        }
    }

    public boolean isConnected() {
        return connectionStatus == Connected;
    }

    public void setConnectionListener(FioTManagerConnectionListener listener) {
        this.connectionListener = listener;
    }

    public void setDataListener(FioTManagerDataListener dataListener) {
        this.dataListener = dataListener;
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
        requestHandler.dequeue();
        requestHandler.impl(ble);

        if (dataListener != null) {
            dataListener.onRead(getCharacteristic(characteristic));
        }
    }

    @Override
    public void onConnectResult(int result, int error) {
        synchronized (connectionStatus) {
            if (connectionStatus == Connecting) {
                if (result == FioTBluetoothLE.CONNECT_SUCCESS) {
                    /* Wait until search services complete */
                    Log.i(TAG, "onConnectResult: success");
                } else if (result == FioTBluetoothLE.CONNECT_FAIL) {
                    Log.i(TAG, "onConnectResult: fail");
                    if (connectionListener != null) connectionListener.onConnectFail(error);
                    stopConnectTimeout();
                    end();
                }
            }
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
            synchronized (connectionStatus) {
                connectionStatus = Connected;
                stopConnectTimeout();
                if (connectionListener != null) {
                    connectionListener.onConnected();
                }
            }

        }
    }

    @Override
    public void onDisconnected() {
        Log.i(TAG, "onDisconnected");
        synchronized (connectionStatus) {
            if (connectionStatus == Connecting) {
                if (connectionListener != null) {
                    connectionListener.onConnectFail(0);
                }

                end();
            } else if (connectionStatus == Connected) {
                connectionStatus = Disconnected;

                ble.disableWrite();

                if (connectionListener != null) {
                    connectionListener.onDisconnected(this);
                }

                end();
            }
        }

    }

    @Override
    public void onReceiveData(BluetoothGatt gatt, BluetoothGattCharacteristic charac, final byte[] data) {
        if (dataListener != null) {
            dataListener.onNotify(getCharacteristic(charac));
        }
    }

    /**
     * Event write a chunk of data completed.
     *
     * @param cha
     * @param status
     */
    @Override
    public void onDidWrite(BluetoothGattCharacteristic cha, int status) {
        Log.i(TAG, "onDidWrite: " + status + " - " + cha.getUuid().toString());
        requestHandler.dequeue();
        requestHandler.impl(ble);
    }

    @Override
    public void onReadRemoteRSSI(int rssi, int status) {
        if (dataListener != null) {
            dataListener.onReadRSSI(rssi);
        }
    }

    /**
     * Callback after register to receive notification from characteristics.
     * After this event the setup ble connection is completed.
     */
    @Override
    public void onStartListenNotificationComplete() {
        Log.i(TAG, "onStartListenNotificationComplete");

        connectionStatus = Connected;
        ble.enableWrite();
        stopConnectTimeout();

        if (connectionListener != null) {
            connectionListener.onConnected();
        }
    }

    @Override
    public void onStatusChange(int interval) {
        if (connectionListener != null) {
            connectionListener.onStatusChange(interval);
        }
    }

}