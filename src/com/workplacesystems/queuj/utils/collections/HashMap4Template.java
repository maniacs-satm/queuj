/*
 * Copyright 2010 Workplace Systems PLC (http://www.workplacesystems.com/).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.workplacesystems.queuj.utils.collections;

import java.util.HashMap;

import org.apache.commons.collections.keyvalue.MultiKey;

/**
 * Convenience class that can be used as a starting point for things that need to
 * accumulate values into four levels. Extends a standard map, but uses the MultiKey
 * class to create the key.
 */
public abstract class HashMap4Template<K1,K2,K3,K4,V> extends HashMap<MultiKey,V>
{
    /**
     * Method to get existing [key1,key2,key3,key4] value or create new one if it's
     * absent. Needs implementation of create(key1,key2,key3,key4) in order to work
     */
    public V getOrCreate(K1 key1, K2 key2, K3 key3, K4 key4)
    {
        // already got it?
        MultiKey multi_key = new MultiKey(key1, key2, key3, key4);
        if (containsKey(multi_key))
            return get(multi_key);
        
        // if not, create and add it
        V result = create(key1, key2, key3, key4);
        put(multi_key, result);
        return result;
    }

    /** Template method to enable getOrCreate() */
    protected abstract V create(K1 key1, K2 key2, K3 key3, K4 key4);
    
    public void put(K1 key1, K2 key2, K3 key3, K4 key4, V value)
    {
        put(new MultiKey(key1, key2, key3, key4), value);
    }
    
    public V get(K1 key1, K2 key2, K3 key3, K4 key4)
    {
        return get(new MultiKey(key1, key2, key3, key4));
    }
}
