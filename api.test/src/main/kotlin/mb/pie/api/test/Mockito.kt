package mb.pie.api.test

import mb.fs.api.node.FSNode
import mb.fs.api.path.FSPath
import mb.pie.api.*
import mb.pie.api.exec.*
import mb.pie.api.fs.stamp.FileSystemStamper
import mb.pie.api.stamp.OutputStamper
import mb.pie.api.stamp.ResourceStamper
import org.mockito.Mockito

inline fun <reified T : Any> safeAny(default: T) = Mockito.any(T::class.java) ?: default


class NoExecReason : ExecReason {
  override fun toString() = ""
}

fun anyER() = safeAny<ExecReason>(NoExecReason())


class NoExecContext : ExecContext {
  override fun <I : In, O : Out> require(task: Task<I, O>): O {
    @Suppress("UNCHECKED_CAST")
    return null as O
  }

  override fun <I : In, O : Out> require(task: Task<I, O>, stamper: OutputStamper): O {
    @Suppress("UNCHECKED_CAST")
    return null as O
  }

  override fun <I : In, O : Out> require(taskDef: TaskDef<I, O>, input: I): O {
    @Suppress("UNCHECKED_CAST")
    return null as O
  }

  override fun <I : In, O : Out> require(taskDef: TaskDef<I, O>, input: I, stamper: OutputStamper): O {
    @Suppress("UNCHECKED_CAST")
    return null as O
  }

  override fun <I : In> require(task: STask<I>): Out {
    @Suppress("UNCHECKED_CAST")
    return null
  }

  override fun <I : In> require(task: STask<I>, stamper: OutputStamper): Out {
    @Suppress("UNCHECKED_CAST")
    return null
  }

  override fun <I : In> require(taskDefId: String, input: I): Out {
    @Suppress("UNCHECKED_CAST")
    return null
  }

  override fun <I : In> require(taskDefId: String, input: I, stamper: OutputStamper): Out {
    @Suppress("UNCHECKED_CAST")
    return null
  }


  override fun <R : Resource> require(resource: R, stamper: ResourceStamper<R>) {}
  override fun <R : Resource> provide(resource: R, stamper: ResourceStamper<R>) {}
  override fun require(path: FSPath) = null!!
  override fun require(path: FSPath, stamper: FileSystemStamper) = null!!
  override val defaultRequireFileSystemStamper: FileSystemStamper get() = null!!
  override fun provide(path: FSPath) {}
  override fun provide(path: FSPath, stamper: FileSystemStamper) {}
  override val defaultProvideFileSystemStamper: FileSystemStamper get() = null!!


  override fun toNode(path: FSPath): FSNode {
    @Suppress("CAST_NEVER_SUCCEEDS")
    return null as FSNode
  }


  override val logger: Logger = null!!
}

fun anyEC() = safeAny<ExecContext>(NoExecContext())

fun anyC() = safeAny<Cancelled>(NullCancelled())
