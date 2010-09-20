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

import com.workplacesystems.queuj.utils.collections.IterativeCallback;

/** 
 * Iterator that iterates a collection an finds the minimum value
 * based on a comparator.
 * <p>
 * @see Collector - collect results of applying a function to all elements visited
 * @see Detector - check if any elements visited
 * @see Retriever - first element visited 
 * @see LastRetriever - last element visited 
 * @see Selector - all elements visited  
 */
public class MinimumRetriever<T extends Comparable<T>> extends IterativeCallback<T,T>
{
    private Comparator<T> comparator;

    public MinimumRetriever() 
    {
        comparator = new Comparator<T>() {
            public int compare(T obj1, T obj2)
            {
                return obj1.compareTo(obj2);
            }
        };
    }
    
    public MinimumRetriever(Comparator<T> comparator)
    {
        this.comparator = comparator;
        setAmendedObject(null);
    }

    @Override
    protected void nextObject(T obj)
    {
      if(getAmendedObject() == null) 
          setAmendedObject(obj);
      else if(comparator.compare(obj, getAmendedObject()) < 0) 
          setAmendedObject(obj);
    }
}
