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

import java.util.Arrays;
/** Decorator for an int[] with some array-level arithmetic */
public class IntArray
{
    public int[] value;
    public int length;
    
    /** Create contents with 'length' items, all zero */
    public IntArray(int length)
    {
        value = new int[length];
        this.length = length;
    }
    
    /** Replace contents with given values */
    public IntArray with(int... values)
    {
        if (values.length != this.length)
            throw new IllegalArgumentException("Array size mismatch");
        value = values.clone();
        return this;
    }

    public void add(int... others)
    {
        if (others.length != this.length)
            throw new IllegalArgumentException("Array size mismatch");
        for (int i=0; i<length; i++)
            value[i] += others[i];
    }
    
    public void inc(boolean... others)
    {
        if (others.length != this.length)
            throw new IllegalArgumentException("Array size mismatch");
        for (int i=0; i<length; i++)
            if (others[i]) value[i]++;
    }
    
    public void add(IntArray other)
    {
        add(other.value);
    }
    
    @Override
    public boolean equals(Object obj)
    {
        return obj instanceof IntArray 
            && Arrays.equals(value, ((IntArray)obj).value);
    }
    
    public Integer[] toIntegers()
    {
        Integer[] result = new Integer[length];
        for (int i=0; i<length; i++)
            result[i] = value[i];
        return result;
    }
}
