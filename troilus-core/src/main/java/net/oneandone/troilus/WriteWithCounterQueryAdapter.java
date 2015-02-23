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




import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import net.oneandone.troilus.AbstractQuery;
import net.oneandone.troilus.Context;
import net.oneandone.troilus.ColumnName;
import net.oneandone.troilus.Result;

import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.Clause;
import com.google.common.collect.ImmutableMap;


 
/**
 * Java8 adapter of a UpdateQuery
 */
class WriteWithCounterQueryAdapter extends AbstractQuery<WriteWithCounterQueryAdapter> implements WriteWithCounter {
    
    private final WriteWithCounterQuery query;
    
    /**
     * @param ctx     the context 
     * @param query   the underlying query
     */
    WriteWithCounterQueryAdapter(Context ctx, WriteWithCounterQuery query) {
        super(ctx);
        this.query = query;
    }


    ////////////////////
    // factory methods
     
    @Override
    protected WriteWithCounterQueryAdapter newQuery(Context newContext) {
        return new WriteWithCounterQueryAdapter(newContext, getQuery().newQuery(newContext));
    }

    private WriteWithCounterQueryAdapter newQuery(WriteWithCounterQuery query) {
        return new WriteWithCounterQueryAdapter(getContext(), query.newQuery(getContext()));
    }
    
    //
    ////////////////////

    
    private WriteWithCounterQuery getQuery() {
        return query;
    }
    
    public CompletableFuture<Statement> getStatementAsync() {
        return CompletableFutures.toCompletableFuture(query.getStatementAsync());
    }
    
    @Override
    public Result execute() {
        return CompletableFutures.getUninterruptibly(executeAsync());
    }
    
    @Override
    public CompletableFuture<Result> executeAsync() {
        return CompletableFutures.toCompletableFuture(query.executeAsync());
    }
    
    @Override
    public BatchMutation combinedWith(Mutation<?> other) {
        return new BatchMutationQueryAdapter(getContext(), query.combinedWith(MutationQueryAdapter.toJava7Mutation(other)));
    }
    

    @Override
    public WriteWithCounterQueryAdapter withTtl(Duration ttl) {
        return newQuery(getQuery().withTtl((int) ttl.getSeconds()));
    }

    @Override
    public Update<Write> onlyIf(Clause... conditions) {
        return newQuery(getQuery().onlyIf(conditions));
    }

    @Override
    public Insertion ifNotExists() {
        return new InsertQueryAdapter(getContext(), getQuery().ifNotExists());
    }

    @Override
    public WriteWithCounterQueryAdapter entity(Object entity) {
        return newQuery(getQuery().entity(entity));
    }
    
    @Override
    public WriteWithCounterQueryAdapter value(String name, Object value) {
        return newQuery(getQuery().value(name, value));
    }
    
    @Override
    public <T> Write value(ColumnName<T> name, T value) {
        return newQuery(getQuery().value(name.getName(), value));
    }
    
    @Override
    public WriteWithCounterQueryAdapter values(ImmutableMap<String, Object> nameValuePairsToAdd) {
        return newQuery(getQuery().values(nameValuePairsToAdd));
    }

    @Override
    public WriteWithCounterQueryAdapter removeSetValue(String name, Object value) {
        return newQuery(getQuery().removeSetValue(name, value));
    }
    
    @Override
    public <T> WriteWithCounterQueryAdapter removeSetValue(ColumnName<Set<T>> name, T value) {
        return removeSetValue(name.getName(), value);
    }

    @Override
    public WriteWithCounterQueryAdapter addSetValue(String name, Object value) {
        return newQuery(getQuery().addSetValue(name, value));
    }
    
    @Override
    public <T> WriteWithCounterQueryAdapter addSetValue(ColumnName<Set<T>> name, T value) {
        return addSetValue(name.getName(), value);
    }
   
    @Override
    public WriteWithCounterQueryAdapter prependListValue(String name, Object value) {
        return newQuery(getQuery().prependListValue(name, value));
    } 
    
    @Override
    public <T> WriteWithCounterQueryAdapter prependListValue(ColumnName<List<T>> name, T value) {
        return prependListValue(name.getName(), value);
    }
    
    @Override
    public WriteWithCounterQueryAdapter appendListValue(String name, Object value) {
        return newQuery(getQuery().appendListValue(name, value));
    }
    
    @Override
    public <T> WriteWithCounterQueryAdapter appendListValue(ColumnName<List<T>> name, T value) {
        return appendListValue(name.getName(), value);
    }
    
    @Override
    public WriteWithCounterQueryAdapter removeListValue(String name, Object value) {
        return newQuery(getQuery().removeListValue(name, value));
    }
    
    @Override
    public <T> WriteWithCounterQueryAdapter removeListValue(ColumnName<List<T>> name, T value) {
        return removeListValue(name.getName(), value);
    }
    
    @Override
    public WriteWithCounterQueryAdapter putMapValue(String name, Object key, Object value) {
        return newQuery(getQuery().putMapValue(name, key, value));
    }
    
    @Override
    public <T, V> WriteWithCounterQueryAdapter putMapValue(ColumnName<Map<T, V>> name, T key, V value) {
        return putMapValue(name.getName(), key, value);
    }
        
    @Override
    public CounterMutationQueryAdapter incr(String name) {
        return new CounterMutationQueryAdapter(getContext(), getQuery().incr(name));
    }
    
    @Override
    public CounterMutationQueryAdapter incr(String name, long value) {
        return new CounterMutationQueryAdapter(getContext(), getQuery().incr(name, value));
    }
    
    @Override
    public CounterMutationQueryAdapter decr(String name) {
        return new CounterMutationQueryAdapter(getContext(), getQuery().decr(name));
    }
    
    @Override
    public CounterMutationQueryAdapter decr(String name, long value) {
        return new CounterMutationQueryAdapter(getContext(), getQuery().decr(name, value));
    }
}
