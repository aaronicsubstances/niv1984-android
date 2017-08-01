package com.aaronicsubstances.niv1984;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

/**
 * Created by Aaron on 8/1/2017.
 */

public class ChapterSelectionDialogFragment extends DialogFragment {
    private static final String KEY_BOOK = "arg.book";

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it.
     */
    public interface ChapterSelectionDialogListener {
        public void onChapterSelected(DialogFragment dialog, int book, int chapter);
    }

    // Use this instance of the interface to deliver action events
    ChapterSelectionDialogListener mListener;

    public static ChapterSelectionDialogFragment newInstance(int book) {
        ChapterSelectionDialogFragment f = new ChapterSelectionDialogFragment();
        Bundle args = new Bundle();
        args.putInt(KEY_BOOK, book);
        f.setArguments(args);
        return f;
    }

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (ChapterSelectionDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final int book = getArguments().getInt(KEY_BOOK);
        int chapterCount = getResources().getIntArray(R.array.chapter_count)[book-1];

        // 1. Instantiate an AlertDialog.Builder with its constructor
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // 2. Chain together various setter methods to set the dialog characteristics
        String[] chapters = new String[chapterCount];
        for (int i = 0; i < chapters.length; i++) {
            chapters[i] = String.valueOf(i + 1);
        }
        builder.setTitle(R.string.select_chapter)
                .setItems(chapters, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // The 'which' argument contains the index position
                        // of the selected item
                        if (mListener != null) {
                            mListener.onChapterSelected(ChapterSelectionDialogFragment.this, book, which+1);
                        }
                    }
                });
        // 3. Get the AlertDialog from create()
        AlertDialog dialog = builder.create();
        return dialog;
    }
}
