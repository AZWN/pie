package mb.pie.example.copyfile

import mb.pie.api.*
import mb.pie.api.stamp.resource.FileSystemStampers
import mb.pie.runtime.PieBuilderImpl
import mb.pie.runtime.logger.StreamLogger
import mb.pie.runtime.taskdefs.MapTaskDefs
import mb.pie.store.lmdb.LMDBStore
import java.io.File
import java.io.Serializable

/**
 * This example demonstrates how to write a PIE build script in Kotlin with the PIE API, and how to incrementally execute that build script
 * with the PIE runtime.
 *
 * The goal of the build script is to first provide a source file, and to then copy that generated source file to a destination file.
 */

/**
 * The [FileGenerator] [task definition][TaskDef] takes as input a [path][PPath] to a file, and then writes "Hello world" to it. This task
 * definition does not return a value, so we use [None] as output type.
 */
class FileGenerator : TaskDef<File, None> {
  /**
   * The [id] property must be overridden to provide a unique identifier for this task definition. In this case, we use reflection to
   * create a unique identifier.
   */
  override fun getId(): String = javaClass.simpleName

  /**
   * The [exec] method must be overridden to implement the logic of this task definition. This function is executed with an
   * [execution context][ExecContext] object as receiver, which is used to tell PIE about dynamic task or file dependencies.
   */
  override fun exec(context: ExecContext, input: File): None {
    // We write "Hello world" to the file.
    input.outputStream().buffered().use {
      it.write("Hello world".toByteArray())
    }
    // Since we have written to or generated the file that [input] points to, we need to tell PIE about this dynamic dependency, by calling
    // provide, which is defined in ExecContext.
    context.provide(input)
    // Since this task does not provide a value, and we use the None type to indicate that, we need to return the singleton instance of None.
    return None.instance
  }
}

/**
 * The [FileCopier] task definition copies a file, generated by [FileGenerator] to another file. In this case, we need to take multiple
 * inputs, so we group them into the [FileCopier.Input] data class.
 */
class FileCopier : TaskDef<FileCopier.Input, File> {
  override fun getId(): String = javaClass.simpleName

  /**
   * A data class that groups multiple inputs to this task definition.
   */
  data class Input(
    /**
     * Path to the file we want to copy from.
     */
    val sourceFile: File,
    /**
     * Task that generated the file we would like to copy.
     * We need to pass this task as an input to this task, so that we can require it, to prevent a hidden dependency error.
     * Tasks can be passed to other tasks using the [STask] or [TaskKey] type.
     */
    val sourceTask: STask,
    /**
     * Path of the destination we want to copy the source file to.
     */
    val destinationFile: File
  ) : Serializable

  override fun exec(context: ExecContext, input: Input): File {
    val (sourceFile, sourceTask, destination) = input
    // Since we are going to read the source file, which was generated by another task, we need to create a dynamic dependency to the task
    // and the file, which we do as follows.
    context.require(sourceTask)
    // We use a hash stamper on the source file, to prevent copies when the contents of the source file does not change.
    context.require(sourceFile, FileSystemStampers.hash())
    // Then we read the source file, add some text to it, and write it to the destination.
    val sourceText = sourceFile.readText() + ", and universe!"
    destination.outputStream().buffered().use {
      it.write(sourceText.toByteArray())
      it.flush()
    }
    // Again, since we have written to/generated a file, we must add a dynamic dependency.
    context.provide(destination)
    return destination
  }
}

/**
 * The main function will start up the PIE runtime and execute the build script.
 */
fun main(args: Array<String>) {
  // We expect two optional arguments: the source file to generate, and the destination file.
  val sourceFile = File(args.getOrElse(0) { "build/run/source.txt" })
  val destinationFile = File(args.getOrElse(1) { "build/run/destination.txt" })

  // Now we instantiate the task definitions.
  val fileCreator = FileGenerator()
  val fileCopier = FileCopier()

  // Then, we add them to a TaskDefs object, which tells PIE about which task definitions are available.
  val taskDefs = MapTaskDefs()
  taskDefs.add(fileCreator)
  taskDefs.add(fileCopier)

  // We need to create the PIE runtime, using a PieBuilderImpl.
  val pieBuilder = PieBuilderImpl()
  // We pass in the TaskDefs object we created.
  pieBuilder.withTaskDefs(taskDefs)
  // For storing build results and the dependency graph, we will use the LMDB embedded database, stored at target/lmdb.
  LMDBStore.withLMDBStore(pieBuilder, File("build/run/lmdb"))
  // For example purposes, we use verbose logging which will output to stdout.
  pieBuilder.withLogger(StreamLogger.verbose())
  // Then we build the PIE runtime.
  pieBuilder.build().use { pie ->
    // Now we create concrete task instances from the task definitions.
    val fileCreatorTask = fileCreator.createTask(sourceFile)
    val fileCopierTask = fileCopier.createTask(FileCopier.Input(sourceFile, fileCreatorTask.toSTask(), destinationFile))

    // We (incrementally) execute the file copier task using the top-down executor.
    val output = pie.topDownExecutor.newSession().requireInitial(fileCopierTask)
    println("Copied to: $output")
  }
  // Finally, we clean up our resources. PIE must be closed to ensure the database has been fully serialized. Using a
  // 'use' block is the best way to ensure that.
}
