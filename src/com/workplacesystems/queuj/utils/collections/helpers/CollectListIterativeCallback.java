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

import java.util.Collection;

import com.workplacesystems.queuj.utils.collections.IterativeCallback;

/** 
 * <b>Obsolete</b> use Selector<p>
 * Iterator that just adds every object iterated over to the given collection. 
 * <p>
 * Consider using the iterators that have been designed to use existing
 * IterativeCallback capabilities, and are named after the Smalltalk enumerators.
 * @see Selector - select all visited elements
 * @see Collector - collect results of applying a function to all visited elements
 * @see Detect - find first visited element
 * @see Contains - check if any elements visited
 */
public class CollectListIterativeCallback<T,R> extends IterativeCallback<T,R>
{

    private Collection<T> coll;

    public CollectListIterativeCallback(Collection<T> coll)
    {
        this.coll = coll;
    }

    @Override
    protected void nextObject(T obj)
    {
        coll.add(obj);
    }

}
