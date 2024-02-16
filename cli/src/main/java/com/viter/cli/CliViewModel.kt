package com.viter.cli

import com.viter.mvi.Store
import com.viter.presentation_core.UIAction
import com.viter.presentation_core.UIState
import com.viter.presentation_core.UserActionReducer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.Closeable
import kotlin.coroutines.CoroutineContext

class CliViewModel {

    class CloseableCoroutineScope(context: CoroutineContext) : Closeable, CoroutineScope {
        override val coroutineContext: CoroutineContext = context

        override fun close() {
            coroutineContext.cancel()
        }
    }

    private val viewModelScope: CoroutineScope =
        CloseableCoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val store: Store<UIState, UIAction> = Store(
        initialState = UIState(),
        reducer = UserActionReducer(),
        coroutineScope = viewModelScope
    )

    val viewState: StateFlow<UIState>
        get() = store.state

    fun dispatchActionToStore(
        action: UIAction,
        dispatcher: CoroutineDispatcher = Dispatchers.IO
    ) {
        viewModelScope.launch(dispatcher) {
            store.dispatch(action)
        }
    }
}