package android.fiot.android_ble;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.bluetooth.le.FioTBluetoothCharacteristic;
import com.bluetooth.le.FioTBluetoothService;
import com.bluetooth.le.FioTManager;
import com.bluetooth.le.FioTScanManager;
import com.bluetooth.le.FiotBluetoothInit;
import com.bluetooth.le.exception.NotFromActivity;
import com.bluetooth.le.exception.NotSupportBleException;
import com.bluetooth.le.utils.ByteUtils;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements FiotBluetoothInit.FiotBluetoothInitListener {
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            FiotBluetoothInit.init(this, this);
        } catch (NotSupportBleException e) {
            e.printStackTrace();
        } catch (NotFromActivity notFromActivity) {
            notFromActivity.printStackTrace();
        }
    }

    @Override
    public void completed() {
        Log.i(TAG, "completed: ");

        final FioTScanManager scanManager = new FioTScanManager(this);

        scanManager.start("Alert Notification", true, new FioTScanManager.ScanManagerListener() {
            @Override
            public void onFoundDevice(BluetoothDevice device, int rssi) {
                Log.i(TAG, "onFoundDevice: " + device.getName() + " " + device.getAddress());

                scanManager.stop();

                ArrayList<FioTBluetoothService> services = new ArrayList<FioTBluetoothService>();
                ArrayList<FioTBluetoothCharacteristic> characteristics = new ArrayList<FioTBluetoothCharacteristic>();
                characteristics.add(new FioTBluetoothCharacteristic("00002a19-0000-1000-8000-00805f9b34fb", true));
                services.add(new FioTBluetoothService("0000180f-0000-1000-8000-00805f9b34fb", characteristics));


                FioTManager manager = new FioTManager(MainActivity.this, device, services);
                manager.connect(10000);
                manager.setFioTConnectManagerListener(new FioTManager.FioTConnectManagerListener() {
                    @Override
                    public void onConnectFail(int error) {

                    }

                    @Override
                    public void onConnected() {
                        Log.i(TAG, "onConnected: ");
                    }

                    @Override
                    public void onDisconnected() {

                    }

                    @Override
                    public void onHasData(byte[] data, FioTBluetoothCharacteristic characteristic) {
                        ByteUtils.printArray(data);
                    }
                });
            }
        });
    }
}
