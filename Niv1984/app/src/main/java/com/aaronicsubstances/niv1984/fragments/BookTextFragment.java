package com.aaronicsubstances.niv1984.fragments;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.RadioButton;

import com.aaronicsubstances.niv1984.R;
import com.aaronicsubstances.niv1984.etc.BookTextViewUtils;
import com.aaronicsubstances.niv1984.etc.CustomWebPageEventListener;
import com.aaronicsubstances.niv1984.etc.SharedPrefsManager;
import com.aaronicsubstances.niv1984.etc.SpinnerHelper;
import com.aaronicsubstances.niv1984.etc.Utils;

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
        CustomWebPageEventListener {
    private static final String ARG_PARAM1 = "param1";

    private String mParam1;

    private static final Logger LOGGER = LoggerFactory.getLogger(BookTextFragment.class);

    private String mBookCode = null;

    private OnBookTextFragmentInteractionListener mListener;

    private SpinnerHelper mChapterSpinner, mZoomSpinner;
    private WebView mBookView;
    private RadioButton cpdvOnly, withDra, withNiv;
    private ProgressBar mWebViewPageLoadIndicator;

    private SharedPrefsManager mPrefMgr;

    private boolean mViewCreated = false;

    private Runnable KEEP_SCREEN_OFF;
    private String[] mChapters;

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
        mBookView = root.findViewById(R.id.bookView);
        mWebViewPageLoadIndicator = root.findViewById(R.id.progressBar1);
        cpdvOnly = root.findViewById(R.id.cpdvOnly);
        withDra = root.findViewById(R.id.withDra);
        withNiv = root.findViewById(R.id.withNiv);

        mPrefMgr = new SharedPrefsManager(getContext());

        setUpBrowserView();
        setUpZoomSpinner();

        mViewCreated = true;
        refreshView();

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        KEEP_SCREEN_OFF = () -> {
            Utils.HANDLER_INSTANCE.removeCallbacks(KEEP_SCREEN_OFF);
            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        };
        if (mPrefMgr.getKeepUserScreenOn()) {
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        else {
            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        boolean viewOutOfDate = false;
        int lastZoomLevelIdx = mPrefMgr.getZoomLevelIndex();
        if (lastZoomLevelIdx >= 0) {
            if (lastZoomLevelIdx != mZoomSpinner.getSelectedItemPosition()) {
                mZoomSpinner.setSelection(lastZoomLevelIdx, false);
                viewOutOfDate = true;
            }
        }
        if (viewOutOfDate) {
            LOGGER.info("WebView out of date. refreshing...");
            refreshView();
        }
    }

    private void postponeKeepScreenOff() {
        if (mPrefMgr.getKeepUserScreenOn()) {
            Utils.HANDLER_INSTANCE.removeCallbacks(KEEP_SCREEN_OFF);
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            Utils.HANDLER_INSTANCE.postDelayed(KEEP_SCREEN_OFF, 5 * 1000 * 60); // 5 minutes
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        Utils.HANDLER_INSTANCE.removeCallbacks(KEEP_SCREEN_OFF);
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mViewCreated = false;
    }

    public void setBookCode(String bookCode) {
        if (bookCode.equals(mBookCode)) {
            LOGGER.warn("Book code is already set to {}.", bookCode);
            return;
        }
        mBookCode = bookCode;
        if (mViewCreated) {
            LOGGER.debug("Calling refreshView from setBookNumber...");
            refreshView();
        }
    }

    public void refreshView() {
        setUpChapterSpinner();
        reloadBookUrl();
    }

    private void setUpChapterSpinner() {
        if (mBookCode == null) return;

        int[] chapterDisplayRange = Utils.determineChapterDisplayRange(mBookCode);
        int chapterDisplayStart = chapterDisplayRange[0];
        int chapterDisplayEnd = chapterDisplayRange[1];
        mChapters = new String[chapterDisplayEnd - chapterDisplayStart + 1];
        for (int i = 0; i < mChapters.length; i++) {
            int cnum = chapterDisplayStart + i;
            String chapterDisplay;
            if (cnum == 0) {
                chapterDisplay = "P";
            }
            else {
                // left padding to 3 chars are for chapters in Psalms
                chapterDisplay = String.format("%3d", cnum);
            }
            mChapters[i] = chapterDisplay;
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                R.layout.spinner_item_2,
                mChapters);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mChapterSpinner.setAdapter(adapter);

        String selectedCnum = mPrefMgr.getLastChapter(mBookCode);
        if (selectedCnum != null) {
            if (selectedCnum.equals("0")) {
                selectedCnum = "P";
            }
            selectedCnum = selectedCnum.trim();
            int selectedIdx = 0;
            for (int i = 0; i < mChapters.length; i++) {
                if (mChapters[i].trim().equals(selectedCnum)) {
                    selectedIdx = i;
                    break;
                }
            }
            // Pass animate=false to avoid unnecessary animation in spinner.
            mChapterSpinner.setSelection(selectedIdx, false);
        }
        else {
            mChapterSpinner.setSelection(0, false);
        }
        mChapterSpinner.setOnItemSelectedListener(this);
    }

    private void setUpBrowserView() {
        int mode = mPrefMgr.getLastBookMode();
        switch (mode) {
            case SharedPrefsManager.BOOK_MODE_WITH_DRA:
                withDra.setChecked(true);
                break;
            case SharedPrefsManager.BOOK_MODE_WITH_NIV:
                withNiv.setChecked(true);
                break;
            default:
                cpdvOnly.setChecked(true);
                break;
        }

        cpdvOnly.setOnClickListener(this);
        withDra.setOnClickListener(this);
        withNiv.setOnClickListener(this);

        BookTextViewUtils.configureBrowser(getActivity(), mBookView, this);
    }

    private void setUpZoomSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                R.layout.spinner_item_2,
                getResources().getStringArray(R.array.zoom_entries_slim));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mZoomSpinner.setAdapter(adapter);

        int zoomLevelIndex = mPrefMgr.getZoomLevelIndex();
        if (zoomLevelIndex >= 0) {
            mZoomSpinner.setSelection(zoomLevelIndex, false);
        }
        else {
            mZoomSpinner.setSelection(BookTextViewUtils.DEFAULT_ZOOM_INDEX, false);
        }
        mZoomSpinner.setOnItemSelectedListener(this);
    }

    private void reloadBookUrl() {
        if (mBookCode == null) return;

        String additionalBook = "";
        switch (mPrefMgr.getLastBookMode()) {
            case SharedPrefsManager.BOOK_MODE_WITH_DRA:
                additionalBook = "dra";
                break;
            case SharedPrefsManager.BOOK_MODE_WITH_NIV:
                additionalBook = "niv";
                break;
        }

        // For change in text size to take effect,
        // it has so far been observed that it is not enough
        // to call loadUrl() on the WebView instance with
        // the same url, because the WebView does not refetch
        // the css which contains the changes required for the
        // new text size.
        // So a query string parameter is added to force a
        // refetching of the css.
        String bookUrl = BookTextViewUtils.resolveUrl(
                String.format("html/%s-cpdv.html", mBookCode),
                "add", additionalBook,
                "zoom", "" + mZoomSpinner.getSelectedItemPosition());

        String lastEffectiveBookmark = mPrefMgr.getLastInternalBookmark(mBookCode, Utils.DEFAULT_VERSION);
        // if url differs only in fragment, don't show progress loading indicator
        int loadIndicatorVisibility = View.VISIBLE;
        if (bookUrl.equals(getUrlWithoutFragment())) {
            loadIndicatorVisibility = View.INVISIBLE;
        }
        if (lastEffectiveBookmark != null) {
            bookUrl += '#' + lastEffectiveBookmark;
        }

        LOGGER.info("Loading book url {}", bookUrl);
        mWebViewPageLoadIndicator.setVisibility(loadIndicatorVisibility);
        mBookView.loadUrl(bookUrl);
    }

    private String getUrlWithoutFragment() {
        String url = mBookView.getUrl();
        if (url == null) {
            return null;
        }
        int hashIdx = url.lastIndexOf('#');
        if (hashIdx < 0) {
            return url;
        }
        else {
            return url.substring(0, hashIdx);
        }
    }

    @Override
    public void onClick(View v) {
        if (v == cpdvOnly || v == withDra || v == withNiv) {
            boolean checked = ((RadioButton)v).isChecked();
            if (checked) {
                int mode = SharedPrefsManager.BOOK_MODE_CPDV;
                if (v == withDra) {
                    mode = SharedPrefsManager.BOOK_MODE_WITH_DRA;
                }
                else if (v == withNiv) {
                    mode = SharedPrefsManager.BOOK_MODE_WITH_NIV;
                }
                mPrefMgr.setLastBookMode(mode);
                reloadBookUrl();
            }
        }
    }

    @Override
    public void onPageLoadCompleted() {
        mWebViewPageLoadIndicator.setVisibility(View.INVISIBLE);
        postponeKeepScreenOff();
    }

    @Override
    public void onPageScrollEvent() {
        postponeKeepScreenOff();
    }

    @Override
    public void onPageChapterMayHaveChanged(String bcode, String cnum) {
        if (!mViewCreated) {
            return;
        }

        if (!bcode.equals(mBookCode)) {
            return;
        }

        int cIdx = -1;
        for (int i = 0; i < mChapters.length; i++) {
            if (mChapters[i].trim().equals(
                    cnum.equals("0") ? "P" : cnum)) {
                cIdx = i;
                break;
            }
        }

        if (cIdx == -1 || mChapterSpinner.getSelectedItemPosition() == cIdx) {
            return;
        }

        // Disable listener before setting selection.
        mChapterSpinner.setOnItemSelectedListener(null);
        // Pass animate=false to avoid unnecessary animation in spinner.
        mChapterSpinner.setSelection(cIdx, false);
        mChapterSpinner.setOnItemSelectedListener(this);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent == mChapterSpinner.getSpinner()) {
            LOGGER.debug("onItemSelected for mChapterSpinner");
            String cnum = mChapters[position].trim();
            if (cnum.equals("P")) {
                cnum = "0";
            }
            mPrefMgr.setLastInternalBookmarkAndChapter(mBookCode, Utils.DEFAULT_VERSION, "chapter-" + cnum, cnum);
            reloadBookUrl();
        }
        else if (parent == mZoomSpinner.getSpinner()) {
            LOGGER.debug("onItemSelected for mZoomSpinner");
            mPrefMgr.setZoomLevelIndex(position);
            reloadBookUrl();
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
