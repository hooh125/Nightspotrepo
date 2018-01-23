package com.anedma.nightspot.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.ProgressBar;

import com.anedma.nightspot.R;

/**
 * Class created by Andrés Mata (andreseduardomp@gmail.com) on 21/01/2018.
 */

public class LoginDialogFragment extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        builder.setView(inflater.inflate(R.layout.login_dialog_fragment, null));
        return builder.create();
    }
}
