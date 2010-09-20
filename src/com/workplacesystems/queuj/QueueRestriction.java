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
 * The QueueRestriction class is an abstract class that can be overridden to
 * restrict how Processes are run. By default all Processes will run in parallel.
 * By overriding this class you can restrict the Processes by checking the
 * Processes that are currently running.
 *
 * Current subclasses include CLIQueueRestriction that mimics a serial queue
 * by only allowing 1 Process to run at a time per Partition and limits the
 * maximum concurrent Processes accross the entire cluster. CrystalQueueRestriction
 * limits the maximum concurrent Processes accross the entire cluster.
 * JavaQueueRestriction and GlobalQueueRestriction currently don't implement
 * any limitations.
 *
 * @author dave
 */
public abstract class QueueRestriction implements Serializable
{
    // Increase the number when an incompatible change is made
    private static final long serialVersionUID = QueueRestriction.class.getName().hashCode() + 1;

    public QueueRestriction()
    {
    }

    /**
     * Can the supplied Process run for the supplied Queue.
     */
    protected abstract boolean canRun(Queue queue, Process process);
}
