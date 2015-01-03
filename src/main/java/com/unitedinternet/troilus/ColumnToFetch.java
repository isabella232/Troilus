/*
 * Copyright (c) 2014 1&1 Internet AG, Germany, http://www.1und1.de
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.unitedinternet.troilus;

import java.util.function.Consumer;

import com.datastax.driver.core.querybuilder.Select;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;


@Deprecated // use Map<String, Boolean> instead; boolean -> with meta data
class ColumnToFetch implements Consumer<Select.Selection> {
    private final String name;
    private final boolean isFetchWritetime;
    private final boolean isFetchTtl;
    
    private ColumnToFetch(String name, boolean isFetchWritetime, boolean isFetchTtl) {
        this.name = name;
        this.isFetchWritetime = isFetchWritetime;
        this.isFetchTtl = isFetchTtl;
    }
    
    public static ColumnToFetch create(String name, boolean isFetchWritetime, boolean isFetchTtl) {
        return new ColumnToFetch(name, isFetchWritetime, isFetchTtl);
    }
    
    public static ImmutableSet<ColumnToFetch> create(ImmutableCollection<String> names) {
        return names.stream().map(name -> new ColumnToFetch(name, false, false)).collect(Immutables.toSet());
    }

    @Override
    public void accept(Select.Selection selection) {
         selection.column(name);

         if (isFetchTtl) {
             selection.ttl(name);
         }

         if (isFetchWritetime) {
             selection.writeTime(name);
         }
    }
}