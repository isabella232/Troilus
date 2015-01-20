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
package net.oneandone.troilus;


import static com.datastax.driver.core.querybuilder.QueryBuilder.bindMarker;
import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.update;

import java.util.List;
import java.util.Map.Entry;

import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.Clause;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;



 

class CounterMutationQueryData {

    private final ImmutableMap<String, Object> keys;
    private final ImmutableList<Clause> whereConditions;

    private final String name;
    private final long diff;

    
    public CounterMutationQueryData() {
        this(ImmutableMap.<String, Object>of(),
             ImmutableList.<Clause>of(),
             null,
             0);
    }
    
    private CounterMutationQueryData(ImmutableMap<String, Object> keys,
                                     ImmutableList<Clause> whereConditions,
                                     String name,
                                     long diff) {
        this.keys = keys;
        this.whereConditions = whereConditions;
        this.name = name; 
        this.diff = diff;
    }
   
    
    public CounterMutationQueryData keys(ImmutableMap<String, Object> keys) {
        return new CounterMutationQueryData(keys,
                                            this.whereConditions, 
                                            this.name,
                                            this.diff);
    }
    
    public CounterMutationQueryData whereConditions(ImmutableList<Clause> whereConditions) {
        return new CounterMutationQueryData(this.keys,
                                            whereConditions, 
                                            this.name,
                                            this.diff);
    }
    
    public CounterMutationQueryData name(String name) {
        return new CounterMutationQueryData(this.keys,
                                            this.whereConditions, 
                                            name,
                                            this.diff);
    }
    
    public CounterMutationQueryData diff(long diff) {
        return new CounterMutationQueryData(this.keys,
                                            this.whereConditions, 
                                            this.name,
                                            diff);
    }
    
    
    public ImmutableMap<String, Object> getKeys() {
        return keys;
    }

    public ImmutableList<Clause> getWhereConditions() {
        return whereConditions;
    }

    public String getName() {
        return name;
    }

    public long getDiff() {
        return diff;
    }
    
    Statement toStatement(Context ctx) {
        com.datastax.driver.core.querybuilder.Update update = update(ctx.getTable());
        
        // key-based update
        if (getWhereConditions().isEmpty()) {
            List<Object> values = Lists.newArrayList();
            
            if (getDiff() > 0) {
                update.with(QueryBuilder.incr(getName(), bindMarker()));
                values.add(getDiff());
                
            } else {
                update.with(QueryBuilder.decr(getName(), bindMarker()));
                values.add(0 - getDiff());
            }
     
            for (Entry<String, Object> entry : getKeys().entrySet()) {
                update.where(eq(entry.getKey(), bindMarker())); 
                values.add(entry.getValue());
            }
            
            if (ctx.getTtlSec() != null) {
                update.using(QueryBuilder.ttl(bindMarker())); 
                values.add(ctx.getTtlSec().intValue());
            }
            
            return ctx.prepare(update).bind(values.toArray());

            
        // where condition-based update
        } else {
            
            if (getDiff() > 0) {
                update.with(QueryBuilder.incr(getName(), getDiff()));
                
            } else {
                update.with(QueryBuilder.decr(getName(), 0 - getDiff()));
            }
                            
            if (ctx.getTtlSec() != null) {
                update.using(QueryBuilder.ttl(ctx.getTtlSec().intValue()));
            }
            
            for (Clause whereCondition : getWhereConditions()) {
                update.where(whereCondition);
            }
            
            return update;
        }
    }
}