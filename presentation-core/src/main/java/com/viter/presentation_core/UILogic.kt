package com.viter.presentation_core

import com.viter.mvi.Action
import com.viter.mvi.Reducer
import com.viter.mvi.State
import com.viter.storage.Command
import com.viter.storage.KeyValueStorage
import com.viter.storage.KeyValueStorageImpl
import com.viter.storage.TransactionManagerImpl
import com.viter.storage.TransactionResult


data class UIState(
    val keyValueStorage: KeyValueStorage<String, String> = KeyValueStorageImpl(
        TransactionManagerImpl()
    ),
    val pendingUserCommand: Command? = null,
    val dialogState: DialogState = DialogState.Hidden,
    val operationsLog: MutableList<String> = mutableListOf(),
    val stateVersion: Int = 0,
) : State

sealed class DialogState {
    data object Hidden : DialogState()
    data class LastCommandConfirmation(val message: String) : DialogState()
}

sealed class UIAction : Action {
    data class ApplyCommand(val command: Command) : UIAction()
}

class UserActionReducer : Reducer<UIState, UIAction> {

    override fun reduce(currentState: UIState, action: UIAction): UIState {
        return when (action) {
            is UIAction.ApplyCommand -> onCommand(currentState, action.command)
        }
    }

    private fun onCommand(currentState: UIState, command: Command): UIState {
        return when (command) {
            Command.Begin -> {
                val transaction = currentState.keyValueStorage.begin()
                currentState.copy(
                    stateVersion = currentState.stateVersion + 1,
                    operationsLog = keepLast_15_elements(
                        currentState.operationsLog.apply {
                            add("${command.name} transaction with id = ${transaction.id}")
                        }
                    )
                )
            }

            Command.Rollback, Command.Commit -> {
                currentState.copy(
                    stateVersion = currentState.stateVersion + 1,
                    pendingUserCommand = command,
                    dialogState = DialogState.LastCommandConfirmation(
                        message = "are you sure you want to perform the operation ${command.name} ? (y/n):"
                    ),
                    operationsLog = keepLast_15_elements(
                        currentState.operationsLog.apply {
                            add("Try to ${command.name} last transaction.")
                        }
                    )
                )
            }

            Command.Confirm -> {
                val transactionResult: TransactionResult? = when (currentState.pendingUserCommand) {
                    Command.Commit -> currentState.keyValueStorage.commit()
                    Command.Rollback -> currentState.keyValueStorage.rollback()
                    else -> {
                        null
                    }
                }
                currentState.copy(
                    stateVersion = currentState.stateVersion + 1,
                    pendingUserCommand = null,
                    dialogState = DialogState.Hidden,
                    operationsLog = keepLast_15_elements(
                        currentState.operationsLog.apply {
                            add("${currentState.pendingUserCommand?.name} result: $transactionResult")
                        }
                    )
                )
            }

            Command.Deny -> {
                currentState.copy(
                    stateVersion = currentState.stateVersion + 1,
                    pendingUserCommand = null,
                    dialogState = DialogState.Hidden,
                    operationsLog = keepLast_15_elements(
                        currentState.operationsLog.apply {
                            add("Deny.")
                        }
                    )
                )
            }

            is Command.Count -> {
                currentState.copy(
                    stateVersion = currentState.stateVersion + 1,
                    operationsLog = keepLast_15_elements(
                        currentState.operationsLog.apply {
                            add("${currentState.keyValueStorage.count(command.value)}")
                        }
                    )
                )
            }

            is Command.Delete -> {
                currentState.copy(
                    stateVersion = currentState.stateVersion + 1,
                    operationsLog = keepLast_15_elements(
                        currentState.operationsLog.apply {
                            currentState.keyValueStorage.delete(command.key)
                            add("Deleted.")
                        }
                    )
                )
            }

            is Command.Get -> {
                currentState.copy(
                    stateVersion = currentState.stateVersion + 1,
                    operationsLog = keepLast_15_elements(
                        currentState.operationsLog.apply {
                            val receivedValue = currentState.keyValueStorage.get(command.key)
                            if (receivedValue == null) {
                                add("key not set.")
                            } else {
                                add("$receivedValue")
                            }
                        }
                    )
                )
            }

            is Command.Set -> {
                currentState.copy(
                    stateVersion = currentState.stateVersion + 1,
                    operationsLog = keepLast_15_elements(
                        currentState.operationsLog.apply {
                            currentState.keyValueStorage.set(command.key, command.value)
                            add("Value accepted.")
                        }
                    )
                )
            }

            is Command.Error -> {
                currentState.copy(
                    stateVersion = currentState.stateVersion + 1,
                    operationsLog = keepLast_15_elements(
                        currentState.operationsLog.apply {
                            add("Error:${command.message}.")
                        }
                    )
                )
            }


            else -> {
                currentState
            }
        }
    }

    private fun keepLast_15_elements(list: MutableList<String>): MutableList<String> {
        while (list.size > 15) list.removeFirst()
        return list
    }
}