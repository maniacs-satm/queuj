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

package com.workplacesystems.queuj.utils.collections.decorators;

import com.workplacesystems.queuj.utils.Callback;
import com.workplacesystems.queuj.utils.collections.Filter;
import com.workplacesystems.queuj.utils.collections.FilterableMap;
import com.workplacesystems.queuj.utils.collections.SyncUtils;

/**
 *
 * @author  Administrator
 */
public class SynchronizedFilterableMap<K,V> extends SynchronizedMap<K,V> implements FilterableMap<K,V> {
    /**
     * Factory method to create a synchronized map.
     * 
     * @param map  the map to decorate, must not be null
     * @throws IllegalArgumentException if map is null
     */
    public static <K,V> FilterableMap<K,V> decorate(FilterableMap<K,V> map) {
        return new SynchronizedFilterableMap<K,V>(map);
    }
    
    /**
     * Constructor that wraps (not copies).
     * 
     * @param map  the map to decorate, must not be null
     * @throws IllegalArgumentException if map is null
     */
    protected SynchronizedFilterableMap(FilterableMap<K,V> map) {
        super(map);
    }

    /**
     * Constructor that wraps (not copies).
     * 
     * @param map  the map to decorate, must not be null
     * @param lock  the lock object to use, must not be null
     * @throws IllegalArgumentException if map is null
     */
    protected SynchronizedFilterableMap(FilterableMap<K,V> map, Object lock) {
        super(map, lock);
    }

    protected FilterableMap<K,V> getFilterableMap()
    {
        return (FilterableMap<K,V>)map;
    }
    
    //-----------------------------------------------------------------------

    public FilterableMap<K,V> filteredMap(final Filter<? super K> filter) {
        return SyncUtils.synchronizeRead(lock, new Callback<FilterableMap<K,V>>() {
            @Override
            protected void doAction() {
                FilterableMap<K,V> _map = getFilterableMap().filteredMap(filter);
                _return(new SynchronizedFilterableMap<K,V>(_map, lock));
            }
        });
    }
}
