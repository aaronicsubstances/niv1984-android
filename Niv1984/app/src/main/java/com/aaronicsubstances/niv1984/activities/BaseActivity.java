package com.aaronicsubstances.niv1984.activities;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AppCompatActivity;

import com.aaronicsubstances.niv1984.fragments.AppDialogFragment;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by aaron on 5/18/17.
 */

public abstract class BaseActivity extends AppCompatActivity implements
        AppDialogFragment.NoticeDialogListener  {
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
