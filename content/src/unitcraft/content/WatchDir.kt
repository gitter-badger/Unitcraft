package unitcraft.content

import java.io.File
import java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY

import java.io.IOException
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.WatchEvent
import java.nio.file.WatchKey
import java.nio.file.WatchService
import java.util.*
import kotlin.concurrent.thread


class WatchDir(dirs:List<File>,val onModify:(File) -> Unit) {
    val dirByKey = HashMap<WatchKey,Path>(2)

    init{
        val watcher = FileSystems.getDefault().newWatchService()
        for (dir in dirs.map{it.toPath()}) {
            val key = dir.register(watcher, ENTRY_MODIFY)
            dirByKey[key] = dir
        }
        thread{
            while (true) {
                val key = watcher.take()
                for (event in key.pollEvents()) {
                    val name = (event as WatchEvent<Path>).context()
                    onModify(dirByKey[key]!!.resolve(name).toFile())
                }
                key.reset()
            }
        }
    }
}
