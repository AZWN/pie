package mb.pie.api.exec

import mb.pie.api.*
import mb.util.async.Cancelled
import mb.util.async.NullCancelled
import mb.vfs.path.PPath

/**
 * Executor using a bottom-up build algorithm and observers for pushing new observed outputs.
 */
interface BottomUpExecutor {
  /**
   * Make up-to-date all tasks affected by changes to given files. Changed outputs of tasks are observed by observers.
   */
  @Throws(ExecException::class, InterruptedException::class)
  fun requireBottomUp(changedFiles: Set<PPath>, cancel: Cancelled = NullCancelled())

  /**
   * Requires given task in a top-down fashion, returning its result.
   */
  @Throws(ExecException::class, InterruptedException::class)
  fun <I : In, O : Out> requireTopDown(task: Task<I, O>, cancel: Cancelled = NullCancelled()): O

  /**
   * Checks whether given task has been required at least once.
   */
  fun <I : In, O : Out> hasBeenRequired(task: Task<I, O>): Boolean

  /**
   * Sets [observer] as the observer for outputs of [task], using given [key] which can be used to remove (unsubscribe from) the observer.
   */
  fun setObserver(key: Any, task: UTask, observer: (Out) -> Unit)

  /**
   * Removes the observer with given key.
   */
  fun removeObserver(key: Any)
}

