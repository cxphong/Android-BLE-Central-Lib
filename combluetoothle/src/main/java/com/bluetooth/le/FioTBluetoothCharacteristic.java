package com.bluetooth.le;

import android.bluetooth.BluetoothGattCharacteristic;

/**
 * Created by caoxuanphong on    7/25/16.
 */
public class FioTBluetoothCharacteristic {
    private String uuid;
    private BluetoothGattCharacteristic characteristic;
    private boolean notify;
    private int writeType;

    public FioTBluetoothCharacteristic(String uuid, boolean notify) {
        this.uuid = uuid;
        this.notify = notify;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public BluetoothGattCharacteristic getCharacteristic() {
        return characteristic;
    }

    public void setCharacteristic(BluetoothGattCharacteristic characteristic) {
        this.characteristic = characteristic;
    }

    public boolean isNotify() {
        return notify;
    }

    public void setNotify(boolean notify) {
        this.notify = notify;
    }

    /**
     * Set write type, must call after ble is in connected state
     *
     * @param type
     * BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
     * BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
     * BluetoothGattCharacteristic.WRITE_TYPE_SIGNED
     */
    public boolean setWriteType(int type) {
        if (characteristic == null) return false;

        characteristic.setWriteType(type);
        return true;
    }

}
