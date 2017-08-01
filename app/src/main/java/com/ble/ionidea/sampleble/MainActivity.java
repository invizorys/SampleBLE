package com.ble.ionidea.sampleble;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.ble.ionidea.sampleble.databinding.ActivityMainBinding;

import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TimerTask;
import java.util.UUID;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private ScannedDevicesAdapter adapter;
    private final String TAG = "MainActivity";
    private BluetoothDevice currentDevice;
    private BLEService bluetoothLeService;
    private BluetoothManager btManager;
    private BluetoothAdapter btAdapter;
    private BluetoothLeScanner btScanner;
    private boolean connState = false;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<>();
    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";
    private final static int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    private final String GATT_SERVICE_HTP = "0x00001809";
    private static final String FORMAT_STR = "%08x-0000-1000-8000-00805f9b34fb";
    public static final UUID UUID_MASK = UUID.fromString("0000ffff-0000-0000-0000-000000000000");

    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.d(TAG, "onServiceConnected()");
            bluetoothLeService = ((BLEService.LocalBinder) service).getService();
            if (!bluetoothLeService.initialize()) {
                Log.d(TAG, "Unable to initialize Bluetooth");
                binding.tvState.setText("Unable to initialize Bluetooth");
            }

            int connectionState = bluetoothLeService.getConnectionState();
            if (BLEService.STATE_DISCONNECTED == connectionState && currentDevice != null) {
                Log.d(TAG, "onServiceConnected(). state = DISCONNECTED");
                binding.tvState.setText("DISCONNECTED");
                boolean connStat = bluetoothLeService.connect(currentDevice.getAddress());
                if (connStat) {
                    Log.d(TAG, "connection successful.");
                }
            } else if (BLEService.STATE_CONNECTED == connectionState) {
                Log.d(TAG, "onServiceConnected(). state = CONNECTED");
                BluetoothDevice currentBluetoothDevice = bluetoothLeService.getCurrentBluetoothDevice();
                binding.tvState.setText("CONNECTED");
                adapter.updateConnectionState(currentBluetoothDevice, BleState.CONNECTED);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bluetoothLeService = null;
            Log.d(TAG, "onServiceDisconnected()");
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        adapter = new ScannedDevicesAdapter(new ScannedDevicesAdapter.Listener() {
            @Override
            public void onConnectClicked(BluetoothDevice device) {
                currentDevice = device;
//            stopScanning();
                if (!connState) {
                    adapter.updateConnectionState(device, BleState.CONNECTING);
                    connectToDevice();
                    connState = true;
                } else {
                    bluetoothLeService.disconnect();
                    connState = false;
                }
            }
        });
        binding.rvBluetoothDevices.setLayoutManager(new LinearLayoutManager(this));
        binding.rvBluetoothDevices.setAdapter(adapter);

        displayData("No data");

        binding.btnStartScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startScanning();
            }
        });

        binding.btnStopScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopScanning();
            }
        });
        binding.btnStopScan.setVisibility(View.INVISIBLE);

//        connectDevice = (Button) findViewById(R.id.ConnectButton);
//        connectDevice.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (!connState) {
//                    ConnectToDevice();
//                    connState = true;
//                } else {
//                    mBluetoothLeService.disconnect();
//                    connState = false;
//                }
//            }
//        });

        btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
        if (btAdapter != null) {
            btScanner = btAdapter.getBluetoothLeScanner();
        }


        if (btAdapter != null && !btAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

        // Make sure we have access coarse location enabled, if not, prompt the user to enable it
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect peripherals.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @RequiresApi(api = Build.VERSION_CODES.M)
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                PERMISSION_REQUEST_COARSE_LOCATION);
                    }
                });
                builder.show();
            }
        }

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothAdapterReceiver, filter);

        bindBluetoothService();

        binding.btnWrite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bluetoothLeService.write("56");
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Your device doesn't support Bluetooth Low Energy technology", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(bluetoothAdapterReceiver);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    // Device scan callback.
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            adapter.addBluetoothDevice(result.getDevice());
            stopScanning();
        }

        @Override
        public void onScanFailed(int errorcode) {
            binding.tvState.setText("Error code " + errorcode);
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    System.out.println("coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialogInterface) {

                        }
                    });
                    builder.show();
                }
                return;
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void startScanning() {
        binding.tvState.setText("Started scanning");
        Log.d(TAG, "Started scanning");
        adapter.clear();
        binding.btnStartScan.setVisibility(View.INVISIBLE);
        binding.btnStopScan.setVisibility(View.VISIBLE);
        if (btScanner != null) {
            AsyncTask.execute(new TimerTask() {
                @Override
                public void run() {
                    btScanner.startScan(leScanCallback);
                }
            });
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void stopScanning() {
        Log.d(TAG, "Stopped scanning");
        binding.tvState.setText("Stopped scanning");
        binding.btnStartScan.setVisibility(View.VISIBLE);
        binding.btnStopScan.setVisibility(View.INVISIBLE);
        if (btScanner != null) {
            AsyncTask.execute(new TimerTask() {
                @Override
                public void run() {
                    btScanner.stopScan(leScanCallback);
                }
            });
        }
    }

    public void connectToDevice() {
        if (bluetoothLeService != null) {
            bluetoothLeService.connect(currentDevice.getAddress());
        } else {
            bindBluetoothService();
        }
    }

    private void bindBluetoothService() {
        Log.d(TAG, "bindBluetoothService()");
        if (bluetoothLeService == null) {
            Log.d(TAG, "connecting to service");
            Intent gattServiceIntent = new Intent(this, BLEService.class);
            bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    private void writeData(String data) {
        //56, 101
        bluetoothLeService.write(data);
    }

    private void displayData(String data) {
        if (data != null) {
            binding.tvData.setText(data);
        }
    }

    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<>();
        mGattCharacteristics = new ArrayList<>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<>();
            uuid = gattService.getUuid().toString();
            String UUID_desc = uuid.substring(4, 8);
            if (UUID_desc.equals("1809")) {
                currentServiceData.put(
                        LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
                currentServiceData.put(LIST_UUID, uuid);
                gattServiceData.add(currentServiceData);

                ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                        new ArrayList<>();
                List<BluetoothGattCharacteristic> gattCharacteristics =
                        gattService.getCharacteristics();
                ArrayList<BluetoothGattCharacteristic> charas;
                charas = new ArrayList<>();

                // Loops through available Characteristics.
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    charas.add(gattCharacteristic);
                    HashMap<String, String> currentCharaData = new HashMap<>();
                    uuid = gattCharacteristic.getUuid().toString();
                    currentCharaData.put(
                            LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                    currentCharaData.put(LIST_UUID, uuid);

                    //mBluetoothLeService.readCharacteristic(gattCharacteristic);
                    bluetoothLeService.setCharacteristicNotification(gattCharacteristic, true);

                    gattCharacteristicGroupData.add(currentCharaData);
                }
                mGattCharacteristics.add(charas);
                gattCharacteristicData.add(gattCharacteristicGroupData);
            }
        }
    }

    private final BroadcastReceiver bluetoothAdapterReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        binding.tvState.setText("STATE_OFF");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        binding.tvState.setText("STATE_TURNING_OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        binding.tvState.setText("STATE_ON");
                        if (currentDevice != null) {
                            if (bluetoothLeService != null) {
                                bluetoothLeService.connect(currentDevice.getAddress());
                            } else {
                                Log.d(TAG, "connecting to service");
                                Intent gattServiceIntent = new Intent(MainActivity.this, BLEService.class);
                                MainActivity.this.bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
                            }
                        }
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        binding.tvState.setText("STATE_TURNING_ON");
                        break;
                }
            }
        }
    };

    @Subscribe
    public void onEvent(BleStateEvent event) {
        if (event.getState().equals(BleState.CONNECTED)) {
            adapter.updateConnectionState(currentDevice, BleState.CONNECTED);
        } else if (event.getState().equals(BleState.DISCONNECTED)) {
            displayData("No data");
            adapter.updateConnectionState(currentDevice, BleState.DISCONNECTED);
        } else if (event.getState().equals(BleState.DISCOVERED)) {
            displayGattServices(bluetoothLeService.getSupportedGattServices());
            displayData("Ready");
        } else if (event.getState().equals(BleState.DATA_AVAILABLE)) {
            String data = event.getData();
            displayData(data);
        }
    }
}
