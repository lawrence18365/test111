/*
 * AccountsSection ViewModel for handling logout functionality
 */
package com.google.jetstream.presentation.screens.profile

import androidx.lifecycle.ViewModel
import com.google.jetstream.data.models.xtream.XtreamCredentials
import com.google.jetstream.data.repositories.xtream.XtreamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class AccountsSectionViewModel @Inject constructor(
    private val xtreamRepository: XtreamRepository
) : ViewModel() {

    val credentials: Flow<XtreamCredentials?> = xtreamRepository.credentialsFlow

    suspend fun logout() {
        xtreamRepository.logout()
    }
}
