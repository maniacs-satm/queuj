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

import com.workplacesystems.queuj.process.ProcessServer;
import com.workplacesystems.queuj.process.ProcessWrapper;
import com.workplacesystems.utilsj.Callback;
import java.util.Locale;

import com.workplacesystems.queuj.process.SequencedProcess;
import com.workplacesystems.queuj.process.QueujFactory;
import com.workplacesystems.queuj.process.QueujTransaction;
import com.workplacesystems.queuj.utils.User;
import com.workplacesystems.utilsj.collections.FilterableArrayList;
import java.io.Serializable;
import java.util.HashMap;

/**
 * ProcessBuilder is provided by a Queue to allow for the easy creation
 * of Processes with methods for setting the required and optional
 * parameters. The Queue may provide an instance of this class or a
 * subclass which may provide further methods for creating instances
 * of different types of Processes.
 *
 * Current subclasses are:
 * CLIProcessBuilder which provides a method for setting command line
 * parameters.
 * XSLProcessBuilder which provides a method for setting the XSLProvider.
 * JavaProcessBuilder which provides a method for setting the parameter types,
 * parameters, class and run object used for reflection.
 *
 * An implementation of ProcessBuilder is NOT guaranteed to behave as per the specification if it is
 * used concurrently by two or more threads. It is recommended to have one instance of the
 * ProcessBuilder per thread or it is upto the application to make sure about the use of ProcessBuilder
 * from more than one thread.
 *
 * @author dave
 */
public class ProcessBuilder
{
    /** The Queue that provided this ProcessBuilder. Used for getting various defaults. */
    private Queue queue;

    /** The Locale of the user that is creating the Process */
    private Locale locale;

    /** The process name. */
    private String process_name;

    /** A description of the process. */
    private String process_description;

    /** The Process output type. Defaults to null. */
    private String report_type = null;

    /** The user. */
    private User user = null;

    /** The partition that the Process will be owned by. */
    private QueueOwner partition;

    /** A collection of post processes. These are run after the completion (successful or otherwise) of the process. */
    private FilterableArrayList<SequencedProcess> post_processes = new FilterableArrayList<SequencedProcess>();
    
    /** A collection of pre processes. These are run before the main process. */
    private FilterableArrayList<SequencedProcess> pre_processes = new FilterableArrayList<SequencedProcess>();

    /** The occurrence of the process to be created. */
    private Occurrence occurrence;

    /** The visibility of the process to be created. */
    private Visibility visibility;

    /** The access of the process to be created. */
    private Access access;

    /** The resilience of the process to be created. */
    private Resilience resilience;

    /** The output of the process to be created. */
    private Output output;

    /** The persistence of the process to be created. */
    private boolean is_persistent = true;

    /** Should completed Processes without a further run time be deleted or kept. */
    private boolean keep_completed = false;

    private String parent_key = null;
    
    protected boolean trace = false;

    private HashMap<String,Serializable> implementation_options = new HashMap<String, Serializable>();

    /** The page name. */
    private String source_name;

    private ProcessBuilder() {}

    /**
     * Creates a new instance of ProcessBuilder using the supplied
     * Queue and Locale.
     */
    protected ProcessBuilder(Queue queue, Locale locale)
    {
        this();

        // Throw if queue or locale are null. Saves throwing later.
        if (queue == null)
            throw new NullPointerException();
        if (locale == null)
            throw new NullPointerException();

        this.queue = queue;
        this.locale = locale;

        // Set the Processes occurrence, visibility and resilience from
        // the defaults provided by the queue.
        setProcessOccurrence(queue.getDefaultOccurrence());
        setProcessVisibility(queue.getDefaultVisibility());
        setProcessAccess(queue.getDefaultAccess());
        setProcessResilience(queue.getDefaultResilience());
        setProcessOutput(queue.getDefaultOutput());
        QueujFactory.setDefaultImplOptions(implementation_options);
        implementation_options.putAll(queue.getImplementationOptions());
    }

    /**
     * Set the Process name.
     */
    public void setProcessName(String process_name)
    {
        this.process_name = process_name;
    }

    /**
     * Set the Process description.
     */
    public void setProcessDescription(String process_description)
    {
        this.process_description = process_description;
    }
    
    /** Set the parent key*/
    public void setParentKey(String parent_key)
    {
        this.parent_key = parent_key;
    }
    
    public void setTrace(boolean trace)
    {
        this.trace = trace;
    }
    
    /**
     * Get the Process description.
     */
    protected String getProcessDescription()
    {
        return process_description;
    }

    /**
     * Set the Process Occurrence.
     */
    public void setProcessOccurrence(Occurrence occurrence)
    {
        this.occurrence = occurrence;
    }

    /**
     * Set the Process Visibility.
     */
    public void setProcessVisibility(Visibility visibility)
    {
        this.visibility = visibility;
    }

    /**
     * Set the Process Access.
     */
    public void setProcessAccess(Access access)
    {
        this.access = access;
    }

    /**
     * Set the Process Resilience.
     */
    public void setProcessResilience(Resilience resilience)
    {
        this.resilience = resilience;
    }

    /**
     * Set the Process Output.
     */
    public void setProcessOutput(Output output)
    {
        this.output = output;
    }

    /**
     * Set the user that is creating the Process.
     */
    public void setUser(User user)
    {
        this.user = user;
    }

    /**
     * Set the Process output type.
     */
    public void setReportType(String report_type)
    {
        this.report_type = report_type;
    }

    /**
     * Set the QueueOwner that will own the Process.
     */
    public void setPartition(QueueOwner partition)
    {
        this.partition = partition;
    }

    public QueueOwner getPartition()
    {
        return partition;
    }
    
    /**
     * Get the user that is creating this Process.
     */
    public User getUser()
    {
        return user;
    }

    /**
     * Set a PostProcess to be run after the Process completes.
     * Clears all currently set PostProcesses.
     */
    public void setPostProcess(SequencedProcess post_process)
    {
        post_processes = new FilterableArrayList<SequencedProcess>();
        addPostProcess(post_process);
    }

    /**
     * Adds a PreProcess to be run before the Process starts.
     */
    public void addPreProcess(SequencedProcess pre_process)
    {
        pre_processes.add(pre_process);
    }
    
    /**
     * Set a PreProcess to be run before the Process starts.
     * Clears all currently set PreProcesses.
     */
    public void setPreProcess(SequencedProcess pre_process)
    {
        pre_processes = new FilterableArrayList<SequencedProcess>();
        addPreProcess(pre_process);
    }

    /**
     * Adds a PostProcess to be run after the Process completes.
     */
    public void addPostProcess(SequencedProcess post_process)
    {
        post_processes.add(post_process);
    }
    

    /**
     * Get the Locale.
     */
    protected Locale getLocale()
    {
        return locale;
    }
    
    /** override previously set locale - this is required since when created within a transaction, locale is taken
     *  from the transaction, but for integration

    /**
     * Set whether the Process should persist between server restarts.
     */
    public void setProcessPersistence(boolean is_persistent)
    {
        this.is_persistent = is_persistent;
    }

    /**
     * Set whether the Process should be kept or deleted on completion.
     */
    public void setProcessKeepCompleted(boolean keep_completed)
    {
        this.keep_completed = keep_completed;
    }

    /**
     * Set options to be used directly by the queuj implementation
     */
    public void setImplementationOption(String option_key, Serializable option_value)
    {
        implementation_options.put(option_key, option_value);
    }

    /**
     * Creates the Process using the parameters that have been set.
     */
    public Process newProcess()
    {
        Callback<ProcessWrapper> callback = new Callback<ProcessWrapper>() {

            @Override
            protected void doAction() {
                ProcessWrapper process = ProcessWrapper.getNewInstance(
                        partition == null ? null : partition.getQueueOwnerKey(), is_persistent, implementation_options);
                process.setDetails(process_name, queue, process_description, user,
                    occurrence, visibility, access, resilience, output,
                    pre_processes, post_processes, keep_completed, locale,
                    implementation_options);

                if (report_type != null)
                    process.setReportType(report_type);

                if(source_name != null)
                    process.setSourceName(source_name);

                setupProcess(new Process(process));

                preSubmitBatchJobProcessing();

                process.getContainingServer().submitProcess(process);

                _return(process);
            }
        };

        QueujTransaction transaction = QueujFactory.getTransaction();
        return new Process(transaction.doTransaction(is_persistent, callback, true));
    }

    /**
     * Allows subclasses to set their local parameters into the Process.
     */
    protected void setupProcess(Process process) {}
    
    /**
     * Allows subclasses a last chance to modify additional database information before
     * job is sumbitted to the batch queue.
     */
    protected void preSubmitBatchJobProcessing() {}

    public String getProcessName()
    {
        return process_name;
    }

    public void setSourceName(String source_name)
    {
        this.source_name = source_name;
    }
}
