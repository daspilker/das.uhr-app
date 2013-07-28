/*
   Copyright 2013 Daniel A. Spilker

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package com.daspilker.uhr.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import static android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_FINISHED;
import static android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE;
import static android.bluetooth.BluetoothDevice.ACTION_FOUND;
import static android.bluetooth.BluetoothDevice.BOND_BONDED;
import static android.bluetooth.BluetoothDevice.EXTRA_DEVICE;
import static android.provider.Settings.ACTION_BLUETOOTH_SETTINGS;

public class DasUhrActivity extends Activity {
    private static final int DIALOG_NO_BLUETOOTH = 0;
    private static final int DIALOG_BLUETOOTH_DISABLED = 1;
    private static final int DIALOG_NOT_IN_RANGE = 2;
    private static final int DIALOG_PAIRING = 3;
    private static final int ACTIVITY_RESULT_CODE_ENABLE_BLUETOOTH = 0;
    private static final UUID UUID_SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final Charset UTF8 = Charset.forName("UTF-8");
    public static final String DEVICE_NAME = "DAS.UHR";

    private final DialogInterface.OnClickListener FINISH_ON_CLICK = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int id) {
            DasUhrActivity.this.finish();
        }
    };

    private final DialogInterface.OnClickListener ENABLE_BLUETOOTH_ON_CLICK = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int id) {
            enableBluetooth();
        }
    };

    private final DialogInterface.OnClickListener CONNECT_ON_CLICK = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int id) {
            connect();
        }
    };

    private final DialogInterface.OnClickListener SETTINGS_ON_CLICK = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int id) {
            showBluetoothSettings();
        }
    };

    private final SeekBar.OnSeekBarChangeListener BRIGHTNESS_SEEK_BAR_CHANGE = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                new SetBrightnessTask().execute(progress);
            }
        }
    };

    private final View.OnClickListener SYNC_TIME_BUTTON_ON_CLICK = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            new SetTimeTask().execute();
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(EXTRA_DEVICE);
                if (device != null && DEVICE_NAME.equals(device.getName())) {
                    bluetoothDeviceDiscovered = true;
                    bluetoothAdapter.cancelDiscovery();
                }
            } else if (ACTION_DISCOVERY_FINISHED.equals(action)) {
                discoveryFinished = true;
                if (bluetoothDeviceDiscovered) {
                    showDialog(DIALOG_PAIRING);
                } else {
                    showDialog(DIALOG_NOT_IN_RANGE);
                }
            }
        }
    };
    private final IntentFilter ACTION_FOUND_FILTER = new IntentFilter(ACTION_FOUND);
    private final IntentFilter ACTION_DISCOVERY_FINISHED_FILTER = new IntentFilter(ACTION_DISCOVERY_FINISHED);

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket socket;
    private OutputStreamWriter writer;
    private BufferedReader reader;
    private boolean bluetoothDeviceDiscovered;
    private ProgressDialog progressDialog;
    private boolean discoveryFinished;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("UHR", "onCreate");
        setContentView(R.layout.main);

        SeekBar seekBarBrightness = (SeekBar) findViewById(R.id.seekBarBrightness);
        seekBarBrightness.setOnSeekBarChangeListener(BRIGHTNESS_SEEK_BAR_CHANGE);

        Button buttonSyncTime = (Button) findViewById(R.id.buttonSyncTime);
        buttonSyncTime.setOnClickListener(SYNC_TIME_BUTTON_ON_CLICK);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            showDialog(DIALOG_NO_BLUETOOTH);
        }

        registerReceiver(mReceiver, ACTION_FOUND_FILTER);
        registerReceiver(mReceiver, ACTION_DISCOVERY_FINISHED_FILTER);
    }

    @Override
    protected void onPause() {
        dispose();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!bluetoothAdapter.isEnabled()) {
            enableBluetooth();
        } else {
            connect();
        }
    }

    @Override
    protected void onDestroy() {
        registerReceiver(mReceiver, ACTION_DISCOVERY_FINISHED_FILTER);
        registerReceiver(mReceiver, ACTION_FOUND_FILTER);
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ACTIVITY_RESULT_CODE_ENABLE_BLUETOOTH) {
            if (resultCode == Activity.RESULT_CANCELED) {
                showDialog(DIALOG_BLUETOOTH_DISABLED);
            } else {
                connect();
            }
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == DIALOG_NO_BLUETOOTH) {
            return new AlertDialog.Builder(this).setMessage(R.string.dialog_message).setCancelable(false)
                    .setNeutralButton(R.string.dialog_button, FINISH_ON_CLICK).setTitle(R.string.dialog_title).create();
        } else if (id == DIALOG_BLUETOOTH_DISABLED) {
            return new AlertDialog.Builder(this).setMessage(R.string.dialog1_message).setCancelable(false)
                    .setNegativeButton(R.string.dialog1_button, FINISH_ON_CLICK)
                    .setPositiveButton(R.string.dialog_button, ENABLE_BLUETOOTH_ON_CLICK)
                    .setTitle(R.string.dialog_title).create();
        } else if (id == DIALOG_NOT_IN_RANGE) {
            return new AlertDialog.Builder(this).setMessage(R.string.dialog_not_in_range_message).setCancelable(false)
                    .setNegativeButton(R.string.dialog_not_in_range_negative_button, FINISH_ON_CLICK)
                    .setPositiveButton(R.string.dialog_not_in_range_positive_button, CONNECT_ON_CLICK)
                    .setTitle(R.string.dialog_title).create();
        } else if (id == DIALOG_PAIRING) {
            return new AlertDialog.Builder(this).setMessage(R.string.dialog_pairing_message).setCancelable(false)
                    .setNegativeButton(R.string.dialog_pairing_negative_button, FINISH_ON_CLICK)
                    .setPositiveButton(R.string.dialog_pairing_positive_button, SETTINGS_ON_CLICK)
                    .setTitle(R.string.dialog_title).create();
        }
        return null;
    }

    private void enableBluetooth() {
        Intent intent = new Intent(ACTION_REQUEST_ENABLE);
        startActivityForResult(intent, ACTIVITY_RESULT_CODE_ENABLE_BLUETOOTH);
    }

    private void showBluetoothSettings() {
        startActivity(new Intent(ACTION_BLUETOOTH_SETTINGS));
    }

    private void connect() {
        new ConnectTask().execute();
    }

    private void dispose() {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                // ignore
            }
            writer = null;
        }
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                // ignore
            }
            reader = null;
        }
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                // ignore
            }
            socket = null;
        }
    }

    private class ConnectTask extends AsyncTask<Void, Void, Integer> {
        @Override
        protected Integer doInBackground(Void... voids) {
            dispose();

            BluetoothDevice device = null;
            Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
            if (bondedDevices != null) {
                for (BluetoothDevice bondedDevice : bondedDevices) {
                    if (DEVICE_NAME.equals(bondedDevice.getName())) {
                        device = bondedDevice;
                        break;
                    }
                }
            }
            if (device == null && !discoveryFinished) {
                bluetoothDeviceDiscovered = false;
                bluetoothAdapter.startDiscovery();
                return -2;
            }
            if (device != null && device.getBondState() != BOND_BONDED) {
                return DIALOG_PAIRING;
            }
            if (device != null) {
                try {
                    socket = device.createRfcommSocketToServiceRecord(UUID_SPP);
                    socket.connect();
                    writer = new OutputStreamWriter(socket.getOutputStream(), UTF8);
                    reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), UTF8));
                    return -1;
                } catch (IOException e) {
                    dispose();
                }
            }
            return DIALOG_NOT_IN_RANGE;
        }

        @Override
        protected void onPreExecute() {
            if (progressDialog == null) {
                progressDialog = ProgressDialog.show(DasUhrActivity.this, "", getString(R.string.connecting), true);
            }
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (result != -2) {
                progressDialog.dismiss();
                if (result == -1) {
                    new GetFirmwareVersionTask().execute();
                } else {
                    showDialog(result);
                }
            }
        }
    }

    private class GetFirmwareVersionTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            try {
                writer.write("v\r\n");
                writer.flush();
                return reader.readLine();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            TextView firmwareVersionTextView = (TextView) findViewById(R.id.textViewFirmwareVersion);
            if (result == null) {
                firmwareVersionTextView.setText(R.string.firmware_version_unknown);
            } else {
                firmwareVersionTextView.setText(result.substring(1));
            }
            new GetBrightnessTask().execute();
        }
    }

    private class GetBrightnessTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            try {
                writer.write("b\r\n");
                writer.flush();
                return reader.readLine();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            SeekBar seekBarBrightness = (SeekBar) findViewById(R.id.seekBarBrightness);
            if (result != null) {
                seekBarBrightness.setProgress(Integer.valueOf(result.substring(1), 16));
            }
        }
    }

    private class SetBrightnessTask extends AsyncTask<Integer, Void, String> {
        @Override
        protected String doInBackground(Integer... params) {
            try {
                writer.write(String.format(Locale.US, "b%02X\r\n", params[0]));
                writer.flush();
                return reader.readLine();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    private class SetTimeTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            try {
                writer.write(String.format("t%s\r\n", DateFormat.format("yyMMddkkmmss", System.currentTimeMillis())));
                writer.flush();
                return reader.readLine();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
    }
}
