package com.viter.mvi

interface Reducer<S : State, A : Action> {
    fun reduce(currentState: S, action: A): S
}
