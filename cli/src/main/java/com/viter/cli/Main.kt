package com.viter.cli

import com.viter.presentation_core.DialogState
import com.viter.presentation_core.UIAction
import com.viter.storage.Command
import com.viter.storage.toCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Scanner

/**
 * type 'q' to exit
 */
fun main() = runBlocking<Unit> {

    val scanner = Scanner(System.`in`)

    val viewModel = CliViewModel()

    launch(Dispatchers.IO) {
        viewModel.viewState.onEach {
            when (val dialogState = it.dialogState) {
                DialogState.Hidden -> it.operationsLog.lastOrNull()?.let { log -> println(log) }
                is DialogState.LastCommandConfirmation -> println(dialogState.message)
            }
        }.collect()
    }

    launch {
        while (scanner.hasNextLine()) {
            delay(100)
            val inputText = scanner.nextLine()
            val command = inputText.toCommand()
            if (command is Command.Exit) {
                this@runBlocking.cancel()
                break
            }
            viewModel.dispatchActionToStore(UIAction.ApplyCommand(command))
        }
    }
}
