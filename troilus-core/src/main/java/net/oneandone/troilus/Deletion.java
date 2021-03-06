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


import java.util.Map;

import com.datastax.driver.core.querybuilder.Clause;



/**
 * delete query
 */
public interface Deletion extends Batchable<Deletion> {

    /**
     * @param conditions  the conditions
     * @return a cloned query instance with lwt (only-if)
     */
    Mutation<Deletion, Result> onlyIf(Clause... conditions);
    
    /**
     * @return a cloned query instance with lwt (if-exits)
     */
    Mutation<Deletion, Result> ifExists();
    
    /**
     * this method will remove a provided map entry for a column 
     * of type "map" in repository
     * 
     * @param columnName
     * @param mapKey
     * @return
     */
    Deletion removeMapValue(String columnName, Object mapKey);
    
    /**
     * this method allows the caller to provide a ColumnName object 
     * and a mapKey to remove a map entry
     * 
     */
    <T,V> Deletion removeMapValue(ColumnName<Map<T, V>> column, Object mapKey);
}