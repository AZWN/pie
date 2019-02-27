package mb.pie.runtime

import mb.fs.java.JavaFileSystem
import mb.pie.api.*
import mb.pie.api.exec.BottomUpExecutor
import mb.pie.api.exec.TopDownExecutor
import mb.pie.api.fs.FileSystemResourceSystem
import mb.pie.api.fs.stamp.FileSystemStampers
import mb.pie.api.stamp.OutputStamper
import mb.pie.api.stamp.output.OutputStampers
import mb.pie.runtime.exec.BottomUpExecutorImpl
import mb.pie.runtime.exec.TopDownExecutorImpl
import mb.pie.runtime.layer.ValidationLayer
import mb.pie.runtime.logger.NoopLogger
import mb.pie.runtime.logger.exec.LoggerExecutorLogger
import mb.pie.runtime.resourcesystems.MapResourceSystems
import mb.pie.runtime.share.NonSharingShare
import mb.pie.runtime.store.InMemoryStore
import java.util.function.Function

public class PieBuilderImpl : PieBuilder {
  private var taskDefs: TaskDefs? = null;
  private var resourceSystems: ResourceSystems? = null;
  private var store: Function<Logger, Store> = Function { _ -> InMemoryStore() };
  private var share: Function<Logger, Share> = Function { NonSharingShare() };
  private var defaultOutputStamper: OutputStamper = OutputStampers.getEquals();
  private var defaultRequireFileSystemStamper: FileSystemStamper = FileSystemStampers.getModified();
  private var defaultProvideFileSystemStamper: FileSystemStamper = FileSystemStampers.getModified();
  private var layerFactory: Function<Logger, Layer> = Function { logger -> ValidationLayer(logger) };
  private var logger: Logger = NoopLogger();
  private var executorLoggerFactory: Function<Logger, ExecutorLogger> = Function { logger -> LoggerExecutorLogger(logger) };

  override fun withTaskDefs(taskDefs: TaskDefs): PieBuilderImpl {
    this.taskDefs = taskDefs;
    return this;
  }

  override fun withResourceSystems(resourceSystems: ResourceSystems): PieBuilder {
    this.resourceSystems = resourceSystems;
    return this;
  }

  override fun withStore(store: Function<Logger, Store>): PieBuilderImpl {
    this.store = store;
    return this;
  }

  override fun withShare(share: Function<Logger, Share>): PieBuilderImpl {
    this.share = share;
    return this;
  }

  override fun withDefaultOutputStamper(stamper: OutputStamper): PieBuilderImpl {
    this.defaultOutputStamper = stamper;
    return this;
  }

  override fun withDefaultRequireFileSystemStamper(stamper: FileSystemStamper): PieBuilderImpl {
    this.defaultRequireFileSystemStamper = stamper;
    return this;
  }

  override fun withDefaultProvideFileSystemStamper(stamper: FileSystemStamper): PieBuilderImpl {
    this.defaultProvideFileSystemStamper = stamper;
    return this;
  }

  override fun withLayer(layer: Function<Logger, Layer>): PieBuilderImpl {
    this.layerFactory = layer;
    return this;
  }

  override fun withLogger(logger: Logger): PieBuilderImpl {
    this.logger = logger;
    return this;
  }

  override fun withExecutorLogger(executorLogger: Function<Logger, ExecutorLogger>): PieBuilderImpl {
    this.executorLoggerFactory = executorLogger;
    return this;
  }


  override fun build(): PieImpl {
    val taskDefs: TaskDefs;
    if(this.taskDefs != null) {
      taskDefs = this.taskDefs!!;
    } else {
      throw RuntimeException("Task definitions were not set before building");
    }

    val resourceSystems: ResourceSystems;
    if(this.resourceSystems != null) {
      resourceSystems = this.resourceSystems!!
    } else {
      val map = HashMap<String, ResourceSystem>();
      map.put(JavaFileSystem.id, FileSystemResourceSystem(JavaFileSystem.instance));
      resourceSystems = MapResourceSystems(map);
    }

    val store = this.store.apply(logger);
    val share = this.share.apply(logger);
    val topDownExecutor = TopDownExecutorImpl(taskDefs, resourceSystems, store, share, defaultOutputStamper, defaultRequireFileSystemStamper, defaultProvideFileSystemStamper, layerFactory, logger, executorLoggerFactory);
    val bottomUpExecutor = BottomUpExecutorImpl(taskDefs, resourceSystems, store, share, defaultOutputStamper, defaultRequireFileSystemStamper, defaultProvideFileSystemStamper, layerFactory, logger, executorLoggerFactory);
    return PieImpl(topDownExecutor, bottomUpExecutor, taskDefs, resourceSystems, store, share, defaultOutputStamper, defaultRequireFileSystemStamper, defaultProvideFileSystemStamper, layerFactory, logger, executorLoggerFactory);
  }
}

public class PieImpl : Pie {
  private val topDownExecutor: TopDownExecutor;
  private val bottomUpExecutor: BottomUpExecutor;
  val taskDefs: TaskDefs;
  val resourceSystems: ResourceSystems;
  val store: Store;
  val share: Share;
  val defaultOutputStamper: OutputStamper;
  val defaultRequireFileSystemStamper: FileSystemStamper;
  val defaultProvideFileSystemStamper: FileSystemStamper;
  val layerFactory: Function<Logger, Layer>;
  val logger: Logger;
  val executorLoggerFactory: Function<Logger, ExecutorLogger>;

  public constructor(
    topDownExecutor: TopDownExecutor,
    bottomUpExecutor: BottomUpExecutor,
    taskDefs: TaskDefs,
    resourceSystems: ResourceSystems,
    store: Store,
    share: Share,
    defaultOutputStamper: OutputStamper,
    defaultRequireFileSystemStamper: FileSystemStamper,
    defaultProvideFileSystemStamper: FileSystemStamper,
    layerFactory: Function<Logger, Layer>,
    logger: Logger,
    executorLoggerFactory: Function<Logger, ExecutorLogger>
  ) {
    this.topDownExecutor = topDownExecutor;
    this.bottomUpExecutor = bottomUpExecutor;
    this.taskDefs = taskDefs;
    this.resourceSystems = resourceSystems;
    this.store = store;
    this.share = share;
    this.defaultOutputStamper = defaultOutputStamper;
    this.defaultRequireFileSystemStamper = defaultRequireFileSystemStamper;
    this.defaultProvideFileSystemStamper = defaultProvideFileSystemStamper;
    this.layerFactory = layerFactory;
    this.logger = logger;
    this.executorLoggerFactory = executorLoggerFactory;
  }

  override fun getTopDownExecutor(): TopDownExecutor {
    return topDownExecutor;
  }

  override fun getBottomUpExecutor(): BottomUpExecutor {
    return bottomUpExecutor;
  }

  override fun dropStore() {
    store.writeTxn().use { it.drop(); };
  }

  override fun close() {
    store.close();
  }

  override fun toString(): String {
    return "PieImpl(" + store + ", " + share + ", " + defaultOutputStamper + ", " + defaultRequireFileSystemStamper + ", " + defaultProvideFileSystemStamper + ", " + layerFactory.apply(logger) + ")";
  }
}
