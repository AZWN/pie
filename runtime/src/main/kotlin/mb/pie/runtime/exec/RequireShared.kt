package mb.pie.runtime.exec

import mb.pie.api.*
import mb.pie.api.exec.Cancelled
import java.io.Serializable

interface RequireTask {
  fun <I : In, O : Out> require(key: TaskKey, task: Task<I, O>, cancel: Cancelled): O
}

internal class RequireShared {
  private val taskDefs: TaskDefs;
  private val resourceSystems: ResourceSystems;
  private val visited: MutableMap<TaskKey, TaskData<*, *>>;
  private val store: Store;
  private val executorLogger: ExecutorLogger;


  constructor(taskDefs: TaskDefs, resourceSystems: ResourceSystems, visited: MutableMap<TaskKey, TaskData<*, *>>, store: Store, executorLogger: ExecutorLogger) {
    this.taskDefs = taskDefs;
    this.resourceSystems = resourceSystems;
    this.visited = visited;
    this.store = store;
    this.executorLogger = executorLogger;
  }


  /**
   * Attempt to get task data from the visited cache.
   */
  fun dataFromVisited(key: TaskKey): TaskData<*, *>? {
    executorLogger.checkVisitedStart(key);
    val data: TaskData<*, *>? = visited.get(key);
    executorLogger.checkVisitedEnd(key, data?.output);
    return data;
  }

  /**
   * Attempt to get task data from the store.
   */
  fun dataFromStore(key: TaskKey): TaskData<*, *>? {
    executorLogger.checkStoredStart(key);
    val data: TaskData<*, *>? = store.readTxn().use { txn: StoreReadTxn -> txn.data(key) };
    executorLogger.checkStoredEnd(key, data?.output);
    return data;
  }


  /**
   * Check if input is internally consistent.
   */
  fun <I : In> checkInput(input: I, task: Task<I, *>): InconsistentInput? {
    if(!input.equals(task.input)) {
      return InconsistentInput(input, task.input);
    }
    return null;
  }

  /**
   * Check if output is internally consistent.
   */
  fun checkOutputConsistency(output: Out): InconsistentTransientOutput? {
    return isTransientInconsistent(output);
  }

  /**
   * Check if a file requires dependency is internally consistent.
   */
  fun checkResourceRequire(key: TaskKey, task: Task<*, *>, fileReq: ResourceRequireDep): InconsistentResourceRequire? {
    executorLogger.checkResourceRequireStart(key, task, fileReq);
    val reason: InconsistentResourceRequire? = fileReq.checkConsistency(resourceSystems);
    executorLogger.checkResourceRequireEnd(key, task, fileReq, reason);
    return reason;
  }

  /**
   * Check if a file generates dependency is internally consistent.
   */
  fun checkResourceProvide(key: TaskKey, task: Task<*, *>, fileGen: ResourceProvideDep): InconsistentResourceProvide? {
    executorLogger.checkResourceProvideStart(key, task, fileGen);
    val reason: InconsistentResourceProvide? = fileGen.checkConsistency(resourceSystems);
    executorLogger.checkResourceProvideEnd(key, task, fileGen, reason);
    return reason;
  }

  /**
   * Check if a task requires dependency is totally consistent.
   */
  fun checkTaskRequire(key: TaskKey, task: Task<*, *>, taskRequire: TaskRequireDep, requireTask: RequireTask, cancel: Cancelled): InconsistentTaskReq? {
    val calleeKey: TaskKey = taskRequire.callee;
    val calleeTask: Task<Serializable, Serializable?> = store.readTxn().use { txn: StoreReadTxn -> calleeKey.toTask(taskDefs, txn) };
    val calleeOutput: Serializable? = requireTask.require(calleeKey, calleeTask, cancel);
    executorLogger.checkTaskRequireStart(key, task, taskRequire);
    val reason: InconsistentTaskReq? = taskRequire.checkConsistency(calleeOutput);
    executorLogger.checkTaskRequireEnd(key, task, taskRequire, reason);
    return reason;
  }
}
