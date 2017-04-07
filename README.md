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
    FioTScanManager scanManager = new FioTScanManager(this);
    
    # Start
    scanManager.start("", true, new FioTScanManager.ScanManagerListener() {
            @Override
            public void onFoundDevice(BluetoothDevice device, int rssi) {
                
            }
    });
    
    # Stop
    scanManager.stop();

- Connect, recieve notify, receive read, ble state
    
    # Define services & characteristics
      ArrayList<FioTBluetoothService> services = new ArrayList<FioTBluetoothService>();
      ArrayList<FioTBluetoothCharacteristic> characteristics = new ArrayList<FioTBluetoothCharacteristic>();
      characteristics.add(new FioTBluetoothCharacteristic("00002a26-0000-1000-8000-00805f9b34fb", false));
      services.add(new FioTBluetoothService("0000180a-0000-1000-8000-00805f9b34fb", characteristics));

      manager = new FioTManager(MainActivity.this,
        devicesList.get((int) view1.getTag()).device,
        services);

    # Listener
    manager.setFioTConnectManagerListener(new FioTManager.FioTConnectManagerListener() {
      @Override
      public void onConnectFail(int error) {

      }

      @Override
      public void onConnected() {
        
      }

      @Override
      public void onDisconnected() {
          Log.i(TAG, "onDisconnected: ");
      }

      @Override
      public void onNofify(FioTBluetoothCharacteristic characteristic) {
          Log.i(TAG, "onNofify: ");
          ByteUtils.printArray(characteristic.getCharacteristic().getValue());
      }

      @Override
      public void onRead(FioTBluetoothCharacteristic characteristic) {
          Log.i(TAG, "onRead: ");
          ByteUtils.printArray(characteristic.getCharacteristic().getValue());
      }
    });

- Disconnect
    manager.end()
    
- read
   manager.read("00002a26-0000-1000-8000-00805f9b34fb");

- write
    manager.write("00002a26-0000-1000-8000-00805f9b34fb", new byte[]{1,2,3});
 
