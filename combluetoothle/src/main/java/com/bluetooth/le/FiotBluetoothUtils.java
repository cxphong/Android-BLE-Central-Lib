package com.bluetooth.le;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.List;
import java.util.Set;

import static android.content.ContentValues.TAG;

/**
 * Created by caoxuanphong on 4/10/17.
 */

public class FiotBluetoothUtils {
    private static final int REQUEST_ENABLE_BT = 2006;
    private static final int REQUEST_PERMISSION = 2007;

    public static Set<BluetoothDevice> getBondedDevices(Context context) {
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = bluetoothManager.getAdapter();
        return adapter.getBondedDevices();
    }

    /**
     * Check phone's bluetooth is enable
     *
     * @return true on enable, false on disable
     */
    public static boolean isBluetoothEnabled(Context context) {
        final BluetoothManager bluetoothManager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        return bluetoothManager.getAdapter().isEnabled();
    }

    public static void enableBluetooth(Context context) {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        ((Activity) context).startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }

    public static void requestPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ((Activity) context).requestPermissions(new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
            }, REQUEST_PERMISSION);
        }
    }

    /**
     * Get connected device
     *
     * @return BluetoothDevice
     */
    public List<BluetoothDevice> getListConnectedDevice(Context context) {
        BluetoothManager mBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);

        if (mBluetoothManager == null) {
            Log.d(TAG, "getConnectedDevice:  + mBluetoothManager is null");
            return null;
        }

        return mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
    }

}
