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

import java.util.Map;

import com.workplacesystems.queuj.utils.Callback;
import com.workplacesystems.queuj.utils.collections.Filter;
import com.workplacesystems.queuj.utils.collections.FilterableBidiMap;
import com.workplacesystems.queuj.utils.collections.FilterableMap;
import com.workplacesystems.queuj.utils.collections.SyncUtils;
import com.workplacesystems.queuj.utils.collections.FilterableSet;
import com.workplacesystems.queuj.utils.collections.TransactionalSortedFilterableBidiMap;

/**
 *
 * @author  Administrator
 */
public class SynchronizedTransactionalSortedFilterableBidiMap<K,V> extends SynchronizedTransactionalSortedBidiMap<K,V> implements TransactionalSortedFilterableBidiMap<K,V> {

    /**
     * Factory method to create a synchronized map.
     * 
     * @param map  the map to decorate, must not be null
     * @throws IllegalArgumentException if map is null
     */
    public static <K,V> TransactionalSortedFilterableBidiMap<K,V> decorate(TransactionalSortedFilterableBidiMap<K,V> map) {
        return new SynchronizedTransactionalSortedFilterableBidiMap<K,V>(map);
    }
    
    /**
     * Constructor that wraps (not copies).
     * 
     * @param map  the map to decorate, must not be null
     * @throws IllegalArgumentException if map is null
     */
    protected SynchronizedTransactionalSortedFilterableBidiMap(TransactionalSortedFilterableBidiMap<K,V> map) {
        super(map);
    }

    /**
     * Constructor that wraps (not copies).
     * 
     * @param map  the map to decorate, must not be null
     * @param lock  the lock object to use, must not be null
     * @throws IllegalArgumentException if map is null
     */
    protected SynchronizedTransactionalSortedFilterableBidiMap(TransactionalSortedFilterableBidiMap<K,V> map, Object lock) {
        super(map, lock);
    }
    
    /** create a string for debug 
    public String rbmDump()
    {
        return ((TransactionalBidiTreeMap)getTransactionalSortedFilterableBidiMap()).rbmDump();
    }
    */
    
    protected TransactionalSortedFilterableBidiMap<K,V> getTransactionalSortedFilterableBidiMap()
    {
        return (TransactionalSortedFilterableBidiMap<K,V>)map;
    }
    
    //-----------------------------------------------------------------------

    public FilterableMap<K,V> filteredMap(final Filter<? super K> filter) {
        return SyncUtils.synchronizeRead(lock, new Callback<FilterableMap<K,V>>() {
            @Override
            protected void doAction() {
                FilterableMap<K,V> _map = getTransactionalSortedFilterableBidiMap().filteredMap(filter);
                _return(new SynchronizedFilterableMap<K,V>(_map, lock));
            }
        });
    }
    
    public FilterableBidiMap<K,V> filteredMapByValue(final Filter<? super V> filter) {
        return SyncUtils.synchronizeRead(lock, new Callback<FilterableBidiMap<K,V>>() {
            @Override
            protected void doAction() {
                FilterableBidiMap<K,V> _map = getTransactionalSortedFilterableBidiMap().filteredMapByValue(filter);
                _return(new SynchronizedFilterableBidiMap<K,V>(_map, lock));
            }
        });
    }

    public FilterableSet<Map.Entry<K,V>> allEntrySet() {
        return SyncUtils.synchronizeRead(lock, new Callback<FilterableSet<Map.Entry<K,V>>>() {
            @Override
            protected void doAction() {
                FilterableSet<Map.Entry<K,V>> _set = getTransactionalSortedFilterableBidiMap().allEntrySet();
                _return(new SynchronizedFilterableSet<Map.Entry<K,V>>(_set, lock));
            }
        });
    }
}
