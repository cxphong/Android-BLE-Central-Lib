package android.fiot.android_ble;

import android.bluetooth.BluetoothDevice;
import android.graphics.Color;
import android.os.Bundle;
import android.os.ParcelUuid;
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
import com.bluetooth.le._enum.CharacteristicProperty;
import com.bluetooth.le.exception.CharacteristicNotFound;
import com.bluetooth.le.exception.NotFromActivity;
import com.bluetooth.le.exception.NotSupportBleException;
import com.bluetooth.le.scanner.ScanFilter;
import com.bluetooth.le.scanner.ScanResult;
import com.bluetooth.le.utils.ByteUtils;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements FiotBluetoothInit.FiotBluetoothInitListener {
    private static final String TAG = "MainActivity";
    private RecyclerView recyclerView;
    private TextView txtNod;
    private static TextView txvS;
    private DevicesAdapter dAdapter;
    public static List<DeviceItem> deviceItems = new ArrayList<>();
    private FioTScanManager scanManager;
    private long rxCount;
    private long rxCountTotal;
    private int index;
    private long startEpoch;
    private long startEpochTotal;
    private long numberLostPacket;

    public static final String SERVICE_UUID = "6E400001-B5A3-F393-E0A9-E50E24DCCA9E";
    public static final String RX_UUID = "6E400002-B5A3-F393-E0A9-E50E24DCCA9E";
    public static final String TX_UUID = "6E400003-B5A3-F393-E0A9-E50E24DCCA9E";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        txtNod = (TextView) findViewById(R.id.textViewNoD);
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        dAdapter = new DevicesAdapter(deviceItems);

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

        startScan();
        txvS.setText("STOP SCAN");
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
        deviceItems.clear();
        dAdapter.notifyDataSetChanged();

        List<ScanFilter> filters = new ArrayList<>();
        filters.add(new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(SERVICE_UUID)).setDeviceName("").build());
        try {
            scanManager.start(filters, null, new FioTScanManager.ScanManagerListener() {
                @Override
                public void onFoundDevice(final FioTBluetoothDevice device, ScanResult result) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            deviceItems.add(new DeviceItem(device));
                            showDevices(deviceItems);
                        }
                    });
                }

                @Override
                public void onScanFailed(int errorCode) {

                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showDevices(List<DeviceItem> devicesList) {
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
        scanManager = new FioTScanManager();
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
        private List<DeviceItem> devicesList;

        public class MyViewHolder extends RecyclerView.ViewHolder {
            public TextView name, mac;
            public TextView tvSpeed;
            public TextView tvSpeed2;
            public TextView tvSpeedtt;

            public Button btnConnect;
            public View view1;

            ArrayList<Long> timespan = new ArrayList<Long>();
            long averageTime2Notify = 0;

            public MyViewHolder(View view) {
                super(view);
                this.view1 = view;
                name = (TextView) view.findViewById(R.id.name);
                mac = (TextView) view.findViewById(R.id.mac);
                tvSpeed = (TextView) view.findViewById(R.id.speed);
                tvSpeed2 = (TextView) view.findViewById(R.id.speed2);
                tvSpeedtt = (TextView) view.findViewById(R.id.speedtt);

                btnConnect = (Button) view.findViewById(R.id.buttonConnect);
                btnConnect.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(final View view) {
                        final DeviceItem deviceItem = devicesList.get((int) view1.getTag());
                        if (deviceItem != null) {
                            if (deviceItem.connected) {
                                if (deviceItem.fioTManager != null) {
                                    deviceItem.fioTManager.end();
                                    btnConnect.setText("connect");
                                    deviceItem.connected = false;
                                    return;
                                }
                            }
                        }

                        btnConnect.setEnabled(false);
                        btnConnect.setBackgroundColor(Color.GRAY);
                        Log.i(TAG, "onClick: " + (int) view1.getTag());
                        stopScan();

                        ArrayList<FioTBluetoothService> services = new ArrayList<FioTBluetoothService>();
                        ArrayList<FioTBluetoothCharacteristic> characteristics2 = new ArrayList<FioTBluetoothCharacteristic>();
                        characteristics2.add(new FioTBluetoothCharacteristic(RX_UUID,
                                CharacteristicProperty.PROPERTY_WRITE));
                        characteristics2.add(new FioTBluetoothCharacteristic(TX_UUID,
                                CharacteristicProperty.PROPERTY_NOTIFY));
                        services.add(new FioTBluetoothService(SERVICE_UUID, characteristics2));

                        FioTManager manager = new FioTManager(MainActivity.this,
                                deviceItem.fioTBluetoothDevice,
                                services);
                        deviceItem.fioTManager = manager;
                        manager.connect();

                        manager.setDataListener(new FioTManager.FioTManagerDataListener() {
                            @Override
                            public void onNotify(FioTBluetoothCharacteristic characteristic) {
                                final long currentTime = System.currentTimeMillis();

                                try {
                                    if (rxCount == 0) {
                                        startEpoch = currentTime;
                                    }
                                    if (rxCountTotal == 0) {
                                        startEpochTotal = currentTime;
                                    }
                                    if (index != 0) {
                                        numberLostPacket += calcNumberLostPacket(index, characteristic.getCharacteristic().getValue());

                                        if (numberLostPacket > 0) {
                                            //Log.i(TAG, "c = " + index + ", new lost packet: " + ByteUtils.toHexString(characteristic.getCharacteristic().getValue()));
                                        }
                                    }

                                    index = characteristic.getCharacteristic().getValue()[0] & 0xff;
                                    rxCount += characteristic.getCharacteristic().getValue().length;
                                    rxCountTotal += characteristic.getCharacteristic().getValue().length;
                                    //Log.d(TAG,""+(currentTime - averageTime2Notify));

                                    if (rxCount != characteristic.getCharacteristic().getValue().length) {
                                        //Log.d(TAG, System.currentTimeMillis() + " " + startEpoch);
                                        final double seconds = (currentTime - startEpoch) / 1000.0;
                                        final double secondsTotal = (currentTime - startEpochTotal) / 1000.0;

                                        //Log.d(TAG, "count = " + rxCount + ", second = " + seconds + ", speed = " + (rxCount / (seconds) / 1024) + " bytes/s");
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                tvSpeed.setText((String.format("%.2f", rxCount*8 / (seconds) / 1024)) + " kb/s, lost: " + numberLostPacket);
                                                tvSpeedtt.setText((String.format("%.2f", rxCountTotal*8 / (secondsTotal) / 1024)) + " kb/s, lost: " + numberLostPacket);
                                                if(currentTime - startEpoch >= 1000){
                                                    tvSpeed2.setText((String.format("%.2f", rxCount*8 / (seconds) / 1024)) + " kb/s, lost: " + numberLostPacket);
                                                    Log.d(TAG,rxCount+"");
                                                    rxCount = 0;
                                                    startEpoch = currentTime;
                                                }
                                            }
                                        });
                                    }

                                    //Log.i(TAG, "onNofify: " + ByteUtils.toHexString(characteristic.getCharacteristic().getValue()));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }


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

                        manager.setConnectionListener(new FioTManager.FioTManagerConnectionListener() {

                            @Override
                            public void onConnectFail(int error) {
                                Log.i(TAG, "onConnectFail: ");
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        btnConnect.setEnabled(true);
                                        btnConnect.setBackgroundColor(getResources().getColor(R.color.btnEble));
                                    }
                                });
                            }

                            @Override
                            public void onConnected() {
                                Log.i(TAG, "onConnected: ");
                                rxCount = 0;
                                numberLostPacket = 0;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        btnConnect.setEnabled(true);
                                        btnConnect.setText("disconnect");
                                        btnConnect.setBackgroundColor(getResources().getColor(R.color.btnEble));
                                    }
                                });

                                deviceItem.connected = true;
                            }

                            @Override
                            public void onDisconnected(FioTManager manager) {
                                Log.i(TAG, "onDisconnected: ");
                                deviceItem.connected = false;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        btnConnect.setText("connect");
//                                        btnConnect.setBackgroundColor(getResources().getColor(R.color.btnEble));
                                    }
                                });

                            }


                        });

                    }
                });

            }
        }

        public DevicesAdapter(List<DeviceItem> devicesList) {
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
            FioTBluetoothDevice device = devicesList.get(position).fioTBluetoothDevice;
            holder.name.setText(device.getBluetoothDevice().getName());
            holder.mac.setText(device.getBluetoothDevice().getAddress());
            holder.view1.setTag(position);
        }

        @Override
        public int getItemCount() {
            Log.i(TAG, "getItemCount: " + devicesList.size());
            return devicesList.size();
        }

        public int calcNumberLostPacket(int currentIndex, byte[] packet) {
            if (packet.length == 0) {
                return 0;
            }

            int numberOfLoss;
            int newIndex = (packet[0] & 0xff);

            if (newIndex < currentIndex) {
                numberOfLoss = newIndex - currentIndex + 255;
            } else if (newIndex > currentIndex) {
                numberOfLoss = newIndex - currentIndex - 1;
            } else {
                numberOfLoss = 0;
            }

            return numberOfLoss;
        }

    }
}
