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

package com.workplacesystems.queuj;

import java.io.Serializable;

/**
 *
 * @author dave
 */
public abstract class Index implements Serializable
{
    // Increase the number when an incompatible change is made
    private static final long serialVersionUID = Index.class.getName().hashCode() + 1;


    /** Creates a new instance of Index */
    public Index()
    {
    }

    protected abstract Object getKey(Process process);

    /**
     * Implement equals to use result of getClass.
     */
    @Override
    public boolean equals(Object object)
    {
        if (object == null || !(object instanceof Index))
            return false;

        return object.getClass().equals(getClass());
    }

    /**
     * Implement hashCode to fulfil the contract of Object.
     */
    @Override
    public int hashCode()
    {
        return 1 + getClass().hashCode();
    }
}
