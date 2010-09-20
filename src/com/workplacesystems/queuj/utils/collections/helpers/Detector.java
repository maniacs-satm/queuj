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
 * Iterator to check if an iterated collection contains an element.  Clients can check if there is
 * an element satisfying a property by using this in conjunction with a filter. 
 * @see Collector - collect results of applying a function to all elements visited
 * @see Detector - check if any elements visited
 * @see Retriever - first element visited 
 * @see Selector - all elements visited
 * @see Count - count of all elements visited 
 */
public class Detector<T> extends IterativeCallback<T,Boolean>
{
    /** Iterator which will return true if and only if it visits any items */
    public Detector()
    {
        super(Boolean.FALSE);
    }

    @Override
    protected void nextObject(Object obj)
    {
        _return(Boolean.TRUE);
    }
}
