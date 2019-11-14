package com.klimaatmobiel.ui.fragments


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders

import com.example.projecten3android.R
import com.klimaatmobiel.ui.ViewModelFactories.WebshopViewModelFactory
import com.klimaatmobiel.ui.viewModels.WebshopViewModel
import com.example.projecten3android.databinding.FragmentBottomNavigationWebshopBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.klimaatmobiel.data.network.KlimaatmobielApi
import com.klimaatmobiel.domain.KlimaatmobielRepository
import com.klimaatmobiel.ui.MainActivity


/**
 * A simple [Fragment] subclass.
 */
class BottomNavigationWebshopFragment : Fragment() {

    private lateinit var viewModel: WebshopViewModel


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {


        val binding = FragmentBottomNavigationWebshopBinding.inflate(inflater)
        binding.setLifecycleOwner(this)

        val group = BottomNavigationWebshopFragmentArgs.fromBundle(arguments!!).group

        (activity as MainActivity).setToolbarTitle("Klimaatmobiel" + " - " + group.groupName + " - " + group.project.projectName)

        val apiService = KlimaatmobielApi.retrofitService

        viewModel = activity?.run {
            ViewModelProviders.of(this, WebshopViewModelFactory(group, KlimaatmobielRepository(apiService)))[WebshopViewModel::class.java]
        } ?: throw Exception("Invalid Activity")
        binding.viewModel = viewModel

        binding.bottomNavigationWebshop.setOnNavigationItemSelectedListener(BottomNavigationView.OnNavigationItemSelectedListener {
            (activity as MainActivity).triggerWebshopBottomNavigation(it)
            true
        })
        binding.bottomNavigationWebshop.selectedItemId = R.id.nav_webshop



        return binding.root
    }


}