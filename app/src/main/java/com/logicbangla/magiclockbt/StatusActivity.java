package com.logicbangla.magiclockbt;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.logicbangla.magiclockbt.database.DatabaseHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class StatusActivity extends AppCompatActivity {
    private static final String TAG = "StatusActivity";

    TextView textViewVolt;
    Button btnRefresh;
    DatabaseHelper dbhelper;
    Handler bluetoothIn;
    final int handlerState = 0;                        //used to identify handler message
    private StringBuilder recDataString = new StringBuilder();

    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;

    private ConnectedThread mConnectedThread;

    // SPP UUID service - this should work for most devices
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // String for MAC address
    private static String address;
    private static String getPassword;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);

        textViewVolt = findViewById(R.id.textVolt);
        btnRefresh = findViewById(R.id.buttonRefresh);
        dbhelper = new DatabaseHelper(StatusActivity.this);

        btAdapter = BluetoothAdapter.getDefaultAdapter();       // get Bluetooth adapter
        checkBTState();

        bluetoothIn = new Handler() {
            public void handleMessage(Message msg) {
                if (msg.what == handlerState) {
                    String readMessage = (String) msg.obj;
                    recDataString.append(readMessage);                                      //keep appending to string until ~
                    int endOfLineIndex = recDataString.indexOf("!");                    // determine the end-of-line
                    if (endOfLineIndex > 0) {                                          // make sure there data before ~
                        String dataInPrint = recDataString.substring(0, endOfLineIndex);    // extract string
                        textViewVolt.setText(dataInPrint);
                        recDataString.delete(0, recDataString.length());                    //clear all string data
                        dataInPrint = "";
                    }
                }
            }
        };
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {

        //creates secure outgoing connecetion with BT device using UUID
        return device.createRfcommSocketToServiceRecord(BTMODULEUUID);
    }

    @Override
    public void onResume() {
        super.onResume();

        //Get MAC address from DeviceListActivity via intent
        Intent intent = getIntent();
        try {
            //Get the MAC address from the MainActivity via EXTRA
            if (getIntent().hasExtra("address")) {
                address = intent.getStringExtra("address");
                Log.i("MAC Address: ", address);
            }

            //create device and set the MAC address
            BluetoothDevice device = btAdapter.getRemoteDevice(address);

            try {
                btSocket = createBluetoothSocket(device);
                Toast.makeText(getBaseContext(), "Socket is created", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
            }

            // Establish the Bluetooth socket connection.
            try {
                btSocket.connect();
                Toast.makeText(getBaseContext(), "Connection established", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                try {
                    btSocket.close();
                } catch (IOException e2) {
                    //insert code to deal with this
                }
            }
            mConnectedThread = new StatusActivity.ConnectedThread(btSocket);
            mConnectedThread.start();

            getPassword = dbhelper.getPassword(1);
            /*if (btSocket.isConnected()) {
                btnRefresh.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            mConnectedThread.write(getPassword+"5");    // Send "status" via Bluetooth
                            Toast.makeText(StatusActivity.this, "Data Refreshed", Toast.LENGTH_LONG).show();
                        } catch (NullPointerException e) {
                            Toast.makeText(StatusActivity.this, "Error!!!", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            } else {
                Toast.makeText(StatusActivity.this, "Connection Error!!!", Toast.LENGTH_SHORT).show();
            }*/
            //I send a character when resuming.beginning transmission to check device is connected
            //If it is not an exception will be thrown in the write method and finish() will be called
            //mConnectedThread.write("x");
        } catch (Exception e) {
            Log.i(TAG, "Error: " + e.getMessage());
            Toast.makeText(getBaseContext(), "Address is not found" + "\n" + "Connect to a paired device", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Toast.makeText(getBaseContext(), "StatusActivity is paused", Toast.LENGTH_SHORT).show();
        if (address != null) {
            try {
                //Don't leave Bluetooth sockets open when leaving activity
                btSocket.close();
            } catch (IOException e2) {
                //insert code to deal with this
            }
        }
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(false);
    }

    //Checks that the Android device Bluetooth is available and prompts to be turned on if off
    private void checkBTState() {

        try {
            if (btAdapter == null) {
                Toast.makeText(getBaseContext(), "Device does not support bluetooth", Toast.LENGTH_LONG).show();
            } else {
                if (!btAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, 0);
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }

    }

    public void btnHome(View view) {
        Intent intent = new Intent(StatusActivity.this, MainActivity.class);
        startActivity(intent);
    }

    public void btnRefresh(View view) {
        try {
            mConnectedThread.write(getPassword+"5");    // Send "status" via Bluetooth
            Toast.makeText(StatusActivity.this, "Data Refreshed", Toast.LENGTH_LONG).show();
        } catch (NullPointerException e) {
            Toast.makeText(StatusActivity.this, "Connection Error!!!", Toast.LENGTH_SHORT).show();
        }
    }

    //create new class for connect thread
    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        //creation of the connect thread
        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                //Create I/O streams for connection
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            // Keep looping to listen for received messages
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);            //read bytes from input buffer
                    String readMessage = new String(buffer, 0, bytes);
                    // Send the obtained bytes to the UI Activity via handler
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        //write method
        public void write(String input) {
            byte[] msgBuffer = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(msgBuffer);                //write bytes over BT connection via outstream
            } catch (IOException e) {
                //if you cannot write, close the application
                Toast.makeText(getBaseContext(), "Connection Failure", Toast.LENGTH_LONG).show();
                //finish();

            }
        }
    }
}
