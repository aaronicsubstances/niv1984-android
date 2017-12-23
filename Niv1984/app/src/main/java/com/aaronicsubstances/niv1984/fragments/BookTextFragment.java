package com.aaronicsubstances.niv1984.fragments;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.Spinner;

import com.aaronicsubstances.niv1984.R;
import com.aaronicsubstances.niv1984.etc.BibleJs;
import com.aaronicsubstances.niv1984.etc.SharedPrefsManager;
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
        AdapterView.OnItemSelectedListener {
    private static final String ARG_PARAM1 = "param1";

    private String mParam1;

    private static final Logger LOGGER = LoggerFactory.getLogger(BookTextFragment.class);
    private static final String[] ZOOM_LEVELS = {"70%", "100%", "150%", "200%"};
    private static final int DEFAULT_ZOOM_INDEX = 1;

    private int mBookNumber = -1, mChapterNumber = -1, mZoomLevelIndex = -1;

    private OnBookTextFragmentInteractionListener mListener;

    private Spinner mChapterSpinner, mZoomSpinner;
    private WebView mBookView;
    private RadioButton nivOnly, kjvOnly, bothBibles;

    private SharedPrefsManager mPrefMgr;

    private boolean mViewCreated = false;

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

        mChapterSpinner = (Spinner)root.findViewById(R.id.chapterDropDown);
        mZoomSpinner = (Spinner)root.findViewById(R.id.fontSizes);
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
        mBookNumber = bookNumber;
        if (mViewCreated) {
            refreshView();
        }
    }

    public void refreshView() {
        reloadBookUrl();
        setUpChapterSpinner();
    }

    private void setUpBrowserView() {
        int mode = mPrefMgr.getLastBookMode();
        switch (mode) {
            case SharedPrefsManager.BOOK_MODE_KJV:
                kjvOnly.setChecked(true);
                break;
            case SharedPrefsManager.BOOK_MODE_KJV_NIV:
                bothBibles.setChecked(true);
                break;
            default:
                nivOnly.setChecked(true);
                break;
        }

        nivOnly.setOnClickListener(this);
        kjvOnly.setOnClickListener(this);
        bothBibles.setOnClickListener(this);

        WebSettings webSettings = mBookView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setSaveFormData(false);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);
        }

        mBookView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress == 100) {

                }
            }

            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                LOGGER.error("{}: {} (at {}:{})",
                        consoleMessage.messageLevel(), consoleMessage.message(),
                        consoleMessage.sourceId(), consoleMessage.lineNumber());
                return true;
            }
        });

        mBookView.addJavascriptInterface(new BibleJs(getContext()),
                BibleJs.NAME);
    }

    private void setUpChapterSpinner() {
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
        mChapterSpinner.setOnItemSelectedListener(null);
        if (mChapterNumber > 0) {
            // Pass animate=false to force immediate firing of listeners.
            mChapterSpinner.setSelection(mChapterNumber - 1, false);
        }
        mChapterSpinner.setOnItemSelectedListener(this);
    }

    private void setUpZoomSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                R.layout.spinner_item_2,
                ZOOM_LEVELS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mZoomSpinner.setAdapter(adapter);

        mZoomLevelIndex = mPrefMgr.getLastZoomLevelIndex();
        if (mZoomLevelIndex >= 0) {
            mZoomSpinner.setSelection(mZoomLevelIndex, false);
        }
        else {
            mZoomSpinner.setSelection(DEFAULT_ZOOM_INDEX, false);
        }
        mZoomSpinner.setOnItemSelectedListener(this);
    }

    private void reloadBookUrl() {
        if (mBookNumber < 1) return;

        int lastBookMode = mPrefMgr.getLastBookMode();
        String suffix;
        switch (lastBookMode) {
            case SharedPrefsManager.BOOK_MODE_KJV:
                suffix = "kjv";
                break;
            case SharedPrefsManager.BOOK_MODE_KJV_NIV:
                suffix = "kjv-niv";
                break;
            default:
                suffix = "niv";
                break;
        }

        String zoom = ZOOM_LEVELS[mZoomLevelIndex < 0 ? DEFAULT_ZOOM_INDEX : mZoomLevelIndex];
        String bookUrl = String.format("file:///android_asset/kjv-niv/%02d-%s.html?zoom=%s",
                mBookNumber, suffix, Uri.encode(zoom));

        mChapterNumber = mPrefMgr.getLastChapter(mBookNumber);
        if (mChapterNumber > 0) {
            bookUrl += String.format("#chapter-%s", mChapterNumber);
        }

        LOGGER.info("Loading book url {}", bookUrl);
        mBookView.loadUrl(bookUrl);
    }

    private void applyTextZoom() {
        String zoom = ZOOM_LEVELS[mZoomLevelIndex < 0 ? DEFAULT_ZOOM_INDEX : mZoomLevelIndex];
        Utils.loadJavascript(mBookView, "applyTextZoom", new String[]{ zoom });
    }

    @Override
    public void onClick(View v) {
        if (v == bothBibles || v == nivOnly || v == kjvOnly) {
            boolean checked = ((RadioButton)v).isChecked();
            if (checked) {
                int mode = SharedPrefsManager.BOOK_MODE_NIV;
                if (v == bothBibles) {
                    mode = SharedPrefsManager.BOOK_MODE_KJV_NIV;
                }
                else if (v == kjvOnly) {
                    mode = SharedPrefsManager.BOOK_MODE_KJV;
                }
                mPrefMgr.setLastBookMode(mode);
                reloadBookUrl();
            }
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent == mChapterSpinner) {
            mPrefMgr.setLastChapter(mBookNumber, position+1);
            reloadBookUrl();
        }
        else if (parent == mZoomSpinner) {
            mZoomLevelIndex = position;
            mPrefMgr.setLastZoomLevelIndex(mZoomLevelIndex);
            applyTextZoom();
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
