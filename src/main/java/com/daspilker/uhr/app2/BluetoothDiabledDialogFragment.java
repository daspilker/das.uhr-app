package com.daspilker.uhr.app2;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import static android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE;

public class BluetoothDiabledDialogFragment extends DialogFragment {
    private static final int ACTIVITY_RESULT_CODE_ENABLE_BLUETOOTH = 0;

    private final DialogInterface.OnClickListener FINISH_ON_CLICK = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int id) {
            Activity activity = getActivity();
            if (activity != null) {
                activity.finish();
            }
        }
    };

    private final DialogInterface.OnClickListener ENABLE_BLUETOOTH_ON_CLICK = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int id) {
            Intent intent = new Intent(ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, ACTIVITY_RESULT_CODE_ENABLE_BLUETOOTH);
        }
    };

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity()).setMessage(R.string.dialog1_message).setCancelable(false)
                .setNegativeButton(R.string.dialog1_button, FINISH_ON_CLICK)
                .setPositiveButton(R.string.dialog_button, ENABLE_BLUETOOTH_ON_CLICK)
                .setTitle(R.string.dialog_title).create();
    }
}
