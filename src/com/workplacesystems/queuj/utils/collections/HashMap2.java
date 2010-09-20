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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of Map2 structure for mapping two-level keys onto values.
 * As for HashMap, methods are not synchronized, so clients should protect against multi-threading
 * if necessary.
 * 
 * @author grahamp
 */
public class HashMap2<K1,K2,V> implements Map2<K1,K2,V>
{

    private HashMap<K1,Map<K2,V>> maps = new HashMap<K1,Map<K2,V>>(16);
    
    public void clear()
    {
        maps.clear();
    }

    public boolean containsKey(Object key1, Object key2)
    {
        // check that key1 map contains key2
        return 
            maps.containsKey(key1)
            && maps.get(key1).containsKey(key2);
    }

    public boolean containsValue(Object value)
    {
        // check all inner maps for presence of value
        for ( Iterator<K1> keys1 = maps.keySet().iterator(); keys1.hasNext(); )
        {
            Map<K2,V> inner_map = maps.get(keys1.next());
            if (inner_map.containsValue(value))
                return true;
        }
        return false;
    }

    public V get(Object key1, Object key2)
    {   // ask key1 map for key2 value
        if (maps.containsKey(key1))
            return maps.get(key1).get(key2);
        return null;
    }

    public Map<K2,V> getMap(Object key1)
    {
        return maps.get(key1);
    }

    public boolean isEmpty()
    {
        return maps.isEmpty();
    }

    public V put(K1 key1, K2 key2, V value)
    {
        // retrieve (create if necessary) inner map for key1
        Map<K2,V> inner_map = maps.get(key1);
        if (inner_map==null)
        {
            inner_map = new HashMap<K2,V>();
            maps.put(key1, inner_map);
        }

        // replace key2 value in inner map
        V old_value = inner_map.get(key2);
        inner_map.put( key2, value);
        return old_value;
    }

    public V remove(Object key1, Object key2)
    {
        // retrieve inner map for key1
        Map<K2,V> inner_map = maps.get(key1);
        if (inner_map==null) return null;
        
        // remove key2 (and whole inner_map if it's now empty)
        V old_value = inner_map.remove(key2);
        if (inner_map.isEmpty())
            maps.remove(key1);
        
        return old_value;
    }

    public int size()
    {
        int result = 0;
        // sum over all inner maps 
        for ( Iterator<K1> keys1 = maps.keySet().iterator(); keys1.hasNext(); )
        {
            Map<K2,V> inner_map = maps.get(keys1.next());
            result += inner_map.size();
        }
        return result;
    }

    public List<V> values()
    {
        List<V> result = new ArrayList<V>();
        // accumulate over all inner maps 
        for ( Iterator<K1> keys1 = maps.keySet().iterator(); keys1.hasNext(); )
        {
            Map<K2,V> inner_map = maps.get(keys1.next());
            for ( Iterator<K2> keys2 = inner_map.keySet().iterator(); keys2.hasNext(); )
            {
                result.add( inner_map.get(keys2.next()) );
            }
        }
        return result;
    }

    public Set<K1> key1Set()
    {
        return maps.keySet();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof HashMap2) || obj == null)
            return false;
        
        return maps.equals( ((HashMap2) obj).maps);
    }
}
