package com.viter.storage

enum class CommandNames(val commandName: String) {
    SET("set"),
    GET("get"),
    DELETE("delete"),
    COUNT("count"),
    BEGIN("begin"),
    COMMIT("commit"),
    ROLLBACK("rollback"),
    EXIT("q"),
    ERROR("error"),
    YES("y"),
    NO("n"),
}

sealed class Command(val name: String) {

    data class Set(val key: String, val value: String) : Command(CommandNames.SET.commandName)

    data class Get(val key: String) : Command(CommandNames.GET.commandName)

    data class Delete(val key: String) : Command(CommandNames.DELETE.commandName)

    data class Count(val value: String) : Command(CommandNames.COUNT.commandName)

    data object Begin : Command(CommandNames.BEGIN.commandName)

    data object Commit : Command(CommandNames.COMMIT.commandName)

    data object Rollback : Command(CommandNames.ROLLBACK.commandName)

    data object Exit : Command(CommandNames.EXIT.commandName)

    data class Error(val message: String? = null) : Command(CommandNames.ERROR.commandName)

    data object Confirm : Command(CommandNames.YES.commandName)

    data object Deny : Command(CommandNames.NO.commandName)
}

fun String.toCommand(): Command {
    val commandComponents = this.split("\\s+".toRegex())
    return if (commandComponents.isNotEmpty()) {
        val commandName = CommandNames.entries.firstOrNull {
            it.commandName == commandComponents[0].trim()
        }
        if (commandName == null) {
            Command.Error("Unknown command.")
        } else {
            when (commandName) {
                CommandNames.SET -> {
                    safeCommandCreatorOrError { Command.Set(commandComponents[1], commandComponents[2]) }
                }

                CommandNames.GET -> {
                    safeCommandCreatorOrError { Command.Get(commandComponents[1]) }
                }


                CommandNames.DELETE -> {
                    safeCommandCreatorOrError { Command.Delete(commandComponents[1]) }
                }

                CommandNames.COUNT -> {
                    safeCommandCreatorOrError { Command.Count(commandComponents[1]) }
                }

                CommandNames.BEGIN -> {
                    safeCommandCreatorOrError { Command.Begin }
                }

                CommandNames.COMMIT -> {
                    safeCommandCreatorOrError { Command.Commit }
                }

                CommandNames.ROLLBACK -> {
                    safeCommandCreatorOrError { Command.Rollback }
                }

                CommandNames.EXIT -> {
                    safeCommandCreatorOrError { Command.Exit }
                }

                CommandNames.ERROR -> {
                    safeCommandCreatorOrError { Command.Error(commandComponents[1]) }
                }

                CommandNames.YES -> {
                    Command.Confirm
                }

                CommandNames.NO -> {
                    Command.Deny
                }
            }
        }
    } else {
        Command.Error("Looks like have no command. Please enter command.")
    }
}

fun safeCommandCreatorOrError(block: () -> Command): Command {
    return try {
        block()
    } catch (e: Exception) {
        Command.Error("Wrong command format.")
    }
}





