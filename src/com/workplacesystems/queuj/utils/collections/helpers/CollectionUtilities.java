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

package com.workplacesystems.queuj.utils.collections.helpers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;
import java.util.Vector;

import com.workplacesystems.queuj.utils.collections.FilterableArrayList;

public class CollectionUtilities
{

    /** Take key and value pairs from source and create map from value to key in target. */
    public static <K,V> void reverse(Map<K,V> source, Map<V,K> target)
    {
        Iterator<K> i = source.keySet().iterator();
        while (i.hasNext())
        {
            K key = i.next();
            V value = source.get(key);
            target.put(value, key);
        }
    }

    public static <K,V> List<V> toValueList(Map<K,V> map)
    {
        return new ArrayList<V>(map.values());
    }

    /** 
     * New empty collection with similar type to source.  The known concrete types replicated exactly are
     * {@link ArrayList}, {@link LinkedList}, {@link TreeSet}, {@link HashSet}, {@link Stack}, {@link Stack},
     * {@link PriorityQueue}, {@link LinkedHashSet}, {@link Vector}.
     * <p>
     * Otherwise, the method only guarantees common conformance to one of the abstractions
     * {@link Queue}, {@link SortedSet}, {@link Set}, {@link List}.
     */
    public static<T> Collection<T> like(Collection<T> source)
    {
        // known concrete types
        if (source instanceof FilterableArrayList)
            return new FilterableArrayList();
        if (source instanceof ArrayList)
            return new ArrayList();
        if (source instanceof LinkedList)
            return new LinkedList();
        if (source instanceof TreeSet)
            return new TreeSet(((TreeSet)source).comparator());
        if (source instanceof LinkedHashSet)
            return new LinkedHashSet();
        if (source instanceof HashSet)
            return new HashSet();
        if (source instanceof Stack)
            return new Stack();
        if (source instanceof PriorityQueue)
            return new PriorityQueue(10, ((PriorityQueue)source).comparator());
        if (source instanceof Vector)
            return new Vector();
        
        // known abstract types
        if (source instanceof Queue)
            return new LinkedList();
        if (source instanceof SortedSet)
            return new TreeSet(((SortedSet)source).comparator());
        if (source instanceof Set)
            return new HashSet();
        if (source instanceof List)
            return new ArrayList();
        
        throw new IllegalArgumentException("Unknown collection type " + source.getClass().getCanonicalName());
    }
}
