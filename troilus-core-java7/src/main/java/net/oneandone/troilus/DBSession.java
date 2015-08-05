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



import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.DataType;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ProtocolVersion;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.exceptions.DriverInternalError;
import com.datastax.driver.core.exceptions.InvalidQueryException;
import com.datastax.driver.core.querybuilder.BuiltStatement;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;



/**
 * DBSession
 */
public class DBSession  {
    private static final Logger LOG = LoggerFactory.getLogger(DBSession.class);

    private final Session session;
    private final MetadataCatalog catalog;
    private final boolean isKeyspacenameAssigned;
    private final String keyspacename;
    private final UDTValueMapper udtValueMapper;
    private final PreparedStatementCache preparedStatementCache;
    
    private final AtomicLong lastCacheCleanTime = new AtomicLong(0);
    

    

    /**
     * constructor 
     * @param session    the underlying session
     * @param catalog    the metadata catalog
     * @param beanMapper the bean mapper
     */
    DBSession(Session session, MetadataCatalog catalog, BeanMapper beanMapper) {
        this.session = session;
        this.catalog = catalog;
        
        this.keyspacename = session.getLoggedKeyspace();
        this.isKeyspacenameAssigned = (keyspacename != null);
        
        //this.udtValueMapper = new UDTValueMapper(session.getCluster().getConfiguration().getProtocolOptions().getProtocolVersion(), beanMapper);
        this.udtValueMapper = new UDTValueMapper(session.getCluster().getConfiguration().getProtocolOptions().getProtocolVersionEnum(), beanMapper);
        this.preparedStatementCache = new PreparedStatementCache(session);
    }


  
    /**
     * @return true, if a keyspacename is assigned to this context
     */
    boolean isKeyspaqcenameAssigned() {
        return isKeyspacenameAssigned; 
    }
    
    /**
     * @return the keyspacename or null
     */
    String getKeyspacename() {
        return keyspacename;
    }
    
    private Session getSession() {
        return session;
    }
    
    
    /**
     * @return the protocol version
     */
    ProtocolVersion getProtocolVersion() {
        //return getSession().getCluster().getConfiguration().getProtocolOptions().getProtocolVersion();
        return getSession().getCluster().getConfiguration().getProtocolOptions().getProtocolVersionEnum();
    }
    
    /**
     * @return the udtvalue mapper
     */
    UDTValueMapper getUDTValueMapper() {
        return udtValueMapper;
    }
 
 
    /**
     * @param statement the statement to prepare
     * @return the prepared statement future
     */
    ListenableFuture<PreparedStatement> prepareAsync(final BuiltStatement statement) {
        return preparedStatementCache.prepareAsync(statement);
    }
    
    /**
     * @param preparedStatementFuture the prepared statement future to bind
     * @param values the values to bind 
     * @return the statement future
     */
    public ListenableFuture<Statement> bindAsync(ListenableFuture<PreparedStatement> preparedStatementFuture, final Object[] values) {
        Function<PreparedStatement, Statement> bindStatementFunction = new Function<PreparedStatement, Statement>() {
            @Override
            public Statement apply(PreparedStatement preparedStatement) {
                return preparedStatement.bind(values);
            }
        };
        return Futures.transform(preparedStatementFuture, bindStatementFunction);
    }
    
    
    /**
     * @param statement  te statement to execute in an async manner
     * @return the resultset future
     */
    public ListenableFuture<ResultSet> executeAsync(Statement statement) {
        try {
            return getSession().executeAsync(statement);
        } catch (InvalidQueryException | DriverInternalError e) {
            cleanUp();
            LOG.warn("could not execute statement", e);
            return Futures.immediateFailedFuture(e);
        }
    }

    
    /**
     * @param tablename  the table name
     * @param name       the columnname
     * @param value      the value 
     * @return the mapped value
     */
    Object toStatementValue(Tablename tablename, String name, Object value) {
        if (isNullOrEmpty(value)) {
            return null;
        } 
        
        DataType dataType = catalog.getColumnMetadata(tablename, name).getType();
        
        // build in
        if (UDTValueMapper.isBuildInType(dataType)) {
            
            // enum
            if (DataTypes.isTextDataType(dataType) && Enum.class.isAssignableFrom(value.getClass())) {
                return value.toString();
            }
            
            // byte buffer (byte[])
            if (dataType.equals(DataType.blob()) && byte[].class.isAssignableFrom(value.getClass())) {
                return ByteBuffer.wrap((byte[]) value);
            }

            
            return value;
         
        // udt    
        } else {
            return getUDTValueMapper().toUdtValue(tablename, catalog, catalog.getColumnMetadata(tablename, name).getType(), value);
        }
    }
    
    /**
     * @param tablename   the tablename
     * @param name        the columnname
     * @param values      the vlaues 
     * @return            the mapped values
     */
    ImmutableList<Object> toStatementValues(Tablename tablename, String name, ImmutableList<Object> values) {
        List<Object> result = Lists.newArrayList(); 

        for (Object value : values) {
            result.add(toStatementValue(tablename, name, value));
        }
        
        return ImmutableList.copyOf(result);
    }

 
    private boolean isNullOrEmpty(Object value) {
        return (value == null) || 
               (Collection.class.isAssignableFrom(value.getClass()) && ((Collection<?>) value).isEmpty()) || 
               (Map.class.isAssignableFrom(value.getClass()) && ((Map<?, ?>) value).isEmpty());
    }
    
    
    
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("preparedStatementsCache", preparedStatementCache.toString())
                          .toString();
    }
  
    
    private void cleanUp() {

        // avoid bulk clean calls within the same time
        if (System.currentTimeMillis() > (lastCacheCleanTime.get() + 1600)) {  // not really thread safe. However this does not matter 
            lastCacheCleanTime.set(System.currentTimeMillis());

            preparedStatementCache.invalidateAll();
        }
    }
    
    
    
    private static final class PreparedStatementCache {
        private final Session session;
        private final Cache<String, PreparedStatement> preparedStatementCache;

        public PreparedStatementCache(Session session) {
            this.session = session;
            this.preparedStatementCache = CacheBuilder.newBuilder().maximumSize(150).<String, PreparedStatement>build();
        }
        
        
        ListenableFuture<PreparedStatement> prepareAsync(final BuiltStatement statement) {
            PreparedStatement preparedStatment = preparedStatementCache.getIfPresent(statement.getQueryString());
            if (preparedStatment == null) {
                ListenableFuture<PreparedStatement> future = session.prepareAsync(statement);
                
                Function<PreparedStatement, PreparedStatement> addToCacheFunction = new Function<PreparedStatement, PreparedStatement>() {
                    
                    public PreparedStatement apply(PreparedStatement preparedStatment) {
                        preparedStatementCache.put(statement.getQueryString(), preparedStatment);
                        return preparedStatment;
                    }
                };
                
                return Futures.transform(future, addToCacheFunction);
            } else {
                return Futures.immediateFuture(preparedStatment);
            }
        }
        
        
        public void invalidateAll() {
            preparedStatementCache.invalidateAll();
        }      
        
        
        @Override
        public String toString() {
            return Joiner.on(", ").withKeyValueSeparator("=").join(preparedStatementCache.asMap());
        }
    }
    
    
    
    }