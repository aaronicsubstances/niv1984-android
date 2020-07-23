package com.aaronicsubstances.niv1984.ui.search

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.Fragment
import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.bootstrap.MyApplication
import com.aaronicsubstances.niv1984.data.SharedPrefManager
import com.aaronicsubstances.niv1984.ui.PrefListenerFragment
import com.aaronicsubstances.niv1984.utils.AppConstants
import javax.inject.Inject

class SearchRequestFragment : Fragment(), PrefListenerFragment {

    companion object {

        @JvmStatic
        fun newInstance() =
            SearchRequestFragment().apply {
                arguments = Bundle().apply {
                }
            }
    }

    private lateinit var bookStartRangeSpinner: Spinner
    private lateinit var bookEndRangeSpinner: Spinner

    @Inject
    internal lateinit var sharedPrefMgr: SharedPrefManager

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (context.applicationContext as MyApplication).appComponent.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.search_request_fragment, container, false)
        bookStartRangeSpinner = root.findViewById(R.id.startBibleBook)
        bookEndRangeSpinner = root.findViewById(R.id.endBibleBook)
        return root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val bibleVersionCode = sharedPrefMgr.getPreferredBibleVersions()[0]
        val startRangeAdapter = createBookSpinnerAdapter(bibleVersionCode)
        val endRangeAdapter = createBookSpinnerAdapter(bibleVersionCode)
        bookStartRangeSpinner.adapter = startRangeAdapter
        bookEndRangeSpinner.adapter = endRangeAdapter
        bookEndRangeSpinner.setSelection(AppConstants.BIBLE_BOOK_COUNT - 1)
    }

    private fun createBookSpinnerAdapter(bibleVersionCode: String): ArrayAdapter<String> {
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
        val bibleVersionCode = bibleVersions[0]
        refreshBookSpinnerAdapter(bookStartRangeSpinner.adapter as ArrayAdapter<String>,
            bibleVersionCode)
        refreshBookSpinnerAdapter(bookEndRangeSpinner.adapter as ArrayAdapter<String>,
            bibleVersionCode)
    }

    override fun onPrefZoomLevelChanged(zoomLevel: Int) {
    }

    override fun onPrefNightModeChanged(isNightMode: Boolean) {
    }

    override fun onPrefMultipleDisplayOptionChanged(displayMultipleSideBySide: Boolean) {
    }

    override fun onPrefKeepScreenOnDuringReadingChanged(keepScreenOn: Boolean) {
    }
}
