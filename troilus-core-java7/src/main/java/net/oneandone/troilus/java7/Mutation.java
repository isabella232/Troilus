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
package net.oneandone.troilus.java7;

import net.oneandone.troilus.Result;



/**
 * @param <Q> the query type
 */
public interface Mutation<Q extends Mutation<Q>> extends ConfiguredQuery<Q, Result> {

    /**
     * @param ttlSec  the time-to-live in sec to set
     * @return a cloned query instance with the modified behavior
     */
    Mutation<Q> withTtl(int ttlSec);

    /**
     * @param microsSinceEpoch  the writetime in since epoch to set
     * @return a cloned query instance with the modified behavior
     */
    Mutation<Q> withWritetime(long microsSinceEpoch);
}
