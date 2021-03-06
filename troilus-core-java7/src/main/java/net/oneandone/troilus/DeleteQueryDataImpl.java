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



import static com.datastax.driver.core.querybuilder.QueryBuilder.bindMarker;
import static com.datastax.driver.core.querybuilder.QueryBuilder.delete;
import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;

import java.util.List;
import java.util.Map.Entry;

import net.oneandone.troilus.interceptor.DeleteQueryData;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.Clause;
import com.datastax.driver.core.querybuilder.Delete;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;


/**
 * Delete query data implementation
 */
class DeleteQueryDataImpl implements DeleteQueryData {
 
    private final Tablename tablename;
    private final ImmutableMap<String, Object> keyNameValuePairs;
    private final ImmutableList<Clause> whereConditions;
    private final ImmutableList<Clause> onlyIfConditions;
    private final Boolean ifExists;
    private final ImmutableMap<String, List<Object>> mapValuesToRemove;

    /**
     * constructor 
     */
    DeleteQueryDataImpl(Tablename tablename) {
        this(tablename,
             ImmutableMap.<String, Object>of(), 
             ImmutableList.<Clause>of(), 
             ImmutableList.<Clause>of(),
             null, 
             null);
    }
    
    private DeleteQueryDataImpl(Tablename tablename,
                                ImmutableMap<String, Object> keyNameValuePairs, 
                                ImmutableList<Clause> whereConditions, 
                                ImmutableList<Clause> onlyIfConditions,
                                Boolean ifExists,
                                ImmutableMap<String, List<Object>> mapValuesToRemove) {
        this.tablename = tablename;
        this.keyNameValuePairs = keyNameValuePairs;
        this.whereConditions = whereConditions;
        this.onlyIfConditions = onlyIfConditions;
        this.ifExists = ifExists;
        this.mapValuesToRemove = mapValuesToRemove;
    }
    
    @Override
    public DeleteQueryDataImpl key(ImmutableMap<String, Object> keyNameValuePairs) {
        return new DeleteQueryDataImpl(this.tablename, 
                                       keyNameValuePairs, 
                                       this.whereConditions, 
                                       this.onlyIfConditions,
                                       this.ifExists,
                                       this.mapValuesToRemove);  
    }
    
    @Override
    public DeleteQueryDataImpl whereConditions(ImmutableList<Clause> whereConditions) {
        return new DeleteQueryDataImpl(this.tablename, 
                                       this.keyNameValuePairs, 
                                       whereConditions, 
                                       this.onlyIfConditions,
                                       this.ifExists,
                                       this.mapValuesToRemove);  
    }
    
    @Override
    public DeleteQueryDataImpl onlyIfConditions(ImmutableList<Clause> onlyIfConditions) {
        return new DeleteQueryDataImpl(this.tablename, 
                                       this.keyNameValuePairs, 
                                       this.whereConditions, 
                                       onlyIfConditions,
                                       this.ifExists,
                                       this.mapValuesToRemove);  
    }
    
    @Override
    public DeleteQueryDataImpl ifExists(Boolean ifExists) {
        return new DeleteQueryDataImpl(this.tablename, 
                                       this.keyNameValuePairs, 
                                       this.whereConditions, 
                                       this.onlyIfConditions,
                                       ifExists,
                                       this.mapValuesToRemove);  
    }
    
    @Override
    public DeleteQueryDataImpl mapValuesToRemove(ImmutableMap<String, List<Object>> mapValuesToRemove) {
    	return new DeleteQueryDataImpl(this.tablename,
    									this.keyNameValuePairs,
    									this.whereConditions,
    									onlyIfConditions,
    									this.ifExists,
    									mapValuesToRemove);
    }
    
    @Override
    public Tablename getTablename() {
        return tablename;
    }
    
    @Override
    public ImmutableMap<String, Object> getKey() {
        return keyNameValuePairs;
    }

    @Override
    public ImmutableList<Clause> getWhereConditions() {
        return whereConditions;
    }

    @Override
    public ImmutableList<Clause> getOnlyIfConditions() {
        return onlyIfConditions;
    }
    
    @Override
    public Boolean getIfExists() {
        return ifExists;
    }
    
    @Override
    public ImmutableMap<String, List<Object>> getMapValuesToRemove() {
    	return mapValuesToRemove;
    }
    
    /**
     * @param data  the data 
     * @param ctx   the context
     * @return the query data statement
     */
    static ListenableFuture<Statement> toStatementAsync(DeleteQueryData data, ExecutionSpec executionSpec, UDTValueMapper udtValueMapper, DBSession dbSession) {
        
    	Delete.Selection deletion = delete();
        Delete delete = (data.getTablename().getKeyspacename() == null) ? deletion.from(data.getTablename().getTablename())
                                                                        : deletion.from(data.getTablename().getKeyspacename(), data.getTablename().getTablename());

        for (Clause onlyIfCondition : data.getOnlyIfConditions()) {
            delete.onlyIf(onlyIfCondition);
        }
        
        if ((data.getIfExists() != null) && data.getIfExists()) {
            delete.ifExists();
        }
        
        if(data.getMapValuesToRemove() !=null) {
        	for(Entry<String, List<Object>> entry : data.getMapValuesToRemove().entrySet()) {
        		for(Object object : entry.getValue()) {
        			deletion.mapElt(entry.getKey(), object);
        		}
        	}
        }
        
        // key-based delete    
        if (data.getWhereConditions().isEmpty()) {
            List<Object> values = Lists.newArrayList();
            
            for (Entry<String, Object> entry : data.getKey().entrySet()) {
                Clause keybasedWhereClause = eq(entry.getKey(), bindMarker());
                delete.where(keybasedWhereClause);
                                
                values.add(udtValueMapper.toStatementValue(data.getTablename(), entry.getKey(), entry.getValue()));
            }
            
            ListenableFuture<PreparedStatement> preparedStatementFuture = dbSession.prepareAsync(delete);
            return dbSession.bindAsync(preparedStatementFuture, values.toArray());
            
        // where condition-based delete    
        } else {
            for (Clause whereCondition : data.getWhereConditions()) {
                delete.where(whereCondition);
            }
           
            return Futures.<Statement>immediateFuture(delete);
        }        
    }
}