/*
 * Copyright 1&1 Internet AG, https://github.com/1and1/
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.oneandone.troilus;




import java.util.concurrent.CompletableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

import com.google.common.util.concurrent.ListenableFuture;



class CompletableFutures {
    
    private CompletableFutures() { }
    
    public static <T> T getUninterruptibly(final CompletableFuture<T> future) {
        try {
            return future.get();
        } catch (ExecutionException | InterruptedException | RuntimeException e) {
            throw ListenableFutures.unwrapIfNecessary(e);
        }
    }
    
    public static <V> CompletableFuture<V> failedFuture(final Throwable throwable) {
        final CompletableFuture<V> future = new CompletableFuture<>();
        future.completeExceptionally(throwable);
        return future;
    }
    
    public static <T> CompletableFuture<T> toCompletableFuture(final ListenableFuture<T> future) {
        return new ListenableToCompletableFutureAdapter<>(future);
    }
    
    
    /**
     * Adapter which maps a ListenableFuture into a CompletableFuture  
     */
    private static class ListenableToCompletableFutureAdapter<T> extends CompletableFuture<T> {
        
        /**
         * @param rsFuture the underlying ResultSetFuture
         */
        public ListenableToCompletableFutureAdapter(final ListenableFuture<T> future) {
            Runnable resultHandler = () -> { 
                try {
                    complete(future.get());
                } catch (ExecutionException ee) {
                    completeExceptionally((ee.getCause() == null) ? ee : ee.getCause());
                } catch (InterruptedException | RuntimeException e) {
                    completeExceptionally(e);
                }
            };
            
            future.addListener(resultHandler, ForkJoinPool.commonPool());
        }
    }   
}