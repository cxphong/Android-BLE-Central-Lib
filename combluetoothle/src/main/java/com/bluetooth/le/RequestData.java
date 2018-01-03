package com.bluetooth.le;

import android.bluetooth.BluetoothGattCharacteristic;

/**
 * Created by caoxuanphong on 1/3/18.
 */

public class RequestData {
    private BluetoothGattCharacteristic characteristic;
    private byte[] data;

    public RequestData(BluetoothGattCharacteristic characteristic, byte[] data) {
        this.characteristic = characteristic;
        this.data = data;
    }

    public BluetoothGattCharacteristic getCharacteristic() {
        return characteristic;
    }

    public void setCharacteristic(BluetoothGattCharacteristic characteristic) {
        this.characteristic = characteristic;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
