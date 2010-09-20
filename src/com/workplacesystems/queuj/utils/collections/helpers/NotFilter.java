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

import com.workplacesystems.queuj.utils.collections.Filter;

/**
 * Used to invert the behavior of a given filter.
 */
public class NotFilter<E> implements Filter<E>
{
    Filter<E> filter;
    
    public NotFilter(Filter<E> filter) 
    {
        this.filter = filter;
    }
    
    public boolean isValid(E obj)
    {
        return !filter.isValid(obj);
    }
}
