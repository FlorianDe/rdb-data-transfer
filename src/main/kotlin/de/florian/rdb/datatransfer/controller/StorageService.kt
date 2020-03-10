package de.florian.rdb.datatransfer.controller

import de.florian.rdb.datatransfer.model.Connection
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.parse
import kotlinx.serialization.stringify
import org.slf4j.LoggerFactory
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.FileWriter

@OptIn(kotlinx.serialization.ImplicitReflectionSerializer::class)
class StorageService {
    private val log = LoggerFactory.getLogger(javaClass)

    @Serializable
    private data class ConnectionStorage(val connections: Collection<Connection>)

    companion object {
        const val CONNECTIONS_FILE_NAME = "connections.json"
    }

    private val jsonParser = Json(JsonConfiguration.Stable.copy())

    fun saveConnections(connections: Collection<Connection>) {
        FileWriter(CONNECTIONS_FILE_NAME).use {
            val json = jsonParser.stringify(
                ConnectionStorage(
                    connections
                )
            )
            it.write(json)
        }
    }

    fun retrieveConnections(): Collection<Connection> {
        return try {
            FileReader(CONNECTIONS_FILE_NAME).use {
                try {
                    jsonParser.parse<ConnectionStorage>(it.readText()).connections
                } catch (e: Exception) {
                    log.debug("The connections file was corrupted or not parsable due to version conflicts, returning empty list.")
                    emptyList()
                }
            }
        } catch (e: FileNotFoundException) {
            log.debug("No connections saved before, returning empty list.")
            emptyList()
        }
    }
}

fun main() {
    val con1 = Connection(
        "localhost",
        1433,
        "SA",
        "PW",
        "db",
        "jdbc://localhost:1433?database=db",
        "SQLDRIVER",
        "testname1",
        "testcomment"
    )

    val con2 = con1.copy(name = "testname2")

    val ss = StorageService()
    ss.retrieveConnections()
    ss.saveConnections(listOf(con1, con2))
    println(ss.retrieveConnections())
}