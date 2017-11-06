package mb.pie.runtime.core.impl

import com.google.inject.Inject
import mb.log.Logger
import mb.pie.runtime.core.*
import mb.vfs.path.PPath
import java.util.*


interface ConsistencyChecker {
  fun isConsistent(): Boolean
}

interface DirtyFlagger {
  fun flag(changedPaths: Collection<PPath>, txn: StoreWriteTxn)
}

class DirtyFlaggerImpl @Inject constructor(
  private val cache: Cache,
  logger: Logger
) : DirtyFlagger {
  private val logger = logger.forContext(DirtyFlaggerImpl::class.java)


  override fun flag(changedPaths: Collection<PPath>, txn: StoreWriteTxn) {
    if(changedPaths.isEmpty()) return

    // Find function applications that are directly marked dirty by changed paths.
    val directDirty = HashSet<UFuncApp>()
    logger.trace("Initial dirty flagging")
    for(changedPath in changedPaths) {
      logger.trace("  changed: $changedPath")
      // Check function applications that require the changed path.
      val requiredBy = txn.requiredBy(changedPath)
      for(funcApp in requiredBy) {
        logger.trace("  required by: ${funcApp.toShortString(200)}")
        if(!pathIsConsistent(changedPath, funcApp, txn, { path, res -> res.pathReqs.firstOrNull { path == it.path } })) {
          directDirty.add(funcApp)
        }
      }
      // Check function applications that generate the changed path.
      val generatedBy = txn.generatedBy(changedPath)
      if(generatedBy != null) {
        logger.trace("  generated by: ${generatedBy.toShortString(200)}")
        if(!pathIsConsistent(changedPath, generatedBy, txn, { path, res -> res.gens.firstOrNull { path == it.path } })) {
          directDirty.add(generatedBy)
        }
      }
    }
    if(directDirty.isEmpty()) return

    // Propagate dirty flags and persist to storage.
    logger.trace("Dirty flag propagation")
    val todo = ArrayDeque(directDirty)
    val seen = HashSet<UFuncApp>()
    while(!todo.isEmpty()) {
      val app = todo.pop()
      if(!seen.contains(app)) {
        logger.trace("  dirty: ${app.toShortString(200)}")
        txn.setIsDirty(app, true)
        seen.add(app)
        val calledBy = txn.calledBy(app)
        calledBy.forEach { logger.trace("  called by: ${it.toShortString(200)}") }
        todo += calledBy
      }
    }
  }

  private fun pathIsConsistent(path: PPath, funcApp: UFuncApp, txn: StoreReadTxn, checkerGenFunc: (PPath, UExecRes) -> ConsistencyChecker?): Boolean {
    val result = cache[funcApp] ?: txn.resultsIn(funcApp)
    if(result != null) {
      val checkConsistency = checkerGenFunc(path, result)
      if(checkConsistency != null) {
        if(!checkConsistency.isConsistent()) {
          logger.trace("  not consistent: $checkConsistency")
          return false
        }
      } else {
        // Should not happen. Log error and assume change.
        logger.error("Could not find consistency checker for path $path in ${result.toShortString(200)}")
        return false
      }
    } else {
      // Can occur when an execution is cancelled and its result is not stored. Assume that it is changed.
      return false
    }
    return true
  }
}