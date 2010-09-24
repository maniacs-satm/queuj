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

import com.workplacesystems.queuj.utils.QueujException;
import com.workplacesystems.queuj.utils.collections.FilterableCollection;
import com.workplacesystems.queuj.utils.collections.IterativeCallback;

/**
 * Counts the items by calling <code>size()</code> on the filtered set.
 * <b>Caveat</b>: saves time by not actually iterating the items.
 *
 * @author  Dave
 */
public final class CounterIterativeCallback<T> extends IterativeCallback<T,Integer>
{
    /** Holds the number of counted items. */
    private int count = 0;
    
    // don't need to actually iterate the objects, we just get the count of the filterable collection
    @Override
    public void nextObject(T obj) { }

    /** 
     * Overridden to just get the count, and nothing else.
     */
    @Override
    public Integer iterate(final FilterableCollection<? extends T> c)
    {
        checkUsed();
        // No point doing the iteration. Just return size of collection.
        count = c.size();
        return count;
    }
    
    /**
     * Return the number of items counted.
     * @throws QueujException if the iterative callback has not been used yet
     */
    public int getCount() throws QueujException
    {
        if (!hasBeenUsed())
            throw new QueujException("CounterIterativeCallback.getCount() called without having been used");
        return count;
    }
}
