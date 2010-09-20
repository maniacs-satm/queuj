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

import java.util.Iterator;
import com.workplacesystems.queuj.utils.Callback;

import com.workplacesystems.queuj.utils.QueujException;
import com.workplacesystems.queuj.utils.collections.decorators.SynchronizedFilterableCollection;

/** IterativeCallback provides a means of threadsafe iteration on collections of records.
 *
 * @author  Dave
 */
abstract public class IterativeCallback<T,R>
{    
    /** Creates a new instance of IterativeCallback */
    public IterativeCallback() {}
 
    /** Creates a new instance of IterativeCallback, accepting an object that may be amended throughout the iteration process */
    public IterativeCallback(R amended_object) 
    {
        this.amended_object = amended_object;
    }
 
    /** public wrapper for the iteration */
    public R iterate(final FilterableCollection<? extends T> c)
    {
        checkUsed();

        return_object = null;
        do_break = false;
        
        // If collection is decorated with a syncronized wrapper then synchronize the iteration
        if (c instanceof SynchronizedFilterableCollection)
        {
            return SyncUtils.synchronizeRead(c, new Callback<R>() {
                @Override
                protected void doAction()
                {
                    _return(doIteration(c.iterator()));
                }
            });
        }
        return doIteration(c.iterator());
    }

    /**
     * Find out if callback has been used.
     * @return true if used, false if not
     */
    protected boolean hasBeenUsed() { return iterative_callback_used; }
    
    /** check callback used only once */
    protected final void checkUsed()
    {
        if (iterative_callback_used)
            throw new QueujException("IterativeCallback can not be used more than once.");
        iterative_callback_used = true;
    }
    
    // Save iterator so that remove can be called
    private Iterator<? extends T> i;
    
    /** do the actual iteration */
    private R doIteration(Iterator<? extends T> it)
    {
        // save the iterator into member variable
        i = it;
        int iterations = 0;
        
        // do the iteration calling nextobject on each
        while (i.hasNext())
        {
            T o = i.next();
            if (iterations == 0)
                firstObject(o);
            else
                nextObject(o);
            iterations++;
            if (do_break == true)
                return return_object;
        }
        return amended_object;
    }

    protected void firstObject(T o)
    {
        nextObject(o);
    }

    protected final boolean hasNext()
    {
        return i.hasNext();
    }
    
    /** allows remove to be called on the iterator */
    public void remove()
    {
        i.remove();
    }

    /** called by each iteration step */
    protected abstract void nextObject(T obj);

    private boolean iterative_callback_used = false;
    private R return_object;
    private R amended_object = null;
    private boolean do_break;
    
    protected void _return(R o)
    {
        return_object = o;
        do_break = true;
    }
    
    protected void _returnNotNull(R o)
    {
        if (o != null)
        {
            return_object = o;
            do_break = true;
        }
    }
    
    protected void _break()
    {
        do_break = true;
    }
    
    protected void setAmendedObject(R o)
    {
        amended_object = o;
    }
    
    protected R getAmendedObject()
    {
        return amended_object;
    }
    
}
