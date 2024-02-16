package com.viter.keyvaluestore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.viter.mvi.Action
import com.viter.mvi.Reducer
import com.viter.mvi.State
import com.viter.mvi.Store
import com.viter.presentation_core.UIAction
import com.viter.presentation_core.UIState
import com.viter.presentation_core.UserActionReducer
import com.viter.storage.CommandNames
import com.viter.storage.toCommand
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AndroidViewModel : ViewModel() {

    private val androidStore: Store<AndroidUiState, AndroidUiAction> = Store(
        initialState = AndroidUiState(),
        reducer = AndroidActionReducer(),
        coroutineScope = viewModelScope
    )

    val viewState: StateFlow<AndroidUiState>
        get() = androidStore.state

    fun dispatchActionToStore(
        action: AndroidUiAction,
        dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate
    ) {
        viewModelScope.launch(dispatcher) {
            androidStore.dispatch(action)
        }
    }
}

data class TextFiled(
    val id: Int,
    val hint: String,
    val visible: Boolean,
    val value: String = ""
)

data class AndroidUiState(
    val commonUiState: UIState = UIState(),
    val selectedCommand: CommandNames = CommandNames.SET,
    val allCommands: List<CommandNames> = listOf(
        CommandNames.SET,
        CommandNames.GET,
        CommandNames.COUNT,
        CommandNames.DELETE,
        CommandNames.BEGIN,
        CommandNames.COMMIT,
        CommandNames.ROLLBACK,
    ),
    val textFields: List<TextFiled> = listOf(
        TextFiled(1, "key", true),
        TextFiled(2, "value", true)
    ),
) : State

sealed class AndroidUiAction : Action {
    data class CommonAction(val action: UIAction) : AndroidUiAction()
    data class TextFieldChanged(val text: String, val id: Int) : AndroidUiAction()
    data class SelectCommand(val commandName: CommandNames) : AndroidUiAction()
    data object Execute : AndroidUiAction()
}

class AndroidActionReducer(
    private val baseReducer: Reducer<UIState, UIAction> = UserActionReducer()
) : Reducer<AndroidUiState, AndroidUiAction> {

    override fun reduce(currentState: AndroidUiState, action: AndroidUiAction): AndroidUiState {
        return when (action) {
            is AndroidUiAction.CommonAction -> {
                currentState.copy(
                    commonUiState = baseReducer.reduce(
                        currentState.commonUiState,
                        action.action
                    )
                )
            }

            is AndroidUiAction.TextFieldChanged -> {
                val newTextFieldsState = currentState.textFields.map {
                    if (it.id == action.id) {
                        it.copy(value = action.text)
                    } else {
                        it
                    }
                }
                currentState.copy(
                    textFields = newTextFieldsState
                )
            }

            is AndroidUiAction.SelectCommand -> {
                val newTextFields = when (action.commandName) {
                    CommandNames.SET -> listOf(
                        TextFiled(1, "key", true),
                        TextFiled(2, "value", true)
                    )

                    CommandNames.GET -> listOf(
                        TextFiled(3, "key", true),
                    )

                    CommandNames.DELETE -> listOf(
                        TextFiled(4, "key", true),
                    )

                    CommandNames.COUNT -> listOf(
                        TextFiled(5, "value", true),
                    )

                    else -> listOf()
                }
                currentState.copy(
                    selectedCommand = action.commandName,
                    textFields = newTextFields,
                )
            }

            AndroidUiAction.Execute -> {
                val stringBuilder = StringBuilder()
                stringBuilder.append(currentState.selectedCommand.commandName)
                currentState.textFields.forEach {
                    stringBuilder.append(" ")
                    stringBuilder.append(it.value)
                }

                val command = stringBuilder.toString().toCommand()

                currentState.copy(
                    commonUiState = baseReducer.reduce(
                        currentState.commonUiState,
                        UIAction.ApplyCommand(command)
                    )
                )
            }
        }
    }
}
