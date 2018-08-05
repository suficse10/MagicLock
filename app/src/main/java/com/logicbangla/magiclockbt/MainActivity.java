package com.logicbangla.magiclockbt;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.logicbangla.magiclockbt.database.DatabaseHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    // Debugging for LOGCAT
    private static final String TAG = "MainActivity";

    ToggleButton startBTN;
    Button btnBatt;
    TextView textView1;
    CoordinatorLayout coordinatorLayout;
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
    private static String getpassword;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startBTN = findViewById(R.id.tbStart);
        btnBatt = findViewById(R.id.buttonBatt);
        textView1 = findViewById(R.id.textConnect);
        textView1.setText("Not Connected");
        textView1.setTextColor(Color.parseColor("#FF0000"));
        coordinatorLayout = findViewById(R.id.coord_layout);

        dbhelper = new DatabaseHelper(MainActivity.this);

        btAdapter = BluetoothAdapter.getDefaultAdapter();       // get Bluetooth adapter
        checkBTState();

        getpassword = dbhelper.getPassword(1);
        if (getpassword != null) {
            getpassword = dbhelper.getPassword(1);
            Log.i("Get Password", getpassword);
        } else {
            dbhelper.insertPassword();
            Log.i("Get Password", "not found");
        }

        bluetoothIn = new Handler() {
            public void handleMessage(Message msg) {
                if (msg.what == handlerState) {
                    String readMessage = (String) msg.obj;
                    recDataString.append(readMessage);                                      //keep appending to string until ~
                    int endOfLineIndex = recDataString.indexOf("!");                    // determine the end-of-line
                    if (endOfLineIndex > 0) {                                          // make sure there data before ~
                        String dataInPrint = recDataString.substring(0, endOfLineIndex);    // extract string
                        //textViewVolt.setText(dataInPrint);

                        //Snackbar
                        /*final Snackbar snackbar = Snackbar.make(coordinatorLayout, "Battery Voltage: " + dataInPrint, Snackbar.LENGTH_INDEFINITE);
                        snackbar.setAction("OK", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                snackbar.dismiss();
                            }
                        });
                        snackbar.show();*/

                        //Dialog Box
                        AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                        dialog.setTitle("Battery Status");
                        dialog.setMessage("Battery Voltage: " + dataInPrint);
                        dialog.show();
                        recDataString.delete(0, recDataString.length());                    //clear all string data
                        dataInPrint = "";
                    }
                }
            }
        };

    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {

        return device.createRfcommSocketToServiceRecord(BTMODULEUUID);
        //creates secure outgoing connecetion with BT device using UUID
    }

    @Override
    public void onResume() {
        super.onResume();

        //Get MAC address from DeviceListActivity via intent
        Intent intent = getIntent();
        try {
            /*Bundle bundle = getIntent().getExtras();
            if (bundle != null) {
                address = bundle.getString("id");
                Log.i("MAC Address: ", address);
            }*/

            //Get the MAC address from the DeviceListActivity via EXTRA
            if (getIntent().hasExtra("id")) {
                address = intent.getStringExtra("id");
                Log.i("MAC Address: ", address);
            }

            //create device and set the MAC address
            BluetoothDevice device = btAdapter.getRemoteDevice(address);

            try {
                btSocket = createBluetoothSocket(device);
               // Toast.makeText(getBaseContext(), "Socket is created", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
            }

            // Establish the Bluetooth socket connection.
            try {
                btSocket.connect();
                Toast.makeText(getBaseContext(), "Connection established", Toast.LENGTH_SHORT).show();
                textView1.setText("Connected");
                textView1.setTextColor(Color.parseColor("#00FF00"));
            } catch (IOException e) {
                try {
                    btSocket.close();
                    Toast.makeText(getBaseContext(), "Connection Failure", Toast.LENGTH_LONG).show();
                    textView1.setText("Not Connected");
                    textView1.setTextColor(Color.parseColor("#FF0000"));
                } catch (IOException e2) {
                    //insert code to deal with this
                }
            }
            mConnectedThread = new ConnectedThread(btSocket);
            mConnectedThread.start();

            if (btSocket.isConnected()) {
                startBTN.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            try {
                                mConnectedThread.write(getpassword + "1");    // Send "1" via Bluetooth
                                Log.i("Data", getpassword + "1");
                                Toast.makeText(MainActivity.this, "Start Enabled", Toast.LENGTH_LONG).show();
                            } catch (NullPointerException e) {
                                Toast.makeText(MainActivity.this, "Error!!!", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            try {
                                mConnectedThread.write(getpassword + "0");    // Send "0" via Bluetooth
                                Log.i("Data", getpassword + "0");
                                Toast.makeText(MainActivity.this, "Start Disabled", Toast.LENGTH_LONG).show();
                            } catch (NullPointerException e) {
                                Toast.makeText(MainActivity.this, "Error!!!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
            } else {
                Toast.makeText(MainActivity.this, "Connection Error!!!", Toast.LENGTH_SHORT).show();
            }

            if (getIntent().hasExtra("pass")) {
                String password = getIntent().getStringExtra("pass");
                mConnectedThread.write("pass=" + password);    // Send "password" via Bluetooth
                Log.i("Password: ", password);
            }

            //I send a character when resuming.beginning transmission to check device is connected
            //If it is not an exception will be thrown in the write method and finish() will be called
            //mConnectedThread.write("x");
        } catch (Exception e) {
            Log.i(TAG, "Error: " + e.getMessage());
            Toast.makeText(getBaseContext(), "Address is not found" + "\n" + "Connect to a paired device", Toast.LENGTH_LONG).show();
            textView1.setText("Not Connected");
            textView1.setTextColor(Color.parseColor("#FF0000"));
        }
    }

    @Override
    public void onPause() {
        super.onPause();
       // Toast.makeText(getBaseContext(), "MainActivity is paused", Toast.LENGTH_SHORT).show();
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

    public void btnConnect(View view) {
        Intent intent = new Intent(MainActivity.this, DeviceListActivity.class);
        startActivity(intent);
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

    public void setPassword(View view) {
        Intent intent = new Intent(MainActivity.this, PasswordActivity.class);
        startActivity(intent);
    }

    public void goto_status(View view) {
        Intent intent = new Intent(MainActivity.this, StatusActivity.class);
        //intent.putExtra("address", address);
        startActivity(intent);
    }

    public void btnBV(View view) {
        try {
            mConnectedThread.write(getpassword + "5");    // Send "status" via Bluetooth
            Log.i("Data", getpassword + "5");
        } catch (NullPointerException e) {
            Toast.makeText(MainActivity.this, "Connection Error!!!", Toast.LENGTH_SHORT).show();
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
