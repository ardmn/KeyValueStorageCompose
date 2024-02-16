package com.viter.mvi

interface MiddleWare<S : State, A : Action> {

    suspend fun process(action: A, currentState: S, store: Store<S, A>)

    suspend fun dispatch(store: Store<S, A>, action: A) {
        store.dispatch(action)
    }

}
