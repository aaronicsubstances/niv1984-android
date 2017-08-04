package com.aaronicsubstances.niv1984;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;

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
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        View root = inflater.inflate(R.layout.fragment_chapter_selection, null);
        builder.setView(root);
        //RecyclerView listView = (RecyclerView)root.findViewById(R.id.list);
        int colCount = getResources().getInteger(R.integer.grid_col_ount);
        //listView.setLayoutManager(new GridLayoutManager(getActivity(), colCount));
        ChapterListAdapter listAdapter = new ChapterListAdapter(getActivity(), chapters);
        //listView.setAdapter(listAdapter);
        /*listAdapter.setItemClickListener(new RecyclerViewItemClickListener() {
            @Override
            public void onItemClicked(int adapterPosition, Object data) {
                if (mListener != null) {
                    mListener.onChapterSelected(ChapterSelectionDialogFragment.this, book, adapterPosition+1);
                }
                dismiss();
            }
        });*/
        GridView gridview = (GridView) root.findViewById(R.id.gridview);
        //gridview.setNumColumns(colCount);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_gallery_item,
                chapters);
        gridview.setAdapter(adapter);
        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                if (mListener != null) {
                    mListener.onChapterSelected(ChapterSelectionDialogFragment.this, book, position+1);
                }
                dismiss();
            }
        });

        builder.setTitle(R.string.select_chapter);

        // 3. Get the AlertDialog from create()
        AlertDialog dialog = builder.create();
        return dialog;
    }
}
