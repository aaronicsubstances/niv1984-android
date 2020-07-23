package com.aaronicsubstances.niv1984.ui.search

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.aaronicsubstances.niv1984.R

class SearchRequestFragment : Fragment() {

    companion object {

        @JvmStatic
        fun newInstance() =
            SearchRequestFragment().apply {
                arguments = Bundle().apply {
                }
            }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_search_request, container, false)
    }
}
