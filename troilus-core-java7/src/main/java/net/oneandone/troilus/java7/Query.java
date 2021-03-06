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
package net.oneandone.troilus.java7;

import com.google.common.util.concurrent.ListenableFuture;



/**
 * The Query 
 * @param <T>  the result type
 */
public interface Query<T> {

    /**
     * performs the query in an async way 
     * @return the result future 
     */
    ListenableFuture<T> executeAsync();
    
    /**
     * performs the query in a sync way 
     * @return the result
     */
    T execute();
} 


