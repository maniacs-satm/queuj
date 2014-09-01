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

package com.workplacesystems.queuj.restriction;

import com.workplacesystems.queuj.Process;
import com.workplacesystems.queuj.Queue;
import com.workplacesystems.queuj.QueueRestriction;
import com.workplacesystems.queuj.process.ProcessIndexes;
import com.workplacesystems.queuj.process.ProcessIndexesCallback;
import com.workplacesystems.queuj.process.ProcessWrapper;
import com.workplacesystems.utilsj.collections.helpers.HasLessThan;

/** SimpleQueueRrestriction implements a simple check on the number of jobs running in the queue so far.
 *  The restriction is specified at time of creation rather than explicitly preconfigured
 *
 */
public class SimpleQueueRestriction extends QueueRestriction
{
    // Increase the number when an incompatible change is made
    private static final long serialVersionUID = SimpleQueueRestriction.class.getName().hashCode() + 1;

    // the max specified
    private final int max_concurrent;
    
    /** Creates a new instance of CrystalQueueRestriction */
    public SimpleQueueRestriction(int max_concurrent)
    {
        this.max_concurrent = max_concurrent;
    }

    @Override
    protected boolean isPredictable() {
        return true;
    }

    @Override
    protected boolean canRun(final Queue queue, Process process)
    {
        return process.getContainingServer().indexesWithReadLock(new ProcessIndexesCallback<Boolean>() {
            public Boolean readIndexes(ProcessIndexes indexes) {

                HasLessThan<ProcessWrapper> max = new HasLessThan<ProcessWrapper>(max_concurrent);
                max = indexes.iterateRunningProcesses(queue, max);
                indexes.iterateWaitingToRunProcesses(queue, max);
                return max.hasLess();
            }
        });
    }

    @Override
    protected String getSelfString() {
        return ", max_concurrent = " + String.valueOf(max_concurrent);
    }
}
