package com.daspilker.uhr.app2;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

public class ProgressDialogFragment extends DialogFragment {
    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setMessage(getString(R.string.connecting))
                .setCancelable(false)
                .setTitle(R.string.dialog_title).create();
    }
}
