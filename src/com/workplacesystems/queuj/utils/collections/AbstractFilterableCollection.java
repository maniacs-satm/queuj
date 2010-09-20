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

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 *
 * @author  Administrator
 */
abstract public class AbstractFilterableCollection<E> extends AbstractCollection<E> implements FilterableCollection<E>
{
    /**
     * Sole constructor.  (For invocation by subclass constructors, typically
     * implicit.)
     */
    protected AbstractFilterableCollection() {
    }

    public FilterableCollection<E> filteredCollection(final Filter<? super E> filter)
    {
        return new AbstractFilterableCollection<E>() {

            @Override
            public Iterator<E> iterator() {

                return new FilterableCollectionIterator(filter);
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

                return AbstractFilterableCollection.this.remove(o);
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

            class FilterableCollectionIterator implements Iterator<E>
            {
                private Iterator<E> i;
                private E lastReturnedObject;
                private E nextObject;
                private Filter<? super E> filter;

                FilterableCollectionIterator(Filter<? super E> filter)
                {
                    this.filter = filter;
                    i = AbstractFilterableCollection.this.iterator();
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

                    AbstractFilterableCollection.this.remove(lastReturnedObject);

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
