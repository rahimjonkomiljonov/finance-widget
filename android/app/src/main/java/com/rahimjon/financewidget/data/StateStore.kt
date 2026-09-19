package com.rahimjon.financewidget.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * The whole app state as one JSON file. Writes go to a temp file first and are renamed into
 * place, so a crash mid-write can never leave a half-written portfolio behind.
 *
 * The widget's list factory reads [snapshot] synchronously, which is why this is a plain
 * in-memory holder rather than an async store.
 */
class StateStore(private val file: File) {
    private val lock = Any()
    private val flow = MutableStateFlow(load())

    val state: StateFlow<AppState> = flow.asStateFlow()

    fun snapshot(): AppState = flow.value

    fun update(transform: (AppState) -> AppState): AppState = synchronized(lock) {
        val next = transform(flow.value)
        if (next != flow.value) {
            write(next)
            flow.value = next
        }
        next
    }

    private fun load(): AppState {
        if (!file.exists()) return AppState()
        return try {
            AppJson.decodeFromString<AppState>(file.readText())
        } catch (e: Exception) {
            // Never overwrite an unreadable file: keep it aside so the data can still be recovered.
            file.renameTo(File(file.parentFile, "${file.name}.corrupt-${System.currentTimeMillis()}"))
            AppState()
        }
    }

    private fun write(state: AppState) {
        file.parentFile?.mkdirs()
        val tmp = File(file.parentFile, "${file.name}.tmp")
        tmp.writeText(AppJson.encodeToString(AppState.serializer(), state))
        if (!tmp.renameTo(file)) {
            // renameTo can refuse to replace an existing file on some filesystems.
            file.delete()
            check(tmp.renameTo(file)) { "Could not save ${file.name}" }
        }
    }
}
