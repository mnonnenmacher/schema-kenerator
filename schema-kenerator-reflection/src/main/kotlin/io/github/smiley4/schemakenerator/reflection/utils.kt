package io.github.smiley4.schemakenerator.reflection

import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

inline fun <reified T> getKType(): KType { // todo: remove & replace directly with typeof
    return typeOf<T>()
}


@Suppress("SwallowedException")
fun KClass<*>.getMembersSafe(): Collection<KCallable<*>> { //todo: move to file with usage after removing getKType
    /*
    Throws error for function-types (for unknown reasons). Catch and ignore this error

    Example:
    class MyClass(
        val myField: (v: Int) -> String
    )
    "Unknown origin of public abstract operator fun invoke(p1: P1):
    R defined in kotlin.Function1[FunctionInvokeDescriptor@162989f2]
    (class kotlin.reflect.jvm.internal.impl.builtins.functions.FunctionInvokeDescriptor)"
    */
    return try {
        this.members
    } catch (e: Throwable) {
        emptyList()
    }
}
