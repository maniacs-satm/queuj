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

import com.workplacesystems.queuj.access.NoAccess;
import com.workplacesystems.queuj.occurrence.RunOnce;
import com.workplacesystems.queuj.output.NoOutput;
import com.workplacesystems.queuj.process.java.JavaProcessBuilder;
import com.workplacesystems.queuj.process.java.JavaProcessServer;
import com.workplacesystems.queuj.resilience.RunOnlyOnce;
import com.workplacesystems.queuj.restriction.GlobalQueueRestriction;
import com.workplacesystems.queuj.restriction.JavaQueueRestriction;
import com.workplacesystems.queuj.schedule.RelativeScheduleBuilder;
import com.workplacesystems.queuj.visibility.Invisible;
import com.workplacesystems.queuj.utils.QueujException;

/**
 * The QueueFactory class provides static methods to create new Queue objects
 * and several pre-defined queues.
 *
 * @author dave
 */
public class QueueFactory
{
    // Some pre-defined queues:

    /**
     * ROOT_QUEUE is the top parent of all other queues.
     * It has a GlobalQueueRestriction which allows for a
     * default restriction for all queues.
     * It has no ProcessBuilder or ProcessServer and can
     * therefore not be used directly.
     */
    public final static Queue<ProcessBuilder> ROOT_QUEUE;

    /**
     * DEFAULT_QUEUE is the queue for all Java jobs. It uses
     * JavaQueueRestriction which currently allows all jobs
     * to run. In future it will be changed to dynamically
     * allow or disallow jobs on a particular server based
     * on load. It uses JavaProcessBuilder which takes the
     * object and method to be run. JavaProcessServer uses
     * reflection to call the  method.
     */
    public final static Queue<JavaProcessBuilder> DEFAULT_QUEUE;


    static
    {
        try
        {
            // All pre-defined queues deafult to a run once schedule
            RunOnce occurrence = new RunOnce();
            RelativeScheduleBuilder rsb = occurrence.newRelativeScheduleBuilder();
            rsb.setRunImmediately();
            rsb.createSchedule();

            // Create the root queue. No builder or server.
            QueueBuilder<ProcessBuilder> qb = newQueueBuilder(null, null);
            qb.setQueueRestriction(new GlobalQueueRestriction());
            qb.setDefaultVisibility(new Invisible());
            qb.setDefaultAccess(new NoAccess());
            qb.setDefaultOutput(new NoOutput());
            ROOT_QUEUE = qb.newQueue();

            // Create the java queue parented by the root queue.
            QueueBuilder<JavaProcessBuilder> jqb = newQueueBuilder(JavaProcessBuilder.class);
            jqb.setQueueRestriction(new JavaQueueRestriction());
            jqb.setBatchProcessServerClass(JavaProcessServer.class);
            jqb.setDefaultOccurrence(occurrence);
            jqb.setDefaultResilience(new RunOnlyOnce());
            DEFAULT_QUEUE = jqb.newQueue();
        }
        catch (Exception e)
        {
            throw new QueujException(e);
        }
    }

    /** Creates a new instance of QueueFactory */
    private QueueFactory()
    {
    }

    /**
     * Retrieve a new QueueBuilder using the ROOT_QUEUE
     * as a parent.
     *
     * @return The new QueueBuilder object.
     */
    public static <B extends ProcessBuilder> QueueBuilder<B> newQueueBuilder(Class<B> process_builder_class)
    {
        return new QueueBuilder<B>(ROOT_QUEUE, process_builder_class);
    }

    /**
     * Retrieve a new QueueBuilder using the specified
     * queue as a parent.
     *
     * @param parent_queue - The parent queue.
     * @return The new QueueBuilder object.
     */
    static <B extends ProcessBuilder> QueueBuilder<B> newQueueBuilder(Queue parent_queue, Class<B> process_builder_class)
    {
        return new QueueBuilder<B>(parent_queue, process_builder_class);
    }
}
