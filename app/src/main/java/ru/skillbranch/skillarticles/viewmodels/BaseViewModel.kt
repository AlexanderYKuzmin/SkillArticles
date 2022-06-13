package ru.skillbranch.skillarticles.viewmodels

import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.*
import java.lang.IllegalArgumentException
import java.lang.RuntimeException

abstract class BaseViewModel<T>(initState: T): ViewModel() {
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    val notifications = MutableLiveData<Event<Notify>>()

    /***
     * Инициализация начального состояния аргументом конструктоа, и объявления состояния как
     * MediatorLiveData - медиатор исспользуется для того чтобы учитывать изменяемые данные модели
     * и обновлять состояние ViewModel исходя из полученных данных
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    val state: MediatorLiveData<T> = MediatorLiveData<T>().apply {
        value = initState
    }

    /***
     * getter для получения not null значения текущего состояния ViewModel
     */
    //not null current state
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    val currentState
        get() = state.value!!

    /***
     * лямбда выражение принимает в качестве аргумента текущее состояние и возвращает
     * модифицированное состояние, которое присваивается текущему состоянию
     */
    @UiThread
    protected inline fun updateState(update: (currentState: T) -> T) {
        val updatedState: T = update(currentState)
        state.value = updatedState
    }

    @UiThread
    protected fun notify(content: Notify) {
        notifications.value = Event(content)
    }

    /***
     * функция для создания уведомления пользователя о событии (событие обрабатывается только один раз)
     * соответсвенно при изменении конфигурации и пересоздании Activity уведомление не будет вызвано
     * повторно
     */
    fun observeState(owner: LifecycleOwner, onChanged: (newState: T) -> Unit) {
        state.observe(owner, Observer { onChanged(it!!) })
    }

    fun observeNotification(owner: LifecycleOwner, onNotify: (notification: Notify) -> Unit) {
        notifications.observe(owner, EventObserver { onNotify(it) })
    }

    protected fun<S> subscribeOnDataSource(
        source: LiveData<S>,
        onChanged: (newValue: S, currentState: T) -> T?
    ) {
        state.addSource(source) {
            state.value = onChanged(it, currentState) ?: return@addSource
        }
    }
}

class ViewModelFactory(private val params: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ArticleViewModel::class.java)) {
            return ArticleViewModel(params) as T
        } else {
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

class Event<out E>(private val content: E) {
    var hasBeenHandled = false

    fun getContentIfNotHandled(): E? {
        return if (hasBeenHandled) null
        else {
            hasBeenHandled = true
            content
        }
    }

    fun peekContent(): E = content
}

class EventObserver<E>(private val onEventUnhandledContent: (E) -> Unit): Observer<Event<E>> {

    override fun onChanged(event: Event<E>?) {
        event?.getContentIfNotHandled()?.let {
            onEventUnhandledContent(it)
        }
    }
}



sealed class Notify(val message: String) {
    data class TextMessage(val msg: String): Notify(msg)

    data class ActionMessage(
        val msg: String,
        val actionLabel: String,
        val actionHandler: (() -> Unit)
    ) : Notify(msg)

    data class ErrorMessage(
        val msg: String,
        val errLabel: String?,
        val errHandler: (() -> Unit)?
    ) : Notify(msg)
}