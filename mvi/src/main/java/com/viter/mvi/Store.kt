package com.viter.mvi

import kotlinx.coroutines.CoroutineScope

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

class Store<S : State, A : Action>(
	private val initialState : S,
	private val reducer : Reducer<S, A>,
	private val middleWares : List<MiddleWare<S, A>> = emptyList(),
	private val coroutineScope : CoroutineScope
) {

	private val _state : MutableStateFlow<S> = MutableStateFlow(initialState)

	val state : StateFlow<S>
		get() = _state.stateIn(
			scope = coroutineScope,
			started = SharingStarted.WhileSubscribed(),
			initialValue = initialState
		)

	private val currentState : S
		get() = _state.value

	suspend fun dispatch(action : A) {
		middleWares.forEach { middleWare ->
			middleWare.process(action, currentState, this)
		}

		val newState = reducer.reduce(currentState, action)

		_state.value = newState
	}

}
