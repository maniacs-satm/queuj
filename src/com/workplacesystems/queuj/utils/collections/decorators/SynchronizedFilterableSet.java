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
import com.workplacesystems.queuj.utils.collections.FilterableCollection;
import com.workplacesystems.queuj.utils.collections.FilterableSet;
import com.workplacesystems.queuj.utils.collections.SyncUtils;

/**
 *
 * @author  Administrator
 */
public class SynchronizedFilterableSet<E> extends SynchronizedSet<E> implements FilterableSet<E> {
    /**
     * Factory method to create a synchronized map.
     * 
     * @param map  the map to decorate, must not be null
     * @throws IllegalArgumentException if map is null
     */
    public static <E> FilterableSet<E> decorate(FilterableSet<E> set) {
        return new SynchronizedFilterableSet<E>(set);
    }
    
    /**
     * Constructor that wraps (not copies).
     * 
     * @param map  the map to decorate, must not be null
     * @throws IllegalArgumentException if map is null
     */
    protected SynchronizedFilterableSet(FilterableSet<E> set) {
        super(set);
    }

    /**
     * Constructor that wraps (not copies).
     * 
     * @param map  the map to decorate, must not be null
     * @param lock  the lock object to use, must not be null
     * @throws IllegalArgumentException if map is null
     */
    protected SynchronizedFilterableSet(FilterableSet<E> set, Object lock) {
        super(set, lock);
    }

    protected FilterableSet<E> getFilterableSet()
    {
        return (FilterableSet<E>) collection;
    }
    
    public FilterableCollection<E> filteredCollection(final Filter<? super E> filter) {
        return SyncUtils.synchronizeRead(lock, new Callback<FilterableCollection<E>>() {
            @Override
            protected void doAction() {
                FilterableCollection<E> _col = getFilterableSet().filteredCollection(filter);
                _return(new SynchronizedFilterableCollection<E>(_col, lock));
            }
        });
    }
    
}
