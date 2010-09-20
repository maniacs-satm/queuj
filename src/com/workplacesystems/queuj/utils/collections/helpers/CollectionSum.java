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
import java.util.Iterator;

/**
 * Utility to provide a sum of a collection.
 * The sum is dynamically recomputed to reflect the state of the collection.
 */
public class CollectionSum extends Number
{
    private Collection<Number> numbers;
    
    public CollectionSum( Collection<Number> numbers )
    {
        this.numbers = numbers;
    }

    @Override
    public double doubleValue()
    {
        double total = 0.0;
        Iterator<Number> itr = numbers.iterator();
        while(itr.hasNext())
        {
           total += itr.next().doubleValue();
        }
        
        return total;
    }

    @Override
    public float floatValue()
    {
        return (float)doubleValue();
    }

    @Override
    public int intValue()
    {
        return (int)doubleValue();
    }

    @Override
    public long longValue()
    {
        return (long)doubleValue();
    }
}
