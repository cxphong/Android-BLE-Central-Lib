package android.fiot.android_ble;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.bluetooth.le.FioTBluetoothCharacteristic;
import com.bluetooth.le.FioTBluetoothDevice;
import com.bluetooth.le.FioTBluetoothService;
import com.bluetooth.le.FioTManager;
import com.bluetooth.le.FioTScanManager;
import com.bluetooth.le.FiotBluetoothInit;
import com.bluetooth.le.exception.BluetoothOffException;
import com.bluetooth.le.exception.CharacteristicNotFound;
import com.bluetooth.le.exception.NotFromActivity;
import com.bluetooth.le.exception.NotSupportBleException;
import com.bluetooth.le.utils.ByteUtils;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements FiotBluetoothInit.FiotBluetoothInitListener {
    private static final String TAG = "MainActivity";
    private RecyclerView recyclerView;
    private TextView txtNod;
    private static TextView txvS;
    private DevicesAdapter dAdapter;
    public static List<FioTBluetoothDevice> devicesList = new ArrayList<>();
    private FioTScanManager scanManager;
    private FioTManager manager;

    public static final String SERVICE_UUID = "00001811-0000-1000-8000-00805f9b34fb";
    public static final String CH44_UUID = "00002a44-0000-1000-8000-00805f9b34fb";
    public static final String CH45_UUID = "00002a45-0000-1000-8000-00805f9b34fb";
    public static final String CH46_UUID = "00002a46-0000-1000-8000-00805f9b34fb";
    public static final String CH47_UUID = "00002a47-0000-1000-8000-00805f9b34fb";
    public static final String CH48_UUID = "00002a48-0000-1000-8000-00805f9b34fb";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        txtNod = (TextView) findViewById(R.id.textViewNoD);
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        dAdapter = new DevicesAdapter(devicesList);

        recyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(dAdapter);

        txvS = (TextView) findViewById(R.id.textViewS);
        txvS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (txvS.getText() == "START SCAN") {
                    txvS.setText("STOP SCAN");
                    startScan();
                } else {
                    stopScan();
                }
            }
        });

        try {
            FiotBluetoothInit.enable(this, this);
        } catch (NotSupportBleException e) {
            e.printStackTrace();
        } catch (NotFromActivity notFromActivity) {
            notFromActivity.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (scanManager != null) {
            scanManager.stop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (scanManager != null) {
            scanManager.stop();
        }
    }

    private void stopScan() {
        scanManager.stop();
        txvS.setText("START SCAN");

    }

    private void startScan() {
        devicesList.clear();
        try {
            scanManager.start("", true, FioTScanManager.ScanMode.CONTINUOUS, null, new FioTScanManager.ScanManagerListener() {
                @Override
                public void onFoundDevice(final FioTBluetoothDevice device, int rssi) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            devicesList.add(device);
                            dAdapter.notifyDataSetChanged();
                        }
                    });
                }
            });
        } catch (BluetoothOffException e) {
            e.printStackTrace();
        }
    }

    private void showDevices(List<BLEDevice> devicesList) {

        if (devicesList.isEmpty()) {
            txtNod.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.INVISIBLE);
        } else {
            txtNod.setVisibility(View.INVISIBLE);
            recyclerView.setVisibility(View.VISIBLE);
        }

        dAdapter.notifyDataSetChanged();

    }

    @Override
    public void completed() {
        Log.i(TAG, "completed: ");
        scanManager = new FioTScanManager(this);
    }

    private class BLEDevice {
        BluetoothDevice device;
        int rssi;

        public BLEDevice(BluetoothDevice device, int rssi) {
            this.device = device;
            this.rssi = rssi;
        }
    }

    public class DevicesAdapter extends RecyclerView.Adapter<DevicesAdapter.MyViewHolder> {


        private List<FioTBluetoothDevice> devicesList;

        public class MyViewHolder extends RecyclerView.ViewHolder {
            public TextView name, mac;
            public TextView rssi;
            public Button btnConnect;
            public View view1;


            public MyViewHolder(View view) {

                super(view);
                this.view1 = view;
                name = (TextView) view.findViewById(R.id.name);
                mac = (TextView) view.findViewById(R.id.mac);
                rssi = (TextView) view.findViewById(R.id.rssi);
                btnConnect = (Button) view.findViewById(R.id.buttonConnect);
                btnConnect.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.i(TAG, "onClick: " + (int) view1.getTag());
                        scanManager.stop();
                        scanManager.end();

                        ArrayList<FioTBluetoothService> services = new ArrayList<FioTBluetoothService>();
                        ArrayList<FioTBluetoothCharacteristic> characteristics2 = new ArrayList<FioTBluetoothCharacteristic>();
                        characteristics2.add(new FioTBluetoothCharacteristic(CH44_UUID, false));
                        characteristics2.add(new FioTBluetoothCharacteristic(CH45_UUID, true));
                        characteristics2.add(new FioTBluetoothCharacteristic(CH46_UUID, true));
                        characteristics2.add(new FioTBluetoothCharacteristic(CH47_UUID, false));
                        characteristics2.add(new FioTBluetoothCharacteristic(CH48_UUID, false));
                        services.add(new FioTBluetoothService(SERVICE_UUID, characteristics2));

                        manager = new FioTManager(MainActivity.this,
                                devicesList.get((int) view1.getTag()),
                                services);
                        manager.connect();

                        manager.setFioTConnectManagerListener(new FioTManager.FioTConnectManagerListener() {

                            @Override
                            public void onConnectFail(int error) {
                                Log.i(TAG, "onConnectFail: ");
                            }

                            @Override
                            public void onConnected() {
                                try {
                                    manager.writeWithQueue(CH44_UUID, "HELLO WORLD!".getBytes());
                                } catch (CharacteristicNotFound characteristicNotFound) {
                                    characteristicNotFound.printStackTrace();
                                }
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

                            @Override
                            public void onReadRSSI(int rssi) {

                            }

                        });

                    }
                });

            }
        }


        public DevicesAdapter(List<FioTBluetoothDevice> devicesList) {
            this.devicesList = devicesList;
        }

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.device_list_row, parent, false);

            return new MyViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, int position) {
            FioTBluetoothDevice device = devicesList.get(position);
            holder.name.setText(device.getBluetoothDevice().getName());
            holder.mac.setText(device.getBluetoothDevice().getAddress());
            holder.view1.setTag(position);
        }

        @Override
        public int getItemCount() {
            Log.i(TAG, "getItemCount: " + devicesList.size());
            return devicesList.size();
        }


    }
}
