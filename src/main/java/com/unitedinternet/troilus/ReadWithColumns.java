/*
 * Copyright (c) 2014 Gregor Roth
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

import com.google.common.collect.ImmutableCollection;








/**
 * The Query
 *
 * @author grro
 */
public interface ReadWithColumns<T> extends Read<T> {
    
    ReadWithColumns<T> column(String name);
    
    ReadWithColumns<T> column(String name, boolean isFetchWritetime, boolean isFetchTtl);
     
    ReadWithColumns<T> columns(String... names);
    
    ReadWithColumns<T> columns(ImmutableCollection<String> nameToRead);
}




