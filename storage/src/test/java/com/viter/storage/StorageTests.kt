package com.viter.storage

import org.junit.Assert
import org.junit.Test

class StorageTests {

    @Test
    fun test_1() {
        val storage: KeyValueStorage<String, String> = KeyValueStorageImpl()
        val actualResults = storage.applyCommands(
            listOf(
                Command.Set("foo", "123"),
                Command.Get("foo"),
            )
        )
        val expectedCommandsResults = listOf(
            "123"
        )
        Assert.assertEquals(expectedCommandsResults, actualResults)
    }

    @Test
    fun test_2() {
        val storage: KeyValueStorage<String, String> = KeyValueStorageImpl()
        val actualResults = storage.applyCommands(
            listOf(
                Command.Set("foo", "123"),
                Command.Get("foo"),
                Command.Delete("foo"),
                Command.Get("foo"),
            )
        )
        val expectedCommandsResults = listOf(
            "123",
            KEY_NO_SET
        )
        Assert.assertEquals(expectedCommandsResults, actualResults)
    }

    @Test
    fun test_3() {
        val storage: KeyValueStorage<String, String> = KeyValueStorageImpl()
        val actualResults = storage.applyCommands(
            listOf(
                Command.Set("foo", "123"),
                Command.Set("bar", "456"),
                Command.Set("baz", "123"),
                Command.Count("123"),
                Command.Count("456"),
            )
        )
        val expectedCommandsResults = listOf(
            "2",
            "1"
        )
        Assert.assertEquals(expectedCommandsResults, actualResults)
    }

    @Test
    fun test_4() {
        val storage: KeyValueStorage<String, String> = KeyValueStorageImpl()
        val actualResults = storage.applyCommands(
            listOf(
                Command.Set("bar", "123"),
                Command.Get("bar"),
                Command.Begin,
                Command.Set("foo", "456"),
                Command.Get("bar"),
                Command.Delete("bar"),
                Command.Commit,
                Command.Get("bar"),
                Command.Rollback,
                Command.Get("foo"),
            )
        )
        val expectedCommandsResults = listOf(
            "123",
            "123",
            KEY_NO_SET,
            NO_TRANSACTION,
            "456"

        )
        Assert.assertEquals(expectedCommandsResults, actualResults)
    }

    @Test
    fun test_5() {
        val storage: KeyValueStorage<String, String> = KeyValueStorageImpl()
        val actualResults = storage.applyCommands(
            listOf(
                Command.Set("foo", "123"),
                Command.Set("bar", "abc"),
                Command.Begin,
                Command.Set("foo", "456"),
                Command.Get("foo"),
                Command.Set("bar", "def"),
                Command.Get("bar"),
                Command.Rollback,
                Command.Get("foo"),
                Command.Get("bar"),
                Command.Commit
            )
        )
        val expectedCommandsResults = listOf(
            "456",
            "def",
            "123",
            "abc",
            NO_TRANSACTION
        )
        Assert.assertEquals(expectedCommandsResults, actualResults)
    }

    @Test
    fun test_6() {
        val storage: KeyValueStorage<String, String> = KeyValueStorageImpl()
        val actualResults = storage.applyCommands(
            listOf(
                Command.Set("foo", "123"),
                Command.Set("bar", "456"),
                Command.Begin,
                Command.Set("foo", "456"),
                Command.Begin,
                Command.Count("456"),
                Command.Get("foo"),
                Command.Set("foo", "789"),
                Command.Get("foo"),
                Command.Rollback,
                Command.Get("foo"),
                Command.Delete("foo"),
                Command.Get("foo"),
                Command.Rollback,
                Command.Get("foo"),
            )
        )
        val expectedCommandsResults = listOf(
            "2",
            "456",
            "789",
            "456",
            KEY_NO_SET,
            "123"
        )
        Assert.assertEquals(expectedCommandsResults, actualResults)
    }
}
