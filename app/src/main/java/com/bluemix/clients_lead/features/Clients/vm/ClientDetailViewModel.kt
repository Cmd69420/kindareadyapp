package com.bluemix.clients_lead.features.Clients.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluemix.clients_lead.domain.model.Client
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.bluemix.clients_lead.core.common.utils.AppResult
import com.bluemix.clients_lead.domain.usecases.GetClientById
import timber.log.Timber

data class ClientDetailUiState(
    val isLoading: Boolean = false,
    val client: Client? = null,
    val error: String? = null
)

/**
 * ViewModel for client detail screen.
 * Receives clientId via loadClient() method, not constructor.
 */
class ClientDetailViewModel(
    private val getClientById: GetClientById
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClientDetailUiState())
    val uiState: StateFlow<ClientDetailUiState> = _uiState.asStateFlow()

    private var currentClientId: String? = null

    /**
     * Load client details by ID.
     * Call this from the UI when the screen is created.
     */
    fun loadClient(clientId: String) {
        if (currentClientId == clientId && _uiState.value.client != null) {
            // Already loaded this client
            return
        }

        currentClientId = clientId
        fetchClient(clientId)
    }

    private fun fetchClient(clientId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            when (val result = getClientById(clientId)) {
                is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        client = result.data
                    )
                }
                is AppResult.Error -> {
                    Timber.e(result.error.message, "Failed to load client: ${result.error.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.error.message ?: "Failed to load client"
                    )
                }
            }
        }
    }

    fun refresh() {
        currentClientId?.let { fetchClient(it) }
    }
}

//data class ClientDetailUiState(
//    val isLoading: Boolean = false,
//    val client: Client? = null,
//    val error: String? = null
//)
//
//class ClientDetailViewModel(
//    private val clientRepository: IClientRepository,
//    private val clientId: String
//) : ViewModel() {
//
//    private val _uiState = MutableStateFlow(ClientDetailUiState())
//    val uiState: StateFlow<ClientDetailUiState> = _uiState.asStateFlow()
//
//    init {
//        loadClient()
//    }
//
//    private fun loadClient() {
//        viewModelScope.launch {
//            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
//
//            clientRepository.getClientById(clientId)
//                .onSuccess { client ->
//                    _uiState.value = _uiState.value.copy(
//                        isLoading = false,
//                        client = client
//                    )
//                }
//                .onFailure { error ->
//                    _uiState.value = _uiState.value.copy(
//                        isLoading = false,
//                        error = error.message ?: "Failed to load client"
//                    )
//                }
//        }
//    }
//
//    fun refresh() {
//        loadClient()
//    }
//}
