package com.agroroute.core.runtime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
object BackgroundNetworkAccess {
    private val mutable = MutableStateFlow(true)
    val allowed = mutable.asStateFlow()
    fun pause() { mutable.value = false }
    fun resume() { mutable.value = true }
}
