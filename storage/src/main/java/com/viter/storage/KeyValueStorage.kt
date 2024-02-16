package com.viter.storage

interface KeyValueStorage<K, V> {

    fun get(key: K): V?

    fun set(key: K, newValue: V)

    fun delete(key: K)

    fun count(searchValue: V): Int

    fun begin(): Transaction

    /**
     * apply changes of last active transaction
     */
    fun commit(): TransactionResult

    /**
     * rollback changes of last active transaction
     */
    fun rollback(): TransactionResult
}