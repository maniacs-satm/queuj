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

import com.workplacesystems.queuj.utils.Callback;
import com.workplacesystems.queuj.utils.collections.FilterableCollection;
import com.workplacesystems.queuj.utils.collections.IterativeCallback;
import com.workplacesystems.queuj.utils.collections.SyncUtils;
import com.workplacesystems.queuj.utils.collections.decorators.SynchronizedFilterableCollection;

/**
 *
 * @author  Administrator
 */
public final class IsEmptyIterativeCallback<T> extends IterativeCallback<T, Boolean>
{
    @Override
    public void nextObject(T obj) { }

    @Override
    public Boolean iterate(final FilterableCollection<? extends T> c)
    {
        checkUsed();
        // This relies on the efficiency of the Collection implementation
        // but AbstractCollection does a size() == 0!
        //return c.isEmpty() ? Boolean.TRUE : Boolean.FALSE;
        // This is guaranteed more efficient:
        if (c instanceof SynchronizedFilterableCollection)
        {
            return SyncUtils.synchronizeRead(c, new Callback<Boolean>() {
                @Override
                protected void doAction()
                {
                    _return(!c.iterator().hasNext() ? Boolean.TRUE : Boolean.FALSE);
                }
            });
        }
        return !c.iterator().hasNext() ? Boolean.TRUE : Boolean.FALSE;
    }
}
