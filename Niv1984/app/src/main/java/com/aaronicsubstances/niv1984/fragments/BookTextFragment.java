package com.aaronicsubstances.niv1984.fragments;

import android.content.Context;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;

import com.aaronicsubstances.niv1984.R;
import com.aaronicsubstances.niv1984.etc.BookTextViewUtils;
import com.aaronicsubstances.niv1984.etc.CurrentChapterChangeListener;
import com.aaronicsubstances.niv1984.etc.SharedPrefsManager;
import com.aaronicsubstances.niv1984.etc.SpinnerHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link BookTextFragment.OnBookTextFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link BookTextFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BookTextFragment extends Fragment implements View.OnClickListener,
        AdapterView.OnItemSelectedListener,
        CurrentChapterChangeListener {
    private static final String ARG_PARAM1 = "param1";

    private String mParam1;

    private static final Logger LOGGER = LoggerFactory.getLogger(BookTextFragment.class);

    private int mBookNumber = -1;

    private OnBookTextFragmentInteractionListener mListener;

    private SpinnerHelper mChapterSpinner, mZoomSpinner;
    private WebView mBookView;
    private RadioButton nivOnly, kjvOnly, bothBibles;

    private SharedPrefsManager mPrefMgr;

    private boolean mViewCreated = false;
    private int mDiffSuffix = 0;

    public BookTextFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @return A new instance of fragment BookTextFragment.
     */
    public static BookTextFragment newInstance(String param1) {
        BookTextFragment fragment = new BookTextFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_book_text, container, false);

        mChapterSpinner = new SpinnerHelper(root.findViewById(R.id.chapterDropDown));
        mZoomSpinner = new SpinnerHelper(root.findViewById(R.id.fontSizes));
        mBookView = (WebView)root.findViewById(R.id.bookView);
        nivOnly = (RadioButton)root.findViewById(R.id.nivOnly);
        kjvOnly = (RadioButton)root.findViewById(R.id.kjvOnly);
        bothBibles = (RadioButton)root.findViewById(R.id.bothOnly);

        mPrefMgr = new SharedPrefsManager(getContext());

        setUpBrowserView();
        setUpZoomSpinner();

        mViewCreated = true;
        refreshView();

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mViewCreated = false;
    }

    public void setBookNumber(int bookNumber) {
        if (mBookNumber == bookNumber) {
            LOGGER.warn("Book number is already set to {}.", bookNumber);
            return;
        }
        mBookNumber = bookNumber;
        if (mViewCreated) {
            LOGGER.debug("Calling refreshView from setBookNumber...");
            refreshView();
        }
    }

    public void refreshView() {
        int cnum = mPrefMgr.getLastChapter(mBookNumber);
        reloadBookUrl(cnum);
        setUpChapterSpinner(cnum);
    }

    private void setUpBrowserView() {
        int mode = mPrefMgr.getLastBookMode();
        switch (mode) {
            case SharedPrefsManager.BOOK_MODE_KJV:
                kjvOnly.setChecked(true);
                break;
            case SharedPrefsManager.BOOK_MODE_NIV_KJV:
                bothBibles.setChecked(true);
                break;
            default:
                nivOnly.setChecked(true);
                break;
        }

        nivOnly.setOnClickListener(this);
        kjvOnly.setOnClickListener(this);
        bothBibles.setOnClickListener(this);

        BookTextViewUtils.configureBrowser(getActivity(), mBookView, this);
    }

    private void setUpChapterSpinner(int selectedCnum) {
        if (mBookNumber < 1) return;

        int chapterCount = getResources().getIntArray(R.array.chapter_count)[mBookNumber-1];
        String[] chapters = new String[chapterCount];
        for (int i = 0; i < chapters.length; i++) {
            // left padding to 3 chars are for chapters in Psalms
            chapters[i] = String.format("%3d", i + 1);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                R.layout.spinner_item_2,
                chapters);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mChapterSpinner.setAdapter(adapter);

        // Disable listener before setting selection.
        //mChapterSpinner.setOnItemSelectedListener(null);
        mChapterSpinner.setOnItemSelectedListener(this);

        if (selectedCnum > 0) {
            // Pass animate=false to force immediate firing of listeners.
            mChapterSpinner.setSelection(selectedCnum - 1, false);
        }
        else {
            mChapterSpinner.setSelection(0, false);
        }
        //mChapterSpinner.setOnItemSelectedListener(this);
    }

    private void setUpZoomSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                R.layout.spinner_item_2,
                getResources().getStringArray(R.array.zoom_entries));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mZoomSpinner.setAdapter(adapter);

        //mZoomSpinner.setOnItemSelectedListener(null);
        mZoomSpinner.setOnItemSelectedListener(this);

        int zoomLevelIndex = mPrefMgr.getLastZoomLevelIndex();
        if (zoomLevelIndex >= 0) {
            mZoomSpinner.setSelection(zoomLevelIndex, false);
        }
        else {
            mZoomSpinner.setSelection(BookTextViewUtils.DEFAULT_ZOOM_INDEX, false);
        }
        //mZoomSpinner.setOnItemSelectedListener(this);
    }

    private void reloadBookUrl(int cnum) {
        reloadBookUrl(cnum, false);
    }

    private void reloadBookUrl(int cnum, boolean forceReload) {
        if (mBookNumber < 1) return;

        int lastBookMode = mPrefMgr.getLastBookMode();
        String suffix;
        switch (lastBookMode) {
            case SharedPrefsManager.BOOK_MODE_KJV:
                suffix = "kjv";
                break;
            case SharedPrefsManager.BOOK_MODE_NIV_KJV:
                suffix = "niv-kjv";
                break;
            default:
                suffix = "niv";
                break;
        }

        // Android 7 seems to optimize browser reloads if url is the same.
        // so zooming wasn't taking immediate effect.
        // webview.reload didn't help since scrolling would have changed
        // chapter fragment.
        // as such artificial url change is introduced in url to force reload.
        // only zooming requires this so far. in particular performance of
        // chapter scrolling depends on maintaining the url during reload.
        if (forceReload) {
            mDiffSuffix++;
        }
        String bookUrl = BookTextViewUtils.resolveUrl(
                String.format("kjv-niv/%02d-%s.html%s",
                mBookNumber, suffix, mDiffSuffix), null);

        if (cnum > 0) {
            bookUrl += String.format("#chapter-%s", cnum);
        }

        LOGGER.info("Loading book url {}", bookUrl);
        mBookView.loadUrl(bookUrl);
    }

    @Override
    public void onClick(View v) {
        if (v == bothBibles || v == nivOnly || v == kjvOnly) {
            boolean checked = ((RadioButton)v).isChecked();
            if (checked) {
                int mode = SharedPrefsManager.BOOK_MODE_NIV;
                if (v == bothBibles) {
                    mode = SharedPrefsManager.BOOK_MODE_NIV_KJV;
                }
                else if (v == kjvOnly) {
                    mode = SharedPrefsManager.BOOK_MODE_KJV;
                }
                mPrefMgr.setLastBookMode(mode);
                reloadBookUrl(mPrefMgr.getLastChapter(mBookNumber));
            }
        }
    }

    @Override
    public void onCurrentChapterChanged(int bnum, int cnum) {
        if (!mViewCreated) {
            return;
        }

        if (mBookNumber != bnum) {
            return;
        }

        // Disable listener before setting selection.
        mChapterSpinner.setOnItemSelectedListener(null);
        // Pass animate=false to force immediate firing of listeners.
        mChapterSpinner.setSelection(cnum > 0 ? cnum - 1 : 0, false);
        mChapterSpinner.setOnItemSelectedListener(this);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent == mChapterSpinner.getSpinner()) {
            LOGGER.debug("onItemSelected for mChapterSpinner");
            int cnum = position+1;
            mPrefMgr.setLastChapter(mBookNumber, cnum);
            reloadBookUrl(cnum);
        }
        else if (parent == mZoomSpinner.getSpinner()) {
            LOGGER.debug("onItemSelected for mZoomSpinner");
            mPrefMgr.setLastZoomLevelIndex(position);
            reloadBookUrl(mPrefMgr.getLastChapter(mBookNumber), true);
        }
        else {
            LOGGER.error("onItemSelected didn't match any spinner.");
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        LOGGER.warn("onNothingSelected.");
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnBookTextFragmentInteractionListener) {
            mListener = (OnBookTextFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnBookTextFragmentInteractionListener {

        void onBookTextInteraction();
    }
}
