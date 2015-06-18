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
import kotlin.platform.platformStatic

class WatchDir(dirs:List<File>,val onModify:(File) -> Unit) {
    init{
        val watcher = FileSystems.getDefault().newWatchService()
        for (dir in dirs) dir.toPath().register(watcher, ENTRY_MODIFY)
        Thread{
            while (true) {
                val key = watcher.take()
                for (event in key.pollEvents()) {
                    onModify((event as WatchEvent<Path>).context().toFile())
                }
                key.reset()
            }
        }.start()
    }
}
