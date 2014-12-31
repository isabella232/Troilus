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

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.BatchStatement.Type;
import com.google.common.collect.ImmutableList;


 
abstract class MutationQuery<Q> extends AbstractQuery<Q> implements Batchable {
    
    private final QueryFactory queryFactory;
    
    public MutationQuery(Context ctx, QueryFactory queryFactory) {
        super(ctx);
        this.queryFactory = queryFactory;
    }
    
    
    public Q withTtl(Duration ttl) {
        return newQuery(getContext().withTtl(ttl));
    }

    public Q withWritetime(long writetimeMicrosSinceEpoch) {
        return newQuery(getContext().withWritetime(writetimeMicrosSinceEpoch));
    }
       
    public Q withSerialConsistency(ConsistencyLevel consistencyLevel) {
        return newQuery(getContext().withSerialConsistency(consistencyLevel));
    }
    
    

    public BatchMutation combinedWith(Batchable other) {
        return queryFactory.newBatchMutation(getContext(), Type.LOGGED, ImmutableList.of(this, other));
    }
    
    
    
    public Result execute() {
        try {
            return executeAsync().get(Long.MAX_VALUE, TimeUnit.DAYS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw Exceptions.unwrapIfNecessary(e);
        } 
    }
    
    
    public CompletableFuture<Result> executeAsync() {
        return getContext().performAsync(getStatement()).thenApply(resultSet -> Result.newResult(resultSet));
    }
    
    
    @Override
    public String toString() {
        return getStatement().toString();
    }
}

