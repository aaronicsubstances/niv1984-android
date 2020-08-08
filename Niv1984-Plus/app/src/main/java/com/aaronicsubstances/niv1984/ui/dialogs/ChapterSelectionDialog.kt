package com.aaronicsubstances.niv1984.ui.dialogs

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.GridView
import androidx.fragment.app.DialogFragment
import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.ui.view_adapters.ChapterSelectionDialogAdapter
import com.aaronicsubstances.niv1984.utils.AppConstants
import android.view.Gravity
import android.graphics.Point
import android.widget.AdapterView
import android.widget.TextView
import com.aaronicsubstances.niv1984.ui.book_reading.BookOrChapterSwitchHandler

class ChapterSelectionDialog: DialogFragment() {

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_BOOK_NUMBER = "bookNumber"
        private const val ARG_CHAPTER_NUMBER = "chapterNumber"

        fun newInstance(title: String, bookNumber: Int, selectedChapterNumber: Int) = ChapterSelectionDialog().apply {
            arguments = Bundle().apply {
                putInt(ARG_BOOK_NUMBER, bookNumber)
                putInt(ARG_CHAPTER_NUMBER, selectedChapterNumber)
                putString(ARG_TITLE, title)
            }
        }
    }

    private var bookNumber = 0
    private var selectedChapterNumber = 0
    private lateinit var title: String

    private lateinit var titleView: TextView
    private lateinit var gridView: GridView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireArguments().let {
            bookNumber = it.getInt(ARG_BOOK_NUMBER)
            selectedChapterNumber = it.getInt(ARG_CHAPTER_NUMBER)
            title = it.getString(ARG_TITLE)!!
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.dialog_chapter_sel, container, false)
        titleView = root.findViewById(R.id.title)
        gridView = root.findViewById(R.id.gridview)
        return root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        titleView.text = title

        val totalChapterCount = AppConstants.BIBLE_BOOK_CHAPTER_COUNT[bookNumber-1]
        val adapter = ChapterSelectionDialogAdapter(totalChapterCount, selectedChapterNumber)
        gridView.adapter = adapter
        gridView.setSelection(selectedChapterNumber - 1)
        gridView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            dismiss()
            if (position + 1 != selectedChapterNumber) {
                val intent = Intent()
                intent.putExtra(
                    BookOrChapterSwitchHandler.INTENT_EXTRA_CHAPTER_SELECTION,
                    position + 1
                )
                targetFragment?.onActivityResult(targetRequestCode, Activity.RESULT_OK, intent)
            }
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
        // Set the width of the dialog proportional to 75% of the screen width
        // and 50% of screen height.
        window.setLayout((size.x * 0.75).toInt(), (size.y * 0.5).toInt())
        window.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, (size.y * 0.5).toInt())
        window.setGravity(Gravity.CENTER)
        // Call super onResume after sizing
        super.onResume()

    }
}