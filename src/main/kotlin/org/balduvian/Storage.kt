package org.balduvian

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.pathString

@OptIn(DelicateCoroutinesApi::class)
inline fun <reified Data>createStorage(
	path: Path,
	crossinline readData: (file: File) -> Data,
	crossinline writeData: (file: File, data: Data) -> Unit,
	crossinline writePlaceholder: (file: File) -> Unit = { },
	crossinline createDefault: () -> Data? = { null }
): Storage<Data> {
	val errorName = "${Data::class.simpleName} (${path.pathString})"

	return object : Storage<Data>() {
		override fun load(): Data {
			val file = File(path.absolutePathString())

			if (file.isDirectory()) {
				file.deleteRecursively()
			}

			if (!file.exists()) {
				path.parent.createDirectories()

				println("missing file for $errorName")

				val default = createDefault()

				if (default != null) {
					writeData(file, default)
					return default
				} else {
					writePlaceholder(file)
					throw Exception("could not load data for $errorName")
				}
			}

			return readData(file)
		}

		override fun save(data: Data) {
			GlobalScope.launch {
				path.parent.createDirectories()
				writeData(path.toFile(), data)
			}
		}
	}
}

abstract class Storage<Data> {
	abstract fun load(): Data
	abstract fun save(data: Data)
}
