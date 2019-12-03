package com.klimaatmobiel.ui.fragments


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders
import com.example.projecten3android.R
import com.example.projecten3android.databinding.FragmentAddGroupBinding
import com.klimaatmobiel.data.database.getDatabase
import com.klimaatmobiel.data.network.KlimaatmobielApi
import com.klimaatmobiel.domain.KlimaatmobielRepository
import com.klimaatmobiel.ui.ViewModelFactories.AddGroupViewModelFactory
import com.klimaatmobiel.ui.ViewModelFactories.MainMenuViewModelFactory
import com.klimaatmobiel.ui.ViewModelFactories.WebshopViewModelFactory
import com.klimaatmobiel.ui.viewModels.AddGroupViewModel
import com.klimaatmobiel.ui.viewModels.MainMenuViewModel
import com.klimaatmobiel.ui.viewModels.WebshopViewModel

/**
 * A simple [Fragment] subclass.
 */
class AddGroupFragment : Fragment() {

    private lateinit var viewModel: AddGroupViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val group = AddGroupFragmentArgs.fromBundle(arguments!!).group

        val apiService = KlimaatmobielApi.retrofitService

        val binding = FragmentAddGroupBinding.inflate(inflater)
        binding.lifecycleOwner = this

        viewModel = activity?.run {
            ViewModelProviders.of(this, AddGroupViewModelFactory(group, KlimaatmobielRepository(apiService, getDatabase(context!!.applicationContext))))[AddGroupViewModel::class.java]
        } ?: throw Exception("Invalid Activity")

        binding.addGroupViewModel = viewModel

        return binding.root
    }


}