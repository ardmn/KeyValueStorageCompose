package com.viter.storage


data class TransactionValue<V>(
    val transactionId: Int,
    // null is indicator of deleted value for specific key
    val value: V?
)

class ValuesWithTransactions<V> {
    val values = mutableListOf<TransactionValue<V>>()
}


class KeyValueStorageImpl<K, V>(
    private val transactionManager: TransactionManager = TransactionManagerImpl(),
) : KeyValueStorage<K, V> {
    private val data = mutableMapOf<K, ValuesWithTransactions<V>>()

    private val transactionAffectedKeys = mutableMapOf<Int, MutableSet<K>>()

    private fun markKeyForTransaction(transactionId: Int, key: K) {
        var keysSet = transactionAffectedKeys[transactionId]
        if (keysSet == null) {
            keysSet = mutableSetOf()
            transactionAffectedKeys[transactionId] = keysSet
        }
        keysSet.add(key)
    }

    private fun V?.asGlobalTransaction(): TransactionValue<V> =
        this.asTransactionValue(transactionManager.globalTransactionId)

    private fun V?.asCurrentTransaction(): TransactionValue<V> =
        this.asTransactionValue(transactionManager.lastActiveTransaction().id)

    private fun V?.asTransactionValue(transactionId: Int): TransactionValue<V> = TransactionValue(
        transactionId = transactionId,
        value = this
    )

    private fun deletingMarker(): TransactionValue<V> {
        return null.transactionDependentValue()
    }

    private fun V?.transactionDependentValue(): TransactionValue<V> {
        return if (transactionManager.hasActiveTransactions()) {
            this.asCurrentTransaction()
        } else {
            this.asGlobalTransaction()
        }
    }

    private fun getOrCreateValuesStore(key: K): ValuesWithTransactions<V> {
        return data[key] ?: ValuesWithTransactions<V>().also { data[key] = it }
    }

    override fun set(key: K, newValue: V) {
        if (transactionManager.hasActiveTransactions()) {
            markKeyForTransaction(transactionManager.lastActiveTransaction().id, key)
        }
        val currentValuesForKey = getOrCreateValuesStore(key)
        val lastValueForKey: TransactionValue<V>? = currentValuesForKey.values.lastOrNull()
        if (transactionManager.lastActiveTransaction().id == lastValueForKey?.transactionId) {
            // replace
            currentValuesForKey.values[currentValuesForKey.values.lastIndex] =
                newValue.transactionDependentValue()
        } else {
            // add new version
            currentValuesForKey.values.add(newValue.transactionDependentValue())
        }
    }

    override fun get(key: K): V? {
        return data[key]?.values?.lastOrNull()?.value
    }

    override fun delete(key: K) {
        if (transactionManager.hasActiveTransactions()) {
            markKeyForTransaction(transactionManager.lastActiveTransaction().id, key)
        }
        if (transactionManager.hasActiveTransactions()) {
            val currentValuesForKey: ValuesWithTransactions<V>? = data[key]
            // if we have no any currentValueForKey for key then we did not have any value for this key even by any previous transaction
            if (currentValuesForKey != null) {
                val lastValueForKey: TransactionValue<V>? = currentValuesForKey.values.lastOrNull()
                if (lastValueForKey == null) {
                    currentValuesForKey.values.add(deletingMarker())
                } else {
                    if (transactionManager.lastActiveTransaction().id == lastValueForKey.transactionId) {
                        // replace
                        currentValuesForKey.values.set(
                            currentValuesForKey.values.size - 1,
                            deletingMarker()
                        )
                    } else {
                        currentValuesForKey.values.add(deletingMarker())
                    }
                }
            }
        } else {
            // just remove from global store
            data.remove(key)
        }
    }

    override fun begin(): Transaction {
        return transactionManager.createNew()
    }

    override fun rollback(): TransactionResult {
        return if (transactionManager.hasActiveTransactions()) {
            val activeTransactionId = transactionManager.lastActiveTransaction().id
            transactionAffectedKeys[activeTransactionId]?.forEach { affectedKey ->
                data[affectedKey]?.values?.let { valuesForKey ->
                    if (activeTransactionId == valuesForKey.lastOrNull()?.transactionId) {
                        valuesForKey.removeLast()
                    }
                    if (valuesForKey.isEmpty()) {
                        data.remove(affectedKey)
                    }
                }
            }
            transactionManager.deleteLastActive()
            TransactionResult.OK
        } else {
            TransactionResult.ActiveTransactionNotFound
        }
    }

    override fun commit(): TransactionResult {
        return if (transactionManager.hasActiveTransactions()) {
            val activeTransactionId = transactionManager.lastActiveTransaction().id
            transactionAffectedKeys[activeTransactionId]?.forEach { affectedKey ->
                data[affectedKey]?.values?.let { valuesForKey ->
                    if (activeTransactionId == valuesForKey.lastOrNull()?.transactionId) {
                        // если есть родительская транзакция то нужно ИД транзакции поменять на родительскую или поменять значение родительской транзакции на текущее
                        val parentTransactionId = transactionManager.getParentForLastActive().id
                        val currentTransactionValue = valuesForKey.removeLast()
                        if (parentTransactionId == transactionManager.globalTransactionId) {
                            val newValue = currentTransactionValue.value.asGlobalTransaction()
                            if (valuesForKey.size > 0) {
                                valuesForKey[0] = newValue
                            } else {
                                valuesForKey.add(newValue)
                            }
                            if (currentTransactionValue.value == null) {
                                data.remove(affectedKey) // delete key
                            }
                        } else {
                            valuesForKey[valuesForKey.size - 1] =
                                currentTransactionValue.value.asTransactionValue(parentTransactionId)
                        }
                    }
                }
            }
            transactionManager.deleteLastActive()
            TransactionResult.OK
        } else {
            TransactionResult.ActiveTransactionNotFound
        }
    }

    override fun count(searchValue: V): Int {
        return data.filter { (_, value) ->
            value.values.lastOrNull()?.value == searchValue
        }.size
    }
}