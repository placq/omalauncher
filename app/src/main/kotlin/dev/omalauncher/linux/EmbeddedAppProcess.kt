package dev.omalauncher.linux

import kotlin.concurrent.thread

/** Owns the proot invocation, including cancellation before the first window appears. */
class EmbeddedAppProcess(
    private val builder: () -> ProcessBuilder,
    private val onExit: (Int, String) -> Unit,
) {
    private val lock = Any()
    private var cancelled = false
    private var process: Process? = null

    fun start() {
        thread(name = "embedded-linux-app", isDaemon = true) {
            val output = ArrayDeque<String>()
            var code = -1
            try {
                if (synchronized(lock) { cancelled }) return@thread
                val child = builder().redirectErrorStream(true).start()
                synchronized(lock) {
                    process = child
                    if (cancelled) child.destroy()
                }
                child.inputStream.bufferedReader().useLines { lines ->
                    lines.forEach {
                        output.addLast(it.take(300))
                        if (output.size > 6) output.removeFirst()
                    }
                }
                code = child.waitFor()
            } catch (e: Exception) {
                output.addLast(e.message.orEmpty().take(300))
            } finally {
                synchronized(lock) {
                    process?.destroy()
                    process = null
                }
                onExit(code, output.joinToString("\n"))
            }
        }
    }

    /** SIGTERM lets proot's --kill-on-exit clean up its guest; never block the UI waiting. */
    fun close() {
        synchronized(lock) {
            cancelled = true
            process?.destroy()
        }
    }
}
