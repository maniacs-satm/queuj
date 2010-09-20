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

import com.workplacesystems.queuj.process.BatchProcessServer;

/**
 * An instance of QueueBuilder is used build a new Queue and provide methods
 * to easily set the properties of the Queue.
 *
 * An implementation of QueueBuilder is NOT guaranteed to behave as per the specification if it is
 * used concurrently by two or more threads. It is recommended to have one instance of the
 * QueueBuilder per thread or it is upto the application to make sure about the use of QueueBuilder
 * from more than one thread.
 *
 * @author dave
 */
public class QueueBuilder<B extends ProcessBuilder>
{
    /** The parent Queue. */
    private Queue<? extends ProcessBuilder> parent_queue;

    /** The QueueRestriction. */
    private QueueRestriction queue_restriction;

    /** The Index */
    private Index index;

    /** The ProcessBuilder class. */
    private Class<B> process_builder_class;

    /** The BatchProcessServer class. */
    private Class<? extends BatchProcessServer> process_server_class;

    /** The default Occurrence for Processes. */
    private Occurrence default_occurence;

    /** The default Visibility for Processes. */
    private Visibility default_visibility;

    /** The default Access for Processes. */
    private Access default_access;

    /** The default Resilience for Processes. */
    private Resilience default_resilience;

    /** The default Output for Processes. */
    private Output default_output;

    /** The deallocate partition delay for Processes. */
    private int deallocate_partition_delay;

    /**
     * Creates a new instance of QueueBuilder with the supplied Queue as the parent
     */
    QueueBuilder(Queue<? extends ProcessBuilder> parent_queue, Class<B> process_builder_class)
    {
        this.parent_queue = parent_queue;
        this.process_builder_class = process_builder_class;
    }

    /**
     * Set the QueueRestriction.
     */
    public void setQueueRestriction(QueueRestriction queue_restriction)
    {
        this.queue_restriction = queue_restriction;
    }

    /**
     * Set the custom queue index.
     */
    public void setIndex(Index index)
    {
        this.index = index;
    }

    /**
     * Set the BatchProcessServer class.
     */
    public void setBatchProcessServerClass(Class<? extends BatchProcessServer> process_server_class)
    {
        this.process_server_class = process_server_class;
    }

    /**
     * Set the default Occurrence.
     */
    public void setDefaultOccurrence(Occurrence default_occurence)
    {
        this.default_occurence = default_occurence;
    }

    /**
     * Set the default Visibility.
     */
    public void setDefaultVisibility(Visibility default_visibility)
    {
        this.default_visibility = default_visibility;
    }

    /**
     * Set the default Access.
     */
    public void setDefaultAccess(Access default_access)
    {
        this.default_access = default_access;
    }

    /**
     * Set the default Resilience.
     */
    public void setDefaultResilience(Resilience default_resilience)
    {
        this.default_resilience = default_resilience;
    }

    /**
     * Set the default Output.
     */
    public void setDefaultOutput(Output default_output)
    {
        this.default_output = default_output;
    }

    /**
     * Set the deallocate partition delay.
     */
    public void setDeallocatePartitionDelay(int deallocate_partition_delay)
    {
        this.deallocate_partition_delay = deallocate_partition_delay;
    }

    /**
     * Create the new Queue using the currently set properties.
     */
    public Queue<B> newQueue()
    {
        return new Queue<B>(parent_queue, queue_restriction, index, process_builder_class, process_server_class,
            default_occurence, default_visibility, default_access, default_resilience, default_output, deallocate_partition_delay);
    }
}
