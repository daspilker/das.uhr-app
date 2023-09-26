package com.daspilker.uhr.app2;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import static android.provider.Settings.ACTION_BLUETOOTH_SETTINGS;

public class PairingDialogFragment extends DialogFragment {
    private final DialogInterface.OnClickListener FINISH_ON_CLICK = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int id) {
            Activity activity = getActivity();
            if (activity != null) {
                activity.finish();
            }
        }
    };

    private final DialogInterface.OnClickListener SETTINGS_ON_CLICK = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int id) {
            Activity activity = getActivity();
            if (activity != null) {
                activity.startActivity(new Intent(ACTION_BLUETOOTH_SETTINGS));
            }
        }
    };

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity()).setMessage(R.string.dialog_pairing_message).setCancelable(false)
                .setNegativeButton(R.string.dialog_pairing_negative_button, FINISH_ON_CLICK)
                .setPositiveButton(R.string.dialog_pairing_positive_button, SETTINGS_ON_CLICK)
                .setTitle(R.string.dialog_title).create();
    }
}
