package net.akat.goolak;

import java.util.concurrent.CompletableFuture;

public final class ObfuscationTaskDispatcher {

  public CompletableFuture<ObfuscationResult> dispatch(ObfuscationTask task, ObfuscationTaskWorker worker) {
    try {
      return CompletableFuture.completedFuture(worker.process(task));
    } catch (Exception exception) {
      return CompletableFuture.failedFuture(exception);
    }
  }

  public void shutdown() {
    // no-op
  }
}
