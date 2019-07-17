package android.fiot.android_ble;

import com.bluetooth.le.FioTBluetoothDevice;
import com.bluetooth.le.FioTManager;

public class DeviceItem {
    FioTBluetoothDevice fioTBluetoothDevice;
    FioTManager fioTManager;
    boolean connected;

    public DeviceItem(FioTBluetoothDevice fioTBluetoothDevice) {
        this.fioTBluetoothDevice = fioTBluetoothDevice;
    }
}
