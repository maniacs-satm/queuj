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
import com.workplacesystems.queuj.utils.collections.BidiMap;
import com.workplacesystems.queuj.utils.collections.FilterableCollection;
import com.workplacesystems.queuj.utils.collections.FilterableSet;
import com.workplacesystems.queuj.utils.collections.SyncUtils;

/**
 *
 * @author  Administrator
 */
public class SynchronizedBidiMap<K,V> extends SynchronizedMap<K,V> implements BidiMap<K,V> {
    /**
     * Factory method to create a synchronized map.
     * 
     * @param map  the map to decorate, must not be null
     * @throws IllegalArgumentException if map is null
     */
    public static <K,V> BidiMap<K,V> decorate(BidiMap<K,V> map) {
        return new SynchronizedBidiMap<K,V>(map);
    }
    
    /**
     * Constructor that wraps (not copies).
     * 
     * @param map  the map to decorate, must not be null
     * @throws IllegalArgumentException if map is null
     */
    protected SynchronizedBidiMap(BidiMap<K,V> map) {
        super(map);
    }

    /**
     * Constructor that wraps (not copies).
     * 
     * @param map  the map to decorate, must not be null
     * @param lock  the lock object to use, must not be null
     * @throws IllegalArgumentException if map is null
     */
    protected SynchronizedBidiMap(BidiMap<K,V> map, Object lock) {
        super(map, lock);
    }

    protected BidiMap<K,V> getBidiMap()
    {
        return (BidiMap<K,V>)map;
    }
    
    //-----------------------------------------------------------------------

    public K getKeyForValue(final Object value) {
        return SyncUtils.synchronizeRead(lock, new Callback<K>() {
            @Override
            protected void doAction() {
                _return(getBidiMap().getKeyForValue(value));
            }
        });
    }
    
    public K removeValue(final Object value) {
        return SyncUtils.synchronizeWrite(lock, new Callback<K>() {
            @Override
            protected void doAction() {
                _return(getBidiMap().removeValue(value));
            }
        });
    }
    
    public FilterableSet<K> keySetByValue() {
        return SyncUtils.synchronizeRead(lock, new Callback<FilterableSet<K>>() {
            @Override
            protected void doAction() {
                FilterableSet<K> _set = getBidiMap().keySetByValue();
                _return(new SynchronizedFilterableSet<K>(_set, lock));
            }
        });
    }
    
    public FilterableCollection<V> valuesByValue() {
        return SyncUtils.synchronizeRead(lock, new Callback<FilterableCollection<V>>() {
            @Override
            protected void doAction() {
                FilterableCollection<V> _col = getBidiMap().valuesByValue();
                _return(new SynchronizedFilterableCollection<V>(_col, lock));
            }
        });
    }
    
    public FilterableSet<Map.Entry<K,V>> entrySetByValue() {
        return SyncUtils.synchronizeRead(lock, new Callback<FilterableSet<Map.Entry<K,V>>>() {
            @Override
            protected void doAction() {
                FilterableSet<Map.Entry<K,V>> _set = getBidiMap().entrySetByValue();
                _return(new SynchronizedFilterableSet<Map.Entry<K,V>>(_set, lock));
            }
        });
    }
}
