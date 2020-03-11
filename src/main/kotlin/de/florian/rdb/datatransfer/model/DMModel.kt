package de.florian.rdb.datatransfer.model

import io.reactivex.subjects.BehaviorSubject
import kotlinx.serialization.Serializable
import java.util.*

data class DB(val name: String, var schemas: MutableList<Schema>)
data class Schema(val name: String, var schemas: MutableList<Table>)
data class Table(val name: String, val columns: MutableList<Column>, var records: Long)
data class Column(val name: String, val type: String)

class DMModel {
    enum class DatabaseType {
        MSSQL
    }

    var selectedDatabaseType = DatabaseType.MSSQL

    var connections: BehaviorSubject<Collection<Connection>> = BehaviorSubject.create()
        private set

    var sourceConnection: BehaviorSubject<Optional<Connection>> = BehaviorSubject.createDefault(Optional.empty())
        private set

    var targetConnection: BehaviorSubject<Optional<Connection>> = BehaviorSubject.createDefault(Optional.empty())
        private set

    var database: BehaviorSubject<Optional<DB>> = BehaviorSubject.createDefault(Optional.empty())
        private set
}

@Serializable
data class Connection(
    var host: String,
    var port: Int = 0,

    var user: String,
    var password: String,
    var database: String,
    var jdbcUrl: String,
    var driverClassName: String,

    var name: String? = "",
    var comment: String? = ""


) {
    companion object {
        fun template() = Connection(
            host = "",
            port = 1433,
            user = "",
            password = "",
            database = "",
            jdbcUrl = "",
            driverClassName = "",
            name = "",
            comment = ""
        )
    }

    override fun toString(): String {
        return when {
            !this.name.isNullOrBlank() -> this.name!!
            !this.user.isBlank() && !this.host.isBlank() -> "$user@$host"
            else -> "Noname"
        }
    }
}
