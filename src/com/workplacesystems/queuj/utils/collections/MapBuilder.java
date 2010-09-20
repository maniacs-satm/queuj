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

import java.util.Hashtable;
import java.util.Map;
/**
 * A map builder in the ToStringBuilder style.  The {@link #put(Object, Object)}
 * calls can be chained, for example:
 * <pre>
 * Map<String,String> capital = new MapBuilder&lt;String,String&gt;()
 *      .put("Angola","Luanda")
 *      .put("Vietnam", "Hanoi")
 *      .put("Paraguay", "Asuncion")
 *      .put("Sweden", "Stockholm")
 *      .toMap();
 * </pre>
 * Some multiple puts are directly implemented, and more can easily be added:
 * <pre>
 * Map<String,String> currency = new MapBuilder&lt;String,String&gt;()
 *      .put("Peru", "Sol", "Brazil", "Real", "China", "Renminbi", "Vietnam", "Dong")
 *      .toMap();
 * </pre>
 * 
 */
public class MapBuilder<K,V>
{
    private Map<K,V> map;
    
    /** new builder which uses supplied concrete map */
    public MapBuilder(Map<K,V> map)
    {
        this.map = map;
    }

    /** new builder which uses default (empty) concrete map */
    public MapBuilder()
    {
        this.map = new Hashtable<K, V>();
    }

    /** the built map */
    public Map<K,V> toMap()
    {
        return map;
    }
    
    public MapBuilder<K,V> put(K key, V value)
    {
        map.put(key, value);
        return this;
    }
    
    public MapBuilder<K,V> put(K key1, V value1, K key2, V value2)
    {
        map.put(key1, value1);
        map.put(key2, value2);
        return this;
    }
    
    public MapBuilder<K,V> put(K key1, V value1, K key2, V value2, K key3, V value3)
    {
        map.put(key1, value1);
        map.put(key2, value2);
        map.put(key3, value3);
        return this;
    }
    
    public MapBuilder<K,V> put(K key1, V value1, K key2, V value2, K key3, V value3, K key4, V value4)
    {
        map.put(key1, value1);
        map.put(key2, value2);
        map.put(key3, value3);
        map.put(key4, value4);
        return this;
    }
    
    /** a new map with just [key--&gt;value] maplet */
    public static<K,V> Map<K,V> map(K key, V value)
    {
        return new MapBuilder().put(key,value).toMap();
    }
    
    /** a new map with just [key1--&gt;value1, key2--&gt;value2] maplet */
    public static<K,V> Map<K,V> map(K key1, V value1, K key2, V value2)
    {
        return new MapBuilder().put(key1,value1,key2,value2).toMap();
    }
}
