package com.history_walk.history_walk_i.ui.components

import android.content.Context
import kotlinx.serialization.json.Json


suspend fun loadPathPoints(context: Context, resourceId: Int): List<PathPoint> {
    val jsonString = context.resources.openRawResource(resourceId).bufferedReader().use { it.readText() }
    return Json.decodeFromString(jsonString)
}