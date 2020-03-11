package de.florian.rdb.datatransfer.controller

import de.florian.rdb.datatransfer.model.Connection
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.parse
import kotlinx.serialization.stringify
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.FileWriter
import java.nio.file.FileSystems
import java.nio.file.Path

@OptIn(kotlinx.serialization.ImplicitReflectionSerializer::class)
class StorageService {
    private val log = LoggerFactory.getLogger(javaClass)
    private val aes = AESService("1234567890qwertz") // TODO: LET THE USER CHOOSE A PW

    @Serializable
    private data class ConnectionStorage(val connections: Collection<Connection>)

    companion object {
        val TEMP_DIR = System.getProperty("java.io.tmpdir")
        const val CONNECTIONS_FILE_NAME = "connections.json"
    }

    private val jsonParser = Json(JsonConfiguration.Stable.copy())

    fun saveConnections(connections: Collection<Connection>) {
        FileWriter(getStorageFile(CONNECTIONS_FILE_NAME)).use {
            val json = jsonParser.stringify(
                ConnectionStorage(
                    connections.map { con -> con.copy(password = aes.encrypt(con.password)!!) }
                )
            )
            it.write(json)
        }
    }

    fun retrieveConnections(): Collection<Connection> {
        return try {
            FileReader(getStorageFile(CONNECTIONS_FILE_NAME)).use {
                try {
                    val connections = jsonParser.parse<ConnectionStorage>(it.readText()).connections
                    connections.map { con -> con.copy(password = aes.decrypt(con.password)!!) }
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

    private fun getStorageFile(identifier: String): File {
        val file = FileSystems.getDefault().getPath(TEMP_DIR, "rdb-data-transfer", identifier).toFile()
        if (!file.exists()) {
            file.parentFile.mkdirs()
        }
        return file
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