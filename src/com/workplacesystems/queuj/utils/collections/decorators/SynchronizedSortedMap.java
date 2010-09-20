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

import java.util.Comparator;
import java.util.SortedMap;
import com.workplacesystems.queuj.utils.Callback;
import com.workplacesystems.queuj.utils.collections.SyncUtils;

/**
 *
 * @author  Dave
 */
public class SynchronizedSortedMap<K,V> extends SynchronizedMap<K,V> implements SortedMap<K,V> {
    /**
     * Factory method to create a synchronized map.
     * 
     * @param map  the map to decorate, must not be null
     * @throws IllegalArgumentException if map is null
     */
    public static <K,V> SortedMap<K,V> decorate(SortedMap<K,V> map) {
        return new SynchronizedSortedMap<K,V>(map);
    }
    
    /**
     * Constructor that wraps (not copies).
     * 
     * @param map  the map to decorate, must not be null
     * @throws IllegalArgumentException if map is null
     */
    protected SynchronizedSortedMap(SortedMap<K,V> map) {
        super(map);
    }

    /**
     * Constructor that wraps (not copies).
     * 
     * @param map  the map to decorate, must not be null
     * @param lock  the lock object to use, must not be null
     * @throws IllegalArgumentException if map is null
     */
    protected SynchronizedSortedMap(SortedMap<K,V> map, Object lock) {
        super(map, lock);
    }

    protected SortedMap<K,V> getSortedMap()
    {
        return (SortedMap<K,V>)map;
    }
    
    //-----------------------------------------------------------------------
    public Comparator<? super K> comparator() {
        return SyncUtils.synchronizeRead(lock, new Callback<Comparator<? super K>>() {
            @Override
            protected void doAction() {
                _return(getSortedMap().comparator());
            }
        });
    }
    
    public K firstKey() {
        return SyncUtils.synchronizeRead(lock, new Callback<K>() {
            @Override
            protected void doAction() {
                _return(getSortedMap().firstKey());
            }
        });
    }
    
    public SortedMap<K,V> headMap(final K toKey) {
        return SyncUtils.synchronizeRead(lock, new Callback<SortedMap<K,V>>() {
            @Override
            protected void doAction() {
                SortedMap<K,V> _map = getSortedMap().headMap(toKey);
                _return(new SynchronizedSortedMap<K,V>(_map, lock));
            }
        });
    }
    
    public K lastKey() {
        return SyncUtils.synchronizeRead(lock, new Callback<K>() {
            @Override
            protected void doAction() {
                _return(getSortedMap().lastKey());
            }
        });
    }
    
    public SortedMap<K,V> subMap(final K fromKey, final K toKey) {
        return SyncUtils.synchronizeRead(lock, new Callback<SortedMap<K,V>>() {
            @Override
            protected void doAction() {
                SortedMap<K,V> _map = getSortedMap().subMap(fromKey, toKey);
                _return(new SynchronizedSortedMap<K,V>(_map, lock));
            }
        });
    }
    
    public SortedMap<K,V> tailMap(final K fromKey) {
        return SyncUtils.synchronizeRead(lock, new Callback<SortedMap<K,V>>() {
            @Override
            protected void doAction() {
                SortedMap<K,V> _map = getSortedMap().tailMap(fromKey);
                _return(new SynchronizedSortedMap<K,V>(_map, lock));
            }
        });
    }
}
