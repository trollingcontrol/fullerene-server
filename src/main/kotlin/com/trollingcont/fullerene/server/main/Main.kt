package com.trollingcont.fullerene.server.main

import com.google.gson.GsonBuilder
import com.google.gson.JsonSerializer
import com.trollingcont.fullerene.server.model.ServerConfig
import org.apache.log4j.PropertyConfigurator
import org.jetbrains.exposed.sql.Database
import java.io.File
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.system.exitProcess

fun main() {
    PropertyConfigurator.configure(loggerPropertiesFileName)

    val dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimePattern)

    val gsonBuilder = GsonBuilder()
    val dateTimeSerializer: JsonSerializer<LocalDateTime> = DateTimeSerializer(dateTimeFormatter)
    gsonBuilder.registerTypeAdapter(LocalDateTime::class.java, dateTimeSerializer)
    val gson = gsonBuilder.create()

    val currentDirectory = Paths.get("").toAbsolutePath().toString()

    println("Current process directory: '$currentDirectory'")

    lateinit var serverConfig: ServerConfig

    try {
        serverConfig = gson.fromJson(
            File(configFileName).readText(Charsets.UTF_8),
            ServerConfig::class.java
        )
    }
    catch (exc: Exception) {
        println("Failed to start server: Can not load configuration file $configFileName: $exc")
        exitProcess(-1)
    }

    println("Configuration file $configFileName loaded")

    println("Database address: ${serverConfig.databaseConnectionParams.host}:" +
            "${serverConfig.databaseConnectionParams.port}, " +
            "database name: ${serverConfig.databaseConnectionParams.dbName}, " +
            "user: ${serverConfig.databaseConnectionParams.user}")

    lateinit var database: Database

    try {
        val connectionParams = serverConfig.databaseConnectionParams

        database = Database.connect(
            "jdbc:mysql://${connectionParams.host}:${connectionParams.port}/${connectionParams.dbName}",
            driver = "com.mysql.jdbc.Driver",
            user = connectionParams.user,
            password = connectionParams.password
        )
    }
    catch (exc: Exception) {
        println("Unable to start server: Can not connect to database: $exc")
        exitProcess(-1)
    }
}