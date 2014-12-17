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
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.BatchStatement.Type;
import com.datastax.driver.core.querybuilder.Clause;
import com.datastax.driver.core.querybuilder.Delete;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Select.Selection;
import com.datastax.driver.core.ConsistencyLevel;

import static com.datastax.driver.core.querybuilder.QueryBuilder.*;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ProtocolVersion;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Statement;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ListenableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


 

@SuppressWarnings("rawtypes")
public class DaoImpl implements Dao {
    
    private static final Logger LOG = LoggerFactory.getLogger(DaoImpl.class);
    private final Context defaultContext;
    
    
    public DaoImpl(Context defaultContext) {
        this.defaultContext = defaultContext;
    }
 
    
    protected Context getDefaultContext() {
        return defaultContext;
    }
    
    
    @Override
    public Dao withConsistency(ConsistencyLevel consistencyLevel) {
        return newDao(getDefaultContext().withConsistency(consistencyLevel));
    }
    
    
    protected Dao newDao(Context ctx) {
        return new DaoImpl(ctx); 
    }
    
    
    ///////////////////////////////
    // INSERT
    

    @Override
    public InsertionWithUnit insert() {
        return newInsertion(getDefaultContext(),  ImmutableMap.of());
    }
    
    @Override
    public Insertion insertObject(Object persistenceObject) {
        return newInsertion(getDefaultContext(), getDefaultContext().getPropertiesMapper(persistenceObject.getClass()).toValues(persistenceObject));
    }
    
    @Override
    public Insertion insertValues(String name1, Object value1, String name2, Object value2) {
        return insert().values(ImmutableMap.of(name1, value1, name2, value2));
    }
    
    
    @Override
    public Insertion insertValues(String name1, Object value1, String name2, Object value2, String name3, Object value3) {
        return insert().values(ImmutableMap.of(name1, value1, name2, value2, name3, value3));
    }
    
    
    @Override
    public Insertion insertValues(String name1, Object value1, String name2, Object value2, String name3, Object value3, String name4, Object value4) {
        return insert().values(ImmutableMap.of(name1, value1, name2, value2, name3, value3, name4, value4));
    }
    
    
    @Override
    public Insertion insertValues(String name1, Object value1, String name2, Object value2, String name3, Object value3, String name4, Object value4, String name5, Object value5) {
        return insert().values(ImmutableMap.of(name1, value1, name2, value2, name3, value3, name4, value4, name5, value5));
    }
  
    
    protected InsertionWithUnit newInsertion(Context ctx, ImmutableMap<String, Object> nameValuePairs) {
        return new InsertQuery(ctx, nameValuePairs);
    }
    
    
    private class InsertQuery implements InsertionWithUnit {
        private final Context ctx;
        private final ImmutableMap<String, Object> nameValuePairs;
        
        public InsertQuery(Context ctx, ImmutableMap<String, Object> nameValuePairs) {
            this.ctx = ctx;
            this.nameValuePairs = nameValuePairs;
        }
        
        @Override
        public Insertion entity(Object persistenceObject) {
            return newInsertion(ctx, ctx.getPropertiesMapper(persistenceObject.getClass()).toValues(persistenceObject));
        }
        
        @Override
        public InsertionWithValues value(String name, Object value) {
            if (value instanceof Optional) {
                if (((Optional) value).isPresent()) {
                    value = ((Optional) value).get();
                } else {
                    return this;
                }
            }
            
            return newInsertion(ctx, Immutables.merge(nameValuePairs, name, value));
        }

        
        @SuppressWarnings("unchecked")
        @Override
        public InsertionWithValues values(ImmutableMap<String , Object> nameValuePairsToAdd) {
            
            // convert optional values
            Map<String, Object> pairs = Maps.newHashMap();
            nameValuePairsToAdd.forEach((name, value) -> {
                                                            if (value instanceof Optional) {
                                                                ((Optional) value).ifPresent(v -> pairs.put(name, v));
                                                            } else {
                                                                pairs.put(name, value);
                                                            }
                                                        });
            
            return newInsertion(ctx, Immutables.merge(nameValuePairs, ImmutableMap.copyOf(pairs)));
        }
        
        
        
        @Override
        public Insertion withConsistency(ConsistencyLevel consistencyLevel) {
            return newInsertion(ctx.withConsistency(consistencyLevel), nameValuePairs);
        }
        
        
        @Override
        public Insertion withSerialConsistency(ConsistencyLevel consistencyLevel) {
            return newInsertion(ctx.withSerialConsistency(consistencyLevel), nameValuePairs);
        }
        
        
        @Override
        public Insertion ifNotExits() {
            return newInsertion(ctx.ifNotExits(), nameValuePairs);
        }
        
        @Override
        public Insertion withTtl(Duration ttl) {
            return newInsertion(ctx.withTtl(ttl), nameValuePairs);
        }

        @Override
        public Insertion withWritetime(long writetimeMicrosSinceEpoch) {
            return newInsertion(ctx.withWritetime(writetimeMicrosSinceEpoch), nameValuePairs);
        }
        
        @Override
        public BatchMutation combinedWith(Mutation other) {
            return newBatchMutation(ctx, Type.LOGGED, ImmutableList.of(this, other));
        }
      
        
        @Override
        public Statement getStatement() {
            
            // statement
            Insert insert = insertInto(ctx.getTable());
            nameValuePairs.keySet().forEach(name -> insert.value(name, bindMarker()));
            
            if (ctx.getIfNotExits()) {
                insert.ifNotExists();
                ctx.getSerialConsistencyLevel().ifPresent(serialCL -> insert.setSerialConsistencyLevel(serialCL));
            }

            if (ctx.getTtl().isPresent())  {
                insert.using(QueryBuilder.ttl(bindMarker()));
            }

            PreparedStatement stmt = ctx.prepare(insert);
            
            // bind variables
            ImmutableList<Object> values = ImmutableList.copyOf(nameValuePairs.values());
            if (ctx.getTtl().isPresent()) {
                values = ImmutableList.<Object>builder().addAll(values).add((int) ctx.getTtl().get().getSeconds()).build();
            }
            
            return stmt.bind(values.toArray());
        }
        
        
        public Void execute() {
            try {
                return executeAsync().get(10000, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                throw Exceptions.unwrapIfNecessary(e);
            } 
        }
        
        
        @Override
        public CompletableFuture<Void> executeAsync() {
            return ctx.performAsync(getStatement()).thenApply(resultSet -> {
                    if (ctx.getIfNotExits()) {
                        // check cas result column '[applied]'
                        if (!resultSet.wasApplied()) {
                            throw new AlreadyExistsConflictException("duplicated entry");  
                        }
                    } 
                    return null;
                });
        }
    }


    
    
    ///////////////////////////////
    // DELETE
    
    @Override
    public Deletion deleteWithKey(String keyName, Object keyValue) {
        return newDeletion(getDefaultContext(), ImmutableMap.of(keyName, keyValue));
    }

    @Override
    public Deletion deleteWithKey(String keyName1, Object keyValue1, String keyName2, Object keyValue2) {
        return newDeletion(getDefaultContext(), ImmutableMap.of(keyName1, keyValue1, keyName2, keyValue2));
    }
    
    @Override
    public Deletion deleteWithKey(String keyName1, Object keyValue1, String keyName2, Object keyValue2, String keyName3, Object keyValue3) {
        return newDeletion(getDefaultContext(), ImmutableMap.of(keyName1, keyValue1, keyName2, keyValue2, keyName3, keyValue3));
    }
    
    @Override
    public Deletion deleteWithKey(String keyName1, Object keyValue1, String keyName2, Object keyValue2, String keyName3, Object keyValue3, String keyName4, Object keyValue4) {
        return newDeletion(getDefaultContext(), ImmutableMap.of(keyName1, keyValue1, keyName2, keyValue2, keyName3, keyValue3, keyName4, keyValue4));
    }
    
    protected Deletion newDeletion(Context ctx, ImmutableMap<String, Object> keyNameValuePairs) {
        return new DeleteQuery(ctx, keyNameValuePairs);
    }
    
    private class DeleteQuery implements Deletion {
        private final Context ctx;
        private final ImmutableMap<String, Object> keyNameValuePairs;
        
        public DeleteQuery(Context ctx, ImmutableMap<String, Object> keyNameValuePairs) {
            this.ctx = ctx;
            this.keyNameValuePairs = keyNameValuePairs;
        }
        
        @Override
        public Deletion withConsistency(ConsistencyLevel consistencyLevel) {
            return newDeletion(ctx.withConsistency(consistencyLevel), keyNameValuePairs);
        }
        
        @Override
        public Deletion withSerialConsistency(ConsistencyLevel consistencyLevel) {
            return newDeletion(ctx.withSerialConsistency(consistencyLevel), keyNameValuePairs);
        }
        
        @Override
        public BatchMutation combinedWith(Mutation other) {
            return newBatchMutation(ctx, Type.LOGGED, ImmutableList.of(this, other));
        }
        
        
        @Override
        public Statement getStatement() {
            Delete delete = delete().from(ctx.getTable());

            Delete.Where where = null;
            for (Clause whereClause : keyNameValuePairs.keySet().stream().map(name -> eq(name, bindMarker())).collect(Immutables.toSet())) {
                if (where == null) {
                    where = delete.where(whereClause);
                } else {
                    where = where.and(whereClause);
                }
            }
            
            return ctx.prepare(delete).bind(keyNameValuePairs.values().toArray());
        }
        
        
        public Void execute() {
            try {
                return executeAsync().get(10000, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                throw Exceptions.unwrapIfNecessary(e);
            } 
        }
        
        
        public CompletableFuture<Void> executeAsync() {
            return ctx.performAsync(getStatement()).thenApply(resultSet -> null);
        }
    }

    
    protected BatchMutation newBatchMutation(Context ctx, Type type, ImmutableList<Mutation<?>> mutations) {
        return new MutationBatchQuery(ctx, type, mutations);
    }
    
     
    
    private final class MutationBatchQuery implements BatchMutation {
        private final Context ctx;
        private final ImmutableList<Mutation<?>> mutations;
        private final Type type;  
        
        public MutationBatchQuery(Context ctx, Type type, ImmutableList<Mutation<?>> mutations) {
            this.ctx = ctx;
            this.type = type;
            this.mutations = mutations;
        }
                
        
        @Override
        public Query<Void> withLockedBatchType() {
            return newBatchMutation(ctx, Type.LOGGED, mutations);
        }
        
        @Override
        public Query<Void> withUnlockedBatchType() {
            return newBatchMutation(ctx, Type.UNLOGGED, mutations);
        }
        
         
        @Override
        public BatchMutation combinedWith(Mutation<?> other) {
            return newBatchMutation(ctx, type, Immutables.merge(mutations, other));
        }
        
        @Override
        public Statement getStatement() {
            BatchStatement batchStmt = new BatchStatement(type);
            mutations.forEach(mutation -> batchStmt.add(((Batchable) mutation).getStatement()));
            return batchStmt;
        }
        
        public Void execute() {
            try {
                return executeAsync().get(10000, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                throw Exceptions.unwrapIfNecessary(e);
            } 
        }
        
        public CompletableFuture<Void> executeAsync() {
            return ctx.performAsync(getStatement()).thenApply(resultSet -> null);
        }
    }
    
    
    
    
    

    
    ///////////////////////////////
    // READ
    

    @Override
    public SingleSelectionWithUnit<Optional<Record>> readWithKey(String keyName, Object keyValue) {
        return newSingleSelection(getDefaultContext(), ImmutableMap.of(keyName, keyValue), Optional.empty());
    }
    
    @Override
    public SingleSelectionWithUnit<Optional<Record>> readWithKey(String keyName1, Object keyValue1, String keyName2, Object keyValue2) {
        return newSingleSelection(getDefaultContext(), ImmutableMap.of(keyName1, keyValue1, keyName2, keyValue2), Optional.of(ImmutableSet.of()));
    }
    
    @Override
    public SingleSelectionWithUnit<Optional<Record>> readWithKey(String keyName1, Object keyValue1, String keyName2, Object keyValue2, String keyName3, Object keyValue3) {
        return newSingleSelection(getDefaultContext(), ImmutableMap.of(keyName1, keyValue1, keyName2, keyValue2, keyName3, keyValue3), Optional.of(ImmutableSet.of()));
    }
    
    @Override
    public SingleSelectionWithUnit<Optional<Record>> readWithKey(String keyName1, Object keyValue1, String keyName2, Object keyValue2, String keyName3, Object keyValue3, String keyName4, Object keyValue4) {
        return newSingleSelection(getDefaultContext(), ImmutableMap.of(keyName1, keyValue1, keyName2, keyValue2, keyName3, keyValue3, keyName4, keyValue4), Optional.of(ImmutableSet.of()));
    }
    
    
    protected SingleSelectionWithUnit<Optional<Record>> newSingleSelection(Context ctx, 
                                                                           ImmutableMap<String, Object> keyNameValuePairs, 
                                                                           Optional<ImmutableSet<ColumnToFetch>> optionalColumnsToFetch) {
        return new SingleSelectionQuery(ctx, keyNameValuePairs, optionalColumnsToFetch);
    }
    

    private class SingleSelectionQuery implements SingleSelectionWithUnit<Optional<Record>> {
        private final Context ctx;
        private final ImmutableMap<String, Object> keyNameValuePairs;
        private final Optional<ImmutableSet<ColumnToFetch>> optionalColumnsToFetch;
         
        
        public SingleSelectionQuery(Context ctx, ImmutableMap<String, Object> keyNameValuePairs, Optional<ImmutableSet<ColumnToFetch>> optionalColumnsToFetch) {
            this.ctx = ctx;
            this.keyNameValuePairs = keyNameValuePairs;
            this.optionalColumnsToFetch = optionalColumnsToFetch;
        }
         
        
        
        @Override
        public <E> SingleSelection<Optional<E>> entity(Class<E> objectClass) {
            return newSingleSelection(ctx, this, objectClass);
        }
        
        @Override
        public SingleSelectionWithUnit<Optional<Record>> column(String name) {
            return column(name, false, false);
        }

        @Override
        public SingleSelectionWithUnit<Optional<Record>> column(String name, boolean isFetchWritetime, boolean isFetchTtl) {
            return newSingleSelection(ctx, keyNameValuePairs, Immutables.merge(optionalColumnsToFetch, ColumnToFetch.create(name, isFetchWritetime, isFetchTtl)));
        }
        
        @Override
        public SingleSelectionWithUnit<Optional<Record>> columns(String... names) {
            return columns(ImmutableSet.copyOf(names));
        }
        
        @Override 
        public SingleSelectionWithUnit<Optional<Record>> columns(ImmutableCollection<String> namesToRead) {
            return newSingleSelection(ctx, keyNameValuePairs, Immutables.merge(optionalColumnsToFetch, ColumnToFetch.create(namesToRead)));
        }
        
        @Override
        public SingleSelection<Optional<Record>> withConsistency(ConsistencyLevel consistencyLevel) {
            return newSingleSelection(ctx.withConsistency(consistencyLevel), keyNameValuePairs, optionalColumnsToFetch);
        }
       
        
        @Override
        public Optional<Record> execute() {
            try {
                return executeAsync().get(10000, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                throw Exceptions.unwrapIfNecessary(e);
            }
        }
        
        
        @Override
        public CompletableFuture<Optional<Record>> executeAsync() {
            
            Selection selection = select();
            
            if (optionalColumnsToFetch.isPresent()) {
                optionalColumnsToFetch.get().forEach(column -> column.accept(selection));

                // add key columns for paranoia checks
                keyNameValuePairs.keySet().forEach(name -> { if(!optionalColumnsToFetch.get().contains(name))  ColumnToFetch.create(name, false, false).accept(selection); });  
                
            } else {
                selection.all();
            }
            
            
            
            Select select = selection.from(ctx.getTable());
            Select.Where where = null;
            for (Clause whereClause : keyNameValuePairs.keySet().stream().map(name -> eq(name, bindMarker())).collect(Immutables.toSet())) {
                if (where == null) {
                    where = select.where(whereClause);
                } else {
                    where = where.and(whereClause);
                }
            }

            Statement statement = ctx.prepare(select).bind(keyNameValuePairs.values().toArray());
            
            
            return ctx.performAsync(statement)
                      .thenApply(resultSet -> {
                                                  Row row = resultSet.one();
                                                  if (row == null) {
                                                      return Optional.empty();
                                                      
                                                  } else {
                                                      Record record = new Record(ctx.getProtocolVersion(), row);
                        
                                                      // paranioa check
                                                      keyNameValuePairs.forEach((name, value) -> { 
                                                                                                   if (record.get(name).equals(value)) {
                                                                                                       LOG.warn("Dataswap error for " + name);
                                                                                                       throw new ProtocolErrorException("Dataswap error for " + name); 
                                                                                                   }
                                                                                                 });
                                                      
                                                      if (!resultSet.isExhausted()) {
                                                          throw new TooManyResultsException("more than one record exists");
                                                      }
                                                      
                                                      return Optional.of(record); 
                                                  }
                      });
        }
    }
     
    
    
    protected <E> SingleSelection<Optional<E>> newSingleSelection(Context ctx, SingleSelection<Optional<Record>> read, Class<?> clazz) {
        return new SingleEntitySelectionQuery<E>(ctx, read, clazz);
    }

    
    
    private class SingleEntitySelectionQuery<E> implements SingleSelection<Optional<E>> {
        private final Context ctx;
        private final SingleSelection<Optional<Record>> read;
        private final Class<?> clazz;
        
        public SingleEntitySelectionQuery(Context ctx, SingleSelection<Optional<Record>> read, Class<?> clazz) {
            this.ctx = ctx;
            this.read = read;
            this.clazz = clazz;
        }
        
        @Override
        public SingleSelection<Optional<E>> withConsistency(ConsistencyLevel consistencyLevel) {
            return newSingleSelection(ctx, read.withConsistency(consistencyLevel), clazz);
        }

        
        @Override
        public Optional<E> execute() {
            try {
                return executeAsync().get(10000, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                throw Exceptions.unwrapIfNecessary(e);
            } 
        }
        
        
        @Override
        public CompletableFuture<Optional<E>> executeAsync() {
            return read.executeAsync().thenApply(optionalRecord -> optionalRecord.map(record -> ctx.getPropertiesMapper(clazz).fromValues(record)));
        }        
    }
     
   

    
    private static class ColumnToFetch implements Consumer<Select.Selection> {
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
    
    
    
    @Override
    public ListSelectionWithUnit<Result<Record>> readWithCondition(Clause... clauses) {
        return newListSelection(getDefaultContext(), 
                                ImmutableSet.copyOf(clauses), 
                                Optional.of(ImmutableSet.of()), 
                                Optional.empty(), 
                                Optional.empty(), 
                                Optional.empty(),
                                Optional.empty());
    }
     
    
    @Override
    public ListSelectionWithUnit<Result<Record>> readAll() {
        return newListSelection(getDefaultContext(), 
                                ImmutableSet.of(), 
                                Optional.of(ImmutableSet.of()), 
                                Optional.empty(), 
                                Optional.empty(), 
                                Optional.empty(),
                                Optional.empty());
    }
    
    protected ListSelectionWithUnit<Result<Record>> newListSelection(Context ctx, 
                                                                     ImmutableSet<Clause> clauses, 
                                                                     Optional<ImmutableSet<ColumnToFetch>> columnsToFetch, 
                                                                     Optional<Integer> optionalLimit, 
                                                                     Optional<Boolean> optionalAllowFiltering,
                                                                     Optional<Integer> optionalFetchSize,    
                                                                     Optional<Boolean> optionalDistinct) {
        return new ListSelectionQuery(ctx, clauses, columnsToFetch, optionalLimit, optionalAllowFiltering, optionalFetchSize, optionalDistinct);
    }

    
    
    private class ListSelectionQuery implements ListSelectionWithUnit<Result<Record>> {
        private final Context ctx;
        private final ImmutableSet<Clause> clauses;
        private final Optional<ImmutableSet<ColumnToFetch>> columnsToFetch;
        private final Optional<Integer> optionalLimit;
        private final Optional<Boolean> optionalAllowFiltering;
        private final Optional<Integer> optionalFetchSize;
        private final Optional<Boolean> optionalDistinct;


        public ListSelectionQuery(Context ctx, 
                                  ImmutableSet<Clause> clauses, 
                                  Optional<ImmutableSet<ColumnToFetch>> columnsToFetch, 
                                  Optional<Integer> optionalLimit, 
                                  Optional<Boolean> optionalAllowFiltering,
                                  Optional<Integer> optionalFetchSize,
                                  Optional<Boolean> optionalDistinct) {
            this.ctx = ctx;
            this.clauses = clauses;
            this.columnsToFetch = columnsToFetch;
            this.optionalLimit = optionalLimit;
            this.optionalAllowFiltering = optionalAllowFiltering;
            this.optionalFetchSize = optionalFetchSize;
            this.optionalDistinct = optionalDistinct;
        }
        
        
        @Override
        public ListSelection<Result<Record>> withConsistency(ConsistencyLevel consistencyLevel) {
            return newListSelection(ctx.withConsistency(consistencyLevel), 
                                    clauses, 
                                    columnsToFetch, 
                                    optionalLimit, 
                                    optionalAllowFiltering, 
                                    optionalFetchSize,
                                    optionalDistinct);
        }
        
        @Override 
        public ListSelectionWithUnit<Result<Record>> columns(ImmutableCollection<String> namesToRead) {
            return newListSelection(ctx, 
                                    clauses, 
                                    Immutables.merge(columnsToFetch, ColumnToFetch.create(namesToRead)), 
                                    optionalLimit, 
                                    optionalAllowFiltering, 
                                    optionalFetchSize,
                                    optionalDistinct);
        }
        
        @Override
        public ListSelectionWithUnit<Result<Record>> column(String name, boolean isFetchWritetime, boolean isFetchTtl) {
            return newListSelection(ctx, 
                                    clauses,  
                                    Immutables.merge(columnsToFetch, ColumnToFetch.create(name, isFetchWritetime, isFetchTtl)), 
                                    optionalLimit, 
                                    optionalAllowFiltering,
                                    optionalFetchSize,
                                    optionalDistinct);
        }
        

        @Override
        public ListSelection<Result<Record>> withLimit(int limit) {
            return newListSelection(ctx,
                                    clauses, 
                                    columnsToFetch, 
                                    Optional.of(limit), 
                                    optionalAllowFiltering, 
                                    optionalFetchSize,
                                    optionalDistinct);
        }
        
        @Override
        public ListSelection<Result<Record>> withAllowFiltering() {
            return newListSelection(ctx, 
                                    clauses, 
                                    columnsToFetch, 
                                    optionalLimit, 
                                    Optional.of(true), 
                                    optionalFetchSize,
                                    optionalDistinct);
        }

        @Override
        public ListSelection<Result<Record>> withFetchSize(int fetchSize) {
            return newListSelection(ctx, 
                                    clauses, 
                                    columnsToFetch, 
                                    optionalLimit, 
                                    optionalAllowFiltering, 
                                    Optional.of(fetchSize),
                                    optionalDistinct);
        }
        
        @Override
        public ListSelection<Result<Record>> withDistinct() {
            return newListSelection(ctx, 
                                    clauses, 
                                    columnsToFetch, 
                                    optionalLimit, 
                                    optionalAllowFiltering, 
                                    optionalFetchSize,
                                    Optional.of(true));
        }
        
       
      
        @Override
        public <E> ListSelection<Result<E>> entity(Class<E> objectClass) {
            return newListSelection(ctx, this, objectClass) ;
        }
        
        @Override
        public ListSelectionWithUnit<Result<Record>> column(String name) {
            return column(name, false, false);
        }

        
        @Override
        public ListSelectionWithUnit<Result<Record>> columns(String... names) {
            return columns(ImmutableSet.copyOf(names));
        }
        
        
                @Override
        public Result<Record> execute() {
            try {
                return executeAsync().get(10000, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                throw Exceptions.unwrapIfNecessary(e);
            }
        }

   
        @Override
        public CompletableFuture<Result<Record>> executeAsync() {
   
            Select.Selection selection = select();

            optionalDistinct.ifPresent(distinct -> { if (distinct) selection.distinct(); });

            
            if (columnsToFetch.isPresent()) {
                columnsToFetch.get().forEach(column -> column.accept(selection));
            } else {
                selection.all();
            }
            
            Select select = selection.from(ctx.getTable());
            Select.Where where = null;
            for (Clause clause : clauses) {
                if (where == null) {
                    where = select.where(clause);
                } else {
                    where = where.and(clause);
                }
            }

            optionalLimit.ifPresent(limit -> select.limit(limit));
            optionalAllowFiltering.ifPresent(allowFiltering -> { if (allowFiltering)  select.allowFiltering(); });
            optionalFetchSize.ifPresent(fetchSize -> select.setFetchSize(fetchSize));
            
            return ctx.performAsync(select)
                      .thenApply(resultSet -> new RecordsImpl(ctx.getProtocolVersion(), resultSet));
        }        
        
        
        private final class RecordsImpl implements Result<Record> {
            private final ProtocolVersion protocolVersion;
            private final Iterator<Row> iterator;
            private final ResultSet rs;
            private final AtomicReference<DatabaseSubscription> subscriptionRef = new AtomicReference<>();
            
            public RecordsImpl(ProtocolVersion protocolVersion, ResultSet rs) {
                this.protocolVersion = protocolVersion;
                this.rs = rs;
                this.iterator = rs.iterator();
            }
            
          
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }
            
            @Override
            public Record next() {
                return new Record(protocolVersion, iterator.next());
            }
            
            @Override
            public void subscribe(Subscriber<? super Record> subscriber) {
                synchronized (subscriptionRef) {
                    if (subscriptionRef.get() == null) {
                        DatabaseSubscription subscription = new DatabaseSubscription(subscriber);
                        subscriptionRef.set(subscription);
                        subscriber.onSubscribe(subscription);
                    } else {
                        subscriber.onError(new IllegalStateException("subription alreday exists. Multi-subscribe is not supported")); 
                    }
                }
            }
            
            
            private final class DatabaseSubscription implements Subscription {
                private final Subscriber<? super Record> subscriber;
                
                private final AtomicLong numPendingReads = new AtomicLong();
                private final AtomicReference<Runnable> runningDatabaseQuery = new AtomicReference<>();
                
                public DatabaseSubscription(Subscriber<? super Record> subscriber) {
                    this.subscriber = subscriber;
                }
                
                public void request(long n) {
                    if (n > 0) {
                        numPendingReads.addAndGet(n);
                        processReadRequests();
                    }
                }
                
                
                private void processReadRequests() {
                    synchronized (this) {
                        long available = rs.getAvailableWithoutFetching();
                        long numToRead = numPendingReads.get();

                        // no records available?
                        if (available == 0) {
                            requestDatabaseForMoreRecords();
                          
                        // all requested available 
                        } else if (available >= numToRead) {
                            numPendingReads.addAndGet(-numToRead);
                            for (int i = 0; i < numToRead; i++) {
                                subscriber.onNext(next());
                            }                    
                            
                        // requested partly available                        
                        } else {
                            requestDatabaseForMoreRecords();
                            numPendingReads.addAndGet(-available);
                            for (int i = 0; i < available; i++) {
                                subscriber.onNext(next());
                            }
                        }
                    }
                }
                
                
                private void requestDatabaseForMoreRecords() {
                    if (rs.isFullyFetched()) {
                        cancel();
                    }
                    
                    synchronized (this) {
                        if (runningDatabaseQuery.get() == null) {
                            Runnable databaseRequest = () -> { runningDatabaseQuery.set(null); processReadRequests(); };
                            runningDatabaseQuery.set(databaseRequest);
                            
                            ListenableFuture<Void> future = rs.fetchMoreResults();
                            future.addListener(databaseRequest, ForkJoinPool.commonPool());
                        }
                    }
                }
           
                
                @Override
                public void cancel() {
                    subscriber.onComplete();
                }
            }
        }
    }  
    
    
    
    protected <E> ListSelection<Result<E>> newListSelection(Context ctx, ListSelection<Result<Record>> read, Class<?> clazz) {
        return new ListEntitySelectionQuery<>(ctx, read, clazz);
    }
    
    
    private class ListEntitySelectionQuery<E> implements ListSelection<Result<E>> {
        private final Context ctx;
        private final ListSelection<Result<Record>> read;
        private final Class<?> clazz;
        
        public ListEntitySelectionQuery(Context ctx, ListSelection<Result<Record>> read, Class<?> clazz) {
            this.ctx = ctx;
            this.read = read;
            this.clazz = clazz;
        }
    
    
        @Override
        public SingleSelection<Result<E>> withConsistency( ConsistencyLevel consistencyLevel) {
            return newListSelection(ctx.withConsistency(consistencyLevel), read, clazz);
        }
    
        @Override
        public ListSelection<Result<E>> withDistinct() {
            return newListSelection(ctx, read.withDistinct(), clazz);
        }
        
        @Override
        public ListSelection<Result<E>> withFetchSize(int fetchSize) {
            return newListSelection(ctx, read.withFetchSize(fetchSize), clazz);
        }
        
        @Override
        public ListSelection<Result<E>> withAllowFiltering() {
            return newListSelection(ctx, read.withAllowFiltering(), clazz);
        }
        
        @Override
        public ListSelection<Result<E>> withLimit(int limit) {
            return newListSelection(ctx, read.withLimit(limit), clazz);
        }

        
        @Override
        public Result<E> execute() {
            try {
                return executeAsync().get(10000, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                throw Exceptions.unwrapIfNecessary(e);
            }
        }
        
        
        @Override
        public CompletableFuture<Result<E>> executeAsync() {
            return read.executeAsync().thenApply(recordIterator -> new ResultIteratorImpl<>(ctx, recordIterator, clazz));
        }
        
        
        
        private final class ResultIteratorImpl<F> implements Result<F> {
            private final Context ctx;
            private final Result<Record> recordIterator;
            private final Class<?> clazz;

            
            public ResultIteratorImpl(Context ctx, Result<Record> recordIterator, Class<?> clazz) {
                this.ctx = ctx;
                this.recordIterator = recordIterator;
                this.clazz = clazz;
            }
            
            
            @Override
            public boolean hasNext() {
                return recordIterator.hasNext();
            }
        
            
            @Override
            public F next() {
                return ctx.getPropertiesMapper(clazz).fromValues(recordIterator.next());
            }
            
          
            @Override
            public void subscribe(Subscriber<? super F> subscriber) {
                recordIterator.subscribe(new MappingSubscriber<F>(ctx, clazz, subscriber));
            }
            
            private final class MappingSubscriber<G> implements Subscriber<Record> {
                private final Context ctx;
                private final Class<?> clazz;
                
                private Subscriber<? super G> subscriber;
                
                public MappingSubscriber(Context ctx, Class<?> clazz, Subscriber<? super G> subscriber) {
                    this.ctx = ctx;
                    this.clazz = clazz;
                    this.subscriber = subscriber;
                }
                
                @Override
                public void onSubscribe(Subscription subscription) {
                    subscriber.onSubscribe(subscription);
                }
                
                @Override
                public void onNext(Record record) {
                    subscriber.onNext(ctx.getPropertiesMapper(clazz).fromValues(record));
                }

                @Override
                public void onError(Throwable t) {
                    subscriber.onError(t);
                }
                
                @Override
                public void onComplete() {
                    subscriber.onComplete();
                }
            }
        }
    }
}

