package com.aaronicsubstances.largelistpaging;

import java.util.List;

public class DefaultPaginationEventListener<T> implements PaginationEventListener<T> {

    @Override
    public void dispose() {

    }

    @Override
    public void onInitialDataLoading(int reqId) {

    }

    @Override
    public void onDataLoading(int reqId, boolean isScrollInForwardDirection) {

    }

    @Override
    public void onDataLoadIgnored(int reqId, boolean isInitialLoad, boolean isScrollInForwardDirection) {

    }

    @Override
    public void onDataLoaded(int reqId, List<T> data, boolean dataValid, boolean isScrollInForwardDirection) {

    }

    @Override
    public void onDataLoadError(int reqId, Throwable error, boolean isScrollInForwardDirection) {
        if (error instanceof Error) {
            throw (Error)error;
        }
        if (error instanceof RuntimeException) {
            throw (RuntimeException) error;
        }
        throw new RuntimeException(error);
    }

    @Override
    public void onDataInvalidated() {

    }
}
