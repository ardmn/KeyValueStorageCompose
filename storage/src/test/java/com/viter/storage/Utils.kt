package com.viter.storage

val KEY_NO_SET = "key not set"
val NO_TRANSACTION = "no transaction"

fun KeyValueStorage<String, String>.applyCommand(command: Command): String? {
    return when (command) {
        Command.Begin -> this.begin().let { null }
        Command.Commit -> {
            when (this.commit()) {
                TransactionResult.ActiveTransactionNotFound -> NO_TRANSACTION
                TransactionResult.OK -> null
            }
        }

        is Command.Count -> this.count(command.value).let { "$it" }
        is Command.Delete -> this.delete(command.key).let { null }

        is Command.Get -> this.get(command.key).let { it ?: KEY_NO_SET}
        Command.Rollback -> {
            when (this.rollback()) {
                TransactionResult.ActiveTransactionNotFound -> NO_TRANSACTION
                TransactionResult.OK -> null
            }
        }

        is Command.Set -> this.set(command.key, command.value).let { null }
        else -> null
    }
}

fun KeyValueStorage<String, String>.applyCommands(commands: List<Command>): List<String> {
    return commands.mapNotNull { this.applyCommand(it) }
}

