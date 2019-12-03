package com.klimaatmobiel.ui.ViewModelFactories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.klimaatmobiel.domain.Group
import com.klimaatmobiel.domain.KlimaatmobielRepository
import com.klimaatmobiel.ui.viewModels.AddGroupViewModel

class AddGroupViewModelFactory(private val group: Group, private val repository: KlimaatmobielRepository) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddGroupViewModel::class.java)) {
            return AddGroupViewModel(group, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}