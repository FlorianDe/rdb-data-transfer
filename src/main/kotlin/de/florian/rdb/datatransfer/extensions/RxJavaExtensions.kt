package de.florian.rdb.datatransfer.extensions

import io.reactivex.subjects.BehaviorSubject
import java.util.*

fun <T> BehaviorSubject<Optional<T>>.getValueOptional(): Optional<T> {
    return synchronized(this) {
        val sourceValue = this.value
        if (sourceValue != null && sourceValue.isPresent) {
            Optional.of(sourceValue.get())
        } else {
            Optional.empty()
        }
    }
}

fun <T> BehaviorSubject<Collection<T>>.getValueOrEmpty(): Collection<T> {
    return synchronized(this) {
        val sourceValue = this.value
        if (sourceValue != null && sourceValue.isNotEmpty()) {
            sourceValue
        } else {
            emptyList()
        }
    }
}