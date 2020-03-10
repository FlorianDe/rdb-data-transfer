package de.florian.rdb.datatransfer.controller

import de.florian.rdb.datatransfer.model.Connection
import de.florian.rdb.datatransfer.model.DMModel
import de.florian.rdb.datatransfer.view.DMView
import io.reactivex.subjects.BehaviorSubject
import java.util.*

class DMController(private var modelField: DMModel) {
    val model get() = modelField

    var storageService = StorageService()
        private set

    var view: DMView? = null
        private set

    fun setView(dmView: DMView) {
        view = dmView
    }

    private fun <T> getOptionalObservableValue(obs: BehaviorSubject<Optional<T>>): Optional<T> {
        return synchronized(obs) {
            val sourceValue = obs.value
            if (sourceValue != null && sourceValue.isPresent) {
                Optional.of(sourceValue.get())
            } else {
                Optional.empty()
            }
        }
    }

    fun getSourceConnection(): Optional<Connection> {
        return getOptionalObservableValue(this.model.sourceConnection)
    }

    fun getTargetConnection(): Optional<Connection> {
        return getOptionalObservableValue(this.model.targetConnection)
    }
}