package com.aaronicsubstances.niv1984.ui.dialogs

import android.app.Activity
import android.content.Intent
import android.graphics.Point
import android.os.Bundle
import android.view.*
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aaronicsubstances.largelistpaging.LargeListEventListenerFactory
import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.ui.book_reading.BookOrChapterSwitchHandler
import com.aaronicsubstances.niv1984.ui.view_adapters.BookSelectionDialogAdapter

class BookSelectionDialog: DialogFragment() {

    companion object {
        private const val ARG_BOOK_NUMBER = "bookNumber"
        private const val ARG_BIBLE_VERSION = "bibleVersion"

        fun newInstance(bibleVersion: String, selectedBookNumber: Int) = BookSelectionDialog().apply {
            arguments = Bundle().apply {
                putInt(ARG_BOOK_NUMBER, selectedBookNumber)
                putString(ARG_BIBLE_VERSION, bibleVersion)
            }
        }
    }

    private lateinit var bibleVersion: String
    private var selectedBookNumber = 0

    private lateinit var mListView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireArguments().let {
            bibleVersion = it.getString(ARG_BIBLE_VERSION)!!
            selectedBookNumber = it.getInt(ARG_BOOK_NUMBER)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.dialog_book_sel, container, false)
        mListView = root.findViewById(R.id.listView)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val onItemClickListenerFactory = object: LargeListEventListenerFactory() {
            override fun <T> create(
                viewHolder: RecyclerView.ViewHolder,
                listenerCls: Class<T>, eventContextData: Any?
            ): T {
                assert(listenerCls == View.OnClickListener::class.java)
                return View.OnClickListener {
                    fireOnBookSelected(getItemPosition(viewHolder))
                } as T
            }
        }
        val adapter = BookSelectionDialogAdapter(bibleVersion, selectedBookNumber,
            onItemClickListenerFactory)

        val layoutMgr = LinearLayoutManager(context)
        mListView.layoutManager = layoutMgr
        //mListView.addItemDecoration(DividerItemDecoration(context, layoutMgr.orientation))
        mListView.adapter = adapter
        mListView.scrollToPosition(selectedBookNumber - 1)
    }

    private fun fireOnBookSelected(position: Int) {
        dismiss()
        if (position + 1 != selectedBookNumber) {
            val intent = Intent()
            intent.putExtra(BookOrChapterSwitchHandler.INTENT_EXTRA_BOOK_SELECTION, position + 1)
            targetFragment?.onActivityResult(targetRequestCode, Activity.RESULT_OK, intent)
        }
    }

    override fun onResume() {
        // Store access variables for window and blank point
        if (dialog == null) {
            return
        }
        val window = dialog!!.window!!
        val size = Point()
        // Store dimensions of the screen in `size`
        val display = window.windowManager.defaultDisplay
        display.getSize(size)
        // Set the width of the dialog proportional to same 75% of both screen width and screen height
        window.setLayout((size.x * 0.75).toInt(), (size.y * 0.75).toInt())
        window.setGravity(Gravity.CENTER)
        // Call super onResume after sizing
        super.onResume()
    }
}