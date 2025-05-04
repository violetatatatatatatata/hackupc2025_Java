package com.example.mirda2;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter bluetoothAdapter;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private ConnectedThread connectedThread;
    private ListView lvDevices;
    private Button btnScan, btnSend;
    private EditText etMessage;
    private TextView tvStatus;
    private BroadcastReceiver discoveryReceiver; // Added
    private ArrayAdapter<String> deviceAdapter;  // Added
    private List<String> deviceList = new ArrayList<>(); // Added
    private boolean isDiscovering = false;  // Tracks if we're currently scanning

    private static final int PERMISSION_REQUEST_CODE = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI
        lvDevices = findViewById(R.id.lvDevices);
        btnScan = findViewById(R.id.btnScan);
        btnSend = findViewById(R.id.btnSend);
        etMessage = findViewById(R.id.etMessage);
        tvStatus = findViewById(R.id.tvStatus);

        btnScan.setOnClickListener(v -> checkBluetoothAndStartDiscovery());
        btnSend.setOnClickListener(v -> sendMessage());

        // Bluetooth setup
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Register receiver
        registerDiscoveryReceiver();

        // Check if Bluetooth is enabled
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            checkPermissionsAndStartDiscovery();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                checkPermissionsAndStartDiscovery();
            } else {
                Toast.makeText(this, "Bluetooth must be enabled", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void checkBluetoothAndStartDiscovery() {
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            checkPermissionsAndStartDiscovery();
        }
    }

    private void checkPermissionsAndStartDiscovery() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    },
                    PERMISSION_REQUEST_CODE
            );
        } else {
            startBluetoothDiscovery();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startBluetoothDiscovery();
            } else {
                Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startBluetoothDiscovery() {
        try {
            // Cancel any existing discovery
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }

            // Clear old results
            deviceList.clear();
            updateDeviceList(); // Show paired devices

            // Start discovery
            isDiscovering = true;
            tvStatus.setText("Status: Scanning...");
            Toast.makeText(this, "Starting discovery...", Toast.LENGTH_SHORT).show();

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            if (!bluetoothAdapter.startDiscovery()) {
                tvStatus.setText("Status: Scan failed");
                Toast.makeText(this, "Discovery couldn't start", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            tvStatus.setText("Status: Error");
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
    // NEW: Populate ListView with paired devices
    private void updateDeviceList() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }


        // Initialize adapter if null
        if (deviceAdapter == null) {
            deviceAdapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_list_item_1,
                    deviceList
            );
            lvDevices.setAdapter(deviceAdapter);
        }

        // Clear and repopulate
        deviceList.clear();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
            deviceList.add(device.getName() + "\n" + device.getAddress());
        }
        deviceAdapter.notifyDataSetChanged();
    }

    private void registerDiscoveryReceiver() {
        discoveryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device == null) return;

                    // Use MainActivity.this instead of just 'this'
                    if (ActivityCompat.checkSelfPermission(MainActivity.this,
                            Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        // If permission missing, request it
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                                PERMISSION_REQUEST_CODE);
                        return;
                    }

                    String deviceName = (device.getName() != null ? device.getName() : "Unknown Device");
                    String deviceEntry = deviceName + "\n" + device.getAddress();

                    if (!deviceList.contains(deviceEntry)) {
                        deviceList.add(deviceEntry);
                        deviceAdapter.notifyDataSetChanged();
                        Log.d("BLUETOOTH", "Found device: " + deviceName);
                    }
                }
                else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    isDiscovering = false;
                    tvStatus.setText("Status: Scan completed");
                    Toast.makeText(MainActivity.this, "Scan completed", Toast.LENGTH_SHORT).show();
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(discoveryReceiver, filter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (discoveryReceiver != null) {
            unregisterReceiver(discoveryReceiver);
        }
    }
    private void sendMessage() {
        String message = etMessage.getText().toString();
        if (connectedThread != null && !message.isEmpty()) {
            connectedThread.write(message);
            etMessage.setText("");
        }
    }

    // Thread for managing Bluetooth connections
    private class ConnectedThread extends Thread {
        private final BluetoothSocket socket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public ConnectedThread(BluetoothSocket socket) {
            this.socket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { e.printStackTrace(); }
            inputStream = tmpIn;
            outputStream = tmpOut;
        }

        public void run() {
            // Check permissions
            if (ActivityCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            // Cancel discovery if active
            if (isDiscovering) {
                bluetoothAdapter.cancelDiscovery();
                isDiscovering = false;
            }

            try {
                // Connect
                socket.connect();
                connectedThread = new ConnectedThread(socket);
                connectedThread.start();

                runOnUiThread(() -> {
                    Class<Object> device = null;
                    tvStatus.setText("Connected to: " + device.getName());
                    Toast.makeText(MainActivity.this, "Connected!", Toast.LENGTH_SHORT).show();
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    tvStatus.setText("Connection failed");
                    Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }
        public void write(String message) {
            try {
                outputStream.write(message.getBytes());
            } catch (IOException e) { e.printStackTrace(); }
        }
    }

    // Thread for initiating Bluetooth connections
    private class ConnectThread extends Thread {
        private final BluetoothDevice device;
        private BluetoothSocket socket;

        public ConnectThread(BluetoothDevice device) {
            this.device = device;
            try {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                socket = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) { e.printStackTrace(); }
        }

        public void run() {
            // Cancel discovery
            if (ActivityCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                bluetoothAdapter.cancelDiscovery();
            }

            try {
                // Connect to device
                socket.connect();

                // Start communication thread
                runOnUiThread(() -> {
                    tvStatus.setText("Connected to: " + device.getName());
                    Toast.makeText(MainActivity.this, "Connected!", Toast.LENGTH_SHORT).show();
                });

                connectedThread = new ConnectedThread(socket);
                connectedThread.start();

            } catch (IOException e) {
                runOnUiThread(() -> {
                    tvStatus.setText("Connection failed");
                    Toast.makeText(MainActivity.this, "Failed to connect: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

                try {
                    socket.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}