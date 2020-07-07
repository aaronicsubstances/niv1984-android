package com.aaronicsubstances.endlesspaginglib;

import java.util.List;

public class EndlessListLoadResult<T> {
    private final List<T> pagedList;
    private final Throwable error;
    private final boolean listValid;

    public EndlessListLoadResult(List<T> pagedList) {
        this(pagedList, null, true);
    }

    public EndlessListLoadResult(List<T> pagedList, Throwable error) {
        this(pagedList, error, true);
    }

    public EndlessListLoadResult(List<T> pagedList, Throwable error, boolean listValid) {
        this.pagedList = pagedList;
        this.error = error;
        this.listValid = listValid;
    }

    public List<T> getPagedList() {
        return pagedList;
    }

    public Throwable getError() {
        return error;
    }

    public boolean isListValid() {
        return listValid;
    }
}
