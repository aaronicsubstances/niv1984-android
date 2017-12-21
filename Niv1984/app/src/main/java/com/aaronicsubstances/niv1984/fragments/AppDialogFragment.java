package com.aaronicsubstances.niv1984.fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

/**
 * Created by aaron on 4/22/17.
 */
public class AppDialogFragment extends DialogFragment {

    private static final String KEY_MESSAGE = "arg.message";
    private static final String KEY_OK = "arg.ok";
    private static final String KEY_CANCEL = "arg.cancel";
    private String mCancel;

    /* The activity that creates an instance of this dialog fragment must
         * implement this interface in order to receive event callbacks.
         * Each method passes the DialogFragment in case the host needs to query it.
         */
    public interface NoticeDialogListener {
        public void onDialogPositiveClick(DialogFragment dialog);
        public void onDialogNegativeClick(DialogFragment dialog);
    }

    // Use this instance of the interface to deliver action events
    NoticeDialogListener mListener;

    public static AppDialogFragment newInstance(String message, String ok, String cancel) {
        AppDialogFragment f = new AppDialogFragment();
        Bundle args = new Bundle();
        args.putString(KEY_MESSAGE, message);
        args.putString(KEY_OK, ok);
        args.putString(KEY_CANCEL, cancel);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        if (mListener != null) {
            if (mCancel == null) {
                mListener.onDialogPositiveClick(this);
            }
            else {
                mListener.onDialogNegativeClick(this);
            }
        }
    }

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (NoticeDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String message = getArguments().getString(KEY_MESSAGE);
        String ok = getArguments().getString(KEY_OK);
        mCancel = getArguments().getString(KEY_CANCEL);
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        if (message != null) {
            builder.setMessage(message);
        }
        if (ok != null) {
            builder.setPositiveButton(ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // FIRE ZE MISSILES!
                    if (mListener != null) {
                        mListener.onDialogPositiveClick(AppDialogFragment.this);
                    }
                }
            });
        }
        if (mCancel != null) {
            builder.setNegativeButton(mCancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User cancelled the dialog
                    if (mListener != null) {
                        mListener.onDialogNegativeClick(AppDialogFragment.this);
                    }
                }
            });
        }
        // Create the AlertDialog object and return it
        Dialog dialog = builder.create();
        return dialog;
    }
}