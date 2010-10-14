package me.prettyprint.cassandra.service;

import me.prettyprint.cassandra.service.CassandraClientMonitor.Counter;
import me.prettyprint.hector.api.exceptions.HectorException;

import org.apache.cassandra.thrift.Cassandra;

/**
 * Defines the interface of an operation performed on cassandra
 *
 * @param <T>
 *          The result type of the operation (if it has a result), such as the
 *          result of get_count or get_column
 *
 *          Oh closures, how I wish you were here...
 */
public abstract class Operation<T> {

  /** Counts failed attempts */
  public final Counter failCounter;

  /** The stopwatch used to measure operation performance */
  public final String stopWatchTagName;

  public final FailoverPolicy failoverPolicy;
  
  public final String keyspaceName;
  
  protected T result;
  private HectorException exception;

  /**
   * Most commonly used for system_* calls as the keyspaceName is null
   * @param operationType
   */
  public Operation(OperationType operationType) {
    this.failCounter = operationType.equals(OperationType.READ) ? Counter.READ_FAIL :
      Counter.WRITE_FAIL;
    this.stopWatchTagName = operationType.name();
    this.failoverPolicy = CassandraClient.DEFAULT_FAILOVER_POLICY;
    this.keyspaceName = null;
  }
  
  public Operation(OperationType operationType, FailoverPolicy failoverPolicy, String keyspaceName) {
    this.failCounter = operationType.equals(OperationType.READ) ? Counter.READ_FAIL :
      Counter.WRITE_FAIL;
    this.stopWatchTagName = operationType.name();
    this.failoverPolicy = failoverPolicy;
    this.keyspaceName = keyspaceName;
  }

  public void setResult(T executionResult) {
    result = executionResult;
  }

  /**
   *
   * @return The result of the operation, if this is an operation that has a
   *         result (such as getColumn etc.
   */
  public T getResult() {
    return result;
  }

  /**
   * Performs the operation on the given cassandra instance.
   */
  public abstract T execute(Cassandra.Client cassandra) throws HectorException;

  public void executeAndSetResult(Cassandra.Client cassandra) throws HectorException {
    setResult(execute(cassandra));
  }

  public void setException(HectorException e) {
    exception = e;
  }

  public boolean hasException() {
    return exception != null;
  }

  public HectorException getException() {
    return exception;
  }
}

/**
 * Specifies the "type" of operation - read or write.
 * It's used for perf4j, so should be in sync with hectorLog4j.xml
 * @author Ran Tavory (ran@outbain.com)
 * 
 */
enum OperationType {
  /** Read operations*/
  READ,
  /** Write operations */
  WRITE,
  /** Meta read operations, such as describe*() */
  META_READ,
  /** Operation on one of the system_ methods */
  META_WRITE;
}