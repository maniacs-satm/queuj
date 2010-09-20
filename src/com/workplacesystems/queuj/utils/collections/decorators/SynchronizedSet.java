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

import java.util.Set;

/**
 * <code>SynchronizedSet</code> decorates another <code>Set</code>
 * to synchronize its behaviour for a multi-threaded environment.
 * <p>
 * Methods are synchronized, then forwarded to the decorated set.
 *
 * @since Commons Collections 3.0
 * @version $Revision: 1.3 $ $Date: 2003/08/31 17:24:46 $
 * 
 * @author Stephen Colebourne
 */
public class SynchronizedSet<E> extends SynchronizedCollection<E> implements Set<E> {

    /**
     * Factory method to create a synchronized set.
     * 
     * @param set  the set to decorate, must not be null
     * @throws IllegalArgumentException if set is null
     */
    public static <E> Set<E> decorate(Set<E> set) {
        return new SynchronizedSet<E>(set);
    }
    
    /**
     * Constructor that wraps (not copies).
     * 
     * @param set  the set to decorate, must not be null
     * @throws IllegalArgumentException if set is null
     */
    protected SynchronizedSet(Set<E> set) {
        super(set);
    }

    /**
     * Constructor that wraps (not copies).
     * 
     * @param set  the set to decorate, must not be null
     * @param lock  the lock object to use, must not be null
     * @throws IllegalArgumentException if set is null
     */
    protected SynchronizedSet(Set<E> set, Object lock) {
        super(set, lock);
    }

    /**
     * Gets the decorated set.
     * 
     * @return the decorated set
     */
    protected Set<E> getSet() {
        return (Set<E>) collection;
    }

}
