package com.aaronicsubstances.niv1984.fragments;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatSpinner;
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

import com.aaronicsubstances.niv1984.etc.BibleJs;
import com.aaronicsubstances.niv1984.R;
import com.aaronicsubstances.niv1984.etc.SharedPrefsManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link BookTextFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link BookTextFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BookTextFragment extends Fragment implements View.OnClickListener,
        AdapterView.OnItemSelectedListener {
    private static final String ARG_BOOK_NUMBER = "bookNumber";

    private static final Logger LOGGER = LoggerFactory.getLogger(BookTextFragment.class);

    private int mBookNumber;

    private OnFragmentInteractionListener mListener;

    private AppCompatSpinner mChapterSpinner;
    private WebView mBookView;
    private RadioButton nivOnly, kjvOnly, bothBibles;

    private SharedPrefsManager mPrefMgr;
    private boolean mSkipRefresh;

    public BookTextFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param bookNumber the book number
     * @return A new instance of fragment BookTextFragment.
     */
    public static BookTextFragment newInstance(int bookNumber) {
        BookTextFragment fragment = new BookTextFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_BOOK_NUMBER, bookNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mBookNumber = getArguments().getInt(ARG_BOOK_NUMBER);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_book_text, container, false);

        mChapterSpinner = (AppCompatSpinner)root.findViewById(R.id.spinner);
        mBookView = (WebView)root.findViewById(R.id.bookView);
        nivOnly = (RadioButton)root.findViewById(R.id.nivOnly);
        kjvOnly = (RadioButton)root.findViewById(R.id.kjvOnly);
        bothBibles = (RadioButton)root.findViewById(R.id.bothOnly);

        setUpBrowserView();
        reloadBookUrl(true);

        return root;
    }

    private void setUpBrowserView() {
        mPrefMgr = new SharedPrefsManager(getContext());

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
                    if (!mSkipRefresh) {
                        mBookView.setVisibility(View.VISIBLE);
                    }
                    mSkipRefresh = true;
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

    private void reloadBookUrl(boolean refresh) {
        if (mBookNumber <= 0) return;

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

        String bookUrl = String.format("file:///android_asset/kjv-niv/%02d-%s.html",
                mBookNumber, suffix);

        int lastChapter = mPrefMgr.getLastChapter(mBookNumber);
        if (lastChapter > 0) {
            bookUrl += "#" + createChapFragId(lastChapter);
        }
        LOGGER.info("Loading book url {}", bookUrl);
        if (refresh) {
            mBookView.setVisibility(View.INVISIBLE);
        }
        mSkipRefresh = !refresh;
        mBookView.loadUrl(bookUrl);
    }

    private static String createChapFragId(int cnum) {
        return String.format("chapter-%s", cnum);
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
                reloadBookUrl(false);
            }
        }
    }

    private void setUpChapterSpinner() {
        int chapterCount = getResources().getIntArray(R.array.chapter_count)[mBookNumber-1];
        String[] chapters = new String[chapterCount];
        for (int i = 0; i < chapters.length; i++) {
            chapters[i] = String.format("%3d", i + 1);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(),
                R.layout.spinner_item,
                chapters);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mChapterSpinner.setAdapter(adapter);

        mChapterSpinner.setOnItemSelectedListener(this);
        mChapterSpinner.setSelection(mBookNumber-1);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        mPrefMgr.setLastChapter(mBookNumber, position+1);
        reloadBookUrl(false);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        LOGGER.warn("onNothingSelected.");
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
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

    public void setBookNumber(int bookNumber) {
        mBookNumber = bookNumber;
        reloadBookUrl(true);
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
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
