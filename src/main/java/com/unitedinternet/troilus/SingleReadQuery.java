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



import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.datastax.driver.core.querybuilder.Clause;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Select.Selection;

import static com.datastax.driver.core.querybuilder.QueryBuilder.*;

import com.datastax.driver.core.DataType;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Statement;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.unitedinternet.troilus.Dao.SingleRead;
import com.unitedinternet.troilus.Dao.SingleReadWithColumns;
import com.unitedinternet.troilus.Dao.SingleReadWithUnit;
import com.unitedinternet.troilus.QueryFactory.ColumnToFetch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


 

class SingleReadQuery extends AbstractQuery<SingleReadWithUnit<Optional<Record>>> implements SingleReadWithUnit<Optional<Record>> {
        private static final Logger LOG = LoggerFactory.getLogger(SingleReadQuery.class);

    private final QueryFactory queryFactory;
    private final ImmutableMap<String, Object> keyNameValuePairs;
    private final Optional<ImmutableSet<ColumnToFetch>> optionalColumnsToFetch;
     
    
    public SingleReadQuery(Context ctx, QueryFactory queryFactory, ImmutableMap<String, Object> keyNameValuePairs, Optional<ImmutableSet<ColumnToFetch>> optionalColumnsToFetch) {
        super(ctx);
        this.queryFactory = queryFactory;
        this.keyNameValuePairs = keyNameValuePairs;
        this.optionalColumnsToFetch = optionalColumnsToFetch;
    }
   
    @Override
    protected SingleReadWithUnit<Optional<Record>> newQuery(Context newContext) {
        return queryFactory.newSingleSelection(newContext, keyNameValuePairs, optionalColumnsToFetch);
    }
    
    @Override
    public SingleRead<Optional<Record>> all() {
        return queryFactory.newSingleSelection(getContext(), keyNameValuePairs, Optional.empty());
    }
    
    @Override
    public <E> SingleRead<Optional<E>> asEntity(Class<E> objectClass) {
        return queryFactory.newSingleSelection(getContext(), this, objectClass);
    }
    
    @Override
    public SingleReadWithUnit<Optional<Record>> column(String name) {
        return queryFactory.newSingleSelection(getContext(), keyNameValuePairs, Immutables.merge(optionalColumnsToFetch, ColumnToFetch.create(name, false, false)));
    }

    @Override
    public SingleReadWithColumns<Optional<Record>> columnWithMetadata(String name) {
        return queryFactory.newSingleSelection(getContext(), keyNameValuePairs, Immutables.merge(optionalColumnsToFetch, ColumnToFetch.create(name, true, true)));
    }
    
    @Override
    public SingleReadWithUnit<Optional<Record>> columns(String... names) {
        return columns(ImmutableSet.copyOf(names));
    }
    
    @Override 
    public SingleReadWithUnit<Optional<Record>> columns(ImmutableCollection<String> namesToRead) {
        return queryFactory.newSingleSelection(getContext(), keyNameValuePairs, Immutables.merge(optionalColumnsToFetch, ColumnToFetch.create(namesToRead)));
    }
  
   
    
    @Override
    public Optional<Record> execute() {
        try {
            return executeAsync().get(Long.MAX_VALUE, TimeUnit.DAYS);
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
        
        
        
        Select select = selection.from(getContext().getTable());
        Select.Where where = null;
        for (Clause whereClause : keyNameValuePairs.keySet().stream().map(name -> eq(name, bindMarker())).collect(Immutables.toSet())) {
            if (where == null) {
                where = select.where(whereClause);
            } else {
                where = where.and(whereClause);
            }
        }

        Statement statement = getContext().prepare(select).bind(keyNameValuePairs.values().toArray());
        
        
        return getContext().performAsync(statement)
                  .thenApply(resultSet -> {
                                              Row row = resultSet.one();
                                              if (row == null) {
                                                  return Optional.empty();
                                                  
                                              } else {
                                                  Record record = new Record(getContext(), Result.newResult(resultSet), row);
                                                  
                                                  // paranioa check
                                                  keyNameValuePairs.forEach((name, value) -> { 
                                                                                              ByteBuffer in = DataType.serializeValue(value, getContext().getProtocolVersion());
                                                                                              ByteBuffer out = record.getBytesUnsafe(name).get();
                                                      
                                                                                              if (in.compareTo(out) != 0) {
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
 
