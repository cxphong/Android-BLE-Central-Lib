package com.bluetooth.le;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.bluetooth.le.exception.BluetoothOffException;
import com.bluetooth.le.utils.ByteUtils;
import com.example.com.bluetooth.le.R;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

/**
 * Represent "Bluetooth low energy" that using to connect to a bluetooth remote device.
 * <p>
 * How to use:
 * 1> Create FioTBluetoothLE instance
 * <p>
 * 2> Scan nearby device
 * + Listen BluetoothLEScanListener callback
 * + call startScanning() to start scan
 * + call stopScanning() to stop scan
 * <p>
 * 3> Connect to nearby device
 * + Listen BluetoothLEListener callback
 * + add remote device's services that we care
 * + call connect() to connect
 * + call closeConnection() to closeConnection
 * <p>
 * 4> Close app
 * + call end()
 */
public class FioTBluetoothLE {
    private static final String TAG = "FioTBluetoothLE";
    public static final int ENABLE_BLUETOOTH_REQUEST_CODE = 123;
    private static FioTBluetoothLE instance;
    private BluetoothLEListener mBluetoothLEListener;
    private BluetoothLEScanListener mBluetoothLEScanListener;
    private Context mContext;
    private volatile boolean mIsConnected;
    private volatile boolean mScanning;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothGatt mBluetoothGatt;
    private List<FioTBluetoothService> mWorkingBluetoothService = new ArrayList<>();
    private Queue<FioTBluetoothCharacteristic> mEnableNotifyQueue = new LinkedList<>();
    private Queue<byte[]> mDataToWriteQueue = new LinkedList<byte[]>();
    private ArrayList<BluetoothGattCharacteristic> mListCharacteristic = new ArrayList<BluetoothGattCharacteristic>();

    public static final int CONNECT_SUCCESS = 1;
    public static final int CONNECT_FAIL = 2;

    // Fix 20 bytes for m2m
    private BluetoothGattCharacteristic compareCharacteristics;
    private byte[] compareBytes = new byte[20];
    private Timer readTimer = new java.util.Timer();
    private int counter;
    private final static int MAX_NUM_READ = 3;
    private Timer writeTimer = new java.util.Timer();

    public synchronized void disableWrite() {
        this.disableWrite = true;
    }

    public synchronized void enableWrite() {
        this.disableWrite = false;
    }

    private volatile boolean disableWrite;

    private Object write = new Object();
    private Object read = new Object();

    public static FioTBluetoothLE createInstance(Context context) {
        if (instance == null) {
            instance = new FioTBluetoothLE(context);
        }

        return instance;
    }

    public FioTBluetoothLE getInstance() {
        return instance;
    }

    /**
     * Create new FioTBluetoothLE and enable bluetooth if it's necessary.
     *
     * @param context
     */
    public FioTBluetoothLE(Context context) {
        this.mContext = context;

        enableWrite();
        mBluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
    }

    /**
     * Finalize FioTBluetoothLE instance. Use when closing app.
     */
    public void end() {
        Log.i(TAG, "end");

        try {
            mBluetoothAdapter = null;
            mBluetoothManager = null;
            mScanning = false;
            mIsConnected = false;
            instance = null;
            mBluetoothDevice = null;
            mBleCallback = null;
            mBluetoothGatt = null;
            mBluetoothLEListener = null;
            writeTimer.cancel();
            readTimer.cancel();
        } catch (Exception e) {

        }
    }

    public int getState() {
        if (mBluetoothAdapter != null) {
            return mBluetoothAdapter.getState();
        }

        return -1;
    }

    public void setBluetoothLEListener(BluetoothLEListener listener) {
        this.mBluetoothLEListener = listener;
    }

    public void setBluetoothLEScanListener(BluetoothLEScanListener listener) {
        this.mBluetoothLEScanListener = listener;
    }

    /**
     * Start scan nearby bluetooth device
     * Founded device will be in @onLeScan()
     */
    public synchronized void startScanning(UUID[] servicesUUID) throws BluetoothOffException {
        if (mScanning) return;

        if (mBluetoothAdapter.getState() != BluetoothAdapter.STATE_ON) {
            throw new BluetoothOffException(mContext.getString(R.string.bluetooth_not_on));
        }

        Log.i(TAG, "startScanning");
        mBluetoothAdapter.startLeScan(servicesUUID, mDeviceFoundCallback);
        mScanning = true;
    }

    /**
     * Stop scanning
     */
    public synchronized void stopScanning() {
        if (!mScanning || (mBluetoothAdapter == null)) return;

        Log.i(TAG, "stopScanning: ");
        mBluetoothAdapter.stopLeScan(mDeviceFoundCallback);
        mScanning = false;
    }

    private void refresh() {
        try {
            Method method = mBluetoothGatt.getClass().getDeclaredMethod("refresh");
            method.setAccessible(true);
            Object r = method.invoke(mBluetoothGatt);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    /**
     * Connect to remote device. Check @onConnectionStateChange() to get result
     *
     * @param deviceAddress Remote device address
     */
    public void connect(String deviceAddress) {
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "connect: mBluetoothAdapter is null");
            return;
        }

        mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(deviceAddress);

        if (mBluetoothGatt != null) {
            if (mBluetoothManager.getConnectionState(mBluetoothDevice, BluetoothProfile.GATT) ==
                    BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Already connect to device");
                return;
            }
        }

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mBluetoothGatt = mBluetoothDevice.connectGatt(mContext, false, mBleCallback, BluetoothDevice.TRANSPORT_LE);
        } else {
            mBluetoothGatt = mBluetoothDevice.connectGatt(mContext, false, mBleCallback);
        }

        refresh();
    }

    /**
     * Reconnect to remote device after it lost connection
     */
    public void reConnect() {
        if (mBluetoothGatt != null) {
            Log.i(TAG, "reConnect");
            mBluetoothGatt.connect();
        }
    }

    public void startPair(String deviceAddress) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(deviceAddress);
            mBluetoothDevice.createBond();
        }
    }

    /**
     * Disconnect to remote device
     */
    public synchronized void closeConnection() {
        mIsConnected = false;
        mWorkingBluetoothService.clear();
        mListCharacteristic.clear();

        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
            gattClose();
        }
    }

    public void addWorkingService(ArrayList<FioTBluetoothService> services) {
        if (services == null) {
            return;
        }

        mWorkingBluetoothService = new ArrayList<>(services);
    }

    /**
     * Write data to remote device. Writing is synchronous, mean one by one.
     *
     * @param ch          Characteristic to writeWithQueue
     * @param dataToWrite data to be written
     */
    public int writeToCharacteristic(BluetoothGattCharacteristic ch, final byte[] dataToWrite) {
        if (null == ch || mBluetoothGatt == null) {
            Log.i(TAG, "writeToCharacteristic: character "
                    + ch
                    + "mBluetoothGatt = "
                    + mBluetoothGatt);
            return 1;
        }

        Log.i(TAG, "writeToCharacteristic: " + ByteUtils.toHexString(dataToWrite));
        ch.setValue(dataToWrite);
        mBluetoothGatt.writeCharacteristic(ch);
        ByteUtils.toHexString(dataToWrite);
        return 0;
    }

    public interface SendListener {
        void sent(int num);
    }

    public void write(final BluetoothGattCharacteristic ch,
                                  final byte[] dataToWrite,
                                  final int delayTime,
                                  final int blockSize) {
        writeWithReadBack(ch, dataToWrite, delayTime, blockSize, null);
    }

    /**
     * Step 1: Write chunk
     * Step 2: Wait until response writeWithQueue success
     * Step 3: Read back data
     * Step 4: If data equal written data, next
     *
     * @param ch
     * @param dataToWrite
     * @param delayTime
     * @param blockSize
     * @param listener
     */
    public void writeWithoutReadBack(final BluetoothGattCharacteristic ch,
                                          final byte[] dataToWrite,
                                          final int delayTime,
                                          final int blockSize,
                                          final SendListener listener) {
        int numBytesSent = 0;
        compareCharacteristics = ch;

        while (numBytesSent < dataToWrite.length && !disableWrite) {
            byte[] bytes = ByteUtils.subByteArray(dataToWrite, numBytesSent, blockSize);

            writeToCharacteristic(ch, bytes);
            numBytesSent += bytes.length;
            compareBytes = bytes;

            Log.i(TAG, "writeWithoutReadBack: " + ByteUtils.toHexString(bytes));
            Log.i(TAG, "remain " + (dataToWrite.length - numBytesSent));

            if (listener != null) {
                listener.sent(numBytesSent);
            }

            // After 1s
            writeTimer = new Timer();
            writeTimer.schedule(
                    new java.util.TimerTask() {
                        @Override
                        public void run() {
                            synchronized (write) {
                                Log.i(TAG, "Write timeout");
                                write.notify();
                            }
                        }
                    },
                    1000
            );

            // Wait until writeWithQueue successful
            try {
                synchronized (write) {
                    write.wait();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Step 1: Write chunk
     * Step 2: Wait until response writeWithQueue success
     * Step 3: Read back data
     * Step 4: If data equal written data, next
     *
     * @param ch
     * @param dataToWrite
     * @param delayTime
     * @param blockSize
     * @param listener
     */
    public void writeWithReadBack(final BluetoothGattCharacteristic ch,
                                  final byte[] dataToWrite,
                                  final int delayTime,
                                  final int blockSize,
                                  final SendListener listener) {
        int numBytesSent = 0;
        compareCharacteristics = ch;

        while (numBytesSent < dataToWrite.length && !disableWrite) {
            byte[] bytes = ByteUtils.subByteArray(dataToWrite, numBytesSent, blockSize);

            writeToCharacteristic(ch, bytes);
            numBytesSent += bytes.length;
            compareBytes = bytes;

            Log.i(TAG, "writeWithReadBack: " + ByteUtils.toHexString(dataToWrite));
            Log.i(TAG, "remain " + (dataToWrite.length - numBytesSent));

            if (listener != null) {
                listener.sent(numBytesSent);
            }

            // After 1s
            writeTimer = new Timer();
            writeTimer.schedule(
                    new java.util.TimerTask() {
                        @Override
                        public void run() {
                            synchronized (write) {
                                Log.i(TAG, "Write timeout");
                                write.notify();
                            }
                        }
                    },
                    1000
            );

            // Wait until writeWithQueue successful
            try {
                synchronized (write) {
                    write.wait();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Read back data to check characteristic updated value
            readTimer = new Timer();
            readTimer.schedule(
                    new java.util.TimerTask() {
                        @Override
                        public void run() {
                            synchronized (FioTBluetoothLE.this) {
                                Log.i(TAG, "read counter = " + counter);
                                if (counter < MAX_NUM_READ) {
                                    requestCharacteristicValue(ch);
                                    counter++;
                                } else {
                                    // Too long (~10s) and characteristic does not update wanted value
                                    // 99% - writeWithQueue fail and need retry
                                    // 1% - writeWithQueue success but firmware did not update it value

                                    counter = 0;
                                    writeToCharacteristic(compareCharacteristics, compareBytes);
                                }
                            }
                        }
                    },
                    0,
                    1000
            );

            // Wait until read successful
            try {
                synchronized (read) {
                    read.wait();
                    readTimer.cancel();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Read characteristic's value
     *
     * @param ch characteristic that we want to read
     */
    public void requestCharacteristicValue(BluetoothGattCharacteristic ch) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) return;

        Log.i(TAG, "requestCharacteristicValue: ");
        mBluetoothGatt.readCharacteristic(ch);
    }

    /**
     * Get a characteristic of connected device
     *
     * @param uuid characteristic uuid
     * @return characteristic, null if not found
     */
    public BluetoothGattCharacteristic getCharacteristic(String uuid) {
        BluetoothGattCharacteristic characteristic = null;

        for (BluetoothGattCharacteristic ch : mListCharacteristic) {
            if (ch.getUuid().toString().equalsIgnoreCase(uuid)) {
                characteristic = ch;
                break;
            }
        }

        return characteristic;
    }

    /**
     * Get RSSI of connected device.
     * Result will be in @onReadRemoteRSSI(int rssi, int status)
     */
    public void readRSSI() {
        mBluetoothGatt.readRemoteRssi();
    }

    private void printConfigService() {
        Log.d(TAG, "================List configure services:================");

        for (FioTBluetoothService fioTBluetoothService : mWorkingBluetoothService) {
            Log.i(TAG, "Service UUID = " + fioTBluetoothService.getUuid());
            for (FioTBluetoothCharacteristic fioTBluetoothCharacteristic : fioTBluetoothService.getCharacteristics()) {
                Log.i(TAG, "Characteristic UUID = " + fioTBluetoothCharacteristic.getUuid());
            }
        }
        Log.d(TAG, "================List configure services:================");
    }

    private void printFoundService() {
        Log.d(TAG, "================List found services:================");

        if (mBluetoothGatt != null) {
            List<BluetoothGattService> services = mBluetoothGatt.getServices();
            for (BluetoothGattService service : services) {
                Log.i(TAG, "Service UUID = " + service.getUuid());
                for (BluetoothGattCharacteristic bluetoothGattCharacteristic : service.getCharacteristics()) {
                    Log.i(TAG, "Characteristic UUID = " + bluetoothGattCharacteristic.getUuid());
                }
            }
        }
        Log.d(TAG, "================List found services:================");
    }

    /**
     * Get working services, enable characteristic notification
     */
    public void getSupportedServices() {
        printConfigService();
        printFoundService();

        if (mBluetoothGatt != null) {
            List<BluetoothGattService> services = mBluetoothGatt.getServices();

            for (BluetoothGattService service : services) {
                for (FioTBluetoothService fioTBluetoothService : mWorkingBluetoothService) {
                    if (service.getUuid().toString().equalsIgnoreCase(fioTBluetoothService.getUuid().toString())) {
                        List<BluetoothGattCharacteristic> chars = service.getCharacteristics();
                        mListCharacteristic.addAll(chars);
                    }
                }
            }
        }
    }

    public void setNotification(FioTBluetoothCharacteristic ch) {
        mEnableNotifyQueue.add(ch);
        if (mEnableNotifyQueue.size() == 1) {
            setNotificationForCharacteristic(mEnableNotifyQueue.element());
        }
    }

    /**
     * Enable characteristic notification
     *
     * @param ch
     */
    private void setNotificationForCharacteristic(FioTBluetoothCharacteristic ch) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null || ch == null) {
            Log.i(TAG, "characteristic " + ch);
            return;
        }

        boolean enabled = ch.isNotify();

        boolean success = mBluetoothGatt.setCharacteristicNotification(ch.getCharacteristic(), enabled);
        if (!success) {
            Log.e(TAG, "Setting proper notification status for characteristic failed!");
        }

        // This is also sometimes required (e.g. for heart rate monitors) to enable notifications/indications
        // see: https://developer.bluetooth.org/gatt/descriptors/Pages/DescriptorViewer.aspx?u=org.bluetooth.descriptor.gatt.client_characteristic_configuration.xml
        BluetoothGattDescriptor descriptor = ch.getCharacteristic().getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
        if (descriptor != null) {
            Log.i(TAG, "setNotificationForCharacteristic: " + ch.getUuid().toString());
            byte[] val = enabled ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
            descriptor.setValue(val);
            mBluetoothGatt.writeDescriptor(descriptor);
        } else {
            mEnableNotifyQueue.remove();
            if (mEnableNotifyQueue.size() > 0) {
                setNotificationForCharacteristic(mEnableNotifyQueue.element());
            } else {
                mBluetoothLEListener.onConnectResult(CONNECT_SUCCESS, 0);
            }
        }
    }

    /**
     * Callback when a bluetooth device founded
     */
    private BluetoothAdapter.LeScanCallback mDeviceFoundCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
            if (mBluetoothLEScanListener != null) {
                mBluetoothLEScanListener.onFoundDevice(device, rssi);
            }
        }
    };

    private BluetoothGattCallback mBleCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            if (newState == BluetoothGatt.STATE_CONNECTED) {
                Log.i(TAG, "Connected");
                mIsConnected = true;

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    mBluetoothGatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
                }

                mBluetoothLEListener.onConnectResult(CONNECT_SUCCESS, 0);

                /* Delay to reduce error 133 */
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        startServicesDiscovery();
                    }
                }, 2000);
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                Log.i(TAG, "onConnectionStateChange: disconnect, " +
                        "status " + status +
                        ", mIsConnected " + mIsConnected);

                if (status != BluetoothGatt.GATT_SUCCESS && !mIsConnected) {
                    mBluetoothLEListener.onConnectResult(CONNECT_FAIL, status);
                } else {
                    synchronized (read) {
                        read.notifyAll();
                    }

                    synchronized (write) {
                        write.notifyAll();
                    }

                    mIsConnected = false;
                    mBluetoothLEListener.onDisconnected();
                }
            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // now, when services discovery is finished, we can call getServices() for Gatt
                getSupportedServices();

                if (mBluetoothLEListener != null) {
                    mBluetoothLEListener.onGetSupportServiceComplete();
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            Log.i(TAG, "onCharacteristicRead: read done");

            if (characteristic == compareCharacteristics) {
                if (ByteUtils.compare2Array(characteristic.getValue(), compareBytes)) {
                    Log.i(TAG, "onCharacteristicRead: same data");
                    readTimer.cancel();
                    synchronized (read) {
                        counter = 0;
                        read.notify();
                    }
                } else {
                    Log.i(TAG, "onCharacteristicRead: different data");
                    Log.i(TAG, "read: " + ByteUtils.toHexString(characteristic.getValue()));
                    Log.i(TAG, "compare: " + Arrays.toString(compareBytes));
                }
            }

            if (readListener != null) {
                readListener.onRead(characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            if (mBluetoothLEListener != null) {
                mBluetoothLEListener.onReceiveData(gatt,
                        characteristic,
                        characteristic.getValue());
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.i(TAG, "onCharacteristicWrite: " + status);

            writeTimer.cancel();
            synchronized (write) {
                write.notify();
            }

            mBluetoothLEListener.onDidWrite(characteristic, status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            mBluetoothLEListener.onReadRemoteRSSI(rssi, status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.i(TAG, "onDescriptorWrite status = " + status);

            if (status != BluetoothGatt.GATT_SUCCESS) {
                if (mBluetoothLEListener != null) {
                    mBluetoothLEListener.onConnectResult(CONNECT_FAIL, status);
                }

                mEnableNotifyQueue.clear();
                return;
            }

            mEnableNotifyQueue.remove();
            if (mEnableNotifyQueue.size() > 0) {
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        setNotificationForCharacteristic(mEnableNotifyQueue.element());
                    }
                }, 500);
            } else {
                if (mBluetoothLEListener != null) {
                    mBluetoothLEListener.onStartListenNotificationComplete();
                }
            }
        }
    };

    /**
     * Discovery service of connected device.
     * Result will be in @onServicesDiscovered() callback
     */
    private void startServicesDiscovery() {
        if (mBluetoothGatt == null) return;

        mBluetoothGatt.discoverServices();
    }

    /**
     * Close gatt connection to remote device
     */
    private void gattClose() {
        if (mBluetoothGatt == null) return;

        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    private BluetoothLEReadListener readListener;

    public void setBluetoothLEReadListener(BluetoothLEReadListener listener) {
        this.readListener = listener;
    }

    public interface BluetoothLEReadListener {
        void onRead(BluetoothGattCharacteristic characteristic);
    }

    public interface BluetoothLEScanListener {
        void onFoundDevice(BluetoothDevice device, final int rssi);
    }

    public interface BluetoothLEListener {
        void onConnectResult(int result, int error);

        void onGetSupportServiceComplete();

        void onDisconnected();

        void onReceiveData(BluetoothGatt gatt, BluetoothGattCharacteristic charac, byte[] data);

        void onDidWrite(BluetoothGattCharacteristic cha, int status);

        void onReadRemoteRSSI(int rssi, int status);

        void onStartListenNotificationComplete();

    }

    /**
     * Get device that we connect before
     *
     * @return
     */
    public BluetoothDevice getBluetoothDevice() {
        return mBluetoothDevice;
    }

}
