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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.Locale;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.workplacesystems.queuj.process.BatchProcessServer;
import com.workplacesystems.queuj.utils.QueujException;
import com.workplacesystems.queuj.utils.collections.FilterableArrayList;

/**
 * A Queue object controls how Processes run and are created. A Queue is not persistent except
 * in the Process that it creates. For example if one of the pre-defined Queue's are changed the
 * Queue that was previously persisted with a Process will no longer be equal to the new Queue
 * and will run with the properties defined at the time the Process was created.
 *
 * A Queue is imutable. Its properties cannot be changed after the Queue has been created. For
 * 2 Queue's queue1 and queue2: if queue1.equals(queue2) then queue1 == queue2 may or may not be
 * true. Also if queue1.equals(queue2) then queue1.toString().equals(queue2.toString()).
 *
 * @author dave
 */
public final class Queue<B extends ProcessBuilder> implements Serializable
{
    // Increase the number when an incompatible change is made
    private static final long serialVersionUID = Queue.class.getName().hashCode() + 1;

    /** log instance */
    private final static Log log = LogFactory.getLog(Queue.class);
    
    /** The parent queue. All queues except ROOT_QUEUE have a parent. */
    private final Queue<? extends ProcessBuilder> parent_queue;

    /** The Queue restriction for Processes owned by this Queue. */
    private final QueueRestriction restriction;

    /** The custom index for this Queue. */
    private final Index index;

    /** The ProcessBuilder class to instantiate for building Processes from this Queue. */
    private final Class<B> process_builder_class;

    /** The BatchProcessServer to instantiate to run the actual Process. */
    private final Class<? extends BatchProcessServer> process_server_class;

    /** The default Occurrence to use for Processes owned by this Queue. */
    private final Occurrence default_occurence;

    /** The default Visibility to use for Processes owned by this Queue. */
    private final Visibility default_visibility;

    /** The default Access to use for Processes owned by this Queue. */
    private final Access default_access;

    /** The default Resilience to use for Processes owned by this Queue. */
    private final Resilience default_resilience;

    /** The default Output to use for Processes owned by this Queue. */
    private final Output default_output;

    private final int deallocate_partition_delay;

    private transient String queue_string = null;

    /** Creates a new instance of QueueBuilder */
    Queue(Queue parent_queue, QueueRestriction restriction, Index index, Class<B> process_builder_class,
        Class<? extends BatchProcessServer> process_server_class, Occurrence default_occurence, Visibility default_visibility,
        Access default_access, Resilience default_resilience, Output default_output, int deallocate_partition_delay)
    {
        this.parent_queue = parent_queue;
        this.restriction = restriction;
        this.index = index;
        this.process_builder_class = process_builder_class;
        this.process_server_class = process_server_class;
        this.default_occurence = default_occurence;
        this.default_visibility = default_visibility;
        this.default_access = default_access;
        this.default_resilience = default_resilience;
        this.default_output = default_output;
        this.deallocate_partition_delay = deallocate_partition_delay;

        setId(this);

        if (log.isDebugEnabled())
            log.debug("Creating Queue:" + new_line + toString());
    }

    /**
     * Get a new ProcessBuilder using the supplied Locale.
     */
    public B newProcessBuilder(Locale locale)
    {
        return newProcessBuilder(new Class[] {Queue.class, Locale.class}, new Object[] {this, locale});
    }

    /**
     * Private method to instantiate the Queues ProcessBuilder. Will ask the parent Queue
     * to instantiate its ProcessBuilder if this Queue doesn't have one.
     */
    private B newProcessBuilder(Class[] param_types, Object[] params)
    {
        if (process_builder_class != null)
        {
            try
            {
                Constructor<B> constructor = process_builder_class.getDeclaredConstructor(param_types);
                return constructor.newInstance(params);
            }
            catch (Exception e)
            {
                throw new QueujException(e);
            }
        }

        if (parent_queue != null)
            return (B)parent_queue.newProcessBuilder(param_types, params);

        // else
        throw new QueujException("No ProcessBuilder exists for Queue");
    }

    /**
     * Gets a new QueueBuilder using this Queue as the parent.
     */
    public QueueBuilder<B> newQueueBuilder()
    {
        return QueueFactory.newQueueBuilder(this, process_builder_class);
    }

    /**
     * Gets a new QueueBuilder using this Queue as the parent.
     */
    public <P extends ProcessBuilder> QueueBuilder<P> newQueueBuilder(Class<P> process_builder_class)
    {
        return QueueFactory.newQueueBuilder(this, process_builder_class);
    }

    /**
     * Instantiate the Queues BatchProcessServer. Will ask the parent Queue
     * to instantiate its BatchProcessServer if this Queue doesn't have one.
     */
    public BatchProcessServer getBatchProcessServer()
    {
        if (process_server_class != null)
        {
            try
            {
                return process_server_class.newInstance();
            }
            catch (Exception e)
            {
                throw new QueujException(e);
            }
        }

        if (parent_queue != null)
            return parent_queue.getBatchProcessServer();

        // else
        throw new QueujException("No BatchProcessServer exists for Queue");
    }

    /**
     * Gets the Queues default Occurrence. Will ask the parent Queue
     * to get its default Occurrence if this Queue doesn't have one.
     */
    Occurrence getDefaultOccurrence()
    {
        if (default_occurence != null)
            return default_occurence;

        if (parent_queue != null)
            return parent_queue.getDefaultOccurrence();

        return null;
    }

    /**
     * Gets the Queues default Visibility. Will ask the parent Queue
     * to get its default Visibility if this Queue doesn't have one.
     */
    Visibility getDefaultVisibility()
    {
        if (default_visibility != null)
            return default_visibility;

        if (parent_queue != null)
            return parent_queue.getDefaultVisibility();

        return null;
    }

    /**
     * Gets the Queues default Access. Will ask the parent Queue
     * to get its default Access if this Queue doesn't have one.
     */
    Access getDefaultAccess()
    {
        if (default_access != null)
            return default_access;

        if (parent_queue != null)
            return parent_queue.getDefaultAccess();

        return null;
    }

    /**
     * Gets the Queues default Resilience. Will ask the parent Queue
     * to get its default Resilience if this Queue doesn't have one.
     */
    Resilience getDefaultResilience()
    {
        if (default_resilience != null)
            return default_resilience;

        if (parent_queue != null)
            return parent_queue.getDefaultResilience();

        return null;
    }

    /**
     * Gets the Queues default Output. Will ask the parent Queue
     * to get its default Output if this Queue doesn't have one.
     */
    Output getDefaultOutput()
    {
        if (default_output != null)
            return default_output;

        if (parent_queue != null)
            return parent_queue.getDefaultOutput();

        return null;
    }

    /**
     * Check with this Queue and the parent Queue whether the supplied Process can run.
     */
    public boolean canRun(Process process)
    {
        boolean can_run = restriction == null ? true : restriction.canRun(process.getQueue(), process);
        if (can_run && parent_queue != null)
            return parent_queue.canRun(process);
        return can_run;
    }

    public boolean hasIndex()
    {
        return index != null;
    }

    public Object getIndexKey(Process process)
    {
        if (index == null)
            return null;
        return index.getKey(process);
    }

    public int getPartitionDeallocateDelay()
    {
        return deallocate_partition_delay;
    }

    private final static String new_line = System.getProperty("line.separator");

    /**
     * Generate a unique String for this queue. Used for uniquely identifying
     * the properties of the Queue.
     */
    @Override
    public String toString()
    {
        if (queue_string != null)
            return queue_string;

        queue_string = "";
        if (parent_queue != null)
            queue_string = parent_queue.getParentString();
        queue_string += "self" + getSelfString();

        return queue_string;
    }
    
   /**
     * Get a short description of queue for debug (standard toString has many lines)  
     */
    public String getShortString()
    {
        return super.toString();
    }
   

    /**
     * Get the unique Strings for the parent Queues.
     */
    private String getParentString()
    {
        if (parent_queue == null)
            return "parent" + getSelfString();
        return parent_queue.getParentString() + "parent" + getSelfString();
    }

    /**
     * Get the unique String for this Queue.
     */
    private String getSelfString()
    {
        String queue_string = " {" + new_line;
        if (restriction != null)
            queue_string += "  restriction: " + restriction.getClass().getName() + new_line;
        if (index != null)
            queue_string += "  index: " + index.getClass().getName() + new_line;
        if (deallocate_partition_delay != 0)
            queue_string += "  deallocate partition delay: " + deallocate_partition_delay + new_line;
        if (process_builder_class != null)
            queue_string += "  process builder: " + process_builder_class.getName() + new_line;
        if (process_server_class != null)
            queue_string += "  process server: " + process_server_class.getName() + new_line;
        if (default_occurence != null)
            queue_string += "  default occurrence: " + default_occurence.toString() + new_line;
        if (default_visibility != null)
            queue_string += "  default visibility: " + default_visibility.getClass().getName() + new_line;
        if (default_access != null)
            queue_string += "  default access: " + default_access.getClass().getName() + new_line;
        if (default_resilience != null)
            queue_string += "  default resilience: " + default_resilience.toString() + new_line;
        if (default_output != null)
            queue_string += "  default output: " + default_output.getClass().toString() + new_line;
        return queue_string + "}" + new_line;
    }

    /** Private static Collection of queues and their id's. */
    private final static FilterableArrayList queues = new FilterableArrayList();

    /**
     * Set the unique id for this Queue.
     */
    private static synchronized int setId(Queue queue)
    {
        String queue_string = queue.toString();
        if (!queues.contains(queue_string))
            queues.add(queue_string);
        return queues.indexOf(queue_string);
    }

    /**
     * Implement readObject to generate the unique id at deserialization.
     */
    private void readObject(ObjectInputStream in)
    throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        setId(this);
    }

    /**
     * Implement equals to use result of toString.
     */
    @Override
    public boolean equals(Object object)
    {
        if (object == null || !(object instanceof Queue))
            return false;

        Queue another = (Queue)object;
        return another.toString().equals(toString());
    }

    /**
     * Implement hashCode to fulfil the contract of Object.
     */
    @Override
    public int hashCode()
    {
        return toString().hashCode();
    }
}
