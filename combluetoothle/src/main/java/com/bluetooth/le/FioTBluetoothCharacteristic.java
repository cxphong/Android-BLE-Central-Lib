package com.bluetooth.le;

import android.bluetooth.BluetoothGattCharacteristic;

import com.bluetooth.le._enum.CharacteristicProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by caoxuanphong on    7/25/16.
 */
public class FioTBluetoothCharacteristic {
    private String uuid;
    private BluetoothGattCharacteristic characteristic;
    private ArrayList<CharacteristicProperty> characteristicProperties = new ArrayList();
    private volatile Queue<byte[]> mDataToWriteQueue = new LinkedList<>();


    public FioTBluetoothCharacteristic(String uuid, CharacteristicProperty... characteristicProperties) {
        this.uuid = uuid;
        this.characteristicProperties.clear();
        Collections.addAll(this.characteristicProperties, characteristicProperties);
    }

    public Queue<byte[]> getmDataToWriteQueue() {
        return mDataToWriteQueue;
    }

    public void clearDataBuffer() {
        mDataToWriteQueue.clear();
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
        return characteristicProperties.contains(CharacteristicProperty.PROPERTY_NOTIFY);
    }

    public boolean isReadable() {
        return characteristicProperties.contains(CharacteristicProperty.PROPERTY_READ);
    }

    public boolean isWriteable() {
        return characteristicProperties.contains(CharacteristicProperty.PROPERTY_WRITE);
    }

    public boolean isIndicate() {
        return characteristicProperties.contains(CharacteristicProperty.PROPERTY_INDICATE);

    }

    public void setNotify(boolean notify) {
        if (notify) {
            if (!this.characteristicProperties.contains(CharacteristicProperty.PROPERTY_NOTIFY)) {
                characteristicProperties.add(CharacteristicProperty.PROPERTY_NOTIFY);
            }
        } else {
            if (this.characteristicProperties.contains(CharacteristicProperty.PROPERTY_NOTIFY)) {
                characteristicProperties.remove(CharacteristicProperty.PROPERTY_NOTIFY);
            }
        }
    }

    public void setReadable(boolean readable) {
        if (readable) {
            if (!this.characteristicProperties.contains(CharacteristicProperty.PROPERTY_READ)) {
                characteristicProperties.add(CharacteristicProperty.PROPERTY_READ);
            }
        } else {
            if (this.characteristicProperties.contains(CharacteristicProperty.PROPERTY_READ)) {
                characteristicProperties.remove(CharacteristicProperty.PROPERTY_READ);
            }
        }
    }

    public void setWriteable(boolean writeable) {
        if (writeable) {
            if (!this.characteristicProperties.contains(CharacteristicProperty.PROPERTY_WRITE)) {
                characteristicProperties.add(CharacteristicProperty.PROPERTY_WRITE);
            }
        } else {
            if (this.characteristicProperties.contains(CharacteristicProperty.PROPERTY_WRITE)) {
                characteristicProperties.remove(CharacteristicProperty.PROPERTY_WRITE);
            }
        }
    }
    public void setIndicate(boolean indicate) {
        if (indicate) {
            if (!this.characteristicProperties.contains(CharacteristicProperty.PROPERTY_INDICATE)) {
                characteristicProperties.add(CharacteristicProperty.PROPERTY_INDICATE);
            }
        } else {
            if (this.characteristicProperties.contains(CharacteristicProperty.PROPERTY_INDICATE)) {
                characteristicProperties.remove(CharacteristicProperty.PROPERTY_INDICATE);
            }
        }
    }
    public void setCharacteristicProperty(CharacteristicProperty... characteristicProperty) {
        this.characteristicProperties.clear();
        this.characteristicProperties.addAll(Arrays.asList(characteristicProperty));
    }

    /**
     * Set writeWithQueue type, must call after ble is in connected state
     *
     * @param type BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
     *             BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
     *             BluetoothGattCharacteristic.WRITE_TYPE_SIGNED
     */
    public boolean setWriteType(int type) {
        if (characteristic == null) return false;

        characteristic.setWriteType(type);
        return true;
    }

}
