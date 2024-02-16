package com.viter.storage


interface Transaction {
    val id: Int
}

interface TransactionManager {

    val globalTransactionId: Int

    fun lastActiveTransaction(): Transaction

    fun hasActiveTransactions(): Boolean

    fun createNew(): Transaction

    fun deleteLastActive()

    fun getParentForLastActive(): Transaction
}

sealed class TransactionResult {
    data object OK : TransactionResult()
    data object ActiveTransactionNotFound : TransactionResult()
}

data class TransactionImpl(override val id: Int) : Transaction

class TransactionManagerImpl : TransactionManager {

    private var transactionCounter = 0;
    private val activeTransactionsIds = mutableListOf<Transaction>()

    private fun newTransactionId(): Int {
        transactionCounter++
        return transactionCounter
    }

    override val globalTransactionId: Int = 0

    private val globalTransaction = TransactionImpl(globalTransactionId)

    override fun lastActiveTransaction(): Transaction =
        activeTransactionsIds.lastOrNull() ?: globalTransaction

    override fun hasActiveTransactions(): Boolean = activeTransactionsIds.isNotEmpty()

    override fun createNew(): Transaction {
        return TransactionImpl(newTransactionId()).also {
            activeTransactionsIds.add(it)
        }
    }

    override fun deleteLastActive() {
        if (activeTransactionsIds.isNotEmpty()) activeTransactionsIds.removeLast()
    }

    /**
     * returns parent transaction id for last active transaction or globalTransactionId
     */
    override fun getParentForLastActive(): Transaction {
        return activeTransactionsIds.getOrNull(activeTransactionsIds.size - 2)
            ?: globalTransaction
    }
}
