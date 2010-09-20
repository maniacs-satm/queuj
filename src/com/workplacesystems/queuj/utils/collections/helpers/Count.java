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

import com.workplacesystems.queuj.utils.collections.IterativeCallback;

/**
 * Iterator to count all the elements it visits.  Clients can count sub-collections
 * by using this in conjunction with a filter. 
 * @see Collector - collect results of applying a function to all elements visited
 * @see Detector - check if any elements visited
 * @see Retriever - first element visited 
 * @see Selector - all elements visited 
 * @see Count - count of all elements visited 
 */
public class Count<T> extends IterativeCallback<T, Integer>
{
    /** Iterator which will return a count of visited items */
    public Count()
    {
        super(new Integer(0));
    }

    /** Iterator which will return an accumulated count of visited items */
    public Count(Integer n)
    {
        super(n);
    }

    @Override
    protected void nextObject(T obj)
    {
        setAmendedObject(getAmendedObject().intValue()+1);
    }
}
