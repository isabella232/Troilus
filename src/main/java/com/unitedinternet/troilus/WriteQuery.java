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



import static com.datastax.driver.core.querybuilder.QueryBuilder.bindMarker;
import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.datastax.driver.core.querybuilder.QueryBuilder.set;
import static com.datastax.driver.core.querybuilder.QueryBuilder.update;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.Clause;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.unitedinternet.troilus.Dao.Insertion;
import com.unitedinternet.troilus.Dao.Mutation;
import com.unitedinternet.troilus.Dao.UpdateWithValues;
import com.unitedinternet.troilus.Dao.Update;
import com.unitedinternet.troilus.Dao.Write;


 
class WriteQuery extends MutationQuery<WriteQuery> implements Write {

    private final ImmutableMap<String, Object> keys;
    private final ImmutableList<Clause> whereConditions;

    private final ImmutableList<? extends ValueToMutate> valuesToMutate;
    
   
    protected static Insertion newInsertionQuery(Context ctx, Object entity) {
        return new InsertionQuery(ctx, entity, false);
    }
    
    protected static InsertionQuery newInsertionQuery(Context ctx, ImmutableList<? extends ValueToMutate> valuesToInsert, boolean ifNotExists) {
        return new InsertionQuery(ctx, valuesToInsert, ifNotExists);
    }
    
    protected static UpdateWithValues<?> newUpdateQuery(Context ctx, ImmutableList<Clause> whereConditions) {
        return new WriteQuery(ctx, ImmutableMap.of(), whereConditions, ImmutableList.of());
    }
    
    protected static Write newUpdateQuery(Context ctx, ImmutableMap<String, Object> keys) {
        return new WriteQuery(ctx, keys, ImmutableList.of(), ImmutableList.of());
    }
    
    
    
    public WriteQuery(Context ctx, ImmutableMap<String, Object> keys, ImmutableList<Clause> whereConditions, ImmutableList<? extends ValueToMutate> valuesToMutate) {
        super(ctx);
        this.keys = keys;
        this.whereConditions = whereConditions;
        this.valuesToMutate = valuesToMutate;
    }

    
    @Override
    protected WriteQuery newQuery(Context newContext) {
        return new WriteQuery(newContext, keys, whereConditions, valuesToMutate);
    }
    
    

    
    @Override
    public Write value(String name, Object value) {
        if (isOptional(value)) {
            Optional<Object> optional = (Optional<Object>) value;
            if (optional.isPresent()) {
                return value(name, optional.get());
            } else {
                return this;
            }
        }
        
        return new WriteQuery(getContext(), 
                              keys, 
                              whereConditions, 
                              ImmutableList.<ValueToMutate>builder().addAll(valuesToMutate).add(ValueToMutate.newValueToMutate(getContext(), name, value)).build());
    }

    @Override
    public Write values(ImmutableMap<String, ? extends Object> nameValuePairsToAdd) {
        Write write = this;
        for (String name : nameValuePairsToAdd.keySet()) {
            write = write.value(name, nameValuePairsToAdd.get(name));
        }
        return write;
    }


    @Override
    public Insertion ifNotExits() {
        return newInsertionQuery(getContext(), 
                                 ImmutableList.<ValueToMutate>builder().addAll(valuesToMutate).addAll(ValueToMutate.newValuesToMutate(getContext(), keys)).build(), 
                                 true);
    }

    @Override
    public Update onlyIf(Clause... conditions) {
        return new UpdateQuery(getContext(), 
                               keys, 
                               whereConditions, 
                               valuesToMutate, 
                               ImmutableList.copyOf(conditions));
    }

    
    @Override
    protected Statement getStatement() {
        return new UpdateQuery(getContext(), 
                               keys, 
                               whereConditions, 
                               valuesToMutate,
                               ImmutableList.of()).getStatement();
    }    
    
    
    
    
    
    private static final class InsertionQuery extends MutationQuery<Insertion> implements Insertion {
        private final ImmutableList<? extends ValueToMutate> valuesToInsert;
        private final boolean ifNotExists;

        
        public InsertionQuery(Context ctx, Object entity, boolean ifNotExists) {
            this(ctx, ValueToMutate.newValuesToMutate(ctx, ctx.toValues(entity)), ifNotExists);
        }
            

        
        public InsertionQuery(Context ctx, ImmutableList<? extends ValueToMutate> valuesToInsert, boolean ifNotExists) {
            super(ctx);
            this.valuesToInsert = valuesToInsert;
            this.ifNotExists = ifNotExists;
        }
        
        
        @Override
        protected Insertion newQuery(Context newContext) {
            return newInsertionQuery(newContext, 
                                     valuesToInsert, 
                                     ifNotExists);
        }

        
        

        @Override
        public Mutation<?> ifNotExits() {
            return newInsertionQuery(getContext(), 
                                     valuesToInsert, 
                                     true);
        }


        @Override
        protected Statement getStatement() {
            // statement
            Insert insert = insertInto(getTable());
            
            List<Object> values = Lists.newArrayList();
            valuesToInsert.forEach(valueToInsert -> values.add(valueToInsert.addPreparedToStatement(insert)));
            
            
            if (ifNotExists) {
                insert.ifNotExists();
                getSerialConsistencyLevel().ifPresent(serialCL -> insert.setSerialConsistencyLevel(serialCL));
            }

            getTtl().ifPresent(ttl-> {
                                            insert.using(QueryBuilder.ttl(bindMarker()));
                                            values.add((int) ttl.getSeconds());
                                         });

            PreparedStatement stmt = prepare(insert);
            return stmt.bind(values.toArray());
        }
        
        
        @Override
        public CompletableFuture<Result> executeAsync() {
            return super.executeAsync().thenApply(result -> {
                    if (ifNotExists) {
                        // check cas result column '[applied]'
                        if (!result.wasApplied()) {
                            throw new IfConditionException("duplicated entry");  
                        }
                    } 
                    return result;
                });
        }
    }

    
    
    
    
    private static final class UpdateQuery extends MutationQuery<Update> implements Update {

        private final ImmutableMap<String, Object> keys;
        private final ImmutableList<? extends ValueToMutate> valuesToMutate;
        private final ImmutableList<Clause> ifConditions;
        private final ImmutableList<Clause> whereConditions;


        
        public UpdateQuery(Context ctx, ImmutableMap<String, Object> keys, ImmutableList<Clause> whereConditions, ImmutableList<? extends ValueToMutate> valuesToMutate, ImmutableList<Clause> ifConditions) {
            super(ctx);
            this.keys = keys;
            this.whereConditions = whereConditions;
            this.valuesToMutate = valuesToMutate;
            this.ifConditions = ifConditions;
        }
        
        @Override
        protected Update newQuery(Context newContext) {
            return new UpdateQuery(newContext, 
                                   keys, 
                                   whereConditions,  
                                   valuesToMutate,  
                                   ifConditions);
        }
   
        @Override
        public Mutation<?> onlyIf(Clause... conditions) {
            return new UpdateQuery(getContext(),
                                   keys, 
                                   whereConditions, 
                                   valuesToMutate, 
                                   ImmutableList.<Clause>builder().addAll(ifConditions).addAll(ImmutableList.copyOf(conditions)).build());
        }
   
        
        @Override
        protected Statement getStatement() {
            
            // statement
            com.datastax.driver.core.querybuilder.Update update = update(getContext().getTable());
            
            ifConditions.forEach(condition -> update.onlyIf(condition));

            // key-based update
            if (whereConditions.isEmpty()) {
                List<Object> values = Lists.newArrayList();
                valuesToMutate.forEach(valueToInsert -> values.add(valueToInsert.addPreparedToStatement(update)));
                
                keys.keySet().forEach(keyname -> { update.where(eq(keyname, bindMarker())); values.add(keys.get(keyname)); } );
                
                
                ifConditions.forEach(condition -> update.onlyIf(condition));
       
                getTtl().ifPresent(ttl-> {
                                                update.using(QueryBuilder.ttl(bindMarker()));
                                                values.add((int) ttl.getSeconds());
                                             });
                
                PreparedStatement stmt = prepare(update);
                return stmt.bind(values.toArray());

                
            // where condition-based update
            } else {
                valuesToMutate.forEach(valueToInsert -> valueToInsert.addToStatement(update));
                
                getTtl().ifPresent(ttl-> update.using(QueryBuilder.ttl((int) ttl.getSeconds())));
                
                com.datastax.driver.core.querybuilder.Update.Where where = null;
                
                for (Clause whereClause : whereConditions) {
                    if (where == null) {
                        where = update.where(whereClause);
                    } else {
                        where = where.and(whereClause);
                    }
                }
                
                return update;
            }
        }
        
        @Override
        public CompletableFuture<Result> executeAsync() {
            return super.executeAsync().thenApply(result ->  {
                if (!ifConditions.isEmpty()) {
                    // check cas result column '[applied]'
                    if (!result.wasApplied()) {
                        throw new IfConditionException("if condition does not match");  
                    }
                } 
                return result;
            });
        }
    }


  

    static interface ValueToMutate {
        
        Object addPreparedToStatement(Insert insert);

        void addToStatement(Insert insert);
        
        Object addPreparedToStatement(com.datastax.driver.core.querybuilder.Update update);
        
        void addToStatement(com.datastax.driver.core.querybuilder.Update update);
        
        

        static ValueToMutate newValueToMutate(Context ctx, String name, Object value) {
            if (ctx.isBuildInType(ctx.getColumnMetadata(name).getType())) {
                return new BuildinValueToMutate(name, value);
            } else {
                return new UDTValueToMutate(ctx, name, value);
            }
        }
        
        
        static ImmutableList<ValueToMutate> newValuesToMutate(Context ctx, ImmutableMap<String, ? extends Object> nameValuePair) {
            List<ValueToMutate> valuesToMutate = Lists.newArrayList();
            nameValuePair.forEach((name, value) -> ctx.toOptional((Object) value).ifPresent(val -> valuesToMutate.add(newValueToMutate(ctx, name, value))));
            return ImmutableList.copyOf(valuesToMutate);
        }
        
    }
    

     
    
    protected static final class BuildinValueToMutate implements ValueToMutate {
        private final String name;
        private final Object value;
        
        @SuppressWarnings("unchecked")
        public BuildinValueToMutate(String name, Object value) {
            this.name = name;
            if (value instanceof Optional) {
                this.value = ((Optional) value).orElse(null);
            } else {
                this.value = value;
            }
        }
        
        @Override
        public Object addPreparedToStatement(Insert insert) {
            insert.value(name, bindMarker());
            return value;
        }
        
        @Override
        public void addToStatement(Insert insert) {
            insert.value(name,  value);
        }

        public Object addPreparedToStatement(com.datastax.driver.core.querybuilder.Update update) {
            update.with(set(name, bindMarker()));
            return value;
        }
        
        
        @Override
        public void addToStatement(com.datastax.driver.core.querybuilder.Update update) {
            update.with(set(name, value));
        }
    }
   
    
    
    protected static final class UDTValueToMutate implements ValueToMutate {
        private final String columnName;
        private final Object value;
        private Context ctx;
        
        
        @SuppressWarnings("unchecked")
        public UDTValueToMutate(Context ctx, String columnName, Object value) {
            this.ctx = ctx;
            this.columnName = columnName;
            if (value instanceof Optional) {
                this.value = ((Optional) value).orElse(null);
            } else {
                this.value = value;
            }
        }
        
        @Override
        public Object addPreparedToStatement(Insert insert) {
            insert.value(columnName, bindMarker());
            return UDTValueMapper.toUdtValue(ctx, ctx.getColumnMetadata(columnName).getType(), value);
        }

        @Override
        public void addToStatement(Insert insert) {
            insert.value(columnName, value);
        }
        
        public Object addPreparedToStatement(com.datastax.driver.core.querybuilder.Update update) {
            update.with(set(columnName, bindMarker()));
            return UDTValueMapper.toUdtValue(ctx, ctx.getColumnMetadata(columnName).getType(), value);
        }
        
        @Override
        public void addToStatement(com.datastax.driver.core.querybuilder.Update update) {
            update.with(set(columnName, UDTValueMapper.toUdtValue(ctx, ctx.getColumnMetadata(columnName).getType(), value)));
        }
    }
}

