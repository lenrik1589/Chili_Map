package here.lenrik.chili_map.client.compat

class CompatHelper {
	fun loadClass(name: String): Class<*>? {
		val loader = this::class.java.classLoader
		return tries(
				{ loader.loadClass("here.lenrik.chili_map.client.compat.$name") },
				{ loader.loadClass(name) },
				or = null
		)
	}

	fun invokeInstance(descriptor: String, vararg args: Any?) {
		val argClasses = args.map { it?.javaClass }

	}

	companion object {
		fun <T> tries(vararg blocks: () -> T, or: T = throw RuntimeException()): T {
			var lastException: Throwable = RuntimeException(if (blocks.isEmpty()) "There must be at least one try block in tries blocks" else "uhh?")
			for (block in blocks) {
				try {
					return block.invoke()
				} catch (e: Exception) {
					lastException = e
				}
			}
			return or.apply {
				if (this is Exception) {
					initCause(lastException)
				}
			}
		}
	}
}