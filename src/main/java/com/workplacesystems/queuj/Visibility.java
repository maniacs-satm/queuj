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

import com.workplacesystems.queuj.utils.User;

/**
 * The Visibility class specifies which users can see Processes.
 * Current subclasses are Invisible which is invisible to
 * everyone, SupportVisible which is visible to only support users and
 * PartitionAndSupportVisible which is visible to support users and
 * users of the same partition as the Process.
 *
 * @author dave
 */
public abstract class Visibility implements Serializable
{
    // Increase the number when an incompatible change is made
    private static final long serialVersionUID = Visibility.class.getName().hashCode() + 1;

    /** Creates a new instance of Visibility */
    public Visibility()
    {
    }

    /**
     * Is the supplied Process visible for the supplied user.
     * @param active_partition TODO
     */
    public abstract boolean isVisible(Process process, User user, QueueOwner active_partition);
}
