package net.akat.goolak;

public final class ObfuscationTaskWorker {

  private final ObfuscationProcessor processor;

  public ObfuscationTaskWorker(ObfuscationProcessor processor) {
    this.processor = processor;
  }

  public ObfuscationResult process(ObfuscationTask task) {
    return this.processor.process(task);
  }
}
