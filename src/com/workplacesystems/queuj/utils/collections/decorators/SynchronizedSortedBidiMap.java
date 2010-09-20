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
import java.util.Comparator;
import java.util.Map;
import java.util.Set;

import com.workplacesystems.queuj.utils.Callback;
import com.workplacesystems.queuj.utils.collections.FilterableCollection;
import com.workplacesystems.queuj.utils.collections.FilterableSet;
import com.workplacesystems.queuj.utils.collections.SortedBidiMap;
import com.workplacesystems.queuj.utils.collections.SyncUtils;

/**
 *
 * @author  Dave
 */
public class SynchronizedSortedBidiMap<K,V> extends SynchronizedSortedMap<K,V> implements SortedBidiMap<K,V> {
    /**
     * Factory method to create a synchronized map.
     * 
     * @param map  the map to decorate, must not be null
     * @throws IllegalArgumentException if map is null
     */
    public static <K,V> SortedBidiMap<K,V> decorate(SortedBidiMap<K,V> map) {
        return new SynchronizedSortedBidiMap<K,V>(map);
    }
    
    /**
     * Constructor that wraps (not copies).
     * 
     * @param map  the map to decorate, must not be null
     * @throws IllegalArgumentException if map is null
     */
    protected SynchronizedSortedBidiMap(SortedBidiMap<K,V> map) {
        super(map);
    }

    /**
     * Constructor that wraps (not copies).
     * 
     * @param map  the map to decorate, must not be null
     * @param lock  the lock object to use, must not be null
     * @throws IllegalArgumentException if map is null
     */
    protected SynchronizedSortedBidiMap(SortedBidiMap<K,V> map, Object lock) {
        super(map, lock);
    }

    protected SortedBidiMap<K,V> getSortedBidiMap()
    {
        return (SortedBidiMap<K,V>)map;
    }
    
    //-----------------------------------------------------------------------
    
    public FilterableSet<Map.Entry<K,V>> entrySetByValue() {
        return SyncUtils.synchronizeRead(lock, new Callback<FilterableSet<Map.Entry<K,V>>>() {
            @Override
            protected void doAction() {
                FilterableSet<Map.Entry<K,V>> _set = getSortedBidiMap().entrySetByValue();
                _return(new SynchronizedFilterableSet<Map.Entry<K,V>>(_set, lock));
            }
        });
    }
    
    public K firstKeyByValue() {
        return SyncUtils.synchronizeRead(lock, new Callback<K>() {
            @Override
            protected void doAction() {
                _return(getSortedBidiMap().firstKeyByValue());
            }
        });
    }
    
    public V firstValue() {
        return SyncUtils.synchronizeRead(lock, new Callback<V>() {
            @Override
            protected void doAction() {
                _return(getSortedBidiMap().firstValue());
            }
        });
    }
    
    public V firstValueByValue() {
        return SyncUtils.synchronizeRead(lock, new Callback<V>() {
            @Override
            protected void doAction() {
                _return(getSortedBidiMap().firstValueByValue());
            }
        });
    }
    
    public K getKeyForValue(final Object value) {
        return SyncUtils.synchronizeRead(lock, new Callback<K>() {
            @Override
            protected void doAction() {
                _return(getSortedBidiMap().getKeyForValue(value));
            }
        });
    }
    
    public SortedBidiMap<K,V> headMapByValue(final V toValue) {
        return SyncUtils.synchronizeRead(lock, new Callback<SortedBidiMap<K,V>>() {
            @Override
            protected void doAction() {
                SortedBidiMap<K,V> _map = getSortedBidiMap().headMapByValue(toValue);
                _return(new SynchronizedSortedBidiMap<K,V>(_map, lock));
            }
        });
    }
    
    @Override
    public Set<K> keySet() {
        synchronized (lock) {
            FilterableSet<K> _set = (FilterableSet<K>)getSortedBidiMap().keySet();
            return new SynchronizedFilterableSet<K>(_set, lock);
        }
    }
    
    public FilterableSet<K> keySetByValue() {
        return SyncUtils.synchronizeRead(lock, new Callback<FilterableSet<K>>() {
            @Override
            protected void doAction() {
                FilterableSet<K> _set = getSortedBidiMap().keySetByValue();
                _return(new SynchronizedFilterableSet<K>(_set, lock));
            }
        });
    }
    
    public K lastKeyByValue() {
        return SyncUtils.synchronizeRead(lock, new Callback<K>() {
            @Override
            protected void doAction() {
                _return(getSortedBidiMap().lastKeyByValue());
            }
        });
    }
    
    public V lastValue() {
        return SyncUtils.synchronizeRead(lock, new Callback<V>() {
            @Override
            protected void doAction() {
                _return(getSortedBidiMap().lastValue());
            }
        });
    }
    
    public V lastValueByValue() {
        return SyncUtils.synchronizeRead(lock, new Callback<V>() {
            @Override
            protected void doAction() {
                _return(getSortedBidiMap().lastValueByValue());
            }
        });
    }
    
    public K removeValue(final Object value) {
        return SyncUtils.synchronizeWrite(lock, new Callback<K>() {
            @Override
            protected void doAction() {
                _return(getSortedBidiMap().removeValue(value));
            }
        });
    }
    
    public SortedBidiMap<K,V> subMapByValue(final V fromValue, final V toValue) {
        return SyncUtils.synchronizeRead(lock, new Callback<SortedBidiMap<K,V>>() {
            @Override
            protected void doAction() {
                SortedBidiMap<K,V> _map = getSortedBidiMap().subMapByValue(fromValue, toValue);
                _return(new SynchronizedSortedBidiMap<K,V>(_map, lock));
            }
        });
    }
    
    public SortedBidiMap<K,V> tailMapByValue(final V fromValue) {
        return SyncUtils.synchronizeRead(lock, new Callback<SortedBidiMap<K,V>>() {
            @Override
            protected void doAction() {
                SortedBidiMap<K,V> _map = getSortedBidiMap().tailMapByValue(fromValue);
                _return(new SynchronizedSortedBidiMap<K,V>(_map, lock));
            }
        });
    }
    
    public Comparator<? super V> valueComparator() {
        return SyncUtils.synchronizeRead(lock, new Callback<Comparator<? super V>>() {
            @Override
            protected void doAction() {
                _return(getSortedBidiMap().valueComparator());
            }
        });
    }
    
    public FilterableCollection<V> valuesByValue() {
        return SyncUtils.synchronizeRead(lock, new Callback<FilterableCollection<V>>() {
            @Override
            protected void doAction() {
                FilterableCollection<V> _col = getSortedBidiMap().valuesByValue();
                _return(new SynchronizedFilterableCollection<V>(_col, lock));
            }
        });
    }

    @Override
    public Collection<V> values() {
        return (Collection<V>)SyncUtils.synchronizeRead(lock, new Callback<Collection<V>>() {
            @Override
            protected void doAction() {
                FilterableCollection<V> _col = (FilterableCollection)map.values();
                _return(new SynchronizedFilterableCollection<V>(_col, lock));
            }
        });
    }
}
