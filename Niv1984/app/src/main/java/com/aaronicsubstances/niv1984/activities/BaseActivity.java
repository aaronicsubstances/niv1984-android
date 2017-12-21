package com.aaronicsubstances.niv1984.activities;

import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import com.aaronicsubstances.niv1984.fragments.AppDialogFragment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by aaron on 5/18/17.
 */

public abstract class BaseActivity extends AppCompatActivity implements
        AppDialogFragment.NoticeDialogListener  {
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseActivity.class);

    private Map<DialogFragment, AppDialogFragment.NoticeDialogListener> mDialogActionMap = new HashMap<>();

    public void showAppDialog(DialogFragment dialogFragment, AppDialogFragment.NoticeDialogListener
            listener) {
        mDialogActionMap.put(dialogFragment, listener);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(dialogFragment, new Date().toString());
        ft.commitAllowingStateLoss();
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        AppDialogFragment.NoticeDialogListener listener = mDialogActionMap.remove(dialog);
        if (listener != null) {
            listener.onDialogPositiveClick(dialog);
        }
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        AppDialogFragment.NoticeDialogListener listener = mDialogActionMap.remove(dialog);
        if (listener != null) {
            listener.onDialogNegativeClick(dialog);
        }
    }
}
