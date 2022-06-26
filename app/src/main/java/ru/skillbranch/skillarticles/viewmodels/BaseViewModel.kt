package ru.skillbranch.skillarticles.viewmodels

import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import androidx.core.os.bundleOf
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistryOwner
import java.lang.IllegalArgumentException
import java.lang.RuntimeException

abstract class BaseViewModel<T>(initState: T, private val savedStateHandle: SavedStateHandle) : ViewModel() {
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
        state.value = updatedState!!
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

    fun <D> observeSubState(owner: LifecycleOwner, transform: (T) -> D, onChanged: (substate: D) -> Unit) {
        state
            .map(transform)
            .distinctUntilChanged()
            .observe(owner, Observer { onChanged(it!!) })
    }

    fun observeNotification(owner: LifecycleOwner, onNotify: (notification: Notify) -> Unit) {
        notifications.observe(owner, EventObserver { onNotify(it) })
    }

    protected fun <S> subscribeOnDataSource(
        source: LiveData<S>,
        onChanged: (newValue: S, currentState: T) -> T?
    ) {
        state.addSource(source) {
            state.value = onChanged(it, currentState) ?: return@addSource
        }
    }

    // save state to bundle
    fun saveState() {
        savedStateHandle.set("state", currentState)
    }


    fun restoreState() {
        val restoredState = savedStateHandle.get<T>("state") //под капотом unchecked cast и, если вернется не тип не Т, то никакого исключения не будет
        restoredState ?: return
        state.value = restoredState!!
    }
}

class ViewModelFactory(owner: SavedStateRegistryOwner, private val params: String) : AbstractSavedStateViewModelFactory(owner, bundleOf()) {

    override fun <T : ViewModel?> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle
    ): T {
        if (modelClass.isAssignableFrom(ArticleViewModel::class.java)) {
            return ArticleViewModel(params, handle) as T
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

class EventObserver<E>(private val onEventUnhandledContent: (E) -> Unit) : Observer<Event<E>> {

    override fun onChanged(event: Event<E>?) {
        event?.getContentIfNotHandled()?.let {
            onEventUnhandledContent(it)
        }
    }
}


sealed class Notify(val message: String) {
    data class TextMessage(val msg: String) : Notify(msg)

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