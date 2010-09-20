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
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import com.workplacesystems.queuj.utils.ThreadSession;

/**
 * Transactional version of HashMap
 *
 * @author andy bell
 */

public class TransactionalHashMap<K,V> extends AbstractMap<K,V> implements TransactionalMap<K,V>
{
    /**
     * Object to get hash from.
     */
    private final Object hash_object = new Object();

    /**
     * The default initial capacity - MUST be a power of two.
     */
    static final int DEFAULT_INITIAL_CAPACITY = 16;
    
    /**
     * The maximum capacity, used if a higher value is implicitly specified
     * by either of the constructors with arguments.
     * MUST be a power of two <= 1<<30.
     */
    static final int MAXIMUM_CAPACITY = 1 << 30;
    
    /**
     * The load factor used when none specified in constructor.
     **/
    static final float DEFAULT_LOAD_FACTOR = 0.75f;
    
    /**
     * The table, resized as necessary. Length MUST Always be a power of two.
     */
    transient Entry<K,V>[] table;
    
    /**
     * The number of key-value mappings contained in this identity hash map.
     */
    transient int size;
    
    /**
     * The next size value at which to resize (capacity * load factor).
     * @serial
     */
    int threshold;
    
    /**
     * The load factor for the hash table.
     *
     * @serial
     */
    final float loadFactor;
    
    /**
     * The number of times this HashMap has been structurally modified
     * Structural modifications are those that change the number of mappings in
     * the HashMap or otherwise modify its internal structure (e.g.,
     * rehash).  This field is used to make iterators on Collection-views of
     * the HashMap fail-fast.  (See ConcurrentModificationException).
     */
    transient volatile int modCount;
    
    /**
     * The transactional mode the map is running in
     */
    private boolean auto_commit = true;
    
    
    /**
     * Constructs an empty <tt>HashMap</tt> with the specified initial
     * capacity and load factor.
     *
     * @param  initialCapacity The initial capacity.
     * @param  loadFactor      The load factor.
     * @throws IllegalArgumentException if the initial capacity is negative
     *         or the load factor is nonpositive.
     */
    @SuppressWarnings("unchecked")
    public TransactionalHashMap(int initialCapacity, float loadFactor)
    {
        if (initialCapacity < 0)
            throw new IllegalArgumentException("Illegal initial capacity: " +
                    initialCapacity);
        if (initialCapacity > MAXIMUM_CAPACITY)
            initialCapacity = MAXIMUM_CAPACITY;
        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            throw new IllegalArgumentException("Illegal load factor: " +
                    loadFactor);
        
        // Find a power of 2 >= initialCapacity
        int capacity = 1;
        while (capacity < initialCapacity)
            capacity <<= 1;
        
        this.loadFactor = loadFactor;
        threshold = (int)(capacity * loadFactor);
        table = new Entry[capacity];
        init();
    }
    
    /**
     * Constructs an empty <tt>HashMap</tt> with the specified initial
     * capacity and the default load factor (0.75).
     *
     * @param  initialCapacity the initial capacity.
     * @throws IllegalArgumentException if the initial capacity is negative.
     */
    public TransactionalHashMap(int initialCapacity)
    {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }
    
    /**
     * Constructs an empty <tt>HashMap</tt> with the default initial capacity
     * (16) and the default load factor (0.75).
     */
    @SuppressWarnings("unchecked")
    public TransactionalHashMap()
    {
        this.loadFactor = DEFAULT_LOAD_FACTOR;
        threshold = (int)(DEFAULT_INITIAL_CAPACITY * DEFAULT_LOAD_FACTOR);
        table = new Entry[DEFAULT_INITIAL_CAPACITY];
        init();
    }
    
    /**
     * Constructs a new <tt>HashMap</tt> with the same mappings as the
     * specified <tt>Map</tt>.  The <tt>HashMap</tt> is created with
     * default load factor (0.75) and an initial capacity sufficient to
     * hold the mappings in the specified <tt>Map</tt>.
     *
     * @param   m the map whose mappings are to be placed in this map.
     * @throws  NullPointerException if the specified map is null.
     */
    public TransactionalHashMap(Map<? extends K,? extends V> m)
    {
        this(Math.max((int) (m.size() / DEFAULT_LOAD_FACTOR) + 1,
                DEFAULT_INITIAL_CAPACITY), DEFAULT_LOAD_FACTOR);
        putAllForCreate(m);
    }
    
    // internal utilities
    
    /**
     * Initialization hook for subclasses. This method is called
     * in all constructors and pseudo-constructors (clone, readObject)
     * after HashMap has been initialized but before any entries have
     * been inserted.  (In the absence of this method, readObject would
     * require explicit knowledge of subclasses.)
     */
    void init()
    {
    }
    
    private final String getThreadSessionKey()
    {
        return ".TransactionalBidiTreeMap(" + hash_object.hashCode() + ")-attach_id";
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
    public void setAutoCommit(final boolean auto_commit)
    {
        
        if (!this.auto_commit && auto_commit)
            commit(null);
        
        this.auto_commit = auto_commit;
    }
    
    public boolean isAutoCommit()
    {
        return auto_commit;
    }
    
    /**
     * Commits the changes to the map so that all threads
     * see them.
     */
    public void commit()
    {
        
        if (auto_commit)
            return;
        
        commit(getCurrentThreadId());
    }
    
    /**
     * Rolls back the changes to the map.
     */
    public void rollback()
    {
        
        if (auto_commit)
            return;
        
        String id = getCurrentThreadId();
        
        Entry<K,V> tab[] = table;
        for (int i = 0; i < tab.length ; i++)
        {
            Entry<K,V> prev = table[i];
            Entry<K,V> e = prev;
            
            while (e != null)
            {
                Entry<K,V> next = e.next;
                if (e.is(Entry.ADDED, id))
                {
                    modCount++;
                    size--;
                    if (prev == e)
                        table[i] = next;
                    else
                        prev.next = next;
                }
                else if (e.is(Entry.DELETED, id))
                    e.setStatus(Entry.NO_CHANGE, null);
                
                prev = e;
                e = next;
            }
        }
    }
    
    /**
     * Commits the changes to the map so that all threads
     * see them.
     *
     * @param id the thread id to commit for, null commits
     *           all threads changes
     */
    private void commit(final String id)
    {
        
        Entry<K,V> tab[] = table;
        for (int i = 0; i < tab.length ; i++)
        {
            Entry<K,V> prev = table[i];
            Entry<K,V> e = prev;
            
            while (e != null)
            {
                Entry<K,V> next = e.next;
                if (e.is(Entry.DELETED, id))
                {
                    modCount++;
                    size--;
                    if (prev == e)
                        table[i] = next;
                    else
                        prev.next = next;
                }
                else if (e.is(Entry.ADDED, id))
                    e.setStatus(Entry.NO_CHANGE, null);
                
                prev = e;
                e = next;
            }
        }
    }
    
    /**
     * Retrieve the current thread id for use by the
     * transaction code.
     *
     * @return the thread id of the current thread
     */
    private String getCurrentThreadId()
    {
        String attach_id = (String)ThreadSession.getValue(getThreadSessionKey());
        if (attach_id != null)
            return attach_id;

        Thread thread = Thread.currentThread();
        return thread.toString() + "(" + thread.hashCode() + ")";
    }
    
    /**
     * Checks that this entry is valid for the current thread
     *
     * @param entry the entry to be checked
     *
     * @return true if entry is valid, otherwise false
     */
    private boolean validEntry(final Entry<K,V> entry)
    {
        if (auto_commit || entry == null)
            return (entry != null);
        
        String id = getCurrentThreadId();
        return !((entry.is(Entry.DELETED, id)) ||
                (entry.is(Entry.ADDED, null) && entry.is(Entry.NO_CHANGE, id)));
    }
    
    /**
     * Value representing null keys inside tables.
     */
    static final Object NULL_KEY = new Object();
    
    /**
     * Returns internal representation for key. Use NULL_KEY if key is null.
     */
    @SuppressWarnings("unchecked")
    static <T> T maskNull(T key)
    {
        return (key == null ? (T)NULL_KEY : key);
    }
    
    /**
     * Returns key represented by specified internal representation.
     */
    static <T> T  unmaskNull(T key)
    {
        return (key == NULL_KEY ? null : key);
    }
    
    /**
     * Returns a hash value for the specified object.  In addition to
     * the object's own hashCode, this method applies a "supplemental
     * hash function," which defends against poor quality hash functions.
     * This is critical because HashMap uses power-of two length
     * hash tables.<p>
     *
     * The shift distances in this function were chosen as the result
     * of an automated search over the entire four-dimensional search space.
     */
    static int hash(Object x)
    {
        int h = x.hashCode();
        
        h += ~(h << 9);
        h ^=  (h >>> 14);
        h +=  (h << 4);
        h ^=  (h >>> 10);
        return h;
    }
    
    /**
     * Check for equality of non-null reference x and possibly-null y.
     */
    static boolean eq(Object x, Object y)
    {
        return x == y || x.equals(y);
    }
    
    /**
     * Returns index for hash code h.
     */
    static int indexFor(int h, int length)
    {
        return h & (length-1);
    }
    
    /**
     * Returns the number of key-value mappings in this map.
     *
     * @return the number of key-value mappings in this map.
     */
    @Override
    public int size()
    {
        return size(false);
    }
    
    public int size(boolean countAll)
    {
        if (countAll || auto_commit)
            return size;
        
        int size = 0;
        for (Iterator<Map.Entry<K,V>> i = entrySet().iterator(); i.hasNext(); i.next())
        {
            size++;
        }
        return size;
    }
    
    /**
     * Returns the value to which the specified key is mapped in this identity
     * hash map, or <tt>null</tt> if the map contains no mapping for this key.
     * A return value of <tt>null</tt> does not <i>necessarily</i> indicate
     * that the map contains no mapping for the key; it is also possible that
     * the map explicitly maps the key to <tt>null</tt>. The
     * <tt>containsKey</tt> method may be used to distinguish these two cases.
     *
     * @param   key the key whose associated value is to be returned.
     * @return  the value to which this map maps the specified key, or
     *          <tt>null</tt> if the map contains no mapping for this key.
     * @see #put(Object, Object)
     */
    @Override
    public V get(Object key)
    {
        Object k = maskNull(key);
        int hash = hash(k);
        int i = indexFor(hash, table.length);
        Entry<K,V> e = table[i];
        while (true)
        {
            if (e == null)
                return null;
            if (e.hash == hash && validEntry(e) && eq(k, e.key))
                return e.value;
            e = e.next;
        }
    }
    
    /**
     * Returns <tt>true</tt> if this map contains a mapping for the
     * specified key.
     *
     * @param   key   The key whose presence in this map is to be tested
     * @return <tt>true</tt> if this map contains a mapping for the specified
     * key.
     */
    @Override
    public boolean containsKey(Object key)
    {
        Object k = maskNull(key);
        int hash = hash(k);
        int i = indexFor(hash, table.length);
        Entry<K,V> e = table[i];
        while (e != null)
        {
            if (e.hash == hash && validEntry(e) && eq(k, e.key))
                return true;
            e = e.next;
        }
        return false;
    }
    
    /**
     * Returns the entry associated with the specified key in the
     * HashMap.  Returns null if the HashMap contains no mapping
     * for this key.
     */
    Entry<K,V> getEntry(Object key)
    {
        Object k = maskNull(key);
        int hash = hash(k);
        int i = indexFor(hash, table.length);
        Entry<K,V> e = table[i];
        while (e != null && !(e.hash == hash && validEntry(e) && eq(k, e.key)))
            e = e.next;
        return e;
    }
    
    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for this key, the old
     * value is replaced.
     *
     * @param key key with which the specified value is to be associated.
     * @param value value to be associated with the specified key.
     * @return previous value associated with specified key, or <tt>null</tt>
     *	       if there was no mapping for key.  A <tt>null</tt> return can
     *	       also indicate that the HashMap previously associated
     *	       <tt>null</tt> with the specified key.
     */
    @Override
    public V put(K key, V value) throws ConcurrentModificationException
    {
        K k = maskNull(key);
        int hash = hash(k);
        int i = indexFor(hash, table.length);
        
        V oldValue = null;
        for (Entry<K,V> e = table[i]; e != null; e = e.next)
        {
            if (e.hash == hash && eq(k, e.key))
            {
                //check if someone else has a pending add for the same key
                if (e.is(Entry.ADDED, null) && !e.is(Entry.ADDED, getCurrentThreadId()))
                    throw new ConcurrentModificationException();
                
                if (validEntry(e))
                {
                    oldValue = e.value;
                    //if not transactional can reuse entries
                    if (auto_commit)
                    {
                        e.value = value;
                        return oldValue;
                    }
                    else
                        e.setStatus(Entry.DELETED, getCurrentThreadId());
                }
            }
        }
        
        modCount++;
        addEntry(hash, k, value, i);
        return oldValue;
    }
    
    /**
     * This method is used instead of put by constructors and
     * pseudoconstructors (clone, readObject).  It does not resize the table,
     * check for comodification, etc.  It calls createEntry rather than
     * addEntry.
     */
    private void putForCreate(K key, V value)
    {
        K k = maskNull(key);
        int hash = hash(k);
        int i = indexFor(hash, table.length);
        
        /**
         * Look for preexisting entry for key.  This will never happen for
         * clone or deserialize.  It will only happen for construction if the
         * input Map is a sorted map whose ordering is inconsistent w/ equals.
         */
        for (Entry<K,V> e = table[i]; e != null; e = e.next)
        {
            if (e.hash == hash && eq(k, e.key))
            {
                e.value = value;
                return;
            }
        }
        
        createEntry(hash, k, value, i);
    }
    
    void putAllForCreate(Map<? extends K, ? extends V> m)
    {
        for (Iterator<? extends Map.Entry<? extends K, ? extends V>> i = m.entrySet().iterator(); i.hasNext(); )
        {
            Map.Entry<? extends K, ? extends V> e = i.next();
            putForCreate(e.getKey(), e.getValue());
        }
    }
    
    /**
     * Rehashes the contents of this map into a new array with a
     * larger capacity.  This method is called automatically when the
     * number of keys in this map reaches its threshold.
     *
     * If current capacity is MAXIMUM_CAPACITY, this method does not
     * resize the map, but but sets threshold to Integer.MAX_VALUE.
     * This has the effect of preventing future calls.
     *
     * @param newCapacity the new capacity, MUST be a power of two;
     *        must be greater than current capacity unless current
     *        capacity is MAXIMUM_CAPACITY (in which case value
     *        is irrelevant).
     */
    @SuppressWarnings("unchecked")
    void resize(int newCapacity)
    {
        Entry<K,V>[] oldTable = table;
        int oldCapacity = oldTable.length;
        if (oldCapacity == MAXIMUM_CAPACITY)
        {
            threshold = Integer.MAX_VALUE;
            return;
        }
        
        Entry<K,V>[] newTable = new Entry[newCapacity];
        transfer(newTable);
        table = newTable;
        threshold = (int)(newCapacity * loadFactor);
    }
    
    /**
     * Transfer all entries from current table to newTable.
     */
    void transfer(Entry<K,V>[] newTable)
    {
        Entry<K,V>[] src = table;
        int newCapacity = newTable.length;
        for (int j = 0; j < src.length; j++)
        {
            Entry<K,V> e = src[j];
            if (e != null)
            {
                src[j] = null;
                do
                {
                    Entry<K,V> next = e.next;
                    int i = indexFor(e.hash, newCapacity);
                    e.next = newTable[i];
                    newTable[i] = e;
                    e = next;
                } while (e != null);
            }
        }
    }
    
    /**
     * Copies all of the mappings from the specified map to this map
     * These mappings will replace any mappings that
     * this map had for any of the keys currently in the specified map.
     *
     * @param m mappings to be stored in this map.
     * @throws NullPointerException if the specified map is null.
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> m)
    {
        int numKeysToBeAdded = m.size();
        if (numKeysToBeAdded == 0)
            return;
        
        /*
         * Expand the map if the map if the number of mappings to be added
         * is greater than or equal to threshold.  This is conservative; the
         * obvious condition is (m.size() + size) >= threshold, but this
         * condition could result in a map with twice the appropriate capacity,
         * if the keys to be added overlap with the keys already in this map.
         * By using the conservative calculation, we subject ourself
         * to at most one extra resize.
         */
        if (numKeysToBeAdded > threshold)
        {
            int targetCapacity = (int)(numKeysToBeAdded / loadFactor + 1);
            if (targetCapacity > MAXIMUM_CAPACITY)
                targetCapacity = MAXIMUM_CAPACITY;
            int newCapacity = table.length;
            while (newCapacity < targetCapacity)
                newCapacity <<= 1;
            if (newCapacity > table.length)
                resize(newCapacity);
        }
        
        for (Iterator<? extends Map.Entry<? extends K, ? extends V>> i = m.entrySet().iterator(); i.hasNext(); )
        {
            Map.Entry<? extends K, ? extends V> e = i.next();
            put(e.getKey(), e.getValue());
        }
    }
    
    /**
     * Removes the mapping for this key from this map if present.
     *
     * @param  key key whose mapping is to be removed from the map.
     * @return previous value associated with specified key, or <tt>null</tt>
     *	       if there was no mapping for key.  A <tt>null</tt> return can
     *	       also indicate that the map previously associated <tt>null</tt>
     *	       with the specified key.
     */
    @Override
    public V remove(Object key) throws ConcurrentModificationException
    {
        Entry<K,V> e = removeEntryForKey(key);
        return (e == null ? null : e.value);
    }
    
    /**
     * Removes and returns the entry associated with the specified key
     * in the HashMap.  Returns null if the HashMap contains no mapping
     * for this key.
     */
    Entry<K,V> removeEntryForKey(Object key) throws ConcurrentModificationException
    {
        Object k = maskNull(key);
        int hash = hash(k);
        int i = indexFor(hash, table.length);
        Entry<K,V> prev = table[i];
        Entry<K,V> e = prev;
        
        while (e != null)
        {
            Entry<K,V> next = e.next;
            if (e.hash == hash && validEntry(e) && eq(k, e.key))
            {
                if (e.is(Entry.DELETED, null) && !e.is(Entry.DELETED, getCurrentThreadId()))
                    throw new ConcurrentModificationException();
                
                if (auto_commit)
                {
                    modCount++;
                    size--;
                    if (prev == e)
                        table[i] = next;
                    else
                        prev.next = next;
                    return e;
                }
                else
                    e.setStatus(Entry.DELETED, getCurrentThreadId());
            }
            prev = e;
            e = next;
        }
        
        return e;
    }
    
    /**
     * Special version of remove for EntrySet.
     */
    @SuppressWarnings("unchecked")
    Entry<K,V> removeMapping(Object o)
    {
        if (!(o instanceof Map.Entry))
            return null;
        
        Map.Entry<K,V> entry = (Map.Entry<K,V>)o;
        Object k = maskNull(entry.getKey());
        int hash = hash(k);
        int i = indexFor(hash, table.length);
        Entry<K,V> prev = table[i];
        Entry<K,V> e = prev;
        
        while (e != null)
        {
            Entry<K,V> next = e.next;
            if (e.hash == hash && validEntry(e) && e.equals(entry))
            {
                if (auto_commit)
                {
                    modCount++;
                    size--;
                    if (prev == e)
                        table[i] = next;
                    else
                        prev.next = next;
                }
                else
                    e.setStatus(Entry.DELETED, getCurrentThreadId());
                return e;
            }
            prev = e;
            e = next;
        }
        
        return e;
    }
    
    /**
     * Removes all mappings from this map.
     */
    @Override
    public void clear()
    {
        modCount++;
        Entry<K,V> tab[] = table;
        for (int i = 0; i < tab.length; i++)
            tab[i] = null;
        size = 0;
    }
    
    /**
     * Returns <tt>true</tt> if this map maps one or more keys to the
     * specified value.
     *
     * @param value value whose presence in this map is to be tested.
     * @return <tt>true</tt> if this map maps one or more keys to the
     *         specified value.
     */
    @Override
    public boolean containsValue(Object value)
    {
        if (value == null)
            return containsNullValue();
        
        Entry<K,V> tab[] = table;
        for (int i = 0; i < tab.length ; i++)
            for (Entry<K,V> e = tab[i] ; e != null ; e = e.next)
                if (validEntry(e) && value.equals(e.value))
                    return true;
        return false;
    }
    
    /**
     * Special-case code for containsValue with null argument
     **/
    private boolean containsNullValue()
    {
        Entry<K,V> tab[] = table;
        for (int i = 0; i < tab.length ; i++)
            for (Entry<K,V> e = tab[i] ; e != null ; e = e.next)
                if (validEntry(e) && e.value == null)
                    return true;
        return false;
    }
    
    private static final class Entry<K,V> implements Map.Entry<K,V>
    {
        final K key;
        V value;
        final int hash;
        Entry<K,V> next;
        private int          transationStatus;
        private String       transactionId;
        
        private static final int NO_CHANGE = 0;
        private static final int DELETED = 1;
        private static final int ADDED = 2;
        
        /**
         * Create new entry.
         */
        Entry(int h, K k, V v, Entry<K,V> n)
        {
            value = v;
            next = n;
            key = k;
            hash = h;
            transationStatus   = NO_CHANGE;
            transactionId      = null;
        }
        
        private void setStatus(final int status, final String id)
        {
            transationStatus = status;
            transactionId = id;
        }
        
        private boolean is(final int status, final String id)
        {
            if (transactionId == null)
                return status == NO_CHANGE;
            
            if (id == null || transactionId.equals(id))
                return transationStatus == status;
            
            return status == NO_CHANGE;
        }
        
        public K getKey()
        {
            return TransactionalHashMap.<K>unmaskNull(key);
        }
        
        public V getValue()
        {
            return value;
        }
        
        public V setValue(Object newValue)
        throws UnsupportedOperationException
        {
            throw new UnsupportedOperationException(
                    "Map.Entry.setValue is not supported");
        }
        
        @SuppressWarnings("unchecked")
        @Override
        public boolean equals(Object o)
        {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry e = (Map.Entry)o;
            Object k1 = getKey();
            Object k2 = e.getKey();
            if (k1 == k2 || (k1 != null && k1.equals(k2)))
            {
                Object v1 = getValue();
                Object v2 = e.getValue();
                if (v1 == v2 || (v1 != null && v1.equals(v2)))
                    return true;
            }
            return false;
        }
        
        @Override
        public int hashCode()
        {
            return (key==NULL_KEY ? 0 : key.hashCode()) ^
                    (value==null   ? 0 : value.hashCode());
        }
        
        @Override
        public String toString()
        {
            return getKey() + "=" + getValue();
        }
        
    }
    
    /**
     * Add a new entry with the specified key, value and hash code to
     * the specified bucket.  It is the responsibility of this
     * method to resize the table if appropriate.
     *
     * Subclass overrides this to alter the behavior of put method.
     */
    void addEntry(int hash, K key, V value, int bucketIndex)
    {
        table[bucketIndex] = new Entry<K,V>(hash, key, value, table[bucketIndex]);
        if (!auto_commit)
            table[bucketIndex].setStatus(Entry.ADDED, getCurrentThreadId());
        if (size++ >= threshold)
            resize(2 * table.length);
    }
    
    /**
     * Like addEntry except that this version is used when creating entries
     * as part of Map construction or "pseudo-construction" (cloning,
     * deserialization).  This version needn't worry about resizing the table.
     *
     * Subclass overrides this to alter the behavior of HashMap(Map),
     * clone, and readObject.
     */
    void createEntry(int hash, K key, V value, int bucketIndex)
    {
        table[bucketIndex] = new Entry<K,V>(hash, key, value, table[bucketIndex]);
        size++;
    }
    
    private abstract class HashIterator<E> implements Iterator<E>
    {
        Entry<K,V> next;                  // next entry to return
        int expectedModCount;        // For fast-fail
        int index;                   // current slot
        Entry<K,V> current;               // current entry
        
        HashIterator()
        {
            expectedModCount = modCount;
            Entry<K,V>[] t = table;
            int i = t.length;
            Entry<K,V> n = null;
            if (size != 0)
            { // advance to first entry
                while (i > 0 && n == null)
                {
                    Entry<K,V> test = t[--i];
                    while (n==null && test!=null)
                    {
                        if (validEntry(test))
                            n = test;
                        else
                            test = test.next;
                    }
                }
            }
            next = n;
            index = i;
        }
        
        public boolean hasNext()
        {
            return next != null;
        }
        
        Entry<K,V> nextEntry()
        {
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            Entry<K,V> e = next;
            if (e == null)
                throw new NoSuchElementException();
            
            Entry<K,V> n = e.next;
            while (n!=null && !validEntry(n))
                n = n.next;
            Entry<K,V>[] t = table;
            int i = index;
            while (i > 0 && n == null)
            {
                Entry<K,V> test = t[--i];
                while (n==null && test!=null)
                {
                    if (validEntry(test))
                        n = test;
                    else
                        test = test.next;
                }
            }
            index = i;
            next = n;
            return current = e;
        }
        
        public void remove()
        {
            if (current == null)
                throw new IllegalStateException();
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            Object k = current.key;
            current = null;
            TransactionalHashMap.this.removeEntryForKey(k);
            if (auto_commit)
                expectedModCount = modCount;
        }
        
    }
    
    private class ValueIterator extends HashIterator<V>
    {
        public V next()
        {
            return nextEntry().value;
        }
    }
    
    private class KeyIterator extends HashIterator<K>
    {
        public K next()
        {
            return nextEntry().getKey();
        }
    }
    
    private class EntryIterator extends HashIterator<Map.Entry<K, V>>
    {
        public Map.Entry<K, V> next()
        {
            return nextEntry();
        }
    }
    
    // Subclass overrides these to alter behavior of views' iterator() method
    Iterator<K> newKeyIterator()
    {
        return new KeyIterator();
    }
    Iterator<V> newValueIterator()
    {
        return new ValueIterator();
    }
    Iterator<Map.Entry<K, V>> newEntryIterator()
    {
        return new EntryIterator();
    }
    
    
    // Views
    
    private transient Set<Map.Entry<K,V>> entrySet = null;
    transient volatile Set<K> keySet = null;
    transient volatile Collection<V> values = null;
    
    /**
     * Returns a set view of the keys contained in this map.  The set is
     * backed by the map, so changes to the map are reflected in the set, and
     * vice-versa.  The set supports element removal, which removes the
     * corresponding mapping from this map, via the <tt>Iterator.remove</tt>,
     * <tt>Set.remove</tt>, <tt>removeAll</tt>, <tt>retainAll</tt>, and
     * <tt>clear</tt> operations.  It does not support the <tt>add</tt> or
     * <tt>addAll</tt> operations.
     *
     * @return a set view of the keys contained in this map.
     */
    @Override
    public Set<K> keySet()
    {
        Set<K> ks = keySet;
        return (ks != null ? ks : (keySet = new KeySet()));
    }
    
    private class KeySet extends AbstractSet<K>
    {
        @Override
        public Iterator<K> iterator()
        {
            return newKeyIterator();
        }
        @Override
        public int size()
        {
            return TransactionalHashMap.this.size();
        }
        @Override
        public boolean contains(Object o)
        {
            return containsKey(o);
        }
        @Override
        public boolean remove(Object o)
        {
            return TransactionalHashMap.this.removeEntryForKey(o) != null;
        }
        @Override
        public void clear()
        {
            TransactionalHashMap.this.clear();
        }
    }
    
    /**
     * Returns a collection view of the values contained in this map.  The
     * collection is backed by the map, so changes to the map are reflected in
     * the collection, and vice-versa.  The collection supports element
     * removal, which removes the corresponding mapping from this map, via the
     * <tt>Iterator.remove</tt>, <tt>Collection.remove</tt>,
     * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt> operations.
     * It does not support the <tt>add</tt> or <tt>addAll</tt> operations.
     *
     * @return a collection view of the values contained in this map.
     */
    @Override
    public Collection<V> values()
    {
        Collection<V> vs = values;
        return (vs != null ? vs : (values = new Values()));
    }
    
    private class Values extends AbstractCollection<V>
    {
        @Override
        public Iterator<V> iterator()
        {
            return newValueIterator();
        }
        @Override
        public int size()
        {
            return TransactionalHashMap.this.size();
        }
        @Override
        public boolean contains(Object o)
        {
            return containsValue(o);
        }
        @Override
        public void clear()
        {
            TransactionalHashMap.this.clear();
        }
    }
    
    /**
     * Returns a collection view of the mappings contained in this map.  Each
     * element in the returned collection is a <tt>Map.Entry</tt>.  The
     * collection is backed by the map, so changes to the map are reflected in
     * the collection, and vice-versa.  The collection supports element
     * removal, which removes the corresponding mapping from the map, via the
     * <tt>Iterator.remove</tt>, <tt>Collection.remove</tt>,
     * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt> operations.
     * It does not support the <tt>add</tt> or <tt>addAll</tt> operations.
     *
     * @return a collection view of the mappings contained in this map.
     * @see Map.Entry
     */
    @Override
    public Set<Map.Entry<K, V>> entrySet()
    {
        Set<Map.Entry<K, V>> es = entrySet;
        return (es != null ? es : (entrySet = new EntrySet()));
    }
    
    private class EntrySet extends AbstractSet<Map.Entry<K, V>>
    {
        @Override
        public Iterator<Map.Entry<K, V>> iterator()
        {
            return newEntryIterator();
        }
        @SuppressWarnings("unchecked")
        @Override
        public boolean contains(Object o)
        {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<K,V> e = (Map.Entry<K,V>)o;
            Entry<K,V> candidate = getEntry(e.getKey());
            return candidate != null && candidate.equals(e);
        }
        @Override
        public boolean remove(Object o)
        {
            return removeMapping(o) != null;
        }
        @Override
        public int size()
        {
            return TransactionalHashMap.this.size();
        }
        @Override
        public void clear()
        {
            TransactionalHashMap.this.clear();
        }
    }
    
}
