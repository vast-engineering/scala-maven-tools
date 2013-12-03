package com.vast.surefire.scalatest20;

/**
 * @author David Pratt (dpratt@vast.com)
 */
public class WrappedCheckedException extends RuntimeException {

    private final Exception wrapped;

    public WrappedCheckedException(Exception cause) {
        this.wrapped = cause;
    }

    public Exception getWrapped() {
        return wrapped;
    }
}
