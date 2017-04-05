package android.fiot.android_ble;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.bluetooth.le.FioTScanManager;
import com.bluetooth.le.FiotBluetoothInit;
import com.bluetooth.le.exception.NotFromActivity;
import com.bluetooth.le.exception.NotSupportBleException;

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
        new FioTScanManager(this).start(false, new FioTScanManager.ScanManagerListener() {
            @Override
            public void onFoundDevice(String name, String address, int type, int bondState, int rssi) {
                Log.d(TAG, "onFoundDevice() called with: name = [" + name + "], address = [" + address + "], type = [" + type + "], bondState = [" + bondState + "], rssi = [" + rssi + "]");
            }
        });
    }
}
