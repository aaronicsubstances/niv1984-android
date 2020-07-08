package com.aaronicsubstances.niv1984.scrollexperiment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aaronicsubstances.endlesspaginglib.EndlessListViewScrollListener
import com.aaronicsubstances.niv1984.R
import com.aaronicsubstances.niv1984.models.BookReadItem

/**
 * A simple [Fragment] subclass.
 */
class ExPosFragment : Fragment() {
    private lateinit var endlessList: RecyclerView
    private lateinit var btnJumpTo50: Button
    private lateinit var btnViewFirst: Button

    private lateinit var adapter: ExPosAdapter
    private lateinit var viewModel: ExPosViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_ex_pos, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val rootView = requireView()
        endlessList = rootView.findViewById(R.id.endless_list)
        val layoutManager = LinearLayoutManager(activity)
        endlessList.addItemDecoration(DividerItemDecoration(activity, layoutManager.orientation))
        endlessList.layoutManager = layoutManager

        viewModel = ViewModelProvider(this).get(ExPosViewModel::class.java)

        adapter = ExPosAdapter(viewModel.endlessListRepo)
        endlessList.adapter = adapter

        endlessList.addOnScrollListener(
            EndlessListViewScrollListener(viewModel.endlessListRepo)
        )

        viewModel.loadLiveData.observe(viewLifecycleOwner,
            Observer<List<BookReadItem>> { adapter.notifyDataSetChanged() }
        )

        viewModel.load()

        btnJumpTo50 = rootView.findViewById(R.id.jumpTo50)
        btnViewFirst = rootView.findViewById(R.id.info)
        btnJumpTo50.setOnClickListener {
            layoutManager.scrollToPosition(50 - 1)
        }
        btnViewFirst.setOnClickListener {
            val firstPos = layoutManager.findFirstVisibleItemPosition()
            Toast.makeText(activity, "First visible item is at: ${firstPos + 1}",
                Toast.LENGTH_SHORT).show()
        }
    }

}
