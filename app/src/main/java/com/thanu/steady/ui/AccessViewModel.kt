package com.thanu.steady.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thanu.steady.platform.BootstrapState
import com.thanu.steady.platform.BootstrapStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccessState(val loading: Boolean = true, val bootstrap: BootstrapState = BootstrapState(),
    val authenticated: Boolean = false, val failed: Boolean = false) {
    val canOpenPrivate get() = !loading && !failed && (!bootstrap.locked || authenticated)
}
class AccessViewModel(private val store: BootstrapStore) : ViewModel() {
    private val _state = MutableStateFlow(AccessState())
    val state = _state.asStateFlow()
    init {
        viewModelScope.launch {
            try { store.states.collect { flags -> _state.update { it.copy(loading = false, bootstrap = flags, failed = false) } } }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { _state.update { it.copy(loading = false, failed = true, authenticated = false) } }
        }
    }
    fun authenticated() { _state.update { it.copy(authenticated = true) } }
    fun relock() { _state.update { it.copy(authenticated = false) } }
}
