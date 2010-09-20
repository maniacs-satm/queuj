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
import com.workplacesystems.queuj.utils.collections.FilterableBidiMap;
import com.workplacesystems.queuj.utils.collections.FilterableMap;
import com.workplacesystems.queuj.utils.collections.SyncUtils;

/**
 *
 * @author  Administrator
 */
public class SynchronizedFilterableBidiMap<K,V> extends SynchronizedBidiMap<K,V> implements FilterableBidiMap<K,V> {

    /**
     * Factory method to create a synchronized map.
     * 
     * @param map  the map to decorate, must not be null
     * @throws IllegalArgumentException if map is null
     */
    public static <K,V> FilterableBidiMap<K,V> decorate(FilterableBidiMap<K,V> map) {
        return new SynchronizedFilterableBidiMap<K,V>(map);
    }
    
    /**
     * Constructor that wraps (not copies).
     * 
     * @param map  the map to decorate, must not be null
     * @throws IllegalArgumentException if map is null
     */
    protected SynchronizedFilterableBidiMap(FilterableBidiMap<K,V> map) {
        super(map);
    }

    /**
     * Constructor that wraps (not copies).
     * 
     * @param map  the map to decorate, must not be null
     * @param lock  the lock object to use, must not be null
     * @throws IllegalArgumentException if map is null
     */
    protected SynchronizedFilterableBidiMap(FilterableBidiMap<K,V> map, Object lock) {
        super(map, lock);
    }

    protected FilterableBidiMap<K,V> getFilterableBidiMap()
    {
        return (FilterableBidiMap<K,V>)map;
    }
    
    //-----------------------------------------------------------------------

    public FilterableMap<K,V> filteredMap(final Filter<? super K> filter) {
        return SyncUtils.synchronizeRead(lock, new Callback<FilterableMap<K,V>>() {
            @Override
            protected void doAction() {
                FilterableMap<K,V> _map = getFilterableBidiMap().filteredMap(filter);
                _return(new SynchronizedFilterableMap<K,V>(_map, lock));
            }
        });
    }
    
    public FilterableBidiMap<K,V> filteredMapByValue(final Filter<? super V> filter) {
        return SyncUtils.synchronizeRead(lock, new Callback<FilterableBidiMap<K,V>>() {
            @Override
            protected void doAction() {
                FilterableBidiMap<K,V> _map = getFilterableBidiMap().filteredMapByValue(filter);
                _return(new SynchronizedFilterableBidiMap<K,V>(_map, lock));
            }
        });
    }
}
