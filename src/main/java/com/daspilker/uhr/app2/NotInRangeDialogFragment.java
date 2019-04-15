package com.daspilker.uhr.app2;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

public class NotInRangeDialogFragment extends DialogFragment {
    private final DialogInterface.OnClickListener FINISH_ON_CLICK = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int id) {
            Activity activity = getActivity();
            if (activity != null) {
                activity.finish();
            }
        }
    };

    private final DialogInterface.OnClickListener CONNECT_ON_CLICK = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int id) {
            DasUhrActivity activity = (DasUhrActivity) getActivity();
            if (activity != null) {
                activity.connect();
            }
        }
    };

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity()).setMessage(R.string.dialog_not_in_range_message).setCancelable(false)
                .setNegativeButton(R.string.dialog_not_in_range_negative_button, FINISH_ON_CLICK)
                .setPositiveButton(R.string.dialog_not_in_range_positive_button, CONNECT_ON_CLICK)
                .setTitle(R.string.dialog_title).create();
    }
}
