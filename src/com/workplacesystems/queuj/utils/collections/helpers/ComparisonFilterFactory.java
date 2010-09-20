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

import java.util.Comparator;

import com.workplacesystems.queuj.utils.collections.Filter;

/**
 * Factory to create various comaprison filters to operate on collections
 * that are mutually comparable or given an object that implements the 
 * Compartor interface compares the objects in a collection with a base 
 * object.
 */
public abstract class ComparisonFilterFactory
{
    /**
     * Creates a filter where valid when an object in a 
     * collection is the <strong>same as</strong> the base object.
     */
    public static <E> Filter<E> createSameAsFilter(final E base) 
    {
        return new Filter<E>() {
            public boolean isValid(E obj)
            {
                return (base == obj);
            }
        };
    }

    /**
     * Creates a filter where valid when an object in a 
     * collection is the <strong>equal to</strong> the base object.
     */
    public static <E> Filter<E> createEqualToFilter(final Object base) 
    {
        return new Filter<E>() {
            public boolean isValid(E obj)
            {
                return obj.equals(base);
            }
        };
    }

    /**
     * Creates a filter where valid when an Comparable in a 
     * collection is considered <strong>greater than</strong> the base object.
     */
    public static <E extends Comparable<E>> Filter<E> createGreaterThanFilter(final E base) 
    {
        return createGreaterThanFilter(base, new Comparator<E>() 
        {
            public int compare(E obj1, E obj2)
            {
                return obj1.compareTo(obj2);
            }
        });
    }

    /**
     * Creates a filter where valid when an Object in a 
     * collection is considered <strong>greater than</strong> the base object, 
     * based on the given Comparator.
     */
    public static <E> Filter<E> createGreaterThanFilter(final E base, final Comparator<E> comparator) 
    {
        return new Filter<E>() {
            public boolean isValid(E obj)
            {
                return comparator.compare(obj, base) > 0;
            }
        };
    }
    
    /**
     * Creates a filter where valid when an Comparable in a 
     * collection is considered <strong>greater than or equal to</strong> the base object.
     */
    public static <E extends Comparable<E>> Filter<E> createGreaterThanOrEqualToFilter(final E base) 
    {
        return createGreaterThanOrEqualToFilter(base, new Comparator<E>() 
        {
            public int compare(E obj1, E obj2)
            {
                return obj1.compareTo(obj2);
            }
        });
    }

    /**
     * Creates a filter where valid when an Object in a 
     * collection is considered <strong>greater than or equal to</strong> the base object, 
     * based on the given Comparator.
     */
    public static <E> Filter<E> createGreaterThanOrEqualToFilter(final E base, final Comparator<E> comparator) 
    {
        return new Filter<E>() {
            public boolean isValid(E obj)
            {
                return comparator.compare(obj, base) >= 0;
            }
        };
    }
    
    /**
     * Creates a filter where valid when an Comparable in a 
     * collection is considered <strong>less than</strong> the base object.
     */
    public static <E extends Comparable<E>> Filter<E> createLessThanFilter(final E base) 
    {
        return createLessThanFilter(base, new Comparator<E>() 
        {
            public int compare(E obj1, E obj2)
            {
                return obj1.compareTo(obj2);
            }
        });
    }
    
    /**
     * Creates a filter where valid when an Object in a 
     * collection is considered <strong>less than</strong> the base object, 
     * based on the given Comparator.
     */
    public static <E> Filter<E> createLessThanFilter(final E base, final Comparator<E> comparator) 
    {
        return new Filter<E>() {
            public boolean isValid(E obj)
            {
                return comparator.compare(obj, base) < 0;
            }
        };
    }
    
    /**
     * Creates a filter where valid when an Comparable in a 
     * collection is considered <strong>less than or equal to</strong> the base object.
     */
    public static <E extends Comparable<E>> Filter<E> createLessThanOrEqualToFilter(final E base)
    {
        return createLessThanOrEqualToFilter(base, new Comparator<E>() 
        {
            public int compare(E obj1, E obj2)
            {
                return obj1.compareTo(obj2);
            }
        });
    }
    
    /**
     * Creates a filter where valid when an Object in a 
     * collection is considered <strong>less than or equal to</strong> the base object, 
     * based on the given Comparator.
     */
    public static <E> Filter<E> createLessThanOrEqualToFilter(final E base, final Comparator<E> comparator)
    {
        return new Filter<E>() {
            public boolean isValid(E obj)
            {
                return comparator.compare(obj, base) <= 0;
            }
        };
    }
}
