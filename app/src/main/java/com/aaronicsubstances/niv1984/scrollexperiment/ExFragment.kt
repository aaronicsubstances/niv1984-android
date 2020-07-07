package com.aaronicsubstances.niv1984.scrollexperiment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aaronicsubstances.niv1984.R

/**
 * A simple [Fragment] subclass.
 */
class ExFragment : Fragment() {
    private lateinit var endlessList: RecyclerView
    private lateinit var btnEggs100: Button
    private lateinit var btnBottle1000: Button
    private lateinit var btnSweet10000: Button
    private lateinit var btnJumpTo50: Button
    private lateinit var btnViewFirst: Button
    private lateinit var scrollToHalfOption: CheckBox

    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var adapter: ExAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_ex, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val rootView = requireView()
        endlessList = rootView.findViewById(R.id.endless_list)
        adapter = ExAdapter("", 0)
        endlessList.adapter = adapter
        layoutManager = LinearLayoutManager(activity)
        endlessList.addItemDecoration(DividerItemDecoration(activity, layoutManager.orientation))
        endlessList.layoutManager = layoutManager

        btnEggs100 = rootView.findViewById(R.id.eggs)
        btnBottle1000 = rootView.findViewById(R.id.bottles)
        btnSweet10000 = rootView.findViewById(R.id.sweets)
        btnJumpTo50 = rootView.findViewById(R.id.jumpTo50)
        btnViewFirst = rootView.findViewById(R.id.info)
        scrollToHalfOption = rootView.findViewById(R.id.scrollToHalfOption)

        btnEggs100.setOnClickListener {
            adapter.size = 100
            adapter.text = "Egg"
            adapter.notifyDataSetChanged()
            if (scrollToHalfOption.isChecked)
                layoutManager.scrollToPosition(adapter.size/2 - 1)
        }
        btnBottle1000.setOnClickListener {
            adapter.size = 1000
            adapter.text = "Bottle"
            adapter.notifyDataSetChanged()
            if (scrollToHalfOption.isChecked)
                layoutManager.scrollToPosition(adapter.size/2 - 1)
        }
        btnSweet10000.setOnClickListener {
            adapter.size = 10000
            adapter.text = "Sweet"
            adapter.notifyDataSetChanged()
            if (scrollToHalfOption.isChecked)
                layoutManager.scrollToPosition(adapter.size/2 - 1)
        }
        btnJumpTo50.setOnClickListener {
            layoutManager.scrollToPosition(50 - 1)
            //layoutManager.requestLayout()
        }
        btnViewFirst.setOnClickListener {
            val firstPos = layoutManager.findFirstCompletelyVisibleItemPosition()
            Toast.makeText(activity, "First completely visible item is at: ${firstPos + 1}",
                Toast.LENGTH_SHORT).show()
        }
    }
}
