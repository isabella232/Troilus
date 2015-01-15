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

import com.datastax.driver.core.BatchStatement;
import com.unitedinternet.troilus.Dao.BatchMutation;
import com.unitedinternet.troilus.Dao.Batchable;
import com.unitedinternet.troilus.Dao.Query;


 

class BatchMutationQueryAdapter extends AbstractQuery<BatchMutation> implements BatchMutation {
    
    private final BatchMutationQuery query;  
    
    
    BatchMutationQueryAdapter(Context ctx, BatchMutationQuery query) {
        super(ctx);
        this.query = query;
    }
    
    @Override
    protected BatchMutationQueryAdapter newQuery(Context newContext) {
        return new BatchMutationQueryAdapter(newContext, query.newQuery(newContext));
    }
    
    @Override
    public Query<Result> withLockedBatchType() {
        return new BatchMutationQueryAdapter(getContext(), query.withLockedBatchType());
    }
    
    @Override
    public Query<Result> withUnlockedBatchType() {
        return new BatchMutationQueryAdapter(getContext(), query.withUnlockedBatchType());
    }

    @Override
    public BatchMutation combinedWith(Batchable other) {
        return new BatchMutationQueryAdapter(getContext(), query.combinedWith(new BatchableAdapter(other)));
    }
    
    public CompletableFuture<Result> executeAsync() {
        return new ListenableToCompletableFutureAdapter<>(query.executeAsync());
    }
    
    
    
    static final class BatchableAdapter implements com.unitedinternet.troilus.minimal.MinimalDao.Batchable {
        private final Batchable batchable;
        
        public BatchableAdapter(Batchable batchable) {
            this.batchable = batchable;
        }
        
        public void addTo(BatchStatement batchStatement) {
            batchable.addTo(batchStatement);
        }
    }
}
