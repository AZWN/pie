package mb.pie.runtime.logger.exec;

import mb.pie.api.*;
import mb.pie.api.exec.ExecReason;
import mb.resource.ResourceKey;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.Serializable;
import java.util.Set;
import java.util.function.Consumer;

public class NoopExecutorLogger implements ExecutorLogger {
    @Override public void requireTopDownInitialStart(TaskKey key, Task<?> task) {}

    @Override public void requireTopDownInitialEnd(TaskKey key, Task<?> task, @Nullable Serializable output) {}

    @Override public void requireTopDownStart(TaskKey key, Task<?> task) {}

    @Override public void requireTopDownEnd(TaskKey key, Task<?> task, @Nullable Serializable output) {}

    @Override public void requireBottomUpInitialStart(Set<? extends ResourceKey> changedResources) {}

    @Override public void requireBottomUpInitialEnd() {}

    @Override public void checkVisitedStart(TaskKey key) {}

    @Override public void checkVisitedEnd(TaskKey key, @Nullable Serializable output) {}

    @Override public void checkStoredStart(TaskKey key) {}

    @Override public void checkStoredEnd(TaskKey key, @Nullable Serializable output) {}

    @Override public void checkResourceProvideStart(TaskKey key, Task<?> task, ResourceProvideDep dep) {}

    @Override
    public void checkResourceProvideEnd(TaskKey key, Task<?> task, @Nullable ResourceProvideDep dep, @Nullable ExecReason reason) {}

    @Override public void checkResourceRequireStart(TaskKey key, Task<?> task, ResourceRequireDep dep) {}

    @Override
    public void checkResourceRequireEnd(TaskKey key, Task<?> task, @Nullable ResourceRequireDep dep, @Nullable ExecReason reason) {}

    @Override public void checkTaskRequireStart(TaskKey key, Task<?> task, TaskRequireDep dep) {}

    @Override
    public void checkTaskRequireEnd(TaskKey key, Task<?> task, @Nullable TaskRequireDep dep, @Nullable ExecReason reason) {}

    @Override public void executeStart(TaskKey key, Task<?> task, ExecReason reason) {}

    @Override public void executeEnd(TaskKey key, Task<?> task, ExecReason reason, TaskData data) {}

    @Override
    public void invokeCallbackStart(@Nullable Consumer<Serializable> observer, TaskKey key, @Nullable Serializable output) {}

    @Override
    public void invokeCallbackEnd(@Nullable Consumer<Serializable> observer, TaskKey key, @Nullable Serializable output) {}
}
