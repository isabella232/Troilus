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
package com.unitedinternet.troilus.minimal;


import java.util.concurrent.CompletableFuture;

import org.reactivestreams.Publisher;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.policies.RetryPolicy;
import com.datastax.driver.core.querybuilder.Clause;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListenableFuture;
import com.unitedinternet.troilus.Count;
import com.unitedinternet.troilus.Name;
import com.unitedinternet.troilus.Result;
import com.unitedinternet.troilus.interceptor.QueryInterceptor;






public interface MinimalDao {
    
    MinimalDao withConsistency(ConsistencyLevel consistencyLevel);

    MinimalDao withSerialConsistency(ConsistencyLevel consistencyLevel);

    MinimalDao withTracking();

    MinimalDao withoutTracking();

    MinimalDao withRetryPolicy(RetryPolicy policy);
        
    MinimalDao withInterceptor(QueryInterceptor queryInterceptor);
    
    
    public interface Query<T> {

        ListenableFuture<T> executeAsync();
        
        T execute();
    }

    
    public static interface ConfiguredQuery<Q, R> extends Query<R> {

        Q withConsistency(ConsistencyLevel consistencyLevel);

        Q withEnableTracking();

        Q withDisableTracking();
        
        Q withRetryPolicy(RetryPolicy policy);
    }

    
    
    
    ////////////////////////////////
    // MUTATIONS

    
    UpdateWithValuesAndCounter writeWhere(Clause... clauses);

    Insertion writeEntity(Object entity);

    
    WriteWithCounter writeWithKey(ImmutableMap<String, Object> composedKeyParts);
    
    WriteWithCounter writeWithKey(String keyName, Object keyValue);

    WriteWithCounter writeWithKey(String composedKeyNamePart1, Object composedKeyValuePart1,
                                  String composedKeyNamePart2, Object composedKeyValuePart2);

    WriteWithCounter writeWithKey(String composedKeyNamePart1, Object composedKeyValuePart1,
                                  String composedKeyNamePart2, Object composedKeyValuePart2, 
                                  String composedKeyNamePart3, Object composedKeyValuePart3);

    <T> WriteWithCounter writeWithKey(Name<T> keyName, T keyValue);

    <T, E> WriteWithCounter writeWithKey(Name<T> composedKeyNamePart1, T composedKeyValuePart1,
                                         Name<E> composedKeyNamePart2, E composedKeyValuePart2);

    <T, E, F> WriteWithCounter writeWithKey(Name<T> composedKeyNamePart1, T composedKeyValuePart1, 
                                            Name<E> composedKeyNamePart2, E composedKeyValuePart2, 
                                            Name<F> composedKeyNamePart3, F composedKeyValuePart3);




    Deletion deleteWithKey(ImmutableMap<String, Object> composedKeyParts);
    
    Deletion deleteWithKey(String keyname, Object keyValue);

    Deletion deleteWithKey(String composedKeyNamePart1, Object composedKeyValuePart1, 
                           String composedKeyNamePart2, Object composedKeyValuePart2);

    Deletion deleteWithKey(String composedKeyNamePart1, Object composedKeyValuePart1, 
                           String composedKeyNamePart2, Object composedKeyValuePart2, 
                           String composedKeyNamePart3, Object composedKeyValuePart3);

    <T> Deletion deleteWithKey(Name<T> keyName, T keyValue);

    <T, E> Deletion deleteWithKey(Name<T> composedKeyNamePart1, T composedKeyValuePart1, 
                                  Name<E> composedKeyNamePart2, E composedKeyValuePart2);

    <T, E, F> Deletion deleteWithKey(Name<T> composedKeyNamePart1, T composedKeyValuePart1, 
                                     Name<E> composedKeyNamePart2, E composedKeyValuePart2, 
                                     Name<F> composedKeyNamePart3, F composedKeyValuePart3);


    Deletion deleteWhere(Clause... whereConditions);

    
    
    
    
    public static interface Mutation<Q extends Mutation<Q>> extends ConfiguredQuery<Q, Result> {

        Mutation<Q> withSerialConsistency(ConsistencyLevel consistencyLevel);

        Mutation<Q> withTtl(int ttlSec);

        Mutation<Q> withWritetime(long microsSinceEpoch);
    }
    
    
    public static interface Batchable {

        void addTo(BatchStatement batchStatement);
    }


    

    public static interface BatchableMutation<Q extends BatchableMutation<Q>> extends Mutation<BatchableMutation<Q>>, Batchable {

        BatchMutation combinedWith(Batchable other);
    }

    
    
    public interface BatchMutation extends ConfiguredQuery<BatchMutation, Result> {

        BatchMutation combinedWith(Batchable other);

        Query<Result> withLockedBatchType();

        Query<Result> withUnlockedBatchType();
    }

    
    
    
    public static interface Insertion extends BatchableMutation<Insertion> {

        Mutation<?> ifNotExits();
    }
  
    public static interface Update<U extends BatchableMutation<U>> extends BatchableMutation<U> {

        Mutation<?> onlyIf(Clause... conditions);
    }

    
    
    public static interface Write extends UpdateWithValues<Write> {
        
        Mutation<?> ifNotExits();
    }

    
    
    public static interface WithCounter {

        CounterMutation decr(String name); 
        
        CounterMutation decr(String name, long value); 
        
        CounterMutation incr(String name);  

        CounterMutation incr(String name, long value);   
    }

    

    public static interface WriteWithCounter extends Write, WithCounter {

    }

    
    
    public static interface UpdateWithValues<U extends Update<U>> extends Update<U> {

        U value(String name, Object value);

        U values(ImmutableMap<String, Object> nameValuePairsToAdd);
        
        <T> U value(Name<T> name, T value);
        
        U removeSetValue(String name, Object value);

        U addSetValue(String name, Object value);

        U appendListValue(String name, Object value);

        U prependListValue(String name, Object value);

        U removeListValue(String name, Object value);

        U putMapValue(String name, Object key, Object value);
    }


    
    public static interface UpdateWithValuesAndCounter extends UpdateWithValues<Write>, WithCounter {
 
    }

    
    public static interface CounterBatchable {

        void addTo(BatchStatement batchStatement);
    }

    
     
    
    public static interface CounterMutation extends ConfiguredQuery<CounterMutation, Result>, CounterBatchable  {

        CounterMutation withSerialConsistency(ConsistencyLevel consistencyLevel);

        CounterMutation withTtl(int ttlSec);

        CounterMutation withWritetime(long microsSinceEpoch);
        
        CounterBatchMutation combinedWith(CounterBatchable other);
    }

    

    public interface CounterBatchMutation extends ConfiguredQuery<CounterBatchMutation, Result> {

        CounterBatchMutation combinedWith(CounterBatchable other);
    }


    
    

    public static interface Deletion extends BatchableMutation<Deletion> {

        Mutation<?> onlyIf(Clause... conditions);
    }


    
    
    ////////////////////////////////
    // READ

    SingleReadWithUnit<Record> readWithKey(ImmutableMap<String, Object> composedKeyParts);
    
    SingleReadWithUnit<Record> readWithKey(String keyName, Object keyValue);

    SingleReadWithUnit<Record> readWithKey(String composedKeyNamePart1, Object composedKeyValuePart1, 
                                           String composedKeyNamePart2, Object composedKeyValuePart2);

    SingleReadWithUnit<Record> readWithKey(String composedKeyNamePart1, Object composedKeyValuePart1, 
                                           String composedKeyNamePart2, Object composedKeyValuePart2,
                                           String composedKeyNamePart3, Object composedKeyValuePart3);


    <T> SingleReadWithUnit<Record> readWithKey(Name<T> keyName, T keyValue);

    <T, E> SingleReadWithUnit<Record> readWithKey(Name<T> composedKeyNamePart1, T composedKeyValuePart1, 
                                                  Name<E> composedKeyNamePart2, E composedKeyValuePart2);

    <T, E, F> SingleReadWithUnit<Record> readWithKey(Name<T> composedKeyNamePart1, T composedKeyValuePart1, 
                                                     Name<E> composedKeyNamePart2, E composedKeyValuePart2, 
                                                     Name<F> composedKeyNamePart3, F composedKeyValuePart3);

    ListReadWithUnit<RecordList> readWithKeys(String name, ImmutableList<Object> values);

    ListReadWithUnit<RecordList> readWithKeys(String composedKeyNamePart1, Object composedKeyValuePart1, 
                                              String composedKeyNamePart2, ImmutableList<Object> composedKeyValuesPart2);

    ListReadWithUnit<RecordList> readWithKeys(String composedKeyNamePart1, Object composedKeyValuePart1, 
                                              String composedKeyNamePart2, Object composedKeyValuePart2,
                                              String composedKeyNamePart3, ImmutableList<Object> composedKeyValuesPart3);

    
    <T> ListReadWithUnit<RecordList> readWithKeys(Name<T> name, ImmutableList<T> values);

    <T, E> ListReadWithUnit<RecordList> readWithKeys(Name<T> composedKeyNamePart1, T composedKeyValuePart1, 
                                                     Name<E> composedKeyNamePart2, ImmutableList<E> composedKeyValuesPart2);

    <T, E, F> ListReadWithUnit<RecordList> readWithKeys(Name<T> composedKeyNamePart1, T composedKeyValuePart1, 
                                                        Name<E> composedKeyNamePart2, E composedKeyValuePart2,
                                                        Name<F> composedKeyNamePart3, ImmutableList<F> composedKeyValuesPart3);
    
    ListReadWithUnit<RecordList> readAll();

    ListReadWithUnit<RecordList> readWhere(Clause... clauses);

    
    
    public static interface RecordList extends Result, Iterable<Record>, Publisher<Record> {   
        
    }

    
    
    
    public static interface SingleRead<T> extends Query<T> {

        SingleRead<T> withEnableTracking();

        SingleRead<T> withDisableTracking();

        SingleRead<T> withConsistency(ConsistencyLevel consistencyLevel);
    }

    public static interface SingleReadWithColumns<T> extends SingleRead<T> {

        SingleReadWithColumns<T> column(String name);

        SingleReadWithColumns<T> columnWithMetadata(String name);

        SingleReadWithColumns<T> columns(String... names);
        
        SingleReadWithColumns<T> column(Name<?> name);

        SingleReadWithColumns<T> columnWithMetadata(Name<?> name);

        SingleReadWithColumns<T> columns(Name<?>... names);

    }

    public static interface SingleReadWithUnit<T> extends SingleReadWithColumns<T> {

        SingleRead<T> all();

        <E> SingleRead<E> asEntity(Class<E> objectClass);
    }
    

       
    public static interface ListRead<T> extends SingleRead<T> {

        ListRead<T> withLimit(int limit);

        ListRead<T> withFetchSize(int fetchSize);

        ListRead<T> withDistinct();

        ListRead<T> withAllowFiltering();
    }

    public static interface ListReadWithColumns<T> extends ListRead<T> {

        ListReadWithColumns<T> column(String name);

        ListReadWithColumns<T> columnWithMetadata(String name);

        ListReadWithColumns<T> columns(String... names);

        ListReadWithColumns<T> column(Name<?> name);

        ListReadWithColumns<T> columnWithMetadata(Name<?> name);

        ListReadWithColumns<T> columns(Name<?>... names);
    }
    
    public static interface ListReadWithUnit<T> extends ListReadWithColumns<T> {

        ListRead<T> all();

        ListRead<Count> count();

        <E> ListRead<EntityList<E>> asEntity(Class<E> objectClass);
    } 
    

    public static interface EntityList<E> extends Result, Iterable<E>, Publisher<E> {

    }
}
