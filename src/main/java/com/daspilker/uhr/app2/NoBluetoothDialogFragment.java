package com.daspilker.uhr.app2;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

public class NoBluetoothDialogFragment extends DialogFragment {
    private final DialogInterface.OnClickListener FINISH_ON_CLICK = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int id) {
            Activity activity = getActivity();
            if (activity != null) {
                activity.finish();
            }
        }
    };

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity()).setMessage(R.string.dialog_message).setCancelable(false)
                .setNeutralButton(R.string.dialog_button, FINISH_ON_CLICK).setTitle(R.string.dialog_title).create();
    }
}
