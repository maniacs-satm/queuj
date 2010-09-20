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

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Simple Map with two-level key.
 * @author grahamp
 */
public interface Map2<K1,K2,V>
{
    public void clear();

    public boolean containsKey(Object key1, Object key2 );

    public boolean containsValue(Object value);

    public V get(Object key1, Object key2);

    public Map<K2,V> getMap(Object key1);

    public boolean isEmpty();

    /** replace [key1,key2] value, returning old value */
    public V put(K1 key1, K2 key2, V value);

    /** remove [key1,key2] value, returning old value */
    public V remove(Object key1, Object key2);

    public int size();

    public Set<K1> key1Set();
    
    /** separate list of values, NOT backed by this map */
    public List<V> values();
}
