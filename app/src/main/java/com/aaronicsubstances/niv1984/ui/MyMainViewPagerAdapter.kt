package com.aaronicsubstances.niv1984.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import kotlin.reflect.KClass

class MyMainViewPagerAdapter(context: FragmentActivity) :
    FragmentStateAdapter(context) {

    private val fragments = mutableListOf<MyMainViewPagerFragment>()

    override fun createFragment(position: Int): Fragment {
        return fragments[position] as Fragment
    }

    override fun getItemCount(): Int {
        return fragments.size
    }

    override fun containsItem(itemId: Long): Boolean {
        return fragments.any { it.getItemId() == itemId }
    }

    override fun getItemId(position: Int): Long {
        return fragments[position].getItemId()
    }

    fun addFragments(vararg newInstances: MyMainViewPagerFragment) {
        fragments.addAll(newInstances)
    }

    fun changeFragment(i: Int, newInstance: MyMainViewPagerFragment) {
        fragments[i] = newInstance
        notifyItemChanged(i)
    }

    fun isFragmentInstanceOf(i: Int, fragClass: KClass<*>): Boolean {
        return fragClass.isInstance(fragments[i])
    }
}