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

/**
 * Convenience class that can be used as a starting point for things that need to
 * accumulate values into three levels. Extends a three-level map, and provides a
 * method to get or create the bottom level values.
 */
public abstract class HashMap3Template<K1,K2,K3,V> extends HashMap3<K1,K2,K3,V>
{
    /**
     * Method to get existing [key1,key2,key3] value or create new one if it's
     * absent. Needs implementation of create(key1,key2,key3) in order to work
     */
    public V getOrCreate(K1 key1, K2 key2, K3 key3)
    {
        // already got it?
        if (containsKey(key1, key2, key3))
            return get(key1, key2, key3);
        
        // if not, create and add it
        V result = create(key1, key2, key3);
        put(key1, key2, key3, result);
        return result;
    }

    /** Template method to enable getOrCreate() */
    protected abstract V create(K1 key1, K2 key2, K3 key3);
}
