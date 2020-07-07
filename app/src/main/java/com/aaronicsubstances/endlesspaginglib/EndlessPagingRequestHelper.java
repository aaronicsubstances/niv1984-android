/*
 * Copyright 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.aaronicsubstances.endlesspaginglib;

import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.AnyThread;
import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

class EndlessPagingRequestHelper {
    private final Object mLock = new Object();
    @GuardedBy("mLock")
    private final RequestQueue[] mRequestQueues = new RequestQueue[]
            { new RequestQueue(RequestType.BEFORE), new RequestQueue(RequestType.AFTER)  };

    /**
     * Runs the given {@link Request} if no other requests in the given request type is already
     * running.
     * <p>
     * If run, the request will be run in the current thread.
     *
     * @param type    The type of the request.
     * @param request The request to run.
     * @return True if the request is run, false otherwise.
     */
    @SuppressWarnings("WeakerAccess")
    @AnyThread
    public boolean runIfNotRunning(@NonNull RequestType type, @NonNull Request request) {
        synchronized (mLock) {
            RequestQueue queue = mRequestQueues[type.ordinal()];
            if (queue.mRunning != null) {
                return false;
            }
            queue.mRunning = request;
        }
        final RequestWrapper wrapper = new RequestWrapper(request, this, type);
        wrapper.run();
        return true;
    }

    @AnyThread
    @VisibleForTesting
    void recordResult(@NonNull RequestWrapper wrapper) {
        synchronized (mLock) {
            RequestQueue queue = mRequestQueues[wrapper.mType.ordinal()];
            queue.mRunning = null;
        }
    }

    static class RequestWrapper implements Runnable {
        @NonNull
        final Request mRequest;
        @NonNull
        final EndlessPagingRequestHelper mHelper;
        @NonNull
        final RequestType mType;
        RequestWrapper(@NonNull Request request, @NonNull EndlessPagingRequestHelper helper,
                       @NonNull RequestType type) {
            mRequest = request;
            mHelper = helper;
            mType = type;
        }
        @Override
        public void run() {
            mRequest.run(new Request.Callback(this, mHelper));
        }
    }

    /**
     * Runner class that runs a request tracked by the {@link EndlessPagingRequestHelper}.
     * <p>
     * When a request is invoked, it must call {@link Callback#recordCompletion()} once and only once. This call
     * can be made any time. Until that method call is made, {@link EndlessPagingRequestHelper} will
     * consider the request is running.
     */
    @FunctionalInterface
    public interface Request {
        /**
         * Should run the request and call the given {@link Callback} with the result of the
         * request.
         *
         * @param callback The callback that should be invoked with the result.
         */
        void run(Callback callback);
        /**
         * Callback class provided to the {@link #run(Callback)} method to report the result.
         */
        class Callback {
            private final AtomicBoolean mCalled = new AtomicBoolean();
            private final RequestWrapper mWrapper;
            private final EndlessPagingRequestHelper mHelper;
            Callback(RequestWrapper wrapper, EndlessPagingRequestHelper helper) {
                mWrapper = wrapper;
                mHelper = helper;
            }

            /**
             * Call this method when the request succeeds and new data is fetched.
             */
            @SuppressWarnings("unused")
            public final void recordCompletion() {
                if (mCalled.compareAndSet(false, true)) {
                    mHelper.recordResult(mWrapper);
                } else {
                    throw new IllegalStateException(
                            "already called recordCompletion");
                }
            }
        }
    }

    /**
     * Available request types.
     */
    public enum RequestType {

        /**
         * Used for loading items at the beginning of a paged list.
         */
        BEFORE,

        /**
         * Used for loading items at the end of a paged list.
         */
        AFTER
    }

    class RequestQueue {
        @NonNull
        final RequestType mRequestType;
        @Nullable
        Request mRunning;
        RequestQueue(@NonNull RequestType requestType) {
            mRequestType = requestType;
        }
    }
}