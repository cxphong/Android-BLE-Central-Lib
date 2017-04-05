package com.bluetooth.le;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import com.bluetooth.le.exception.NotFromActivity;
import com.bluetooth.le.exception.NotSupportBleException;
import com.example.com.bluetooth.le.R;

/**
 * Created by caoxuanphong on 4/5/17.
 */

public class FiotBluetoothInit {
    private static final String TAG = "FiotBluetoothInit";
    private static final int REQUEST_ENABLE_BT = 2006;
    private static final int REQUEST_PERMISSION = 2007;
    private Context context;

    public FiotBluetoothInit(Context context) {
        this.context = context;
    }

    private void checkSupportBle() throws  NotSupportBleException {
        if (!isBleHardwareAvailable()) {
            throw new NotSupportBleException(context.getResources().getString(R.string.exception_not_support_ble));
        }
    }

    private void checkBluetoothAndEnablePermission() {
        if (!isBluetoothEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            ((Activity)context).startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            requestPermission();
        }
    }


    public void init() throws NotSupportBleException, NotFromActivity {
        if (!(context instanceof Activity)) {
            throw new NotFromActivity("Given Context must be an Activity");
        }

        ((Activity)context).getApplication().registerActivityLifecycleCallbacks(activityLifecycleCallbacks);

        checkSupportBle();
        checkBluetoothAndEnablePermission();
    }

    /**
     * Check if phone supports BLE
     *
     * @return true on support, false on not.
     */
    public boolean isBleHardwareAvailable() {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    /**
     * Check phone's bluetooth is enable
     *
     * @return true on enable, false on disable
     */
    public boolean isBluetoothEnabled() {
        final BluetoothManager bluetoothManager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        return bluetoothManager.getAdapter().isEnabled();
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ((Activity) context).requestPermissions(new String[]{
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                }, REQUEST_PERMISSION);
            }
        }
    }

    Application.ActivityLifecycleCallbacks activityLifecycleCallbacks = new Application.ActivityLifecycleCallbacks() {
        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

        }

        @Override
        public void onActivityStarted(Activity activity) {

        }

        @Override
        public void onActivityResumed(Activity activity) {
            checkBluetoothAndEnablePermission();
        }

        @Override
        public void onActivityPaused(Activity activity) {

        }

        @Override
        public void onActivityStopped(Activity activity) {

        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

        }

        @Override
        public void onActivityDestroyed(Activity activity) {

        }

    };


}
