package com.lightimageloaderdownloader.lild.cache.diskcache

import android.util.Log
import kotlinx.coroutines.*
import java.io.*
import java.nio.charset.StandardCharsets

class DiskCache(
    val folder: File,
    private val maxSize: Long,
    private val appVersion: Int
) : Closeable, Flushable {

    private var initialized = false
    private var closed = false

    private val journal = File(folder.absolutePath + File.separator + JOURNAL)
    private val journalTmp = File(folder.absolutePath + File.separator + JOURNAL_TMP)
    private var operationsSinceRewrite = 0
    private var journalWriter: BufferedWriter? = null
    private var hasJournalError = false

    private var mostRecentTrimFailed = false
    private var mostRecentRebuildFailed = false

    private var size = 0L
    private val entries = LinkedHashMap<String, Entry>(0, 0.75f, true)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val coroutineScope = CoroutineScope(Dispatchers.IO.limitedParallelism(1))

    /**
     * Journal methods
     */

    @Synchronized
    private fun initialize() {
        if (initialized) {
            return
        }

        folder.mkdirs()
        journalTmp.delete()

        if (journal.exists()) {
            try {
                readJournal()
                processJournal()
                initialized = true
                return
            } catch (_: IOException) {

            }

            try {
                delete()
            } finally {
                closed = false
            }
        }

        writeJournal()
        initialized = true
    }

    private fun readJournal() {
        BufferedReader(FileReader(journal)).use { bufferedReader ->
            val version = bufferedReader.readLine()
            val start = bufferedReader.readLine()

            if (version != appVersion.toString() || start != JOURNAL_ENTRIES_START) {
                throw IOException()
            }

            var lineCount = 0
            var currentLine = bufferedReader.readLine()
            var hasError = false
            while (currentLine != null) {
                try {
                    readJournalLine(currentLine)
                    currentLine = bufferedReader.readLine()
                    lineCount++
                } catch (e: Exception) {
                    hasError = true
                    break
                }
            }

            operationsSinceRewrite = lineCount - entries.size

            if (hasError) {
                writeJournal()
            } else {
                journalWriter = newJournalWriter()
            }
        }
    }

    private fun readJournalLine(line: String) {
        val parts = line.split(" ")
        val operation = parts[0]
        val key = parts[1]

        val entry = entries.getOrPut(key) { Entry(key) }

        when(operation) {
            CLEAN -> {
                entry.readable = true
                entry.editor = null
                entry.setLength(parts[2])
            }
            DIRTY -> {
                entry.editor = Editor(entry)
            }
            READ -> {
                entries[key]
            }
            REMOVE -> {
                entries.remove(key)
            }
            else -> throw IOException()
        }
    }

    private fun processJournal() {
        var size = 0L
        val iterator = entries.values.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.editor == null) {
                size += entry.length
            } else {
                entry.editor = null
                entry.cleanFile().delete()
                entry.dirtyFile().delete()
                iterator.remove()
            }
        }
        this.size = size
    }

    @Synchronized
    private fun writeJournal() {
        journalWriter?.flush()
        journalWriter?.close()

        if (journalTmp.exists())  {
            journalTmp.delete()
        }
        if (!journal.exists()) {
            journal.createNewFile()
        }
        folder.mkdirs()
        journalTmp.createNewFile()

        FileOutputStream(journalTmp).bufferedWriter(StandardCharsets.UTF_8).apply {
            append(appVersion.toString())
            newLine()
            append(JOURNAL_ENTRIES_START)
            newLine()
            entries.values.forEach { entry ->
                if (entry.editor == null) {
                    append(CLEAN)
                    append(" ")
                    append(entry.key)
                    entry.writeLength(this)
                    newLine()
                } else {
                    append(DIRTY)
                    append(" ")
                    append(entry.key)
                    newLine()
                }
            }
            flush()
            close()
        }

        rename(journalTmp, journal)
        operationsSinceRewrite = 0
        hasJournalError = false
        mostRecentRebuildFailed = false
        journalWriter = newJournalWriter()
    }

    private fun newJournalWriter(): BufferedWriter {
        return FaultHidingWriter(journal, true) {
            hasJournalError = true
        }
    }

    private fun journalRewriteRequired() = operationsSinceRewrite >= 2000

    /**
     * Cache methods
     */

    fun edit(key: String): Editor? {
        validateKey(key)
        checkIfClosed()
        initialize()

        var entry = entries[key]

        if (entry?.editor != null) {
            return null
        }

        if (entry != null && entry.snapshotOpenedCount > 0) {
            return null
        }

        if (mostRecentTrimFailed || mostRecentRebuildFailed) {
            launchCleanup()
            return null
        }

        if (hasJournalError) {
            launchCleanup()
            return null // Don't edit; the journal can't be written.
        }

        journalWriter!!.apply {
            append(DIRTY)
            append(" ")
            append(key)
            newLine()
            flush()
        }

        if (entry == null) {
            entry = Entry(key)
            entries[key] = entry
        }

        val editor = Editor(entry)
        entry.editor = editor
        return editor
    }

    fun get(key: String): Snapshot? {
        validateKey(key)
        checkIfClosed()
        initialize()

        val snapshot = entries[key]?.snapshot() ?: return null

        operationsSinceRewrite++
        journalWriter!!.apply {
            append(READ)
            append(" ")
            append(key)
            newLine()
            flush()
        }

        if (journalRewriteRequired()) {
            launchCleanup()
        }

        return snapshot
    }

    @Synchronized
    fun size(): Long {
        initialize()
        return size
    }

    private fun delete() {
        close()
        folder.listFiles()?.forEach { it.delete() }
    }

    @Synchronized
    private fun commitEntry(editor: Editor, success: Boolean) {
        val entry = editor.entry

        if (success && !entry.zombie) {
            entry.readable = true

            val oldSize = entry.length
            val newSize = entry.dirtyFile().length()
            size = size - oldSize + newSize

            entry.length = newSize
            rename(entry.dirtyFile(), entry.cleanFile())
        } else {
            entry.dirtyFile().delete()
        }

        entry.editor = null
        if (entry.zombie) {
            removeEntry(entry)
        }

        operationsSinceRewrite++
        journalWriter!!.apply {
            if (success || entry.readable) { // if success or was prev success and this edit not
                append(CLEAN)
                append(" ")
                append(entry.key)
                entry.writeLength(this)
                newLine()
                flush()
            } else { // if not success or published remove
                append(REMOVE)
                append(" ")
                append(entry.key)
                newLine()
                flush()
            }
        }

        if (size > maxSize || journalRewriteRequired()) {
            launchCleanup()
        }

        Log.d("DISKCACHE", "Edit entry endend, current size $size on $maxSize")
    }

    @Synchronized
    fun remove(key: String) {
        checkIfClosed()
        validateKey(key)
        initialize()
        val entry = entries[key] ?: return

        removeEntry(entry)
        if (size <= maxSize) mostRecentTrimFailed = false
    }

    private fun removeEntry(entry: Entry) {
        if (entry.snapshotOpenedCount > 0) {
            journalWriter!!.apply {
                append(DIRTY)
                append(" ")
                append(entry.key)
                newLine()
                flush()
            }
        }

        if (entry.editor != null || entry.snapshotOpenedCount > 0) {
            entry.zombie = true
            return
        }

        size -= entry.length

        entry.cleanFile().delete()
        entry.dirtyFile().delete()

        operationsSinceRewrite++
        journalWriter!!.apply {
            append(REMOVE)
            append(" ")
            append(entry.key)
            newLine()
            flush()
        }

        entries.remove(entry.key)

        if (journalRewriteRequired()) {
            launchCleanup()
        }
    }

    private fun launchCleanup() {
        Log.d("DISKCACHE", "Cleanup started, current size $size over $maxSize")
        coroutineScope.launch {
            synchronized(this@DiskCache) {
                if (!initialized || closed) return@launch
                try {
                    cleanupEntries()
                } catch (_: Exception) {
                    mostRecentTrimFailed = true
                }

                Log.d("DISKCACHE", "Cleanup entry endend, current size $size on $maxSize")
                if (mostRecentTrimFailed) Log.d("DISKCACHE", "Most recently trim failed")

                try {
                    if (journalRewriteRequired() || hasJournalError) {
                        writeJournal()
                    }
                    Log.d("DISKCACHE", "Cleanup write journal success")
                } catch (_: IOException) {
                    mostRecentRebuildFailed = true
                    journalWriter = FakeWriter(journal)
                    Log.d("DISKCACHE", "Cleanup write journal failed")
                }
            }
        }
    }

    private fun cleanupEntries() {
        while (size > maxSize * 0.8) {
            if (!removeOldestEntry()) return
        }
        mostRecentTrimFailed = false
    }

    @Synchronized
    fun evictAll() {
        initialize()
        for (entry in entries.values.toTypedArray()) {
            removeEntry(entry)
        }
        mostRecentTrimFailed = false
    }

    private fun removeOldestEntry() : Boolean {
        for (entry in entries.values) {
            if (!entry.zombie) {
                removeEntry(entry)
                return true
            }
        }
        return false
    }

    @Synchronized
    override fun close() {
        if (!initialized || closed) {
            closed = true
            return
        }

        for (entry in entries.values.toTypedArray()) {
            if (entry.editor != null) {
                entry.editor?.detach()
            }
        }

        cleanupEntries()
        coroutineScope.cancel()
        journalWriter!!.flush()
        journalWriter!!.close()
        journalWriter = null
        closed = true
    }

    @Synchronized
    override fun flush() {
        if (!initialized) return

        checkIfClosed()
        cleanupEntries()
        journalWriter!!.flush()
    }

    private fun validateKey(key: String) {
        require(KEY_PATTERN matches key) {
            "keys must match regex [a-zA-Z0-9_-]{1,120}: \"$key\""
        }
    }

    private fun checkIfClosed() {
        check(!closed) { "cache is closed" }
    }

    /**
     * Files utilities
     */

    private fun rename(from: File, to: File) {
        if (to.exists()) {
            to.delete()
        }

        from.renameTo(to)
    }

    /**
     * Classes
     */

    inner class Entry(val key: String) {
        var snapshotOpenedCount = 0
        var zombie = false
        var readable = false

        var editor: Editor? = null

        var length = 0L

        fun snapshot() : Snapshot? {
            if (!readable) return null
            if (editor != null || zombie) return null

            if (!cleanFile().exists()) {
                removeEntry(this)
                return null
            }

            snapshotOpenedCount++
            return Snapshot(this)
        }

        fun setLength(length: String) {
            try {
                this.length = length.toLong()
            } catch (_: NumberFormatException) {
                throw IOException()
            }
        }

        fun writeLength(writer: Writer) {
            writer.append(" ")
            writer.append(length.toString())
        }

        fun cleanFile(): File {
            return File(folder.absolutePath + File.separator + key)
        }

        fun dirtyFile(): File {
            return File(folder.absolutePath + File.separator + key + FILE_TMP)
        }
    }

    inner class Editor(val entry: Entry) {

        private var closed = false

        fun file(): File {
            val file = entry.dirtyFile()

            if (!file.exists()) {
                folder.mkdirs()
                file.createNewFile()
            }

            return file
        }

        fun abort() {
            complete(false)
        }

        fun commit() {
            complete(true)
        }

        private fun complete(success: Boolean) {
            synchronized(this@DiskCache) {
                if (entry.editor == this && !closed) {
                    commitEntry(this, success)
                }
                closed = true
            }
        }

        fun detach() {
            if (entry.editor == this) {
                entry.zombie = true
            }
        }
    }

    inner class Snapshot(private val entry: Entry) {

        fun file(): File {
            return entry.cleanFile()
        }

        fun close() {
            synchronized(this@DiskCache) {
                entry.snapshotOpenedCount--
                if (entry.zombie && entry.snapshotOpenedCount == 0) {
                    removeEntry(entry)
                }
            }
        }
    }

    companion object {
        private val KEY_PATTERN = "[a-zA-Z0-9_-]{1,120}".toRegex()
        private const val FILE_TMP = ".tmp"
        private const val JOURNAL = "journal"
        private const val JOURNAL_TMP = "journal.tmp"
        private const val CLEAN = "CLEAN"
        private const val READ = "READ"
        private const val DIRTY = "DIRTY"
        private const val REMOVE = "REMOVE"
        private const val JOURNAL_ENTRIES_START = "START"
    }
}