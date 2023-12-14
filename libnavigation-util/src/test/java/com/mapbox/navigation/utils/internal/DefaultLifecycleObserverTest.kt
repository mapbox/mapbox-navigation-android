package com.mapbox.navigation.utils.internal

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Test

class DefaultLifecycleObserverTest {

    private val observer = spyk(
        object : DefaultLifecycleObserver() {

            override fun onStart(owner: LifecycleOwner) {
            }

            override fun onCreate(owner: LifecycleOwner) {
            }

            override fun onResume(owner: LifecycleOwner) {
            }

            override fun onPause(owner: LifecycleOwner) {
            }

            override fun onStop(owner: LifecycleOwner) {
            }

            override fun onDestroy(owner: LifecycleOwner) {
            }
        }
    )
    private val owner = mockk<LifecycleOwner>()

    @Test
    fun onCreate() {
        observer.onStateChanged(owner, Lifecycle.Event.ON_CREATE)

        verify(exactly = 1) {
            observer.onCreate(owner)
        }
        verify(exactly = 0) {
            observer.onStart(any())
            observer.onResume(any())
            observer.onPause(any())
            observer.onStop(any())
            observer.onDestroy(any())
        }
    }

    @Test
    fun onStart() {
        observer.onStateChanged(owner, Lifecycle.Event.ON_START)

        verify(exactly = 1) {
            observer.onStart(owner)
        }
        verify(exactly = 0) {
            observer.onCreate(any())
            observer.onResume(any())
            observer.onPause(any())
            observer.onStop(any())
            observer.onDestroy(any())
        }
    }

    @Test
    fun onResume() {
        observer.onStateChanged(owner, Lifecycle.Event.ON_RESUME)

        verify(exactly = 1) {
            observer.onResume(owner)
        }
        verify(exactly = 0) {
            observer.onStart(any())
            observer.onCreate(any())
            observer.onPause(any())
            observer.onStop(any())
            observer.onDestroy(any())
        }
    }

    @Test
    fun onPause() {
        observer.onStateChanged(owner, Lifecycle.Event.ON_PAUSE)

        verify(exactly = 1) {
            observer.onPause(owner)
        }
        verify(exactly = 0) {
            observer.onStart(any())
            observer.onResume(any())
            observer.onCreate(any())
            observer.onStop(any())
            observer.onDestroy(any())
        }
    }

    @Test
    fun onStop() {
        observer.onStateChanged(owner, Lifecycle.Event.ON_STOP)

        verify(exactly = 1) {
            observer.onStop(owner)
        }
        verify(exactly = 0) {
            observer.onStart(any())
            observer.onResume(any())
            observer.onPause(any())
            observer.onCreate(any())
            observer.onDestroy(any())
        }
    }

    @Test
    fun onDestroy() {
        observer.onStateChanged(owner, Lifecycle.Event.ON_DESTROY)

        verify(exactly = 1) {
            observer.onDestroy(owner)
        }
        verify(exactly = 0) {
            observer.onStart(any())
            observer.onResume(any())
            observer.onPause(any())
            observer.onStop(any())
            observer.onCreate(any())
        }
    }

    @Test
    fun onAny() {
        observer.onStateChanged(owner, Lifecycle.Event.ON_ANY)

        verify(exactly = 0) {
            observer.onCreate(any())
            observer.onStart(any())
            observer.onResume(any())
            observer.onPause(any())
            observer.onStop(any())
            observer.onDestroy(any())
        }
    }
}
