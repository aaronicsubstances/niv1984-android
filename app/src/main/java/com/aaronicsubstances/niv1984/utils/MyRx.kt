package com.aaronicsubstances.niv1984.utils

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.*

interface EventReceiver<Message> {
    val eventFlow: Flow<Message>
}

interface EventSender<Message> {
    fun postEvent(message: Message): Boolean
    val initialMessage: Message?
}

class LifecycleEventSender<Message>(
    lifecycle: Lifecycle,
    private val coroutineScope: CoroutineScope,
    private val channel: BroadcastChannel<Message>,
    override val initialMessage: Message?
) : EventSender<Message>, LifecycleObserver {

    init {
        lifecycle.addObserver(this)
    }

    override fun postEvent(message: Message): Boolean {
        if (channel.isClosedForSend) {
            return false
        }
        coroutineScope.launch { channel.send(message) }
        return true
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun create() {
        channel.openSubscription()
        initialMessage?.let { postEvent(it) }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun destroy() {
        channel.close()
    }
}

class ChannelEventReceiver<Message>(channel: BroadcastChannel<Message>) :
    EventReceiver<Message> {
    override val eventFlow: Flow<Message> = channel.asFlow()
}

abstract class EventRelay<Message>(
    lifecycle: Lifecycle,
    coroutineScope: CoroutineScope,
    channel: BroadcastChannel<Message>,
    initialMessage: Message? = null
) : EventReceiver<Message> by ChannelEventReceiver<Message>(channel),
    EventSender<Message> by LifecycleEventSender<Message>(
        lifecycle,
        coroutineScope,
        channel,
        initialMessage
    )

class BehaviorSubject<T>(
    lifecycle: Lifecycle,
    coroutineScope: CoroutineScope,
    initialMessage: T?
) : EventRelay<T>(
    lifecycle,
    coroutineScope,
    ConflatedBroadcastChannel(),
    initialMessage
)

class PublishSubject<T>(
    lifecycle: Lifecycle,
    coroutineScope: CoroutineScope,
    initialMessage: T?
) : EventRelay<T>(
    lifecycle,
    coroutineScope,
    BroadcastChannel(Channel.BUFFERED),
    initialMessage
)