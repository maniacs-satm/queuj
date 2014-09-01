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
     * A predictable restriction is one that, given the same job queue state, will
     * always return the same result from canRun. Returning true from isPredictable
     * improves the scalability of the job queue. For jobs that have non-predictable
     * restrictions scalability needs to be taken into consideration manually by
     * limiting the number of these jobs that get added to the queue at one time.
     * 
     * A restriction is considered not predictable if the result from canRun would
     * vary based on variables not considered part of the queue. For instance, if
     * canRun is dependant on a parameter in the process passed into the canRun
     * method or if canRun is dependant on the current availability of system
     * resources.
     * 
     * For jobs of the same queue with a predictable restriction the scheduler
     * will call canRun on the queue for each job until one returns false at which
     * point it will not check anymore during that notify queue cycle. For jobs
     * without a predictable restriction the scheduler will call canRun on every
     * job for each notify cycle.
     * 
     * @return whether the restriction implemented by this class is predictable or not?
     */
    protected abstract boolean isPredictable();

    /**
     * Can the supplied Process run for the supplied Queue.
     */
    protected abstract boolean canRun(Queue queue, Process process);

    /**
     * toString provides a unique String for this QueueRestriction.
     */
    @Override
    public final String toString()
    {
        return getClass().getName() + getSelfString();
    }

    /**
     * Called by toString and overridden by subclasses.
     * Allows subclasses to output their unique String so that QueueRestrictions
     * of the same class can be differentiated from each other.
     */
    protected String getSelfString() { return ""; }

}
