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

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;

import com.workplacesystems.queuj.utils.Callback;
import com.workplacesystems.queuj.utils.QueujException;
import com.workplacesystems.queuj.utils.ThreadSession;
import com.workplacesystems.queuj.utils.collections.decorators.SynchronizedSet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Red-Black tree-based implementation of Map. This class guarantees
 * that the map will be in both ascending key order and ascending
 * value order, sorted according to the natural order for the key's
 * and value's classes.<p>
 *
 * This Map is intended for applications that need to be able to look
 * up a key-value pairing by either key or value, and need to do so
 * with equal efficiency.<p>
 *
 * While that goal could be accomplished by taking a pair of TreeMaps
 * and redirecting requests to the appropriate TreeMap (e.g.,
 * containsKey would be directed to the TreeMap that maps values to
 * keys, containsValue would be directed to the TreeMap that maps keys
 * to values), there are problems with that implementation,
 * particularly when trying to keep the two TreeMaps synchronized with
 * each other. And if the data contained in the TreeMaps is large, the
 * cost of redundant storage becomes significant.<p>
 *
 * This solution keeps the data properly synchronized and minimizes
 * the data storage. The red-black algorithm is based on TreeMap's,
 * but has been modified to simultaneously map a tree node by key and
 * by value. This doubles the cost of put operations (but so does
 * using two TreeMaps), and nearly doubles the cost of remove
 * operations (there is a savings in that the lookup of the node to be
 * removed only has to be performed once). And since only one node
 * contains the key and value, storage is significantly less than that
 * required by two TreeMaps.<p>
 *
 * There are some limitations placed on data kept in this Map. The
 * biggest one is this:<p>
 *
 * When performing a put operation, neither the key nor the value may
 * already exist in the Map. In the java.util Map implementations
 * (HashMap, TreeMap), you can perform a put with an already mapped
 * key, and neither cares about duplicate values at all ... but this
 * implementation's put method with throw an IllegalArgumentException
 * if either the key or the value is already in the Map.<p>
 *
 * Obviously, that same restriction (and consequence of failing to
 * heed that restriction) applies to the putAll method.<p>
 *
 * The Map.Entry instances returned by the appropriate methods will
 * not allow setValue() and will throw an
 * UnsupportedOperationException on attempts to call that method.<p>
 *
 * New methods are added to take advantage of the fact that values are
 * kept sorted independently of their keys:<p>
 *
 * Object getKeyForValue(Object value) is the opposite of get; it
 * takes a value and returns its key, if any.<p>
 *
 * Object removeValue(Object value) finds and removes the specified
 * value and returns the now un-used key.<p>
 *
 * Set entrySetByValue() returns the Map.Entry's in a Set whose
 * iterator will iterate over the Map.Entry's in ascending order by
 * their corresponding values.<p>
 *
 * Set keySetByValue() returns the keys in a Set whose iterator will
 * iterate over the keys in ascending order by their corresponding
 * values.<p>
 *
 * Collection valuesByValue() returns the values in a Collection whose
 * iterator will iterate over the values in ascending order.<p>
 *
 * @since 3.2
 * 
 * @author Dave Oxley (david.oxley@workplace-systems.plc.uk)
 * @author John Donnelly (john.donnelly@workplace-systems.plc.uk)
 */
@SuppressWarnings("unchecked")
public class TransactionalBidiTreeMap<K,V> extends AbstractMap<K,V> implements TransactionalSortedFilterableBidiMap<K,V>, Serializable
{
    // Increase the number when an incompatible change is made
    private static final long serialVersionUID = -1890000639743995893L;
    
    //  final for performance
    private static final int KEY = 0;
    private static final int VALUE = 1;
    private static final int SUM_OF_INDICES = KEY + VALUE;
    private static final int FIRST_INDEX = 0;
    private static final int NUMBER_OF_INDICES = 2;
    private static final String[] dataName = new String[] { "key", "value" };
    
    private final Node<K,V>[] rootNode = new Node[] { null, null };
    private int nodeCount = 0;
    private int modifications = 0;
    private transient FilterableSet<K> setOfKeysByKey = null;
    private transient FilterableSet<K> setOfKeysByValue = null;
    private transient FilterableSet<Entry<K,V>> setOfEntriesByKey = null;
    private transient FilterableSet<Entry<K,V>> setOfEntriesByValue = null;
    private transient FilterableSet<Entry<K,V>> setOfAllEntries = null;
    private transient FilterableCollection<V> collectionOfValuesByKey = null;
    private transient FilterableCollection<V> collectionOfValuesByValue = null;
    /* ? Was getting errors re. null comparator. Is there a better way to do this? */
   // private Comparator[] comparators = new Comparator[] { new DefaultComparator(), new DefaultComparator() };
    private Comparator[] comparators = new Comparator[] { null, null};
    private boolean auto_commit = true;

    private final static Log log = LogFactory.getLog(TransactionalBidiTreeMap.class);

    /**
     * Construct a new, empty TransactionalBidiTreeMap
     */
    public TransactionalBidiTreeMap() {}

    /**
     * Constructs a new TransactionalBidiTreeMap from an existing Map, with keys and
     * values sorted
     *
     * @param map the map whose mappings are to be placed in this map.
     *
     * @throws ClassCastException if the keys in the map are not
     *                               Comparable, or are not mutually
     *                               comparable; also if the values in
     *                               the map are not Comparable, or
     *                               are not mutually Comparable
     * @throws NullPointerException if any key or value in the map
     *                                 is null
     * @throws IllegalArgumentException if there are duplicate keys
     *                                     or duplicate values in the
     *                                     map
     */
    public TransactionalBidiTreeMap(final Map<? extends K,? extends V> map)
            throws ClassCastException, NullPointerException,
                   IllegalArgumentException {
        putAll(map);
    }

    /**
     * Constructs a new, empty TransactionalBidiTreeMap, sorted according to
     * the given comparators for key and value
     *
     * @param key_c the comparator that will be used to sort the keys
     *        in this map.  A <tt>null</tt> value indicates that the
     *        keys' <i>natural ordering</i> should be used.
     * @param value_c the comparator that will be used to sort the values
     *        in this map.  A <tt>null</tt> value indicates that the
     *        values' <i>natural ordering</i> should be used.
     */
    public TransactionalBidiTreeMap(final Comparator<? super K> key_c,
                                   final Comparator<? super V> value_c) {
        comparators[0] = (Comparator<Object>) key_c;
        comparators[1] = (Comparator<Object>) value_c;
    }
    
    /** create a string for debug 
    public String rbmDump()
    {
        Set keys = this.entrySet();
        String dump = "EntrySet - size=" + keys.size() + ": ";
        Iterator ik = keys.iterator();
        while (ik.hasNext())
        {
            Map.Entry ike = (Map.Entry) ik.next();
            dump += "[" + ike.getKey() + "/" + ike.getValue().hashCode() + "]" + "\n"; 
        }
        
        Set values = this.entrySetByValue();
        dump += "ValueSet - size=" + values.size() + ": ";
        Iterator iv = values.iterator();
        while (iv.hasNext())
        {
            Map.Entry ive = (Map.Entry) iv.next();
            dump += "[" + ive.getKey() + "/" + ive.getValue().hashCode() + "]" + "\n"; 
        }
        
        return dump;
    }*/

    protected final String getThreadSessionKey()
    {
        // Use the hashCode of the rootNode array to get a unique id for this Map.
        // AbstractMap.hashCode returns the hashCode of all elements and therefore changes over time.
        return ".TransactionalBidiTreeMap(" + rootNode.hashCode() + ")-attach_id";
    }

    /**
     * Attach the current thread to a specific id rather than using the
     * current thread for transactional changes. This allows transactional
     * changes to be done by different threads.
     */
    public void attach(final String attach_id)
    {
        ThreadSession.putValue(getThreadSessionKey(), attach_id);
    }

    /**
     * Detach the current thread from the attach_id.
     */
    public void detach()
    {
        ThreadSession.removeValue(getThreadSessionKey());
    }

    /**
     * Sets this map's  auto commit state to the given state.
     * If a map is in auto commit mode then all changes are 
     * available to all threads immediately, otherwise commit
     * must be called.
     * <P>
     * The default for the map is auto commit is on.
     * <P>
     * <B>NOTE:</B>  If this method is called while there are outstanding
     * transactions, commit is called.
     *
     * @param auto_commit - false to enable transaction support
     *                      within this map.
     */
    public void setAutoCommit(final boolean auto_commit) {

        if (!this.auto_commit && auto_commit)
            commit(null);

        this.auto_commit = auto_commit;
    }

    public boolean isAutoCommit()
    {
        return auto_commit;
    }

    public interface CommitNotifiable extends Serializable
    {
        void addedToMap(Object key, Object value);

        void removedFromMap(Object key, Object value);
    }

    private transient Set commit_notifiers = null;

    public void setCommitNotifier(CommitNotifiable commit_notifier)
    {
        if (commit_notifiers == null)
            commit_notifiers = SynchronizedSet.decorate(new HashSet());
        commit_notifiers.add(commit_notifier);
    }

    public void removeCommitNotifier(CommitNotifiable commit_notifier)
    {
        commit_notifiers.remove(commit_notifier);
    }

    /**
     * Commits the changes to the map so that all threads
     * see them.
     */
    public void commit() {
        
        if (auto_commit)
            return;
        
        commit(getCurrentThreadId());
    }
    
    /**
     * Rolls back the changes to the map.
     */
    public void rollback() {

        if (auto_commit)
            return;
        
        String id = getCurrentThreadId();

        ArrayList<Entry<K,V>> list = new ArrayList<Entry<K,V>>(allEntrySet());
        for (Iterator<Entry<K,V>> i = list.iterator(); i.hasNext(); ) {
            Node<K,V> node = (Node<K,V>)i.next();

            if (node.is(Node.ADDED, id))
                doRedBlackDelete(node);

            if (node.is(Node.DELETED, id))
                node.setStatus(Node.NO_CHANGE, null);
        }
    }
    
    /**
     * Commits the changes to the map so that all threads
     * see them.
     *
     * @param id the thread id to commit for, null commits
     *           all threads changes
     */
    private void commit(final String id) {

        ArrayList<Entry<K,V>> list = new ArrayList<Entry<K,V>>(allEntrySet());
        for (Iterator<Entry<K,V>> i = list.iterator(); i.hasNext(); ) {
            final Node<K,V> node = (Node<K,V>)i.next();

            if (node.is(Node.DELETED, id)) {
                doRedBlackDelete(node);
                if (commit_notifiers != null)
                {
                    SyncUtils.synchronizeRead(commit_notifiers, new Callback() {
                        @Override
                        protected void doAction()
                        {
                            for (Iterator i2 = commit_notifiers.iterator(); i2.hasNext(); )
                                ((CommitNotifiable)i2.next()).removedFromMap(node.getKey(), node.getValue());
                        }
                    });
                }

            }

            if (node.is(Node.ADDED, id)) {
                node.setStatus(Node.NO_CHANGE, null);
                if (commit_notifiers != null)
                {
                    SyncUtils.synchronizeRead(commit_notifiers, new Callback() {
                        @Override
                        protected void doAction()
                        {
                            for (Iterator i2 = commit_notifiers.iterator(); i2.hasNext(); )
                                ((CommitNotifiable)i2.next()).addedToMap(node.getKey(), node.getValue());
                        }
                    });
                }
            }
        }
    }

    /**
     * Retrieve the current thread id for use by the
     * transaction code.
     *
     * @return the thread id of the current thread
     */
    protected String getCurrentThreadId() {

        String attach_id = (String)ThreadSession.getValue(getThreadSessionKey());
        if (attach_id != null)
            return attach_id;

        Thread thread = Thread.currentThread();
        return thread.toString() + "(" + thread.hashCode() + ")";
    }
    
    /**
     * Checks that this node is valid for the current thread
     *
     * @param node the node to be checked
     *
     * @return true if node is valid, otherwise false
     */
    private boolean validNode(final Node<K,V> node, final String thread_id) {
        if (auto_commit || node == null)
            return (node != null);

        return !((node.is(Node.DELETED, thread_id)) ||
                   (node.is(Node.ADDED, null) && node.is(Node.NO_CHANGE, thread_id)));
    }
    
    /**
     * Returns the key to which this map maps the specified value.
     * Returns null if the map contains no mapping for this value.
     *
     * @param value value whose associated key is to be returned.
     *
     * @return the key to which this map maps the specified value, or
     *         null if the map contains no mapping for this value.
     *
     * @throws ClassCastException if the value is of an
     *                               inappropriate type for this map.
     * @throws NullPointerException if the value is null
     */
    public K getKeyForValue(final Object value)
            throws ClassCastException, NullPointerException {
        return (K)doGet(value, VALUE);
    }

    /**
     * Removes the mapping for this value from this map if present
     *
     * @param value value whose mapping is to be removed from the map.
     *
     * @return previous key associated with specified value, or null
     *         if there was no mapping for value.
     *
     * @throws ConcurrentModificationException if the value has been
     *                                     removed by another thread
     */
    public K removeValue(final Object value)
	throws ConcurrentModificationException {
        return (K)doRemove(value, VALUE);
    }

    /**
     * Returns a set view of the mappings contained in this map. Each
     * element in the returned set is a Map.Entry. The set is backed
     * by the map, so changes to the map are reflected in the set, and
     * vice-versa.  If the map is modified while an iteration over the
     * set is in progress, the results of the iteration are
     * undefined. The set supports element removal, which removes the
     * corresponding mapping from the map, via the Iterator.remove,
     * Set.remove, removeAll, retainAll and clear operations.  It does
     * not support the add or addAll operations.<p>
     *
     * The difference between this method and entrySet is that
     * entrySet's iterator() method returns an iterator that iterates
     * over the mappings in ascending order by key. This method's
     * iterator method iterates over the mappings in ascending order
     * by value.
     *
     * @return a set view of the mappings contained in this map.
     */
    public FilterableSet<Entry<K,V>> entrySetByValue() {

        if (setOfEntriesByValue == null) {
            setOfEntriesByValue = new AbstractFilterableSet<Entry<K,V>>() {

                @Override
                public Iterator<Entry<K,V>> iterator() {

                    return new TransactionalBidiTreeMapIterator<Entry<K,V>>(VALUE) {

                        @Override
                        protected Entry<K,V> doGetNext() {
                            return lastReturnedNode;
                        }
                    };
                }

                @Override
                public boolean contains(Object o) {

                    if (!(o instanceof Map.Entry)) {
                        return false;
                    }

                    Map.Entry<K,V> entry = (Map.Entry<K,V>) o;
                    Object         key   = entry.getKey();
                    Node<K,V>      node  = lookupValid(entry.getValue(),
                                             VALUE,
                                             getCurrentThreadId());

                    return (node!=null) && node.getData(KEY).equals(key);
                }

                @Override
                public boolean remove(Object o) {

                    if (!(o instanceof Map.Entry)) {
                        return false;
                    }

                    String thread_id = getCurrentThreadId();

                    Map.Entry<K,V> entry = (Map.Entry<K,V>) o;
                    Object         key   = entry.getKey();
                    Node<K,V>      node  = lookupValid(entry.getValue(),
                                             VALUE,
                                             thread_id);

                    if ((node!=null) && node.getData(KEY).equals(key)) {
                        if (auto_commit)
                            doRedBlackDelete(node);
                        else
                            node.setStatus(Node.DELETED, thread_id);

                        return true;
                    }

                    return false;
                }

                @Override
                public int size() {
                    return TransactionalBidiTreeMap.this.size();
                }

                @Override
                public void clear() {
                    TransactionalBidiTreeMap.this.clear();
                }
            };
        }

        return setOfEntriesByValue;
    }

    /**
     * Returns a set view of the keys contained in this map.  The set
     * is backed by the map, so changes to the map are reflected in
     * the set, and vice-versa. If the map is modified while an
     * iteration over the set is in progress, the results of the
     * iteration are undefined. The set supports element removal,
     * which removes the corresponding mapping from the map, via the
     * Iterator.remove, Set.remove, removeAll, retainAll, and clear
     * operations. It does not support the add or addAll
     * operations.<p>
     *
     * The difference between this method and keySet is that keySet's
     * iterator() method returns an iterator that iterates over the
     * keys in ascending order by key. This method's iterator method
     * iterates over the keys in ascending order by value.
     *
     * @return a set view of the keys contained in this map.
     */
    public FilterableSet<K> keySetByValue() {

        if (setOfKeysByValue == null) {
            setOfKeysByValue = new AbstractFilterableSet<K>() {

                @Override
                public Iterator<K> iterator() {

                    return new TransactionalBidiTreeMapIterator<K>(VALUE) {

                        @Override
                        protected K doGetNext() {
                            return (K)lastReturnedNode.getData(KEY);
                        }
                    };
                }

                @Override
                public int size() {
                    return TransactionalBidiTreeMap.this.size();
                }

                @Override
                public boolean contains(Object o) {
                    return containsKey(o);
                }

                @Override
                public boolean remove(Object o) {

                    int oldnodeCount = nodeCount;

                    TransactionalBidiTreeMap.this.remove(o);

                    return nodeCount != oldnodeCount;
                }

                @Override
                public void clear() {
                    TransactionalBidiTreeMap.this.clear();
                }
            };
        }

        return setOfKeysByValue;
    }

    /**
     * Returns a collection view of the values contained in this
     * map. The collection is backed by the map, so changes to the map
     * are reflected in the collection, and vice-versa. If the map is
     * modified while an iteration over the collection is in progress,
     * the results of the iteration are undefined. The collection
     * supports element removal, which removes the corresponding
     * mapping from the map, via the Iterator.remove,
     * Collection.remove, removeAll, retainAll and clear operations.
     * It does not support the add or addAll operations.<p>
     *
     * The difference between this method and values is that values's
     * iterator() method returns an iterator that iterates over the
     * values in ascending order by key. This method's iterator method
     * iterates over the values in ascending order by key.
     *
     * @return a collection view of the values contained in this map.
     */
    public FilterableCollection<V> valuesByValue() {

        if (collectionOfValuesByValue == null) {
            collectionOfValuesByValue = new AbstractFilterableCollection<V>() {

                @Override
                public Iterator<V> iterator() {

                    return new TransactionalBidiTreeMapIterator<V>(VALUE) {

                        @Override
                        protected V doGetNext() {
                            return (V)lastReturnedNode.getData(VALUE);
                        }
                    };
                }

                @Override
                public int size() {
                    return TransactionalBidiTreeMap.this.size();
                }

                @Override
                public boolean contains(Object o) {
                    return containsValue(o);
                }

                @Override
                public boolean remove(Object o) {

                    int oldnodeCount = nodeCount;

                    removeValue(o);

                    return nodeCount != oldnodeCount;
                }

                @Override
                public boolean removeAll(Collection<?> c) {

                    boolean     modified = false;
                    Iterator<?> iter     = c.iterator();

                    while (iter.hasNext()) {
                        if (removeValue(iter.next()) != null) {
                            modified = true;
                        }
                    }

                    return modified;
                }

                @Override
                public void clear() {
                    TransactionalBidiTreeMap.this.clear();
                }
            };
        }

        return collectionOfValuesByValue;
    }

    

    /**
     * common remove logic (remove by key or remove by value)
     *
     * @param o the key, or value, that we're looking for
     * @param index KEY or VALUE
     *
     * @return the key, if remove by value, or the value, if remove by
     *         key. null if the specified key or value could not be
     *         found
     *
     * @throws ConcurrentModificationException if the node has been
     *                                     removed by another thread
     */
    private Object doRemove(final Object o, final int index)
                throws ConcurrentModificationException {

        checkNonNullComparable(o, index);

        String thread_id = getCurrentThreadId();

        Node<K,V>   node = lookupValid(o, index, thread_id);
        Object      rval = null;

        if (validNode(node, thread_id)) {
            if (node != null && node.is(Node.DELETED, null) && !node.is(Node.DELETED, thread_id))
                throw new ConcurrentModificationException();

            rval = node.getData(oppositeIndex(index));

            if (auto_commit || node.is(Node.ADDED, thread_id))
                doRedBlackDelete(node);
            else {
                node.setStatus(Node.DELETED, thread_id);
            }
        }


        return rval;
    }

    /**
     * common get logic, used to get by key or get by value
     *
     * @param o the key or value that we're looking for
     * @param index KEY or VALUE
     *
     * @return the key (if the value was mapped) or the value (if the
     *         key was mapped); null if we couldn't find the specified
     *         object
     */
    private Object doGet(final Object o, final int index) {

        checkNonNullComparable(o, index);

        Node<K,V> node = lookupValid(o, index, getCurrentThreadId());

        return (node==null)
                ? null
                : node.getData(oppositeIndex(index));
    }

    /**
     * Get the opposite index of the specified index
     *
     * @param index KEY or VALUE
     *
     * @return VALUE (if KEY was specified), else KEY
     */
    private int oppositeIndex(final int index) {

        // old trick ... to find the opposite of a value, m or n,
        // subtract the value from the sum of the two possible
        // values. (m + n) - m = n; (m + n) - n = m
        return SUM_OF_INDICES - index;
    }

    /**
     * do the actual lookup of a piece of valid data
     *
     * @param data the key or value to be looked up
     * @param index KEY or VALUE
     *
     * @return the desired Node, or null if there is no mapping of the
     *         specified data
     */
    private Node<K,V> lookupValid(final Object data, final int index, final String thread_id) {
        return nextEqualValid(getFloorEqualNode(lookup(data, index), index), index, thread_id);
    }
    
    /**
     * do the actual lookup of a piece of data
     *
     * @param data the key or value to be looked up
     * @param index KEY or VALUE
     *
     * @return the desired Node, or null if there is no mapping of the
     *         specified data
     */
    private Node<K,V> lookup(final Object data, final int index) {

        Node<K,V> rval = null;
        Node<K,V> node = rootNode[index];

        while (node != null) {
            int cmp = compare(Node.NO_CHANGE, data, node.getStatus(), node.getData(index), index);

            if (cmp == 0) {
                rval = node;

                break;
            } else {
                node = (cmp < 0)
                       ? node.getLeft(index)
                       : node.getRight(index);
            }
        }

        return rval;
    }

    /**
     * Compare two objects
     *
     * @param o1 the first object
     * @param o2 the second object
     *
     * @return negative value if o1 < o2; 0 if o1 == o2; positive
     *         value if o1 > o2
     */
    private int compare(final int o1_status, final Object o1, final int o2_status, final Object o2, final int index) {
        if (comparators[index]==null) {
            if (o1 instanceof TransactionalComparable)
                return ((TransactionalComparable)o1).compareTo(o1_status, o2, o2_status);
            else
                return ((Comparable)o1).compareTo(o2);
        } else {
            return comparators[index].compare(o1, o2);
        }
    }

    /**
     * find the least node from a given node. very useful for starting
     * a sorting iterator ...
     *
     * @param node the node from which we will start searching
     * @param index KEY or VALUE
     *
     * @return the smallest node, from the specified node, in the
     *         specified mapping
     */
    private Node<K,V> leastNode(final Node<K,V> node, final int index) {

        Node<K,V> lval = node;

        if (lval != null) {
            while (lval.getLeft(index) != null) {
                lval = lval.getLeft(index);
            }
        }

        return lval;
    }

    /**
     * find the most node from a given node.
     *
     * @param node the node from which we will start searching
     * @param index KEY or VALUE
     *
     * @return the largest node, from the specified node, in the
     *         specified mapping
     */
    private Node<K,V> mostNode(final Node<K,V> node, final int index) {

        Node<K,V> rval = node;

        if (rval != null) {
            while (rval.getRight(index) != null) {
                rval = rval.getRight(index);
            }
        }

        return rval;
    }

    /**
     * get the next larger node from the specified node
     *
     * @param node the node to be searched from
     * @param index KEY or VALUE
     *
     * @return the specified node
     */
    private Node<K,V> nextGreater(final Node<K,V> node, final int index) {

        Node<K,V> rval = null;

        if (node == null) {
            rval = null;
        } else if (node.getRight(index) != null) {

            // everything to the node's right is larger. The least of
            // the right node's descendants is the next larger node
            rval = leastNode(node.getRight(index), index);
        } else {

            // traverse up our ancestry until we find an ancestor that
            // is null or one whose left child is our ancestor. If we
            // find a null, then this node IS the largest node in the
            // tree, and there is no greater node. Otherwise, we are
            // the largest node in the subtree on that ancestor's left
            // ... and that ancestor is the next greatest node
            Node<K,V> parent = node.getParent(index);
            Node<K,V> child  = node;

            while ((parent != null) && (child == parent.getRight(index))) {
                child  = parent;
                parent = parent.getParent(index);
            }

            rval = parent;
        }

        return rval;
    }
    
    /**
     * get the next smaller (previous) node from the specified node
     *
     * @param node the node to be searched from
     * @param index KEY or VALUE
     *
     * @return the specified node
     */
    private Node<K,V> nextSmaller(final Node<K,V> node, final int index) {

        Node<K,V> lval = null;

        if (node == null) {
            lval = null;
        } else if (node.getLeft(index) != null) {

            // everything to the node's left is smaller. The most of
            // the right node's descendants is the next smaller node
            lval = mostNode(node.getLeft(index), index);
        } else {

            // traverse up our ancestry until we find an ancestor that
            // is null or one whose right child is our ancestor. If we
            // find a null, then this node IS the smallest node in the
            // tree, and there is no smaller node. Otherwise, we are
            // the smallest node in the subtree on that ancestor's right
            // ... and that ancestor is the next smallest node
            Node<K,V> parent = node.getParent(index);
            Node<K,V> child  = node;

            while ((parent != null) && (child == parent.getLeft(index))) {
                child  = parent;
                parent = parent.getParent(index);
            }

            lval = parent;
        }

        return lval;
    }

    /**
     * get the most valid node from the specified node
     *
     * @param node the node to be searched from
     * @param index KEY or VALUE
     *
     * @return the specified node
     */
    private Node<K,V> mostValidNode(final Node<K,V> node, final int index, final String thread_id) {
        Node<K,V> rval = node;

        while (rval != null && !validNode(rval, thread_id)) {
            rval = nextGreater(rval, index);
        }
        return rval;
    }
    
    /**
     * find the least valid node from a given node. very useful for starting
     * a sorting iterator ...
     *
     * @param node the node from which we will start searching
     * @param index KEY or VALUE
     *
     * @return the smallest valid node, from the specified node, in the
     *         specified mapping
     */
    private Node<K,V> leastValidNode(final Node<K,V> node, final int index, final String thread_id) {
        Node<K,V> lval = node;

        while (lval != null && !validNode(lval, thread_id)) {
            lval = nextSmaller(lval, index);
        }
        return lval;
    }
    
    private Node<K,V> getFloorEqualNode(Node<K,V> node, final int index) {
        Node<K,V> current = node;
        while (node != null && compare(current.getStatus(), current.getData(index), node.getStatus(), node.getData(index), index) == 0) {
            current = node;
            node = nextSmaller(node, index);
        }
        return current;
    }
    
    private Node<K,V> nextEqualValid(Node<K,V> node, final int index, final String thread_id) {
        Node<K,V> current = node;
        while (node != null && !validNode(node, thread_id) && compare(current.getStatus(), current.getData(index), node.getStatus(), node.getData(index), index) == 0) {
            node = nextGreater(node, index);
        }
        if (node != null && current != null &&
                compare(current.getStatus(), current.getData(index), node.getStatus(), node.getData(index), index) == 0)
            return node;
        return null;
    }
    
    /**
     * copy the color from one node to another, dealing with the fact
     * that one or both nodes may, in fact, be null
     *
     * @param from the node whose color we're copying; may be null
     * @param to the node whose color we're changing; may be null
     * @param index KEY or VALUE
     */
    private static <K,V> void copyColor(final Node<K,V> from, final Node<K,V> to,
                                  final int index) {

        if (to != null) {
            if (from == null) {

                // by default, make it black
                to.setBlack(index);
            } else {
                to.copyColor(from, index);
            }
        }
    }

    /**
     * is the specified node red? if the node does not exist, no, it's
     * black, thank you
     *
     * @param node the node (may be null) in question
     * @param index KEY or VALUE
     */
    private static <K,V> boolean isRed(final Node<K,V> node, final int index) {

        return ((node == null)
                ? false
                : node.isRed(index));
    }

    /**
     * is the specified black red? if the node does not exist, sure,
     * it's black, thank you
     *
     * @param node the node (may be null) in question
     * @param index KEY or VALUE
     */
    private static <K,V> boolean isBlack(final Node<K,V> node, final int index) {

        return ((node == null)
                ? true
                : node.isBlack(index));
    }

    /**
     * force a node (if it exists) red
     *
     * @param node the node (may be null) in question
     * @param index KEY or VALUE
     */
    private static <K,V> void makeRed(final Node<K,V> node, final int index) {

        if (node != null) {
            node.setRed(index);
        }
    }

    /**
     * force a node (if it exists) black
     *
     * @param node the node (may be null) in question
     * @param index KEY or VALUE
     */
    private static <K,V> void makeBlack(final Node<K,V> node, final int index) {

        if (node != null) {
            node.setBlack(index);
        }
    }

    /**
     * get a node's grandparent. mind you, the node, its parent, or
     * its grandparent may not exist. no problem
     *
     * @param node the node (may be null) in question
     * @param index KEY or VALUE
     */
    private static <K,V> Node<K,V> getGrandParent(final Node<K,V> node, final int index) {
        return getParent(getParent(node, index), index);
    }

    /**
     * get a node's parent. mind you, the node, or its parent, may not
     * exist. no problem
     *
     * @param node the node (may be null) in question
     * @param index KEY or VALUE
     */
    private static <K,V> Node<K,V> getParent(final Node<K,V> node, final int index) {

        return ((node == null)
                ? null
                : node.getParent(index));
    }

    /**
     * get a node's right child. mind you, the node may not exist. no
     * problem
     *
     * @param node the node (may be null) in question
     * @param index KEY or VALUE
     */
    private static <K,V> Node<K,V> getRightChild(final Node<K,V> node, final int index) {

        return (node == null)
               ? null
               : node.getRight(index);
    }

    /**
     * get a node's left child. mind you, the node may not exist. no
     * problem
     *
     * @param node the node (may be null) in question
     * @param index KEY or VALUE
     */
    private static <K,V> Node<K,V> getLeftChild(final Node<K,V> node, final int index) {

        return (node == null)
               ? null
               : node.getLeft(index);
    }

    /**
     * is this node its parent's left child? mind you, the node, or
     * its parent, may not exist. no problem. if the node doesn't
     * exist ... it's its non-existent parent's left child. If the
     * node does exist but has no parent ... no, we're not the
     * non-existent parent's left child. Otherwise (both the specified
     * node AND its parent exist), check.
     *
     * @param node the node (may be null) in question
     * @param index KEY or VALUE
     */
    private static <K,V> boolean isLeftChild(final Node<K,V> node, final int index) {

        return (node == null)
               ? true
               : ((node.getParent(index) == null)
                  ? false
                  : (node == node.getParent(index).getLeft(index)));
    }

    /**
     * is this node its parent's right child? mind you, the node, or
     * its parent, may not exist. no problem. if the node doesn't
     * exist ... it's its non-existent parent's right child. If the
     * node does exist but has no parent ... no, we're not the
     * non-existent parent's right child. Otherwise (both the
     * specified node AND its parent exist), check.
     *
     * @param node the node (may be null) in question
     * @param index KEY or VALUE
     */
    private static <K,V> boolean isRightChild(final Node<K,V> node, final int index) {

        return (node == null)
               ? true
               : ((node.getParent(index) == null)
                  ? false
                  : (node == node.getParent(index).getRight(index)));
    }

    /**
     * do a rotate left. standard fare in the world of balanced trees
     *
     * @param node the node to be rotated
     * @param index KEY or VALUE
     */
    private void rotateLeft(final Node<K,V> node, final int index) {

        Node<K,V> rightChild = node.getRight(index);

        node.setRight(rightChild.getLeft(index), index);

        if (rightChild.getLeft(index) != null) {
            rightChild.getLeft(index).setParent(node, index);
        }

        rightChild.setParent(node.getParent(index), index);

        if (node.getParent(index) == null) {

            // node was the root ... now its right child is the root
            rootNode[index] = rightChild;
        } else if (node.getParent(index).getLeft(index) == node) {
            node.getParent(index).setLeft(rightChild, index);
        } else {
            node.getParent(index).setRight(rightChild, index);
        }

        rightChild.setLeft(node, index);
        node.setParent(rightChild, index);
    }

    /**
     * do a rotate right. standard fare in the world of balanced trees
     *
     * @param node the node to be rotated
     * @param index KEY or VALUE
     */
    private void rotateRight(final Node<K,V> node, final int index) {

        Node<K,V> leftChild = node.getLeft(index);

        node.setLeft(leftChild.getRight(index), index);

        if (leftChild.getRight(index) != null) {
            leftChild.getRight(index).setParent(node, index);
        }

        leftChild.setParent(node.getParent(index), index);

        if (node.getParent(index) == null) {

            // node was the root ... now its left child is the root
            rootNode[index] = leftChild;
        } else if (node.getParent(index).getRight(index) == node) {
            node.getParent(index).setRight(leftChild, index);
        } else {
            node.getParent(index).setLeft(leftChild, index);
        }

        leftChild.setRight(node, index);
        node.setParent(leftChild, index);
    }

    /**
     * complicated red-black insert stuff. Based on Sun's TreeMap
     * implementation, though it's barely recognizable any more
     *
     * @param insertedNode the node to be inserted
     * @param index KEY or VALUE
     */
    private void doRedBlackInsert(final Node<K,V> insertedNode, final int index) {

        Node<K,V> currentNode = insertedNode;

        makeRed(currentNode, index);

        while ((currentNode != null) && (currentNode != rootNode[index])
                && (isRed(currentNode.getParent(index), index))) {
            if (isLeftChild(getParent(currentNode, index), index)) {
                Node<K,V> y = getRightChild(getGrandParent(currentNode, index),
                                       index);

                if (isRed(y, index)) {
                    makeBlack(getParent(currentNode, index), index);
                    makeBlack(y, index);
                    makeRed(getGrandParent(currentNode, index), index);

                    currentNode = getGrandParent(currentNode, index);
                } else {
                    if (isRightChild(currentNode, index)) {
                        currentNode = getParent(currentNode, index);

                        rotateLeft(currentNode, index);
                    }

                    makeBlack(getParent(currentNode, index), index);
                    makeRed(getGrandParent(currentNode, index), index);

                    if (getGrandParent(currentNode, index) != null) {
                        rotateRight(getGrandParent(currentNode, index),
                                    index);
                    }
                }
            } else {

                // just like clause above, except swap left for right
                Node<K,V> y = getLeftChild(getGrandParent(currentNode, index),
                                      index);

                if (isRed(y, index)) {
                    makeBlack(getParent(currentNode, index), index);
                    makeBlack(y, index);
                    makeRed(getGrandParent(currentNode, index), index);

                    currentNode = getGrandParent(currentNode, index);
                } else {
                    if (isLeftChild(currentNode, index)) {
                        currentNode = getParent(currentNode, index);

                        rotateRight(currentNode, index);
                    }

                    makeBlack(getParent(currentNode, index), index);
                    makeRed(getGrandParent(currentNode, index), index);

                    if (getGrandParent(currentNode, index) != null) {
                        rotateLeft(getGrandParent(currentNode, index), index);
                    }
                }
            }
        }

        makeBlack(rootNode[index], index);
    }

    /**
     * complicated red-black delete stuff. Based on Sun's TreeMap
     * implementation, though it's barely recognizable any more
     *
     * @param deletedNode the node to be deleted
     */
    private void doRedBlackDelete(final Node<K,V> deletedNode) {

        for (int index = FIRST_INDEX; index < NUMBER_OF_INDICES; index++) {

            // if deleted node has both left and children, swap with
            // the next greater node
            if ((deletedNode.getLeft(index) != null)
                    && (deletedNode.getRight(index) != null)) {
                swapPosition(nextGreater(deletedNode, index), deletedNode,
                             index);
            }

            Node<K,V> replacement = ((deletedNode.getLeft(index) != null)
                                ? deletedNode.getLeft(index)
                                : deletedNode.getRight(index));

            if (replacement != null) {
                replacement.setParent(deletedNode.getParent(index), index);

                if (deletedNode.getParent(index) == null) {
                    rootNode[index] = replacement;
                } else if (deletedNode
                           == deletedNode.getParent(index).getLeft(index)) {
                    deletedNode.getParent(index).setLeft(replacement, index);
                } else {
                    deletedNode.getParent(index).setRight(replacement, index);
                }

                deletedNode.setLeft(null, index);
                deletedNode.setRight(null, index);
                deletedNode.setParent(null, index);

                if (isBlack(deletedNode, index)) {
                    doRedBlackDeleteFixup(replacement, index);
                }
            } else {

                // replacement is null
                if (deletedNode.getParent(index) == null) {

                    // empty tree
                    rootNode[index] = null;
                } else {

                    // deleted node had no children
                    if (isBlack(deletedNode, index)) {
                        doRedBlackDeleteFixup(deletedNode, index);
                    }

                    if (deletedNode.getParent(index) != null) {
                        if (deletedNode
                                == deletedNode.getParent(index)
                                    .getLeft(index)) {
                            deletedNode.getParent(index).setLeft(null, index);
                        } else {
                            deletedNode.getParent(index).setRight(null,
                                                  index);
                        }

                        deletedNode.setParent(null, index);
                    }
                }
            }
        }

        shrink();
    }

    /**
     * complicated red-black delete stuff. Based on Sun's TreeMap
     * implementation, though it's barely recognizable any more. This
     * rebalances the tree (somewhat, as red-black trees are not
     * perfectly balanced -- perfect balancing takes longer)
     *
     * @param replacementNode the node being replaced
     * @param index KEY or VALUE
     */
    private void doRedBlackDeleteFixup(final Node<K,V> replacementNode,
                                       final int index) {

        Node<K,V> currentNode = replacementNode;

        while ((currentNode != rootNode[index])
                && (isBlack(currentNode, index))) {
            if (isLeftChild(currentNode, index)) {
                Node<K,V> siblingNode =
                    getRightChild(getParent(currentNode, index), index);

                if (isRed(siblingNode, index)) {
                    makeBlack(siblingNode, index);
                    makeRed(getParent(currentNode, index), index);
                    rotateLeft(getParent(currentNode, index), index);

                    siblingNode = getRightChild(getParent(currentNode, index), index);
                }

                if (isBlack(getLeftChild(siblingNode, index), index)
                        && isBlack(getRightChild(siblingNode, index),
                                   index)) {
                    makeRed(siblingNode, index);

                    currentNode = getParent(currentNode, index);
                } else {
                    if (isBlack(getRightChild(siblingNode, index), index)) {
                        makeBlack(getLeftChild(siblingNode, index), index);
                        makeRed(siblingNode, index);
                        rotateRight(siblingNode, index);

                        siblingNode =
                            getRightChild(getParent(currentNode, index), index);
                    }

                    copyColor(getParent(currentNode, index), siblingNode,
                              index);
                    makeBlack(getParent(currentNode, index), index);
                    makeBlack(getRightChild(siblingNode, index), index);
                    rotateLeft(getParent(currentNode, index), index);

                    currentNode = rootNode[index];
                }
            } else {
                Node<K,V> siblingNode = getLeftChild(getParent(currentNode, index), index);

                if (isRed(siblingNode, index)) {
                    makeBlack(siblingNode, index);
                    makeRed(getParent(currentNode, index), index);
                    rotateRight(getParent(currentNode, index), index);

                    siblingNode = getLeftChild(getParent(currentNode, index), index);
                }

                if (isBlack(getRightChild(siblingNode, index), index)
                        && isBlack(getLeftChild(siblingNode, index), index)) {
                    makeRed(siblingNode, index);

                    currentNode = getParent(currentNode, index);
                } else {
                    if (isBlack(getLeftChild(siblingNode, index), index)) {
                        makeBlack(getRightChild(siblingNode, index), index);
                        makeRed(siblingNode, index);
                        rotateLeft(siblingNode, index);

                        siblingNode =
                            getLeftChild(getParent(currentNode, index), index);
                    }

                    copyColor(getParent(currentNode, index), siblingNode,
                              index);
                    makeBlack(getParent(currentNode, index), index);
                    makeBlack(getLeftChild(siblingNode, index), index);
                    rotateRight(getParent(currentNode, index), index);

                    currentNode = rootNode[index];
                }
            }
        }

        makeBlack(currentNode, index);
    }

    /**
     * swap two nodes (except for their content), taking care of
     * special cases where one is the other's parent ... hey, it
     * happens.
     *
     * @param x one node
     * @param y another node
     * @param index KEY or VALUE
     */
    private void swapPosition(final Node<K,V> x, final Node<K,V> y, final int index) {

        // Save initial values.
        Node<K,V>    xFormerParent     = x.getParent(index);
        Node<K,V>    xFormerLeftChild  = x.getLeft(index);
        Node<K,V>    xFormerRightChild = x.getRight(index);
        Node<K,V>    yFormerParent     = y.getParent(index);
        Node<K,V>    yFormerLeftChild  = y.getLeft(index);
        Node<K,V>    yFormerRightChild = y.getRight(index);
        boolean xWasLeftChild     =
            (x.getParent(index) != null)
            && (x == x.getParent(index).getLeft(index));
        boolean yWasLeftChild     =
            (y.getParent(index) != null)
            && (y == y.getParent(index).getLeft(index));

        // Swap, handling special cases of one being the other's parent.
        if (x == yFormerParent) {    // x was y's parent
            x.setParent(y, index);

            if (yWasLeftChild) {
                y.setLeft(x, index);
                y.setRight(xFormerRightChild, index);
            } else {
                y.setRight(x, index);
                y.setLeft(xFormerLeftChild, index);
            }
        } else {
            x.setParent(yFormerParent, index);

            if (yFormerParent != null) {
                if (yWasLeftChild) {
                    yFormerParent.setLeft(x, index);
                } else {
                    yFormerParent.setRight(x, index);
                }
            }

            y.setLeft(xFormerLeftChild, index);
            y.setRight(xFormerRightChild, index);
        }

        if (y == xFormerParent) {    // y was x's parent
            y.setParent(x, index);

            if (xWasLeftChild) {
                x.setLeft(y, index);
                x.setRight(yFormerRightChild, index);
            } else {
                x.setRight(y, index);
                x.setLeft(yFormerLeftChild, index);
            }
        } else {
            y.setParent(xFormerParent, index);

            if (xFormerParent != null) {
                if (xWasLeftChild) {
                    xFormerParent.setLeft(y, index);
                } else {
                    xFormerParent.setRight(y, index);
                }
            }

            x.setLeft(yFormerLeftChild, index);
            x.setRight(yFormerRightChild, index);
        }

        // Fix children's parent pointers
        if (x.getLeft(index) != null) {
            x.getLeft(index).setParent(x, index);
        }

        if (x.getRight(index) != null) {
            x.getRight(index).setParent(x, index);
        }

        if (y.getLeft(index) != null) {
            y.getLeft(index).setParent(y, index);
        }

        if (y.getRight(index) != null) {
            y.getRight(index).setParent(y, index);
        }

        x.swapColors(y, index);

        // Check if root changed
        if (rootNode[index] == x) {
            rootNode[index] = y;
        } else if (rootNode[index] == y) {
            rootNode[index] = x;
        }
    }

    /**
     * check if an object is fit to be proper input ... has to be
     * Comparable if the comparator has not been set and non-null
     *
     * @param o the object being checked
     * @param index KEY or VALUE (used to put the right word in the
     *              exception message)
     *
     * @throws NullPointerException if o is null
     * @throws ClassCastException if o is not Comparable and the 
     *         equivalent comparator has not been set
     */
    private void checkNonNullComparable(final Object o,
                                               final int index) {

        if (o == null) {
            throw new NullPointerException(dataName[index]
                                           + " cannot be null");
        }

        if (comparators[index] == null && !(o instanceof Comparable)) {
            throw new ClassCastException(dataName[index]
                                         + " must be Comparable");
        }
    }

    /**
     * check a key for validity
     *
     * @param key the key to be checked
     *
     * @throws NullPointerException if key is null
     * @throws ClassCastException if key is not appropriate
     */
    private void checkKey(final Object key) {
        checkNonNullComparable(key, KEY);
    }

    /**
     * check a value for validity
     *
     * @param value the value to be checked
     *
     * @throws NullPointerException if value is null
     * @throws ClassCastException if value is not appropriate
     */
    private void checkValue(final Object value) {
        checkNonNullComparable(value, VALUE);
    }

    /**
     * check a key and a value for validity
     *
     * @param key the key to be checked
     * @param value the value to be checked
     *
     * @throws NullPointerException if key or value is null
     * @throws ClassCastException if key or value is not appropriate
     */
    private void checkKeyAndValue(final Object key,
                                         final Object value) {
        checkKey(key);
        checkValue(value);
    }

    /**
     * increment the modification count -- used to check for
     * concurrent modification of the map through the map and through
     * an Iterator from one of its Set or Collection views
     */
    private void modify() {
        modifications++;
    }

    /**
     * bump up the size and note that the map has changed
     */
    private void grow() {

        modify();

        nodeCount++;
    }

    /**
     * decrement the size and note that the map has changed
     */
    private void shrink() {

        modify();

        nodeCount--;
    }

    /**
     * insert a node by its value
     *
     * @param newNode the node to be inserted
     *
     * @throws IllegalArgumentException if the node already exists
     *                                     in the value mapping
     */
    private void insertValue(final Node<K,V> newNode, final String thread_id)
            throws IllegalArgumentException {

        Node<K,V> node = rootNode[VALUE];

        while (true) {
            int cmp = compare(Node.ADDED, newNode.getData(VALUE), node.getStatus(), node.getData(VALUE), VALUE);

            if (cmp == 0) 
            {
                if (nextEqualValid(getFloorEqualNode(node, VALUE), VALUE, thread_id) != null)
                {
                    String debug_message = "Cannot store a duplicate value (\"" + newNode.getData(VALUE) 
                                 + "\") in this Map. Value already exists for key " + node.getKey();
                    log.debug (debug_message);             
                    
                    throw new IllegalArgumentException(debug_message); 
                 }
                
                if (node.is(Node.ADDED, null))
                    throw new ConcurrentModificationException();

                if (node.getRight(VALUE) != null) {
                    node = node.getRight(VALUE);
                } else if (node.getLeft(VALUE) != null) {
                    node = node.getLeft(VALUE);
                } else {
                    node.setRight(newNode, VALUE);
                    newNode.setParent(node, VALUE);
                    doRedBlackInsert(newNode, VALUE);

                    break;
                }
            } 
            else if (cmp < 0) {
                if (node.getLeft(VALUE) != null) {
                    node = node.getLeft(VALUE);
                } else {
                    node.setLeft(newNode, VALUE);
                    newNode.setParent(node, VALUE);
                    doRedBlackInsert(newNode, VALUE);

                    break;
                }
            } else {    // cmp > 0
                if (node.getRight(VALUE) != null) {
                    node = node.getRight(VALUE);
                } else {
                    node.setRight(newNode, VALUE);
                    newNode.setParent(node, VALUE);
                    doRedBlackInsert(newNode, VALUE);

                    break;
                }
            }
        }
    }

    /* ********** START implementation of Map ********** */

    /**
     * Returns the number of key-value mappings in this map. If the
     * map contains more than Integer.MAXVALUE elements, returns
     * Integer.MAXVALUE.
     *
     * @return the number of key-value mappings in this map.
     */
    @Override
    public int size() {
        return size(false);
    }

    public int size(boolean countAll) {
        if (countAll || auto_commit)
            return nodeCount;

        int size = 0;
        for (Iterator<Entry<K,V>> i = entrySet().iterator(); i.hasNext(); i.next()) {
            size++;
        }
        return size;
    }

    /**
     * Returns true is the map is empty.
     *
     * <p>This implementation returns <tt>!entrySet().iterator().hasNext()</tt>.
     */
    @Override
    public boolean isEmpty() {
	return !entrySet().iterator().hasNext();
    }

    /**
     * Returns true if this map contains a mapping for the specified
     * key.
     *
     * @param key key whose presence in this map is to be tested.
     *
     * @return true if this map contains a mapping for the specified
     *         key.
     *
     * @throws ClassCastException if the key is of an inappropriate
     *                               type for this map.
     * @throws NullPointerException if the key is null
     */
    @Override
    public boolean containsKey(final Object key)
            throws ClassCastException, NullPointerException {

        checkKey(key);

        return lookupValid(key, KEY, getCurrentThreadId()) != null;
    }

    /**
     * Returns true if this map maps one or more keys to the
     * specified value.
     *
     * @param value value whose presence in this map is to be tested.
     *
     * @return true if this map maps one or more keys to the specified
     *         value.
     */
    @Override
    public boolean containsValue(final Object value) {

        checkValue(value);

        return lookupValid(value, VALUE, getCurrentThreadId()) != null;
    }

    /**
     * Returns the value to which this map maps the specified
     * key. Returns null if the map contains no mapping for this key.
     *
     * @param key key whose associated value is to be returned.
     *
     * @return the value to which this map maps the specified key, or
     *         null if the map contains no mapping for this key.
     *
     * @throws ClassCastException if the key is of an inappropriate
     *                               type for this map.
     * @throws NullPointerException if the key is null
     */
    @Override
    public V get(final Object key)
            throws ClassCastException, NullPointerException {

        checkKey(key);

        return (V)doGet(key, KEY);
    }

    /**
     * Associates the specified value with the specified key in this
     * map.
     *
     * @param key key with which the specified value is to be
     *            associated.
     * @param value value to be associated with the specified key.
     *
     * @return null
     *
     * @throws ClassCastException if the class of the specified key
     *                               or value prevents it from being
     *                               stored in this map.
     * @throws NullPointerException if the specified key or value
     *                                 is null
     * @throws IllegalArgumentException if the key duplicates an
     *                                     existing key, or if the
     *                                     value duplicates an
     *                                     existing value
     */
    @Override
    public V put(final K key, final V value)
            throws ClassCastException, NullPointerException,
                   IllegalArgumentException, ConcurrentModificationException {

        checkKeyAndValue(key, value);

        Node<K,V> node = rootNode[KEY];

        String thread_id = getCurrentThreadId();

        if (node == null) {
            Node<K,V> root = new Node<K,V>(key, value);

            rootNode[KEY]   = root;
            rootNode[VALUE] = root;

            if (!auto_commit)
                root.setStatus(Node.ADDED, thread_id);

            grow();
        } else {
            while (true) {
                int cmp = compare(Node.ADDED, key, node.getStatus(), node.getData(KEY), KEY);

                if (cmp == 0) {
                    if (nextEqualValid(getFloorEqualNode(node, KEY), KEY, thread_id) != null)
                    {
                        String debug_message = "Cannot store a duplicate key (\"" + key + "\") in this Map";
                        log.debug (debug_message);             
                        throw new IllegalArgumentException(debug_message); 
                    }
                    
                    if (node.is(Node.ADDED, null))
                        throw new ConcurrentModificationException();
                    
                    if (node.getRight(KEY) != null) {
                        node = node.getRight(KEY);
                    } else if (node.getLeft(KEY) != null) {
                        node = node.getLeft(KEY);
                    } else {
                        Node<K,V> newNode = new Node<K,V>(key, value);

                        insertValue(newNode, thread_id);
                        node.setRight(newNode, KEY);
                        newNode.setParent(node, KEY);
                        doRedBlackInsert(newNode, KEY);
                        grow();

                        if (!auto_commit)
                            newNode.setStatus(Node.ADDED, thread_id);

                        break;
                    }
                } else if (cmp < 0) {
                    if (node.getLeft(KEY) != null) {
                        node = node.getLeft(KEY);
                    } else {
                        Node<K,V> newNode = new Node<K,V>(key, value);

                        insertValue(newNode, thread_id);
                        node.setLeft(newNode, KEY);
                        newNode.setParent(node, KEY);
                        doRedBlackInsert(newNode, KEY);
                        grow();

                        if (!auto_commit)
                            newNode.setStatus(Node.ADDED, thread_id);

                        break;
                    }
                } else {    // cmp > 0
                    if (node.getRight(KEY) != null) {
                        node = node.getRight(KEY);
                    } else {
                        Node<K,V> newNode = new Node<K,V>(key, value);

                        insertValue(newNode, thread_id);
                        node.setRight(newNode, KEY);
                        newNode.setParent(node, KEY);
                        doRedBlackInsert(newNode, KEY);
                        grow();

                        if (!auto_commit)
                            newNode.setStatus(Node.ADDED, thread_id);

                        break;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Removes the mapping for this key from this map if present
     *
     * @param key key whose mapping is to be removed from the map.
     *
     * @return previous value associated with specified key, or null
     *         if there was no mapping for key.
     */
    @Override
    public V remove(final Object key)
	throws ConcurrentModificationException {

        checkKey(key);

        return (V)doRemove(key, KEY);
    }

    /**
     * Removes all mappings from this map
     */
    @Override
    public void clear() {

        if (auto_commit)
        {
            modify();

            nodeCount   = 0;
            rootNode[KEY]   = null;
            rootNode[VALUE] = null;
        }
        else
        {
            String thread_id = getCurrentThreadId();
            ArrayList<Entry<K,V>> list = new ArrayList<Entry<K,V>>(entrySet());
            for (Iterator<Entry<K,V>> i = list.iterator(); i.hasNext(); ) {
                Node<K,V> node = (Node<K,V>)i.next();
                if (node.is(Node.ADDED, thread_id))
                    doRedBlackDelete(node);
                else {
                    node.setStatus(Node.DELETED, thread_id);
                }
            }
        }
    }

    /**
     * Returns a set view of the keys contained in this map.  The set
     * is backed by the map, so changes to the map are reflected in
     * the set, and vice-versa. If the map is modified while an
     * iteration over the set is in progress, the results of the
     * iteration are undefined. The set supports element removal,
     * which removes the corresponding mapping from the map, via the
     * Iterator.remove, Set.remove, removeAll, retainAll, and clear
     * operations.  It does not support the add or addAll operations.
     *
     * @return a set view of the keys contained in this map.
     */
    @Override
    public Set<K> keySet() {

        if (setOfKeysByKey == null) {
            setOfKeysByKey = new AbstractFilterableSet<K>() {

                @Override
                public Iterator<K> iterator() {

                    return new TransactionalBidiTreeMapIterator<K>(KEY) {

                        @Override
                        protected K doGetNext() {
                            return (K)lastReturnedNode.getData(KEY);
                        }
                    };
                }

                @Override
                public int size() {
                    return TransactionalBidiTreeMap.this.size();
                }

                @Override
                public boolean contains(Object o) {
                    return containsKey(o);
                }

                @Override
                public boolean remove(Object o) {

                    int oldNodeCount = nodeCount;

                    TransactionalBidiTreeMap.this.remove(o);

                    return nodeCount != oldNodeCount;
                }

                @Override
                public void clear() {
                    TransactionalBidiTreeMap.this.clear();
                }
            };
        }

        return setOfKeysByKey;
    }

    /**
     * Returns a collection view of the values contained in this
     * map. The collection is backed by the map, so changes to the map
     * are reflected in the collection, and vice-versa. If the map is
     * modified while an iteration over the collection is in progress,
     * the results of the iteration are undefined. The collection
     * supports element removal, which removes the corresponding
     * mapping from the map, via the Iterator.remove,
     * Collection.remove, removeAll, retainAll and clear operations.
     * It does not support the add or addAll operations.
     *
     * @return a collection view of the values contained in this map.
     */
    @Override
    public Collection<V> values() {

        if (collectionOfValuesByKey == null) {
            collectionOfValuesByKey = new AbstractFilterableCollection<V>() {

                @Override
                public Iterator<V> iterator() {

                    return new TransactionalBidiTreeMapIterator<V>(KEY) {

                        @Override
                        protected V doGetNext() {
                            return (V)lastReturnedNode.getData(VALUE);
                        }
                    };
                }

                @Override
                public int size() {
                    return TransactionalBidiTreeMap.this.size();
                }

                @Override
                public boolean contains(Object o) {
                    return containsValue(o);
                }

                @Override
                public boolean remove(Object o) {

                    int oldNodeCount = nodeCount;

                    removeValue(o);

                    return nodeCount != oldNodeCount;
                }

                @Override
                public boolean removeAll(Collection<?> c) {

                    boolean  modified = false;
                    Iterator<?> iter     = c.iterator();

                    while (iter.hasNext()) {
                        if (removeValue(iter.next()) != null) {
                            modified = true;
                        }
                    }

                    return modified;
                }

                @Override
                public void clear() {
                    TransactionalBidiTreeMap.this.clear();
                }
            };
        }

        return collectionOfValuesByKey;
    }

    /**
     * Returns a set view of the mappings contained in this map. Each
     * element in the returned set is a Map.Entry. The set is backed
     * by the map, so changes to the map are reflected in the set, and
     * vice-versa.  If the map is modified while an iteration over the
     * set is in progress, the results of the iteration are
     * undefined. The set supports element removal, which removes the
     * corresponding mapping from the map, via the Iterator.remove,
     * Set.remove, removeAll, retainAll and clear operations.  It does
     * not support the add or addAll operations.
     *
     * @return a set view of the mappings contained in this map.
     */
    @Override
    public Set<Entry<K,V>> entrySet() {

        if (setOfEntriesByKey == null) {
            setOfEntriesByKey = new AbstractFilterableSet<Entry<K,V>>() {

                @Override
                public Iterator<Entry<K,V>> iterator() {

                    return new TransactionalBidiTreeMapIterator<Entry<K,V>>(KEY) {

                        @Override
                        protected Entry<K,V> doGetNext() {
                            return lastReturnedNode;
                        }
                    };
                }

                @Override
                public boolean contains(Object o) {

                    if (!(o instanceof Map.Entry)) {
                        return false;
                    }

                    Map.Entry<K,V> entry = (Map.Entry<K,V>) o;
                    V              value = entry.getValue();
                    Node<K,V>      node  = lookupValid(entry.getKey(), KEY, getCurrentThreadId());

                    return (node!=null)
                           && node.getData(VALUE).equals(value);
                }

                @Override
                public boolean remove(Object o) {

                    if (!(o instanceof Map.Entry)) {
                        return false;
                    }

                    String thread_id = getCurrentThreadId();

                    Map.Entry<K,V> entry = (Map.Entry<K,V>) o;
                    V              value = entry.getValue();
                    Node<K,V>      node  = lookupValid(entry.getKey(), KEY, thread_id);

                    if ((node!=null) && node.getData(VALUE).equals(value)) {
                        if (auto_commit)
                            doRedBlackDelete(node);
                        else
                            node.setStatus(Node.DELETED, thread_id);

                        return true;
                    }

                    return false;
                }

                @Override
                public int size() {
                    return TransactionalBidiTreeMap.this.size();
                }

                @Override
                public void clear() {
                    TransactionalBidiTreeMap.this.clear();
                }
            };
        }

        return setOfEntriesByKey;
    }

    /**
     * It is very rare that this method would be required. You probably
     * want to use entrySet instead. This method returns all Entry's
     * in this Map no matter what its transactional status.
     * 
     * Returns a set view of the mappings contained in this map. Each
     * element in the returned set is a Map.Entry. The set is backed
     * by the map, so changes to the map are reflected in the set, and
     * vice-versa.  If the map is modified while an iteration over the
     * set is in progress, the results of the iteration are
     * undefined. The set supports element removal, which removes the
     * corresponding mapping from the map, via the Iterator.remove,
     * Set.remove, removeAll, retainAll and clear operations.  It does
     * not support the add or addAll operations.
     *
     * @return a set view of the mappings contained in this map.
     */
    public FilterableSet<Entry<K,V>> allEntrySet() {

        if (setOfAllEntries == null) {
            setOfAllEntries = new AbstractFilterableSet<Entry<K,V>>() {

                @Override
                public Iterator<Entry<K,V>> iterator() {

                    return new TransactionalBidiTreeMapIterator<Entry<K,V>>(KEY) {

                        @Override
                        protected Entry<K,V> doGetNext() {
                            return lastReturnedNode;
                        }
                        @Override
                        protected Node<K,V> getNextValidNode(Node<K,V> node, String thread_id) {
                            return node;
                        }
                    };
                }

                //cannot have contains or remove methods 
                //as we have ALL the nodes and so may have duplicates 
                //which are in the provess of being deleted
                @Override
                public boolean contains(Object o) {
                    throw new QueujException("method not supported");
                }

                @Override
                public boolean remove(Object o) {
                    throw new QueujException("method not supported");
                }

                @Override
                public int size() {
                    return TransactionalBidiTreeMap.this.size(true);
                }

                @Override
                public void clear() {
                    TransactionalBidiTreeMap.this.clear();
                }
            };
        }

        return setOfAllEntries;
    }

    /**
     * Copy all entries including transaction statuses from this map into the supplied map.
     * Do not use this method unless you know exactly what you are doing. The auto commit flag
     * of the supplied map may be changed as a result of calling this method, check that this is
     * valid first.
     */
    public final void copyEntries(TransactionalBidiTreeMap<K,V> new_map)
    {
        K key;
        V val;
        int transaction_status;
        String transaction_id;

        new_map.setAutoCommit(isAutoCommit());

        if (!isAutoCommit())
        {
            // Do committed and deleted first
            for(Iterator<Entry<K,V>> i = allEntrySet().iterator(); i.hasNext(); ) {
                TransactionalBidiTreeMap.Node<K,V> entry = (TransactionalBidiTreeMap.Node<K,V>)i.next();
                key=entry.getKey();
                val=entry.getValue();
                transaction_status=entry.getStatus();
                transaction_id=entry.getTransactionId();

                if (transaction_status != TransactionalBidiTreeMap.Node.ADDED)
                {
                    try
                    {
                        // Put the value against the key
                        new_map.put(key, val);

                        // As the transaction status is deleted or no change then we need to commit the entry now.
                        new_map.commit();
                    }
                    catch (Exception e) {} // Duplicate keys can be ignored, this means we already have the value

                    try
                    {
                        // If transaction status is deleted we need to now attach to the transaction id and remove.
                        if (transaction_status == TransactionalBidiTreeMap.Node.DELETED)
                        {
                            new_map.attach(transaction_id);
                            new_map.remove(key);
                        }
                    }
                    catch (Exception e) {} // The entry may have already been deleted 

                    // Finally detach
                    new_map.detach();
                }
            }

            // Then do added
            for(Iterator<Entry<K,V>> i = allEntrySet().iterator(); i.hasNext(); ) {
                TransactionalBidiTreeMap.Node<K,V> entry = (TransactionalBidiTreeMap.Node<K,V>)i.next();
                key=entry.getKey();
                val=entry.getValue();
                transaction_status=entry.getStatus();
                transaction_id=entry.getTransactionId();

                if (transaction_status == TransactionalBidiTreeMap.Node.ADDED)
                {
                    // As the transaction status is added then attach to the transaction id before putting.
                    new_map.attach(transaction_id);

                    try
                    {
                        // Put the value against the key
                        new_map.put(key, val);
                    }
                    catch (Exception e) {} // Duplicate keys can be ignored, this means we already have the value

                    // Finally detach
                    new_map.detach();
                }
            }
        }
        else
        {
            for(Iterator<Entry<K,V>> i = allEntrySet().iterator(); i.hasNext(); ) {
                TransactionalBidiTreeMap.Node<K,V> entry = (TransactionalBidiTreeMap.Node<K,V>)i.next();
                key=entry.getKey();
                val=entry.getValue();

                try
                {
                    new_map.put(key, val);
                }
                catch (Exception e) {} // Duplicate keys can be ignored, this means we already have the value
            }
        }
    }

    public void copyEntriesToSetState(TransactionalBidiTreeMap<K,V> new_rep_map) {}

    public Comparator<? super K> comparator() {
        return comparators[KEY];
    }
    
    public Comparator<? super V> valueComparator() {
        return comparators[VALUE];
    }
    
    public K firstKey() {
        return mostValidNode(leastNode(rootNode[KEY], KEY), KEY, getCurrentThreadId()).getKey();
    }
    
    public V firstValue() {
        return mostValidNode(leastNode(rootNode[KEY], KEY), KEY, getCurrentThreadId()).getValue();
    }
    
    public K firstKeyByValue() {
        return mostValidNode(leastNode(rootNode[VALUE], VALUE), VALUE, getCurrentThreadId()).getKey();
    }
    
    public V firstValueByValue() {
        final Node<K,V> most = mostValidNode(leastNode(rootNode[VALUE], VALUE), VALUE, getCurrentThreadId());
        return most==null ? null : most.getValue();
    }
    
    public K lastKey() {
        return leastValidNode(mostNode(rootNode[KEY], KEY), KEY, getCurrentThreadId()).getKey();
    }
    
    public V lastValue() {
        return leastValidNode(mostNode(rootNode[KEY], KEY), KEY, getCurrentThreadId()).getValue();
    }
    
    public K lastKeyByValue() {
        return leastValidNode(mostNode(rootNode[VALUE], VALUE), VALUE, getCurrentThreadId()).getKey();
    }

    public V lastValueByValue() {
        final Node<K,V> least = leastValidNode(mostNode(rootNode[VALUE], VALUE), VALUE, getCurrentThreadId());
        return least==null ? null : least.getValue();
    }

    public SortedMap<K,V> headMap(K toKey) {
        SubMapRestriction r = new SubMapRestriction(null, toKey, null, null, null, null);
        return new SubMap(r);
    }
    
    public SortedBidiMap<K,V> headMapByValue(V toValue) {
        SubMapRestriction r = new SubMapRestriction(null, null, null, toValue, null, null);
        return new SubMap(r);
    }
    
    public SortedMap<K,V> subMap(K fromKey, K toKey) {
        SubMapRestriction r = new SubMapRestriction(fromKey, toKey, null, null, null, null);
        return new SubMap(r);
    }
    
    public SortedBidiMap<K,V> subMapByValue(V fromValue, V toValue) {
        SubMapRestriction r = new SubMapRestriction(null, null, fromValue, toValue, null, null);
        return new SubMap(r);
    }
    
    public SortedMap<K,V> tailMap(K fromKey) {
        SubMapRestriction r = new SubMapRestriction( fromKey, null, null, null, null, null);
        return new SubMap(r);
    }
    
    public SortedBidiMap<K,V> tailMapByValue(V fromValue) {
        SubMapRestriction r = new SubMapRestriction(null, null, fromValue, null, null, null);
        return new SubMap(r);
    }
    public FilterableMap<K,V> filteredMap(Filter<? super K> filter) {
		SubMapRestriction r = new SubMapRestriction(null, null, null, null, filter, null);
		return new SubMap(r);
    }
    
    public FilterableBidiMap<K,V> filteredMapByValue(Filter<? super V> filter) {
		SubMapRestriction r = new SubMapRestriction(null, null, null, null, null, filter);
		return new SubMap(r);
    }
    
    /**
     * Gets the entry corresponding to the specified key; if no such entry
     * exists, returns the entry for the least key greater than the specified
     * key; if no such entry exists (i.e., the greatest key in the Tree is less
     * than the specified key), returns <tt>null</tt>.
     */
    private Node<K,V> getCeilNode(Object lookup, int type) {
        Node<K,V> p = TransactionalBidiTreeMap.this.rootNode[type];
        Object compareval;
        if (p==null)
            return null;
        while (true) {
            compareval = type == KEY ? p.getKey() : p.getValue();
            int cmp =  TransactionalBidiTreeMap.this.compare(Node.NO_CHANGE, lookup, p.getStatus(), compareval, type);
            if (cmp == 0) {
                return p;
            } else if (cmp < 0) {
                if (p.getLeft(type) != null)
                    p = p.getLeft(type);
                else
                    return p;
            } else {
                if (p.getRight(type) != null) {
                    p = p.getRight(type);
                } else {
                    Node<K,V> parent = p.getParent(type);
                    Node<K,V> ch = p;
                    while (parent != null && ch == parent.getRight(type)) {
                        ch = parent;
                        parent = parent.getParent(type);
                    }
                    return parent;
                }
            }
        }
    }

     /**
     * Returns the entry for the greatest key less or equal to the specified key; if
     * no such entry exists (i.e., the least key in the Tree is greater than
     * the specified key), returns <tt>null</tt>.
     */
    private Node<K,V> getFloorNode(Object lookup, int type) {

        Node<K,V> p = TransactionalBidiTreeMap.this.rootNode[type];
        Object nodeCompareVal;
        if (p==null)
            return null;
        if (lookup==null)
            return null;

        while (true) {
            nodeCompareVal = ((type == KEY) ? p.getKey() : p.getValue());
            int cmp = TransactionalBidiTreeMap.this.compare(Node.NO_CHANGE, lookup, p.getStatus(), nodeCompareVal, type);
            if (cmp > 0) {
                if (p.getRight(type) != null)
                    p = p.getRight(type);
                else
                    return p;
            } else if(cmp == 0){
                return p;
            } else { // implicit cmp < 0
                if (p.getLeft(type) != null) {
                    p = p.getLeft(type);
                } else {
                    Node<K,V> parent = p.getParent(type);
                    Node<K,V> ch = p;
                    while (parent != null && ch == parent.getLeft(type)) {
                        ch = parent;
                        parent = parent.getParent(type);
                    }
                    return parent;
                }
            }
        }
    }


    private class SubMapRestriction implements java.io.Serializable {

        public final Object fromKey, toKey, fromValue, toValue;
        public final Filter[] filters = new Filter[] {null, null};

        SubMapRestriction(K fromKey, K toKey, V fromValue, V toValue,
        		Filter<? super K> filterKey, Filter<? super V> filterValue){
            this.fromKey = fromKey;
            this.toKey = toKey;
            this.fromValue = fromValue;
            this.toValue = toValue;
			this.filters[KEY] = filterKey;
			this.filters[VALUE] = filterValue;
        }

        /*
         * Test obeys SubMap description in javadoc
         * 
         * start >= valid range > end
         * nulls are false
         *
         */
        public boolean inRange(Object obj, int type) {
            boolean r;
            if (obj == null)
                return false;
            if (type == KEY)
                r = (fromKey == null || compare(Node.NO_CHANGE, obj, Node.NO_CHANGE, fromKey, KEY) >= 0) &&
                       (toKey == null || compare(Node.NO_CHANGE, obj, Node.NO_CHANGE, toKey, KEY)   <  0) &&
            		   (filters[KEY] == null ? true : filters[KEY].isValid(obj));
            else
                r = (fromValue == null || compare(Node.NO_CHANGE, obj, Node.NO_CHANGE, fromValue, VALUE) >= 0) &&
                       (toValue == null || compare(Node.NO_CHANGE, obj, Node.NO_CHANGE, toValue, VALUE) < 0) &&
            		   (filters[VALUE] == null ? true : filters[VALUE].isValid(obj));
            return r;
        }
        
        /*
         * Special test for checking new range restrictions
         *
         * start >= valid range >= end
         * nulls are true
         *
         */
        public boolean InRangeInclusive(Object obj, int type){
            boolean r;
            if (obj == null)
                return true;
            if (type == KEY)
                r =  (fromKey == null || compare(Node.NO_CHANGE, obj, Node.NO_CHANGE, fromKey, KEY) >= 0) &&
                       (toKey == null || compare(Node.NO_CHANGE, obj, Node.NO_CHANGE, toKey, KEY)   <=  0) &&
            		   (filters[KEY] == null ? true : filters[KEY].isValid(obj));
            else
                r =  (fromValue == null || compare(Node.NO_CHANGE, obj, Node.NO_CHANGE, fromValue, VALUE) >= 0) &&
                       (toValue == null || compare(Node.NO_CHANGE, obj, Node.NO_CHANGE, toValue, VALUE) <= 0) &&
            		   (filters[VALUE] == null ? true : filters[VALUE].isValid(obj));
            return r;
        }
        
        /*
         * Check if a key is in range and the equivalent value is in range
         * Because it needs to check the equivalent value, the key must also
         * exist in the underlying representation
         *
         * Essentially this method and its counterpart inRangeValueAndKey return the
         * same result for a part of any given tuple and differ only in the intial value
         * searched and optimization from the order of the checks
         */
         public boolean inRangeKeyAndValue(Object key) {
             Object value;
             if (!inRange(key, KEY))
                    return false;
             value = TransactionalBidiTreeMap.this.get(key);
             if (value == null)
                    return false;
             
             return inRange(value, VALUE);
        }
         
        /*
         * Check if a value is in range and the equivalent key is in range
         * Because it needs to check the equivalent key, the value must also
         * exist in the underlying representation
         *
         * Essentially this method and its counterpart inRangeValueAndKey return the
         * same result for a part of any given tuple and differ only in the intial value
         * searched and optimization from the order of the checks
         */
        public boolean inRangeValueAndKey(Object value) {
             Object key;
             if (!inRange(value, VALUE))
                    return false;
             key = TransactionalBidiTreeMap.this.getKeyForValue(value);
             if (key == null)
                    return false;
             
             return inRange(key, KEY);        
        }
        /*
         * Check if a key or value is in range, checktype indicates if the incoming value is a KEY or VALUE
         * and so determines the form of check that is made. No check is made on the counterpart value
         * so this test is suitable for occasions where the comparison object is not also a member of the
         * underlying data
         */
        public boolean inRangeSingle(Object obj, int checktype) {
            return inRange(obj, checktype);
        }
        
        public boolean inRangeDouble(Object obj, int checktype) {
            return (checktype == KEY) ? inRangeKeyAndValue(obj) : inRangeValueAndKey(obj);
        }
        /*
         * Check both parts of a key value pair are in range
         */
        public boolean inRangeTuple(Object key, Object value){
            return inRange(key, KEY) && inRange(value, VALUE);
        }
        public Object MaxWithNull(Object obj1, Object obj2, int type){
            if(obj1 == null) return obj2;
            if(obj2 == null) return obj1;
            return (compare(Node.NO_CHANGE, obj1, Node.NO_CHANGE, obj2, type) > 0) ? obj1 : obj2;
        }
        public Object MinWithNull(Object obj1, Object obj2, int type){
            if(obj1 == null) return obj2;
            if(obj2 == null) return obj1;
            return (compare(Node.NO_CHANGE, obj1, Node.NO_CHANGE, obj2, type) < 0) ? obj1 : obj2;
        }
        public Filter mergedFilter(final Filter filter1, final Filter filter2)
        {
			if(filter1 == null) return filter2;
			if(filter2 == null) return filter1;
        	return new Filter() {
        		public boolean isValid(Object obj)
        		{
        			return filter1.isValid(obj) && filter2.isValid(obj);
        		}
        	};
        }
        public SubMapRestriction Conjunction(Object fromKey, Object toKey, Object fromValue, Object toValue,
											 Filter filterKey, Filter filterValue){
            return new SubMapRestriction(
                (K)MaxWithNull(this.fromKey, fromKey, KEY),
                (K)MinWithNull(this.toKey, toKey, KEY),
                (V)MaxWithNull(this.fromValue, fromValue, VALUE),
                (V)MinWithNull(this.toValue, toValue, VALUE),
            	(Filter<K>)mergedFilter(this.filters[KEY], filterKey),
				(Filter<V>)mergedFilter(this.filters[VALUE], filterValue));
        }
    }
    
    /*
     * SubMap provides for a subset of the TreeMap with filters (by range) 
     * set on keys and/or the values.
     *
     * This dual filter raises the following problem. Take a mapping as follows
     * with filters allowing Keys from 2 to the end of the range and Values 
     * from B to the end.
     *
     *<pre>
     * Key Map   Value Map  Directly Filtered  Filtered by partner
     * 1 -> C    A -> 5     /                  X
     * 2 -> B    B -> 3     /                  X
     * 3 -> E    C -> 1     X                  /
     * 4 -> A    D -> 5     X                  /
     * 5 -> D    E -> 3     X                  X
     *</pre>
     * 
     * this implies that the only valid value in the filtered SubMap is (5,D).
     *
     * Finding this is comparatively expensive as it is necessary to
     * iterate through the required ordered subset checking each
     * key against the range allowed in the paired value filter
     * 
     * Where there is a filter set on value but you access by key a similar
     * problem occurs however there are still shortcuts that can be taken 
     * where the SubMap has only a single filter
     */
    private class SubMap extends AbstractMap<K,V>
                             implements SortedFilterableBidiMap<K,V>, java.io.Serializable {

        private static final long serialVersionUID = -7288136081567652280L;
        
        private SubMapRestriction restriction;

        SubMap(SubMapRestriction restriction){
            this.restriction = restriction;
        }

        @Override
        public boolean isEmpty() {
            return this.entrySet().isEmpty();
        }

        @Override
        public boolean containsKey(Object key) {
            return (restriction.inRangeKeyAndValue(key));
        }

        @Override
        public boolean containsValue(Object value) {
            return (restriction.inRangeValueAndKey(value));
        }

        private Node<K,V> nextInRange(Node<K,V> node, int index, final String thread_id) {
            while (node != null && (!restriction.inRangeSingle(node.getKey(), KEY) ||
                    !validNode(node, thread_id) ||
                    !restriction.inRangeSingle(node.getValue(), VALUE)))
                node = mostValidNode(nextGreater(node, index), index, thread_id);
            return node;
        }
        private Node<K,V> previousInRange(Node<K,V> node, int index, final String thread_id) {
            
            while (node != null && (!restriction.inRangeSingle(node.getKey(), KEY) ||
                    !validNode(node, thread_id) ||
                    !restriction.inRangeSingle(node.getValue(), VALUE)))
                node = leastValidNode(nextSmaller(node, index), index, thread_id);
            return node;
        }

        @Override
        public V get(Object key) {
            V value;
            if (!restriction.inRangeSingle(key, KEY))
                return null;
            value = TransactionalBidiTreeMap.this.get(key);
            return (restriction.inRangeSingle(value, VALUE) ? value : null);
        }

        public K getKeyForValue(Object value) {
            if (!restriction.inRangeSingle(value, VALUE))
                return null;
            return TransactionalBidiTreeMap.this.getKeyForValue(value);
        }
        
        @Override
        public V put(K key, V value) {
            if (!restriction.inRangeTuple(key, value))
                throw new IllegalArgumentException("key or out of range");
            return TransactionalBidiTreeMap.this.put(key, value);
        }

        public K removeValue(Object value) {
            if (!restriction.inRangeValueAndKey(value))
                throw new IllegalArgumentException("value out of range");
            return TransactionalBidiTreeMap.this.removeValue(value);
        }

        public Comparator<K> comparator() {
            return comparators[KEY];
        }

        public Comparator<V> valueComparator() {
            return comparators[VALUE];
        }
        
        private Node<K,V> firstNodeByKey(){
            Node<K,V> first = 
                restriction.fromKey == null
                ? TransactionalBidiTreeMap.this.leastNode(rootNode[KEY], KEY)
                : getCeilNode(restriction.fromKey, KEY);
            first = nextInRange(first, KEY, getCurrentThreadId());
            if (restriction.toKey != null && compare(Node.NO_CHANGE, first.getKey(), Node.NO_CHANGE, restriction.toKey, KEY) >= 0)
                throw(new NoSuchElementException());
            return first;
        }

        /*
         * Returns the first key when the collection is ordered by key
         */
        public K firstKey() {
            return firstNodeByKey().getKey();
        }

         /*
         * Returns the first value when the collection is ordered by key
         */
         public V firstValue() {
            return firstNodeByKey().getValue();
        }


         private Node<K,V> firstNodeByValue(){
            Node<K,V> first = 
                restriction.fromValue == null
                ? TransactionalBidiTreeMap.this.leastNode(rootNode[VALUE], VALUE)
                : getCeilNode(restriction.fromValue, VALUE);
            first = nextInRange(first, VALUE, getCurrentThreadId());
            if (restriction.toValue != null && compare(Node.NO_CHANGE, first.getValue(), Node.NO_CHANGE, restriction.toValue, VALUE) >= 0)
                throw(new NoSuchElementException());
            return first;
        }

         /*
         * Returns the first key when the collection is ordered by value
         */
        public K firstKeyByValue() {
            return firstNodeByValue().getKey();
        }
        
        /*
         * Returns the first value when the collection is ordered by value
         */
        public V firstValueByValue() {
            return firstNodeByValue().getValue();
        }
        
         private Node<K,V> lastNodeByKey(){
            Node<K,V> last;
             if (restriction.toKey == null){
                last = TransactionalBidiTreeMap.this.mostNode(rootNode[KEY], KEY);
            } else {
                last = getFloorNode(restriction.toKey, KEY);
            }
            last = previousInRange(last, KEY, getCurrentThreadId());
            if (restriction.toKey != null && compare(Node.NO_CHANGE, last.getKey(), Node.NO_CHANGE, restriction.toKey, KEY) > 0)
                throw(new NoSuchElementException());
            return last;
        }
        /*
         * Returns the last key when the collection is ordered by key
         */
        public K lastKey() {
            return lastNodeByKey().getKey();
        }

        /*
         * Returns the last value when the collection is ordered by key
         */
        public V lastValue() {
            return lastNodeByKey().getValue();
        }

         private Node<K,V> lastNodeByValue(){
             Node<K,V> last;
             if(restriction.toValue == null){
                last = TransactionalBidiTreeMap.this.mostNode(rootNode[VALUE], VALUE);
             }
             else{
                last = getFloorNode(restriction.toValue, VALUE);
             }
            last = previousInRange(last, VALUE, getCurrentThreadId());
            if (restriction.toValue != null && compare(Node.NO_CHANGE, last.getValue(), Node.NO_CHANGE, restriction.toValue, VALUE) > 0)
                throw(new NoSuchElementException(last.getValue().toString()));
            return last;
        }

        /*
         * Returns the last key when the collection is ordered by value
         */
        public K lastKeyByValue() {
            return lastNodeByValue().getKey();
        }
        
        /*
         * Returns the last value when the collection is ordered by value
         */
        public V lastValueByValue() {
            return lastNodeByValue().getValue();
        }
        
        private transient FilterableSet entrySet[] = {new EntrySetView(KEY),
                                                      new EntrySetView(VALUE)};

        @Override
        public Set<Entry<K,V>> entrySet() {
            return entrySet[KEY];
        }

        public FilterableSet<Entry<K,V>> entrySetByValue() {
            return entrySet[VALUE];
        }
        
        public FilterableSet<K> keySetByValue() {
	    return new AbstractFilterableSet<K>() {
		@Override
        public Iterator<K> iterator() {
		    return new Iterator<K>() {
			private Iterator<Entry<K,V>> i = entrySetByValue().iterator();

			public boolean hasNext() {
			    return i.hasNext();
			}

			public K next() {
			    return i.next().getKey();
			}

			public void remove() {
			    i.remove();
			}
                    };
		}

		@Override
        public int size() {
		    return TransactionalBidiTreeMap.SubMap.this.size();
		}

		@Override
        public boolean contains(Object k) {
		    return TransactionalBidiTreeMap.SubMap.this.containsKey(k);
		}
	    };
        }

        public FilterableCollection<V> valuesByValue() {
	    return new AbstractFilterableCollection<V>() {
		@Override
        public Iterator<V> iterator() {
		    return new Iterator<V>() {
			private Iterator<Entry<K,V>> i = entrySetByValue().iterator();

			public boolean hasNext() {
			    return i.hasNext();
			}

			public V next() {
			    return i.next().getValue();
			}

			public void remove() {
			    i.remove();
			}
                    };
                }

		@Override
        public int size() {
		    return TransactionalBidiTreeMap.SubMap.this.size();
		}

		@Override
        public boolean contains(Object v) {
		    return TransactionalBidiTreeMap.SubMap.this.containsValue(v);
		}
	    };
        }

        @Override
        public Collection<V> values() {
	    return new AbstractFilterableCollection<V>() {
		@Override
        public Iterator<V> iterator() {
		    return new Iterator<V>() {
			private Iterator<Entry<K,V>> i = entrySet().iterator();

			public boolean hasNext() {
			    return i.hasNext();
			}

			public V next() {
			    return i.next().getValue();
			}

			public void remove() {
			    i.remove();
			}
                    };
                }

		@Override
        public int size() {
		    return TransactionalBidiTreeMap.SubMap.this.size();
		}

		@Override
        public boolean contains(Object v) {
		    return TransactionalBidiTreeMap.SubMap.this.containsValue(v);
		}
	    };
        }

        public SortedMap<K,V> subMap(K fromKey, K toKey) {
            return new SubMap(restriction.Conjunction(fromKey, toKey, null, null, null, null));
        }

        public SortedBidiMap<K,V> subMapByValue(V fromValue, V toValue) {
            return new SubMap(restriction.Conjunction(null, null, fromValue, toValue, null, null));
        }
        
        public SortedMap<K,V> headMap(K toKey) {
            return new SubMap(restriction.Conjunction(null, toKey, null, null, null, null));
        }

        public SortedBidiMap<K,V> headMapByValue(V toValue) {
            return new SubMap(restriction.Conjunction(null, null, null, toValue, null, null));
        }
        
        public SortedMap<K,V> tailMap(K fromKey) {
            return new SubMap(restriction.Conjunction(fromKey, null, null, null, null, null));
        }

        public SortedBidiMap<K,V> tailMapByValue(V fromValue) {
            return new SubMap(restriction.Conjunction(null, null, fromValue, null, null, null));
        }
        
        public FilterableMap<K,V> filteredMap(Filter<? super K> filter) {
			return new SubMap(restriction.Conjunction(null, null, null, null, filter, null));
        }

        public FilterableBidiMap<K,V> filteredMapByValue(Filter<? super V> filter) {
			return new SubMap(restriction.Conjunction(null, null, null, null, null, filter));
        }

        /*
        private boolean inRange(Object obj) {
            if (obj == null)
                return false;
            return (fromStart || compare(obj, from, type) >= 0) &&
                   (toEnd     || compare(obj, to, type)   <  0);
        }
         */


        /*******************************************************
         *
         * EntrySetView inner class start
         *
         *******************************************************/
        private class EntrySetView extends AbstractFilterableSet<Entry<K,V>>
        {       
            private transient int size = -1, sizeModCount, type;

            private EntrySetView(int type) 
            {
                    this.type = type;
            }

            private boolean valEquals(Object obj1, Object obj2){
                return TransactionalBidiTreeMap.this.compare(Node.NO_CHANGE, obj1, Node.NO_CHANGE, obj2, this.type) == 0;
            }
            
            @Override
            public int size() 
            {
                if (size == -1 || sizeModCount != TransactionalBidiTreeMap.this.modifications) {
                    size = 0;  
                    sizeModCount = TransactionalBidiTreeMap.this.modifications;
                    Iterator i = iterator();
                    while (i.hasNext()) {
                        size++;
                        i.next();
                    }
                }
                return size;
            }

            @Override
            public boolean isEmpty() 
            {
                    return !iterator().hasNext();
            }

            @Override
            public boolean contains(Object o) 
            {
                    if (!(o instanceof Map.Entry))
                     return false;
                     Map.Entry<K,V> entry = (Map.Entry<K,V>)o;
                     Object key = entry.getKey();
                     if (!TransactionalBidiTreeMap.SubMap.this.restriction.inRangeKeyAndValue(key))
                     return false;
                     Node<K,V> node = lookupValid(key, this.type, getCurrentThreadId());
                     return node != null && valEquals(entry.getValue(), node.getValue());
            }

            @Override
            public boolean remove(Object o) 
            {
                 if (!(o instanceof Map.Entry))
                    return false;
                 Map.Entry<K,V> entry = (Map.Entry<K,V>)o;
                 Object key = entry.getKey();
                 if (!TransactionalBidiTreeMap.SubMap.this.restriction.inRangeKeyAndValue(key))
                 return false;
                 Node<K,V> node = lookupValid(key, KEY, getCurrentThreadId());
                 if (node!=null && valEquals(node.getValue(),entry.getValue())){
                     TransactionalBidiTreeMap.this.doRedBlackDelete(node);
                     return true;
                 }
                 return false;
            }

            @Override
            public Iterator iterator() 
            {
                return new SubMapEntryIterator(type == KEY ?
                        TransactionalBidiTreeMap.SubMap.this.firstNodeByKey() :
                        TransactionalBidiTreeMap.SubMap.this.firstNodeByValue(),
                        TransactionalBidiTreeMap.SubMap.this.restriction,
                        type) {
                };
            }
        }
    }

    /* **********  END  implementation of Map ********** */
    private abstract class TransactionalBidiTreeMapIterator<E> implements Iterator<E> {

        private int    expectedModifications;
        protected Node<K,V> lastReturnedNode;
        private Node<K,V>   nextNode;
        protected int  iteratorType;

        /**
         * Constructor
         *
         * @param type
         */
        TransactionalBidiTreeMapIterator(final int type) {

            iteratorType          = type;
            expectedModifications = TransactionalBidiTreeMap.this.modifications;
            lastReturnedNode      = null;
            nextNode              = leastNode(rootNode[iteratorType],
                                              iteratorType);
            nextNode = getNextValidNode(nextNode, getCurrentThreadId());
        }

        /**
         * Constructor
         *
         * @param type
         */
        TransactionalBidiTreeMapIterator(final Node<K,V> startNode, final int type) {

            iteratorType          = type;
            expectedModifications = TransactionalBidiTreeMap.this.modifications;
            lastReturnedNode      = null;
            nextNode              = startNode;
            nextNode = getNextValidNode(nextNode, getCurrentThreadId());
        }

        /**
         * @return 'next', whatever that means for a given kind of
         *         TransactionalBidiTreeMapIterator
         */
        protected abstract E doGetNext();

        /* ********** START implementation of Iterator ********** */

        /**
         * @return true if the iterator has more elements.
         */
        public final boolean hasNext() {
            return nextNode != null;
        }

        /**
         * @return the next element in the iteration.
         *
         * @throws NoSuchElementException if iteration has no more
         *                                   elements.
         * @throws ConcurrentModificationException if the
         *                                            TransactionalBidiTreeMap is
         *                                            modified behind
         *                                            the iterator's
         *                                            back
         */
        public final E next()
                throws NoSuchElementException,
                       ConcurrentModificationException {

            if (nextNode == null) {
                throw new NoSuchElementException();
            }

            if (modifications != expectedModifications) {
                throw new ConcurrentModificationException();
            }

            lastReturnedNode = nextNode;
            nextNode = nextGreater(nextNode, iteratorType);
            nextNode = getNextValidNode(nextNode, getCurrentThreadId());

            return doGetNext();
        }
        
        protected Node<K,V> getNextValidNode(Node<K,V> node, String thread_id) {
            if (auto_commit)
                return node;
            
            return mostValidNode(node, iteratorType, thread_id);
        }

        /**
         * Removes from the underlying collection the last element
         * returned by the iterator. This method can be called only
         * once per call to next. The behavior of an iterator is
         * unspecified if the underlying collection is modified while
         * the iteration is in progress in any way other than by
         * calling this method.
         *
         * @throws IllegalStateException if the next method has not
         *                                  yet been called, or the
         *                                  remove method has already
         *                                  been called after the last
         *                                  call to the next method.
         * @throws ConcurrentModificationException if the
         *                                            TransactionalBidiTreeMap is
         *                                            modified behind
         *                                            the iterator's
         *                                            back
         */
        public final void remove()
                throws IllegalStateException,
                       ConcurrentModificationException {

            if (lastReturnedNode == null) {
                throw new IllegalStateException();
            }

            if (modifications != expectedModifications) {
                throw new ConcurrentModificationException();
            }

            if (auto_commit)
            {
                doRedBlackDelete(lastReturnedNode);
                expectedModifications++;
            }
            else
                lastReturnedNode.setStatus(Node.DELETED, getCurrentThreadId());

            lastReturnedNode = null;
        }

        /* **********  END  implementation of Iterator ********** */
    }    // end private abstract class TransactionalBidiTreeMapIterator


    private class SubMapEntryIterator extends TransactionalBidiTreeMapIterator<Entry<K,V>> {

        private TransactionalBidiTreeMap<K,V>.SubMapRestriction restriction;

        SubMapEntryIterator(Node<K,V> first, TransactionalBidiTreeMap<K,V>.SubMapRestriction restriction, int type) {
            super(first, type);
            this.restriction = restriction;
        }

        @Override
        protected Node<K,V> getNextValidNode(Node<K,V> node, final String thread_id) {
            if (restriction == null)
                return super.getNextValidNode(node, thread_id);
            while (node != null && (!validNode(node, thread_id) ||
                    !restriction.inRangeSingle(node.getKey(), KEY) ||
                    !restriction.inRangeSingle(node.getValue(), VALUE)))
            {
                node = nextGreater(node, iteratorType);
                node = super.getNextValidNode(node, thread_id);
            }
            return node;
        }

        @Override
        protected Entry<K,V> doGetNext() {
            return lastReturnedNode;
        }
    }

    // final for performance
    protected static final class Node<K,V> implements Map.Entry<K,V>, java.io.Serializable {

        private static final long serialVersionUID = -5178097310251692266L;
        
        private K       dataKey;
        private Node<K,V>    leftNodeKey;
        private Node<K,V>    rightNodeKey;
        private Node<K,V>    parentNodeKey;
        private boolean      blackColorKey;
        private V            dataValue;
        private Node<K,V>    leftNodeValue;
        private Node<K,V>    rightNodeValue;
        private Node<K,V>    parentNodeValue;
        private boolean      blackColorValue;
        private int          hashcodeValue;
        private boolean      calculatedHashCode;
        private int          transactionStatus;
        private String       transactionId;
        
        public static final int NO_CHANGE = 0;
        public static final int DELETED = 1;
        public static final int ADDED = 2;

        /**
         * Make a new cell with given key and value, and with null
         * links, and black (true) colors.
         *
         * @param key
         * @param value
         */
        Node(final K key, final V value) {
            dataKey = key;
            dataValue = value;
            blackColorKey      = true;
            blackColorValue    = true;
            calculatedHashCode = false;
            transactionStatus   = NO_CHANGE;
            transactionId      = null;
        }

        private void setStatus(final int status, final String id) {
            transactionStatus = status;
            transactionId = id;
        }
        
        public int getStatus() {
            return transactionStatus;
        }

        public String getTransactionId() {
            return transactionId;
        }

        private boolean is(final int status, final String id) {
            if (transactionId == null)
                return status == NO_CHANGE;
            
            if (id == null || transactionId.equals(id))
                return transactionStatus == status;

            return status == NO_CHANGE;
        }

        /**
         * get the specified data
         *
         * @param index KEY or VALUE
         *
         * @return the key or value
         */
        private Object getData(final int index) {
            return index == KEY ? dataKey : dataValue;
        }

        /**
         * Set this node's left node
         *
         * @param node the new left node           if(!toEnd)
                return TransactionalBidiTreeMap.this.getKeyForValue(getFloorEntry(this.to));

            return (type == KEY) ? 
                TransactionalBidiTreeMap.this.get(TransactionalBidiTreeMap.this.lastKey()) :
                TransactionalBidiTreeMap.this.firstValue();
         * @param index KEY or VALUE
         */
        private void setLeft(final Node<K,V> node, final int index) {
            if (index == KEY)
                leftNodeKey = node;
            else
                leftNodeValue = node;
        }

        /**
         * get the left node
         *
         * @param index KEY or VALUE
         *
         * @return the left node -- may be null
         */
        private Node<K,V> getLeft(final int index) {
            return index == KEY ? leftNodeKey : leftNodeValue;
        }

        /**
         * Set this node's right node
         *
         * @param node the new right node
         * @param index KEY or VALUE
         */
        private void setRight(final Node<K,V> node, final int index) {
            if (index == KEY)
                rightNodeKey = node;
            else
                rightNodeValue = node;
        }

        /**
         * get the right node
         *
         * @param index KEY or VALUE
         *
         * @return the right node -- may be null
         */
        private Node<K,V> getRight(final int index) {
            return index == KEY ? rightNodeKey : rightNodeValue;
        }

        /**
         * Set this node's parent node
         *
         * @param node the new parent node
         * @param index KEY or VALUE
         */
        private void setParent(final Node<K,V> node, final int index) {
            if (index == KEY)
                parentNodeKey = node;
            else
                parentNodeValue = node;
        }

        /**
         * get the parent node
         *
         * @param index KEY or VALUE
         *
         * @return the parent node -- may be null
         */
        private Node<K,V> getParent(final int index) {
            return index == KEY ? parentNodeKey : parentNodeValue;
        }

        /**
         * exchange colors with another node
         *
         * @param node the node to swap with
         * @param index KEY or VALUE
         */
        private void swapColors(final Node<K,V> node, final int index) {

            // Swap colors -- old hacker's trick
            if (index == KEY) {
                blackColorKey      ^= node.blackColorKey;
                node.blackColorKey ^= blackColorKey;
                blackColorKey      ^= node.blackColorKey;
            }
            else {
                blackColorValue      ^= node.blackColorValue;
                node.blackColorValue ^= blackColorValue;
                blackColorValue      ^= node.blackColorValue;
            }
        }

        /**
         * is this node black?
         *
         * @param index KEY or VALUE
         *
         * @return true if black (which is represented as a true boolean)
         */
        private boolean isBlack(final int index) {
            return index == KEY ? blackColorKey : blackColorValue;
        }

        /**
         * is this node red?
         *
         * @param index KEY or VALUE
         *
         * @return true if non-black
         */
        private boolean isRed(final int index) {
            return index == KEY ? !blackColorKey : !blackColorValue;
        }

        /**
         * make this node black
         *
         * @param index KEY or VALUE
         */
        private void setBlack(final int index) {
            if (index == KEY)
                blackColorKey = true;
            else
                blackColorValue = true;
        }

        /**
         * make this node red
         *
         * @param index KEY or VALUE
         */
        private void setRed(final int index) {
            if (index == KEY)
                blackColorKey = false;
            else
                blackColorValue = false;
        }

        /**
         * make this node the same color as another
         *
         * @param node the node whose color we're adopting
         * @param index KEY or VALUE
         */
        private void copyColor(final Node<K,V> node, final int index) {
            if (index == KEY)
                blackColorKey = node.blackColorKey;
            else
                blackColorValue = node.blackColorValue;
        }

        /* ********** START implementation of Map.Entry ********** */

        /**
         * @return the key corresponding to this entry.
         */
        public K getKey() {
            return dataKey;
        }

        /**
         * @return the value corresponding to this entry.
         */
        public V getValue() {
            return dataValue;
        }

        /**
         * Optional operation that is not permitted in this
         * implementation
         *
         * @param ignored
         *
         * @return does not return
         *
         * @throws UnsupportedOperationException
         */
        public V setValue(Object ignored)
                throws UnsupportedOperationException {
            throw new UnsupportedOperationException(
                "Map.Entry.setValue is not supported");
        }

        /**
         * Compares the specified object with this entry for equality.
         * Returns true if the given object is also a map entry and
         * the two entries represent the same mapping.
         *
         * @param o object to be compared for equality with this map
         *          entry.
         * @return true if the specified object is equal to this map
         *         entry.
         */
        @Override
        public boolean equals(Object o) {

            if (this == o) {
                return true;
            }

            if (!(o instanceof Map.Entry)) {
                return false;
            }

            Map.Entry e = (Map.Entry) o;

            return dataKey.equals(e.getKey())
                   && dataValue.equals(e.getValue());
        }

        /**
         * @return the hash code value for this map entry.
         */
        @Override
        public int hashCode() {

            if (!calculatedHashCode) {
                hashcodeValue      = dataKey.hashCode()
                                     ^ dataValue.hashCode();
                calculatedHashCode = true;
            }

            return hashcodeValue;
        }

        /* **********  END  implementation of Map.Entry ********** */
    }
}    // end public class TransactionalBidiTreeMap
