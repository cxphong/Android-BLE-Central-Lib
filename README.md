# FioTBle-Android
This is a wrapper of android bluetooth low energy API. It's simple and fast.

# Install 
Copy 'combluetoothle' into your project and make it as library dependency

# Usage
- Enable
    ``` java
    try {
        FiotBluetoothInit.enable(this, this);
    } catch (NotSupportBleException e) {
        e.printStackTrace();
    } catch (NotFromActivity notFromActivity) {
        notFromActivity.printStackTrace();
    }
    
    @Override
    public void completed() 
    
    }
    ```
- Scan:

    ```java
    FioTScanManager scanManager = new FioTScanManager(this);
    
    // Start
    scanManager.start("", true, new FioTScanManager.ScanManagerListener() {
            @Override
            public void onFoundDevice(BluetoothDevice device, int rssi) {
                
            }
    });
    
    // Stop
    scanManager.stop();
    ```
    
- Connect, recieve notify, receive read, ble state
``` java
// Define service & characteristic
ArrayList<FioTBluetoothService> services = new ArrayList<FioTBluetoothService>();
ArrayList<FioTBluetoothCharacteristic> characteristics1 = new ArrayList<FioTBluetoothCharacteristic>();
characteristics1.add(new FioTBluetoothCharacteristic("00002a29-0000-1000-8000-00805f9b34fb", false));
services.add(new FioTBluetoothService("0000180a-0000-1000-8000-00805f9b34fb", characteristics1));

ArrayList<FioTBluetoothCharacteristic> characteristics2 = new ArrayList<FioTBluetoothCharacteristic>();
characteristics2.add(new FioTBluetoothCharacteristic("9fbf120d-6301-42d9-8c58-25e699a21dbd", true));
characteristics2.add(new FioTBluetoothCharacteristic("22eac6e9-24d6-4bb5-be44-b36ace7c7bfb", true));
services.add(new FioTBluetoothService("7905f431-b5ce-4e99-a40f-4b1e122d00d0", characteristics2));

manager = new FioTManager(MainActivity.this,
        devicesList.get((int) view1.getTag()).device,
        services);
manager.connect(10000);

manager.setFioTConnectManagerListener(new FioTManager.FioTConnectManagerListener() {

    @Override
    public void onConnectFail(int error) {

    }

    @Override
    public void onConnected() {
        Log.i(TAG, "onConnected: ");
        manager.read("00002a29-0000-1000-8000-00805f9b34fb");
    }

    @Override
    public void onDisconnected(FioTManager manager) {

    }

    @Override
    public void onNotify(FioTBluetoothCharacteristic characteristic) {
        Log.i(TAG, "onNotify: " + characteristic.getUuid());
        Log.i(TAG, "onNofify: " + ByteUtils.toHexString(characteristic.getCharacteristic().getValue()));
    }

    @Override
    public void onRead(FioTBluetoothCharacteristic characteristic) {
        Log.i(TAG, "onRead: ");
        Log.i(TAG, "onRead: " + ByteUtils.toHexString(characteristic.getCharacteristic().getValue()));
        Log.i(TAG, "onRead: " + ByteUtils.toString(characteristic.getCharacteristic().getValue()));
    }
});
```
- Disconnect
    ```java
    manager.end()
    ```
    
- read
    ``` java
   manager.read("00002a26-0000-1000-8000-00805f9b34fb");
    ```
- write
    ``` java
    manager.write("00002a26-0000-1000-8000-00805f9b34fb", new byte[]{1,2,3});
    ```
