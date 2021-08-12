package com.trollingcont.fullerene.server.main

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DateTimeSerializer(private val dateTimeFormatter: DateTimeFormatter) : JsonSerializer<LocalDateTime> {

    override fun serialize(src: LocalDateTime?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement =
        JsonPrimitive(src!!.format(dateTimeFormatter))
}