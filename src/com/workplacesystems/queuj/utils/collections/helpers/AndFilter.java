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
import java.util.Arrays;
import java.util.List;

import com.workplacesystems.queuj.utils.collections.Filter;

/**
 * Combines many individual filters into one. Filters out any item that 
 * returns false for any of the filters, ie. a logial and of all filters.
 * This has the same operating mode as the java shortcut and operator 
 * '&amp;&amp;'.
 *
 * @author henningt
 */
public final class AndFilter<E> implements Filter<E> {
    
    /** List of and conditions. */
    private final List<Filter<? super E>> conditions;

    /**
     * Constructor that takes a minimum of two filters.
     * Addtional filters are specified as a variable argument.
     * @param conditions the filters to use
     */
    public AndFilter(final Filter<? super E> f1, final Filter<? super E> f2, 
            final Filter<? super E>...optional_conditions) 
    {
        List<Filter<? super E>> conditions = new ArrayList<Filter<? super E>>();
        
        conditions.add(f1);
        conditions.add(f2);
        conditions.addAll(Arrays.asList(optional_conditions));
        this.conditions = conditions;
    }
    
    /**
     * Constructor that takes a list of filters.
     * @param conditions the filters to use
     */
    public AndFilter(final List<Filter<? super E>> conditions) 
    {
        this.conditions = new ArrayList<Filter<? super E>>(conditions);
    }

    /**
     * Part of the {@link Filter} interface.
     * @param obj the object to filter
     */
    public boolean isValid(E obj) 
    {
        for(Filter<? super E> condition : conditions)
            if(!condition.isValid(obj)) return false;
        return true;
    }
}