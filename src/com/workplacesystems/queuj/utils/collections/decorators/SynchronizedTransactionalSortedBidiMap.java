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
import com.workplacesystems.queuj.utils.collections.SyncUtils;
import com.workplacesystems.queuj.utils.collections.TransactionalSortedBidiMap;

/**
 *
 * @author  Dave
 */
public class SynchronizedTransactionalSortedBidiMap<K,V> extends SynchronizedSortedBidiMap<K,V> implements TransactionalSortedBidiMap<K,V> {

    /**
     * Factory method to create a synchronized map.
     * 
     * @param map  the map to decorate, must not be null
     * @throws IllegalArgumentException if map is null
     */
    public static <K,V> TransactionalSortedBidiMap<K,V> decorate(TransactionalSortedBidiMap<K,V> map) {
        return new SynchronizedTransactionalSortedBidiMap<K,V>(map);
    }
    
    /**
     * Constructor that wraps (not copies).
     * 
     * @param map  the map to decorate, must not be null
     * @throws IllegalArgumentException if map is null
     */
    protected SynchronizedTransactionalSortedBidiMap(TransactionalSortedBidiMap<K,V> map) {
        super(map);
    }

    /**
     * Constructor that wraps (not copies).
     * 
     * @param map  the map to decorate, must not be null
     * @param lock  the lock object to use, must not be null
     * @throws IllegalArgumentException if map is null
     */
    protected SynchronizedTransactionalSortedBidiMap(TransactionalSortedBidiMap<K,V> map, Object lock) {
        super(map, lock);
    }

    protected TransactionalSortedBidiMap<K,V> getTransactionalSortedBidiMap()
    {
        return (TransactionalSortedBidiMap<K,V>)map;
    }
    
    //-----------------------------------------------------------------------
    public void commit() {
        SyncUtils.synchronizeWrite(lock, new Callback<Object>() {
            @Override
            protected void doAction() {
                getTransactionalSortedBidiMap().commit();
            }
        });
    }
    
    public void rollback() {
        SyncUtils.synchronizeWrite(lock, new Callback<Object>() {
            @Override
            protected void doAction() {
                getTransactionalSortedBidiMap().rollback();
            }
        });
    }
    
    public void setAutoCommit(final boolean auto_commit) {
        SyncUtils.synchronizeWrite(lock, new Callback<Object>() {
            @Override
            protected void doAction() {
                getTransactionalSortedBidiMap().setAutoCommit(auto_commit);
            }
        });
    }

    public boolean isAutoCommit() {
        Boolean ret = SyncUtils.synchronizeRead(lock, new Callback<Boolean>() {
            @Override
            protected void doAction() {
                _return(getTransactionalSortedBidiMap().isAutoCommit() ? Boolean.TRUE : Boolean.FALSE);
            }
        });
        return ret.booleanValue();
    }

    public void attach(final String attach_id) {
        getTransactionalSortedBidiMap().attach(attach_id);
    }

    public void detach() {
        getTransactionalSortedBidiMap().detach();
    }
}
