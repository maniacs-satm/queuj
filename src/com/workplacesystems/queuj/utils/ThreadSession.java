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

package com.workplacesystems.queuj.utils;

import java.util.HashMap;
import java.util.Iterator;

/**
 *
 * @author dave
 */
public class ThreadSession
{
    private static final ThreadLocal<ThreadSession> thread_session = new ThreadLocal<ThreadSession>() {
        @Override
        protected synchronized ThreadSession initialValue()
        {
            return new ThreadSession();
        }
    };

    /** Creates a new instance of ThreadSession */
    private ThreadSession()
    {
    }

    private final static String getThreadId(final Thread the_thread)
    {
        return the_thread.toString() + "(" + the_thread.hashCode() + ")";
    }

    private final static ThreadSession get()
    {
        return thread_session.get();
    }

    private final HashMap<Object,Object> values = new HashMap<Object,Object>();

    public final static Object putValue(final Object key, Object value)
    {
        return get().values.put(key, value);
    }

    public final static Object getValue(final Object key)
    {
        Object value = get().values.get(key);

        return value;
    }

    public final static Object removeValue(final Object key)
    {
        return get().values.remove(key);
    }

    private final static String SEPARATOR = System.getProperty("line.separator");

    final static String dump()
    {
        String result = "";

        ThreadSession session = get();
        if (session == null)
            return result;

        for (Iterator<?> i = session.values.keySet().iterator(); i.hasNext(); )
        {
            Object key = i.next();
            Object value = session.values.get(key);
            if (value instanceof String)
            {
                result += key + ": " + value + SEPARATOR;
            }
        }
        return result;
    }
}
