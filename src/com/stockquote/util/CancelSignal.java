package com.stockquote.util;

/** Lets plain (non-Android) worker classes poll whether their AsyncTask was cancelled. */
public interface CancelSignal {
    boolean isCancelled();
}
