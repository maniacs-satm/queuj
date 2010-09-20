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

/** A comparator that wraps and reverses the order of any other comparator. */
public class ReverseOrder implements Comparator
{
    private final Comparator comparator;

    public ReverseOrder(Comparator c)
    {
        this.comparator = c;
    }

    public int compare(Object o1, Object o2)
    {
        return comparator.compare(o1, o2) * -1;
    }
}
