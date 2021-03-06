package unitcraft.inject

import unitcraft.server.Err
import unitcraft.server.lzy
import java.util.*
import kotlin.reflect.KClass

val valuesInject = HashMap<KClass<out Any>, Any>()

inline fun <reified T : Any> register(value: T) {
    valuesInject[T::class] = value
}

inline fun <reified T : Any> inject(): Lazy<T> = lzy {
    (valuesInject[T::class] ?: throw Err("no inject ${T::class.simpleName}")) as T
}

inline fun <reified T : Any> injectValue(): T = (valuesInject[T::class] ?: throw Err("no inject ${T::class.simpleName}")) as T

fun clearAllInjects(){
    valuesInject.clear()
}