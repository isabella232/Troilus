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



import java.util.concurrent.CompletableFuture;

import com.datastax.driver.core.querybuilder.Clause;
import com.datastax.driver.core.querybuilder.Delete;

import static com.datastax.driver.core.querybuilder.QueryBuilder.*;

import com.datastax.driver.core.Statement;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.unitedinternet.troilus.Dao.Deletion;



 
class DeleteQuery extends MutationQuery<Deletion> implements Deletion {
    private final ImmutableMap<String, Object> keyNameValuePairs;
    private final ImmutableList<Clause> whereConditions;
    private final ImmutableList<Clause> ifConditions;
     
    
      
    protected DeleteQuery(Context ctx, 
                          QueryFactory queryFactory,
                          ImmutableMap<String, Object> keyNameValuePairs, 
                          ImmutableList<Clause> whereConditions, 
                          ImmutableList<Clause> ifConditions) {
        super(ctx, queryFactory);
        this.keyNameValuePairs = keyNameValuePairs;
        this.whereConditions = whereConditions;
        this.ifConditions = ifConditions;
    }
    

    @Override
    protected Deletion newQuery(Context newContext) {
        return newDeleteQuery(newContext,
                              keyNameValuePairs, 
                              whereConditions, 
                              ifConditions);
    }
    
    
    @Override
    public Deletion onlyIf(Clause... conditions) {
        return newDeleteQuery(keyNameValuePairs, 
                              whereConditions, 
                              ImmutableList.copyOf(conditions));
    }
    
    
 
    @Override
    public Statement getStatement() {
        Delete delete = delete().from(getTable());

        // key-based delete    
        if (whereConditions.isEmpty()) {
            ifConditions.forEach(condition -> delete.onlyIf(condition));
            
            ImmutableSet<Clause> whereClauses = keyNameValuePairs.keySet().stream().map(name -> eq(name, bindMarker())).collect(Immutables.toSet());
            whereClauses.forEach(whereClause -> delete.where(whereClause));
            
            return prepare(delete).bind(keyNameValuePairs.values().toArray());

            
        // where condition-based delete    
        } else {
            ifConditions.forEach(condition -> delete.onlyIf(condition));
            whereConditions.forEach(whereClause -> delete.where(whereClause));
           
            return delete;
        }
    }
    

    @Override
    public CompletableFuture<Result> executeAsync() {
        return super.executeAsync().thenApply(result -> {
                                                            // check cas result column '[applied]'
                                                            if (!ifConditions.isEmpty() && !result.wasApplied()) {
                                                                throw new IfConditionException("if condition does not match");  
                                                            } 
                                                            return result;
                                                        });
    }
}