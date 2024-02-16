package com.viter.keyvaluestore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.viter.keyvaluestore.ui.theme.KeyValueStoreTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.Dialog
import com.viter.presentation_core.DialogState
import com.viter.presentation_core.UIAction
import com.viter.storage.Command

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KeyValueStoreTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    viewModel: AndroidViewModel = viewModel()
) {
    val uiState by viewModel.viewState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(
                horizontal = 10.dp,
                vertical = 10.dp
            )
    ) {
        Column {

            Dropdown(uiState, viewModel = viewModel)

            Spacer(modifier = Modifier.height(5.dp))

            uiState.textFields.forEach { textField ->
                SimpleOutlinedTextFieldSample(textField) { newValue ->
                    viewModel.dispatchActionToStore(
                        AndroidUiAction.TextFieldChanged(
                            newValue,
                            textField.id
                        )
                    )
                }

                Spacer(modifier = Modifier.height(5.dp))
            }

            Button(onClick = {
                viewModel.dispatchActionToStore(AndroidUiAction.Execute)
            }) {
                Text("execute")
            }
            Spacer(modifier = Modifier.height(5.dp))
        }
        Column {
            LazyColumn(
                contentPadding = PaddingValues(8.dp),
                modifier = Modifier.fillMaxHeight()
            ) {
                items(uiState.commonUiState.operationsLog) { item ->
                    Text(text = item)
                    Divider()
                }
            }
        }
    }

    when (val dialogState = uiState.commonUiState.dialogState) {
        DialogState.Hidden -> {

        }

        is DialogState.LastCommandConfirmation -> com.viter.keyvaluestore.Dialog(
            onDismissRequest = {
                viewModel.dispatchActionToStore(
                    AndroidUiAction.CommonAction(
                        UIAction.ApplyCommand(Command.Deny)
                    )
                )
            },
            onConfirmation = {
                viewModel.dispatchActionToStore(
                    AndroidUiAction.CommonAction(
                        UIAction.ApplyCommand(Command.Confirm)
                    )
                )
            },
            message = dialogState.message
        )
    }
}

@Composable
fun SimpleOutlinedTextFieldSample(
    textFiled: TextFiled,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = textFiled.value,
        onValueChange = onValueChange,
        label = { Text(textFiled.hint) },
    )
}


@Composable
fun Dropdown(uiState: AndroidUiState, viewModel: AndroidViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val commandItems = uiState.allCommands.map { it.commandName }
    var selectedIndex by remember { mutableStateOf(uiState.allCommands.indexOf(uiState.selectedCommand)) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentSize(Alignment.TopStart)
            .padding(10.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = { expanded = true })
        ) {
            Text(
                commandItems[selectedIndex],
                modifier = Modifier.padding(8.dp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            commandItems.forEachIndexed { index, s ->
                DropdownMenuItem(
                    onClick = {
                        selectedIndex = index
                        expanded = false
                        viewModel.dispatchActionToStore(
                            AndroidUiAction.SelectCommand(
                                uiState.allCommands[index]
                            )
                        )
                    },
                    text = {
                        Text(text = s)
                    }
                )
            }
        }
    }
}

@Composable
fun Dialog(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
    message: String,
) {
    Dialog(onDismissRequest = { onDismissRequest() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {

                Text(
                    text = message,
                    modifier = Modifier.padding(16.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TextButton(
                        onClick = { onDismissRequest() },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text("Deny")
                    }
                    TextButton(
                        onClick = { onConfirmation() },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}
