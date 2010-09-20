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
import java.util.Iterator;
import com.workplacesystems.queuj.utils.Callback;
import com.workplacesystems.queuj.utils.collections.SyncUtils;

/**
 * <code>SynchronizedCollection</code> decorates another <code>Collection</code>
 * to synchronize its behaviour for a multi-threaded environment.
 * <p>
 * Iterators must be manually synchronized:
 * <pre>
 * synchronized (coll) {
 *   Iterator it = coll.iterator();
 *   // do stuff with iterator
 * }
 *
 * @since Commons Collections 3.0
 * @version $Revision: 1.5 $ $Date: 2003/09/05 03:35:07 $
 * 
 * @author Stephen Colebourne
 */
public class SynchronizedCollection<E> implements Collection<E>, SynchronizedDecorator {

    /** The collection to decorate */
    protected final Collection<E> collection;
    /** The object to lock on, needed for List/SortedSet views */
    protected final Object lock;

    /**
     * Factory method to create a synchronized collection.
     * 
     * @param coll  the collection to decorate, must not be null
     * @throws IllegalArgumentException if collection is null
     */
    public static <E> Collection<E> decorate(Collection<E> coll) {
        return new SynchronizedCollection<E>(coll);
    }
    
    /**
     * Constructor that wraps (not copies).
     * 
     * @param collection  the collection to decorate, must not be null
     * @throws IllegalArgumentException if the collection is null
     */
    protected SynchronizedCollection(Collection<E> collection) {
        if (collection == null) {
            throw new IllegalArgumentException("Collection must not be null");
        }
        this.collection = collection;
        this.lock = SyncUtils.createMutex(collection);
    }

    /**
     * Constructor that wraps (not copies).
     * 
     * @param collection  the collection to decorate, must not be null
     * @param lock  the lock object to use, must not be null
     * @throws IllegalArgumentException if the collection is null
     */
    protected SynchronizedCollection(Collection<E> collection, Object lock) {
        if (collection == null) {
            throw new IllegalArgumentException("Collection must not be null");
        }
        this.collection = collection;
        this.lock = lock;
    }


    public Object getLockObject() {
        return lock;
    }
    
    //-----------------------------------------------------------------------
    public boolean add(final E object) {
        Boolean ret = SyncUtils.synchronizeWrite(lock, new Callback<Boolean>() {
            @Override
            protected void doAction() {
                _return(collection.add(object) ? Boolean.TRUE : Boolean.FALSE);
            }
        });
        return ret.booleanValue();
    }

    public boolean addAll(final Collection<? extends E> coll) {
        Boolean ret = SyncUtils.synchronizeWrite(lock, new Callback<Boolean>() {
            @Override
            protected void doAction() {
                _return(collection.addAll(coll) ? Boolean.TRUE : Boolean.FALSE);
            }
        });
        return ret.booleanValue();
    }

    public void clear() {
        SyncUtils.synchronizeWrite(lock, new Callback<Object>() {
            @Override
            protected void doAction() {
                collection.clear();
            }
        });
    }

    public boolean contains(final Object object) {
        Boolean ret = SyncUtils.synchronizeRead(lock, new Callback<Boolean>() {
            @Override
            protected void doAction() {
                _return(collection.contains(object) ? Boolean.TRUE : Boolean.FALSE);
            }
        });
        return ret.booleanValue();
    }

    public boolean containsAll(final Collection<?> coll) {
        Boolean ret = SyncUtils.synchronizeRead(lock, new Callback<Boolean>() {
            @Override
            protected void doAction() {
                _return(collection.containsAll(coll) ? Boolean.TRUE : Boolean.FALSE);
            }
        });
        return ret.booleanValue();
    }

    public boolean isEmpty() {
        Boolean ret = SyncUtils.synchronizeRead(lock, new Callback<Boolean>() {
            @Override
            protected void doAction() {
                _return(collection.isEmpty() ? Boolean.TRUE : Boolean.FALSE);
            }
        });
        return ret.booleanValue();
    }

    /**
     * Iterators must be manually synchronized.
     * <pre>
     * synchronized (coll) {
     *   Iterator it = coll.iterator();
     *   // do stuff with iterator
     * }
     * 
     * @return an iterator that must be manually synchronized on the collection
     */
    public Iterator<E> iterator() {
        return collection.iterator();
    }

    public Object[] toArray() {
        return SyncUtils.synchronizeRead(lock, new Callback<Object[]>() {
            @Override
            protected void doAction() {
                _return(collection.toArray());
            }
        });
    }

    public <T> T[] toArray(final T[] object) {
        return SyncUtils.synchronizeRead(lock, new Callback<T[]>() {
            @Override
            protected void doAction() {
                _return(collection.toArray(object));
            }
        });
    }

    public boolean remove(final Object object) {
        Boolean ret = SyncUtils.synchronizeWrite(lock, new Callback<Boolean>() {
            @Override
            protected void doAction() {
                _return(collection.remove(object) ? Boolean.TRUE : Boolean.FALSE);
            }
        });
        return ret.booleanValue();
    }

    public boolean removeAll(final Collection<?> coll) {
        Boolean ret = SyncUtils.synchronizeWrite(lock, new Callback<Boolean>() {
            @Override
            protected void doAction() {
                _return(collection.removeAll(coll) ? Boolean.TRUE : Boolean.FALSE);
            }
        });
        return ret.booleanValue();
    }

    public boolean retainAll(final Collection<?> coll) {
        Boolean ret = SyncUtils.synchronizeWrite(lock, new Callback<Boolean>() {
            @Override
            protected void doAction() {
                _return(collection.retainAll(coll) ? Boolean.TRUE : Boolean.FALSE);
            }
        });
        return ret.booleanValue();
    }

    public int size() {
        Integer ret = SyncUtils.synchronizeRead(lock, new Callback<Integer>() {
            @Override
            protected void doAction() {
                _return(new Integer(collection.size()));
            }
        });
        return ret.intValue();
    }

    @Override
    public boolean equals(final Object object) {
        Boolean ret = SyncUtils.synchronizeRead(lock, new Callback<Boolean>() {
            @Override
            protected void doAction() {
                if (object == SynchronizedCollection.this) {
                    _return(Boolean.TRUE);
                }
                _return(collection.equals(object) ? Boolean.TRUE : Boolean.FALSE);
            }
        });
        return ret.booleanValue();
    }

    @Override
    public int hashCode() {
        Integer ret = SyncUtils.synchronizeRead(lock, new Callback<Integer>() {
            @Override
            protected void doAction() {
                _return(new Integer(collection.hashCode()));
            }
        });
        return ret.intValue();
    }

    @Override
    public String toString() {
        return SyncUtils.synchronizeRead(lock, new Callback<String>() {
            @Override
            protected void doAction() {
                _return(collection.toString());
            }
        });
    }
}
