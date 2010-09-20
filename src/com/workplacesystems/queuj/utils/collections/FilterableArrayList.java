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

package com.workplacesystems.queuj.utils.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 *
 * @author  Administrator
 */
public class FilterableArrayList<E> extends ArrayList<E> implements FilterableList<E> {
    
    /**
     * 
     */
    private static final long serialVersionUID = -5347161874580619825L;

    /**
     * Constructs an empty list with the specified initial capacity.
     *
     * @param   initialCapacity   the initial capacity of the list.
     * @exception IllegalArgumentException if the specified initial capacity
     *            is negative
     */
    public FilterableArrayList(int initialCapacity) {
        super(initialCapacity);
    }

    /**
     * Constructs an empty list with an initial capacity of ten.
     */
    public FilterableArrayList() {
        super();
    }

    /**
     * Constructs a list containing the elements of the specified
     * collection, in the order they are returned by the collection's
     * iterator.  The <tt>ArrayList</tt> instance has an initial capacity of
     * 110% the size of the specified collection.
     *
     * @param c the collection whose elements are to be placed into this list.
     * @throws NullPointerException if the specified collection is null.
     */
    public FilterableArrayList(Collection<? extends E> c) {
        super(c);
    }

    public FilterableCollection<E> filteredCollection(final Filter<? super E> filter)
    {
        return new AbstractFilterableCollection<E>() {

            @Override
            public Iterator<E> iterator() {

                return new FilterableArrayIterator(filter);
            }

            @Override
            public int size() {
                int size = 0;
                for (Iterator<E> i = iterator(); i.hasNext(); )
                {
                    i.next();
                    size++;
                }
                return size;
            }

            @Override
            public boolean contains(Object o) {
                for (Iterator<E> i = iterator(); i.hasNext(); ) {
                    Object e = i.next();
                    if ((o==null ? e==null : o.equals(e)))
                        return true;
                }
                return false;
            }

            @Override
            public boolean remove(Object o) {

                if (!contains(o))
                    return false;

                return FilterableArrayList.this.remove(o);
            }

            @Override
            public boolean removeAll(Collection<?> c) {

                boolean  modified = false;
                Iterator<?> iter     = c.iterator();

                while (iter.hasNext()) {
                    if (remove(iter.next()) == true) {
                        modified = true;
                    }
                }

                return modified;
            }

            @Override
            public void clear() {
                for (Iterator<E> i = iterator(); i.hasNext(); )
                    remove(i.next());
            }

            class FilterableArrayIterator implements Iterator<E>
            {
                private Iterator<E> i;
                private E lastReturnedObject;
                private E nextObject;
                private Filter<? super E> filter;

                FilterableArrayIterator(Filter<? super E> filter)
                {
                    this.filter = filter;
                    i = FilterableArrayList.this.iterator();
                    lastReturnedObject = null;
                    nextObject = getNextValidObject();
                }

                public boolean hasNext() {
                    return nextObject != null;
                }

                public E next()
                        throws NoSuchElementException {

                    if (nextObject == null) {
                        throw new NoSuchElementException();
                    }

                    lastReturnedObject = nextObject;
                    nextObject = getNextValidObject();
                    return lastReturnedObject;
                }

                public final void remove()
                        throws IllegalStateException {

                    if (lastReturnedObject == null) {
                        throw new IllegalStateException();
                    }

                    FilterableArrayList.this.remove(lastReturnedObject);

                    lastReturnedObject = null;
                }

                private E getNextValidObject()
                {
                    while (i.hasNext())
                    {
                        E o = i.next();
                        if (filter.isValid(o))
                            return o;
                    }
                    return null;
                }
            }
        };
    }
}
