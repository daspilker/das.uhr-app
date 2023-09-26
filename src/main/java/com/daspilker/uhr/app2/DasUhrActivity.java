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
package com.daspilker.uhr.app2;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
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

public class DasUhrActivity extends FragmentActivity {
    private static final int DIALOG_NOT_IN_RANGE = 2;
    private static final int DIALOG_PAIRING = 3;
    private static final int ACTIVITY_RESULT_CODE_ENABLE_BLUETOOTH = 0;
    private static final UUID UUID_SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final String DEVICE_NAME = "DAS.UHR";



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
                new SetBrightnessTask(writer, reader).execute(progress);
            }
        }
    };

    private final View.OnClickListener SYNC_TIME_BUTTON_ON_CLICK = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            new SetTimeTask(writer, reader).execute();
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
                    DialogFragment dialogFragment = new PairingDialogFragment();
                    dialogFragment.show(getSupportFragmentManager(), "PairingDialogFragment");
                } else {
                    DialogFragment dialogFragment = new NotInRangeDialogFragment();
                    dialogFragment.show(getSupportFragmentManager(), "NotInRangeDialogFragment");
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
    private DialogFragment progressDialog;
    private boolean discoveryFinished;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("UHR", "onCreate");
        setContentView(R.layout.main);

        SeekBar seekBarBrightness = findViewById(R.id.seekBarBrightness);
        seekBarBrightness.setOnSeekBarChangeListener(BRIGHTNESS_SEEK_BAR_CHANGE);

        Button buttonSyncTime = findViewById(R.id.buttonSyncTime);
        buttonSyncTime.setOnClickListener(SYNC_TIME_BUTTON_ON_CLICK);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            DialogFragment noBluetoothDialogFragment = new NoBluetoothDialogFragment();
            noBluetoothDialogFragment.show(getSupportFragmentManager(), "NoBluetoothDialogFragment");
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
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ACTIVITY_RESULT_CODE_ENABLE_BLUETOOTH) {
            if (resultCode == Activity.RESULT_CANCELED) {
                DialogFragment dialogFragment = new BluetoothDiabledDialogFragment();
                dialogFragment.show(getSupportFragmentManager(), "BluetoothDisabledDialogFragment");
            } else {
                connect();
            }
        }
    }

    private void enableBluetooth() {
        Intent intent = new Intent(ACTION_REQUEST_ENABLE);
        startActivityForResult(intent, ACTIVITY_RESULT_CODE_ENABLE_BLUETOOTH);
    }

    void connect() {
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
                progressDialog = new ProgressDialogFragment();
                progressDialog.show(getSupportFragmentManager(), "ProgressDialogFragment");
            }
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (result != -2) {
                progressDialog.dismiss();
                if (result == -1) {
                    new GetFirmwareVersionTask(writer, reader).execute();
                } else if (result == DIALOG_PAIRING) {
                    DialogFragment dialogFragment = new PairingDialogFragment();
                    dialogFragment.show(getSupportFragmentManager(), "PairingDialogFragment");
                } else if (result == DIALOG_NOT_IN_RANGE) {
                    DialogFragment dialogFragment = new NotInRangeDialogFragment();
                    dialogFragment.show(getSupportFragmentManager(), "NotInRangeDialogFragment");
                }
            }
        }
    }

    private class GetFirmwareVersionTask extends AsyncTask<Void, Void, String> {
        private final OutputStreamWriter writer;
        private final BufferedReader reader;

        GetFirmwareVersionTask(OutputStreamWriter writer, BufferedReader reader) {
            this.writer = writer;
            this.reader = reader;
        }

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
            TextView firmwareVersionTextView = findViewById(R.id.textViewFirmwareVersion);
            if (result == null) {
                firmwareVersionTextView.setText(R.string.firmware_version_unknown);
            } else {
                firmwareVersionTextView.setText(result.substring(1));
            }
            new GetBrightnessTask(writer, reader).execute();
        }
    }

    private class GetBrightnessTask extends AsyncTask<Void, Void, String> {
        private final OutputStreamWriter writer;
        private final BufferedReader reader;

        GetBrightnessTask(OutputStreamWriter writer, BufferedReader reader) {
            this.writer = writer;
            this.reader = reader;
        }

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
            SeekBar seekBarBrightness = findViewById(R.id.seekBarBrightness);
            if (result != null) {
                seekBarBrightness.setProgress(Integer.valueOf(result.substring(1), 16));
            }
        }
    }

    private static class SetBrightnessTask extends AsyncTask<Integer, Void, String> {
        private final OutputStreamWriter writer;
        private final BufferedReader reader;

        SetBrightnessTask(OutputStreamWriter writer, BufferedReader reader) {
            this.writer = writer;
            this.reader = reader;
        }

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

    private static class SetTimeTask extends AsyncTask<Void, Void, String> {
        private final OutputStreamWriter writer;
        private final BufferedReader reader;

        private SetTimeTask(OutputStreamWriter writer, BufferedReader reader) {
            this.writer = writer;
            this.reader = reader;
        }

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
