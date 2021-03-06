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
package net.oneandone.troilus.interceptor;

import com.google.common.collect.ImmutableMap;



 
/**
 * The reqd query data
 *
 */
public interface SingleReadQueryData {

    /**
     * @param key  the key 
     * @return the new read query data
     */
    SingleReadQueryData key(ImmutableMap<String, Object> key);
    
    /**
     * @param columnsToFetchs  the columns to fetch
     * @return the new read query data
     */
    SingleReadQueryData columnsToFetch(ImmutableMap<String, Boolean> columnsToFetchs);

    /**
     * @return the key
     */
    ImmutableMap<String, Object> getKey();

    /**
     * @return the columns to fetch
     */
    ImmutableMap<String, Boolean> getColumnsToFetch();
}