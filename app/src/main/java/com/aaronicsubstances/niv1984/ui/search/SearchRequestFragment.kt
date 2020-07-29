package com.aaronicsubstances.niv1984.ui.search

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.fragment.app.Fragment
import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.bootstrap.MyApplication
import com.aaronicsubstances.niv1984.data.SharedPrefManager
import com.aaronicsubstances.niv1984.ui.PrefListenerFragment
import com.aaronicsubstances.niv1984.utils.AppConstants
import com.aaronicsubstances.niv1984.utils.AppUtils
import javax.inject.Inject

class SearchRequestFragment : Fragment(), PrefListenerFragment {

    companion object {

        @JvmStatic
        fun newInstance() = SearchRequestFragment()
    }

    interface SearchRequestListener {
        fun onProcessSearchRequest(f: SearchResponseFragment)
    }

    private var searchRequestListener: SearchRequestListener? = null

    private lateinit var bookStartRangeSpinner: Spinner
    private lateinit var bookEndRangeSpinner: Spinner
    private lateinit var searchBox: EditText
    private lateinit var advSearchBtn: Button
    private lateinit var bibleVersionCheckBoxes: LinearLayout

    @Inject
    internal lateinit var sharedPrefMgr: SharedPrefManager

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (context.applicationContext as MyApplication).appComponent.inject(this)
        if (context is SearchRequestListener) {
            searchRequestListener = context
        }
        else {
            throw IllegalArgumentException("${context.javaClass} must " +
                    "implement ${SearchRequestListener::class}")
        }
    }

    override fun onDetach() {
        super.onDetach()
        searchRequestListener = null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.search_request_fragment, container, false)

        bookStartRangeSpinner = root.findViewById(R.id.startBibleBook)
        bookEndRangeSpinner = root.findViewById(R.id.endBibleBook)
        searchBox = root.findViewById(R.id.searchBox)
        advSearchBtn = root.findViewById(R.id.advSearch)

        bibleVersionCheckBoxes = root.findViewById(R.id.bibleVersionCheckBoxes)

        return root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val topBibleVersions = sharedPrefMgr.getPreferredBibleVersions()
        resetAdvancedViewsRelatedToBibleVersions(topBibleVersions)

        val startRangeAdapter = createBookSpinnerAdapter(topBibleVersions[0])
        val endRangeAdapter = createBookSpinnerAdapter(topBibleVersions[0])
        bookStartRangeSpinner.adapter = startRangeAdapter
        bookEndRangeSpinner.adapter = endRangeAdapter
        bookEndRangeSpinner.setSelection(AppConstants.BIBLE_BOOK_COUNT - 1)

        advSearchBtn.setOnClickListener { startSearch() }
    }

    private fun resetAdvancedViewsRelatedToBibleVersions(topBibleVersions: List<String>) {
        val allBooks = AppUtils.getAllBooks(topBibleVersions)

        // dynamically add check boxes for each supported bible version
        // after clearing it.
        bibleVersionCheckBoxes.removeAllViews()
        for (i in allBooks.indices) {
            val checkBox = CheckBox(context)
            if (i == 0) {
                checkBox.isChecked = true
            }
            checkBox.tag = allBooks[i]
            checkBox.text = AppConstants.bibleVersions.getValue(allBooks[i]).description
            checkBox.id = View.generateViewId()
            val prms = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT)
            bibleVersionCheckBoxes.addView(checkBox, prms)
        }
    }

    private fun createBookSpinnerAdapter(bibleVersionCode: String): ArrayAdapter<String> {
        // make mutable list instead of using book names directly, so
        // clearing and re-adding can be done during refresh.
        val items = mutableListOf<String>()
        items.addAll(AppConstants.bibleVersions.getValue(bibleVersionCode).bookNames)

        val adapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item,
            items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter
    }

    private fun refreshBookSpinnerAdapter(adapter: ArrayAdapter<String>, bibleVersionCode: String) {
        adapter.setNotifyOnChange(false)
        adapter.clear()
        val items = AppConstants.bibleVersions.getValue(bibleVersionCode).bookNames
        adapter.addAll(items)
        adapter.notifyDataSetChanged()
    }

    override fun onPrefBibleVersionsChanged(bibleVersions: List<String>) {
        resetAdvancedViewsRelatedToBibleVersions(bibleVersions)

        refreshBookSpinnerAdapter(bookStartRangeSpinner.adapter as ArrayAdapter<String>,
            bibleVersions[0])
        refreshBookSpinnerAdapter(bookEndRangeSpinner.adapter as ArrayAdapter<String>,
            bibleVersions[0])
    }

    override fun onPrefZoomLevelChanged(zoomLevel: Int) {
    }

    override fun onPrefMultipleDisplayOptionChanged(displayMultipleSideBySide: Boolean) {
    }

    override fun onPrefKeepScreenOnDuringReadingChanged(keepScreenOn: Boolean) {
    }

    private fun startSearch() {
        // hide virtual keyboard
        requireContext().let {
            val imm = it.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(searchBox.windowToken, 0);
        }
        val q = (searchBox.text?.toString() ?: "").trim()
        if (q.isEmpty()) {
            AppUtils.showShortToast(context, "Please type some text into search box")
            return
        }
        val bibleVersions = arrayListOf<String>()
        for (i in 0 until bibleVersionCheckBoxes.childCount) {
            var checkBox = bibleVersionCheckBoxes.getChildAt(i) as CheckBox
            if (checkBox.isChecked) {
                bibleVersions.add(checkBox.tag as String)
            }
        }
        if (bibleVersions.isEmpty()) {
            AppUtils.showShortToast(context, "Please select at least one bible version")
            return
        }
        val startBookNumber = bookStartRangeSpinner.selectedItemPosition + 1
        val inclEndBookNumber = bookEndRangeSpinner.selectedItemPosition + 1
        val f = SearchResponseFragment.newInstance(q, bibleVersions, startBookNumber,
            inclEndBookNumber)
        searchRequestListener?.onProcessSearchRequest(f)
    }
}
