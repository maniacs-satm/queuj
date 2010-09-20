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

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 *
 * @author  Administrator
 */
public class FilterableTreeSet<E> extends TreeSet<E> implements FilterableSet<E> {
    
    /**
     * 
     */
    private static final long serialVersionUID = -2911328522871769288L;

    /**
     * Constructs a new, empty set, sorted according to the elements' natural
     * order.  All elements inserted into the set must implement the
     * <tt>Comparable</tt> interface.  Furthermore, all such elements must be
     * <i>mutually comparable</i>: <tt>e1.compareTo(e2)</tt> must not throw a
     * <tt>ClassCastException</tt> for any elements <tt>e1</tt> and
     * <tt>e2</tt> in the set.  If the user attempts to add an element to the
     * set that violates this constraint (for example, the user attempts to
     * add a string element to a set whose elements are integers), the
     * <tt>add(Object)</tt> call will throw a <tt>ClassCastException</tt>.
     *
     * @see Comparable
     */
    public FilterableTreeSet() {
        super();
    }

    /**
     * Constructs a new, empty set, sorted according to the specified
     * comparator.  All elements inserted into the set must be <i>mutually
     * comparable</i> by the specified comparator: <tt>comparator.compare(e1,
     * e2)</tt> must not throw a <tt>ClassCastException</tt> for any elements
     * <tt>e1</tt> and <tt>e2</tt> in the set.  If the user attempts to add
     * an element to the set that violates this constraint, the
     * <tt>add(Object)</tt> call will throw a <tt>ClassCastException</tt>.
     *
     * @param c the comparator that will be used to sort this set.  A
     *        <tt>null</tt> value indicates that the elements' <i>natural
     *        ordering</i> should be used.
     */
    public FilterableTreeSet(Comparator<? super E> c) {
        super(c);
    }

    /**
     * Constructs a new set containing the elements in the specified
     * collection, sorted according to the elements' <i>natural order</i>.
     * All keys inserted into the set must implement the <tt>Comparable</tt>
     * interface.  Furthermore, all such keys must be <i>mutually
     * comparable</i>: <tt>k1.compareTo(k2)</tt> must not throw a
     * <tt>ClassCastException</tt> for any elements <tt>k1</tt> and
     * <tt>k2</tt> in the set.
     *
     * @param c The elements that will comprise the new set.
     *
     * @throws ClassCastException if the keys in the specified collection are
     *         not comparable, or are not mutually comparable.
     * @throws NullPointerException if the specified collection is null.
     */
    public FilterableTreeSet(Collection<E> c) {
        super(c);
    }

    /**
     * Constructs a new set containing the same elements as the specified
     * sorted set, sorted according to the same ordering.
     *
     * @param s sorted set whose elements will comprise the new set.
     * @throws NullPointerException if the specified sorted set is null.
     */
    public FilterableTreeSet(SortedSet<E> s) {
        super(s);
    }

    public FilterableCollection<E> filteredCollection(final Filter<? super E> filter)
    {
        return new AbstractFilterableCollection<E>() {

            @Override
            public Iterator<E> iterator() {

                return new FilterableTreeSetIterator(filter);
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

                return FilterableTreeSet.this.remove(o);
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

            class FilterableTreeSetIterator implements Iterator<E>
            {
                private Iterator<E> i;
                private E lastReturnedObject;
                private E nextObject;
                private Filter<? super E> filter;

                FilterableTreeSetIterator(Filter<? super E> filter)
                {
                    this.filter = filter;
                    i = FilterableTreeSet.this.iterator();
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

                    FilterableTreeSet.this.remove(lastReturnedObject);

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
