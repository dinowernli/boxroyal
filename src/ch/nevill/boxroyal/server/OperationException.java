package ch.nevill.boxroyal.server;

import ch.nevill.boxroyal.proto.OperationError;

class OperationException extends Exception {
  private static final long serialVersionUID = -2405849614106891603L;
  private final OperationError code;

  public OperationException(OperationError code) {
    this.code = code;
  }

  public OperationError getCode() {
    return code;
  }
}