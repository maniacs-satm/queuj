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

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import com.workplacesystems.queuj.utils.Callback;
import com.workplacesystems.queuj.utils.collections.SyncUtils;

/**
 *
 * @author  Dave
 */
public class SynchronizedMap<K,V> implements Map<K,V>, SynchronizedDecorator {
    /** The object to lock on, needed for List/SortedSet views */
    protected final Object lock;
    
    protected final Map<K,V> map;

    /**
     * Factory method to create a synchronized map.
     * 
     * @param map  the map to decorate, must not be null
     * @throws IllegalArgumentException if map is null
     */
    public static <K,V> Map<K,V> decorate(Map<K,V> map) {
        return new SynchronizedMap<K,V>(map);
    }
    
    /**
     * Constructor that wraps (not copies).
     * 
     * @param map  the map to decorate, must not be null
     * @throws IllegalArgumentException if map is null
     */
    protected SynchronizedMap(Map<K,V> map) {
        if (map == null) {
            throw new IllegalArgumentException("Map must not be null");
        }

        this.map = map;
        this.lock = SyncUtils.createMutex(map);
    }

    /**
     * Constructor that wraps (not copies).
     * 
     * @param map  the map to decorate, must not be null
     * @param lock  the lock object to use, must not be null
     * @throws IllegalArgumentException if map is null
     */
    protected SynchronizedMap(Map<K,V> map, Object lock) {
        if (map == null) {
            throw new IllegalArgumentException("Map must not be null");
        }

        this.map = map;
        this.lock = lock;
    }

    
    public Object getLockObject() {
        return lock;
    }
    
    //-----------------------------------------------------------------------
    public void clear() {
        SyncUtils.synchronizeWrite(lock, new Callback<Object>() {
            @Override
            protected void doAction() {
                map.clear();
            }
        });
    }
    
    public boolean containsKey(final Object key) {
        Boolean ret = SyncUtils.synchronizeRead(lock, new Callback<Boolean>() {
            @Override
            protected void doAction() {
                _return(map.containsKey(key) ? Boolean.TRUE : Boolean.FALSE);
            }
        });
        return ret.booleanValue();
    }
    
    public boolean containsValue(final Object value) {
        Boolean ret = SyncUtils.synchronizeRead(lock, new Callback<Boolean>() {
            @Override
            protected void doAction() {
                _return(map.containsValue(value) ? Boolean.TRUE : Boolean.FALSE);
            }
        });
        return ret.booleanValue();
    }
    
    public Set<Map.Entry<K,V>> entrySet() {
        return SyncUtils.synchronizeRead(lock, new Callback<Set<Map.Entry<K,V>>>() {
            @Override
            protected void doAction() {
                Set<Map.Entry<K,V>> _set = map.entrySet();
                _return(new SynchronizedSet<Map.Entry<K,V>>(_set, lock));
            }
        });
    }
    
    public V get(final Object key) {
        return SyncUtils.synchronizeRead(lock, new Callback<V>() {
            @Override
            protected void doAction() {
                _return(map.get(key));
            }
        });
    }
    
    public boolean isEmpty() {
        Boolean ret = SyncUtils.synchronizeRead(lock, new Callback<Boolean>() {
            @Override
            protected void doAction() {
                _return(map.isEmpty() ? Boolean.TRUE : Boolean.FALSE);
            }
        });
        return ret.booleanValue();
    }
    
    public Set<K> keySet() {
        return SyncUtils.synchronizeRead(lock, new Callback<Set<K>>() {
            @Override
            protected void doAction() {
                Set<K> _set = map.keySet();
                _return(new SynchronizedSet<K>(_set, lock));
            }
        });
    }
    
    public V put(final K key, final V value) {
        return SyncUtils.synchronizeWrite(lock, new Callback<V>() {
            @Override
            protected void doAction() {
                _return(map.put(key, value));
            }
        });
    }
    
    public void putAll(final Map<? extends K,? extends V> t) {
        SyncUtils.synchronizeWrite(lock, new Callback<Object>() {
            @Override
            protected void doAction() {
                map.putAll(t);
            }
        });
    }
    
    public V remove(final Object key) {
        return SyncUtils.synchronizeWrite(lock, new Callback<V>() {
            @Override
            protected void doAction() {
                _return(map.remove(key));
            }
        });
    }
    
    public int size() {
        Integer ret = SyncUtils.synchronizeRead(lock, new Callback<Integer>() {
            @Override
            protected void doAction() {
                _return(new Integer(map.size()));
            }
        });
        return ret.intValue();
    }
    
    public Collection<V> values() {
        return SyncUtils.synchronizeRead(lock, new Callback<Collection<V>>() {
            @Override
            protected void doAction() {
                Collection<V> _col = map.values();
                _return(new SynchronizedCollection<V>(_col, lock));
            }
        });
    }
}
