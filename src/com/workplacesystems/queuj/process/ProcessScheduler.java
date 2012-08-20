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

package com.workplacesystems.queuj.process;

import com.workplacesystems.queuj.utils.QueujException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool.Config;
import com.workplacesystems.queuj.utils.BackgroundProcess;
import com.workplacesystems.utilsj.Callback;
import com.workplacesystems.utilsj.collections.FilterableArrayList;
import com.workplacesystems.utilsj.collections.IterativeCallback;
import com.workplacesystems.utilsj.threadpool.ThreadObjectFactory;
import com.workplacesystems.utilsj.threadpool.ThreadPoolCreator;

/**
 *
 * @author dave
 */
public class ProcessScheduler extends BackgroundProcess
{
    private final static Log log = LogFactory.getLog(ProcessScheduler.class);

    private volatile boolean running = false;

    private volatile boolean do_notify = false;

    private final TreeMap processes = new TreeMap();
    private final TreeMap process_times = new TreeMap(new Comparator() {
        @Override
        public int compare(Object o1, Object o2)
        {
            GregorianCalendar gc1 = (GregorianCalendar)o1;
            GregorianCalendar gc2 = (GregorianCalendar)o2;
            long time1 = gc1.getTime().getTime();
            long time2 = gc2.getTime().getTime();
            return (time1<time2 ? -1 : (time1==time2 ? 0 : 1));
        }
    });

    private final static ThreadPoolCreator pool_creator = new ThreadPoolCreator() {
        public ThreadObjectFactory getThreadObjectFactory()
        {
            return new ThreadObjectFactory() {
                @Override
                public void initialiseThread(Thread thread)
                {
                    thread.setName("LocalProcessScheduler");
                }

                @Override
                public void activateThread(Thread thread)
                {
                    thread.setPriority(Thread.NORM_PRIORITY + 1);
                }

                @Override
                public void passivateThread(Thread thread)
                {
                    thread.setPriority(Thread.NORM_PRIORITY);
                }
            };
        }

        @Override
        public Config getThreadPoolConfig() {
            Config config = new Config();
            config.maxActive = -1; // No upper limit on the number of threads.
            config.minIdle   = 5;  // Always have 5 threads waiting to go.
            config.maxIdle   = 10; // Maximum number of idle threads.
            config.testOnBorrow = false; // Don't test on borrow to improve performance........
            config.testOnReturn = true; // But test on return instead
            config.whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_GROW;
            return config;
        }

        @Override
        public String getThreadPoolName()
        {
            return "ProcessSchedulerPool";
        }
    };

    private final UnParkBackgroundProcess unpark_process = new UnParkBackgroundProcess();

    /**
     * Creates a new instance of LocalProcessScheduler
     */
    public ProcessScheduler()
    {
        super(pool_creator);
        if (log.isDebugEnabled())
        {
            /*StringWriter sw = new StringWriter();
            PrintWriter pr = new PrintWriter(sw);
            Throwable t = new Throwable();
            t.printStackTrace(pr);
            log.debug("Initialised process_scheduler: " + hashCode() + System.getProperty("line.separator") + sw.toString());*/
            log.debug("Initialised process_scheduler: " + hashCode());
        }
    }

    synchronized void scheduleProcess(ProcessWrapper process, GregorianCalendar scheduled_time)
    {
        if (log.isDebugEnabled())
            log.debug("schedule process, scheduled_time: " + scheduled_time.getTime().toString() +
                                      ", process_scheduler: " + hashCode() +
                                      ", runner: " + process.runnerHashCode());

        if (!scheduled_time.after(new GregorianCalendar()))
            scheduled_time = null;

        // Firstly remove any previously scheduled instance of this Process
        unScheduleProcess(process);

        // Now lets schedule the Process
        processes.put(process, scheduled_time);

        if (scheduled_time != null)
        {
            FilterableArrayList list = (FilterableArrayList)process_times.get(scheduled_time);
            if (list == null)
            {
                list = new FilterableArrayList();
                process_times.put(scheduled_time, list);
            }
            list.add(process);

            // Wake the thread if the new time is the first
            if (process_times.firstKey().equals(scheduled_time))
            {
                if (running)
                    interrupt();
                else
                {
                    running = true;
                    start();
                }
            }
        }
    }

    synchronized void unScheduleProcess(ProcessWrapper process)
    {
        unScheduleProcessWrapper(process);
        unpark_process.unQueue(process);
    }

    private synchronized void unScheduleProcessWrapper(ProcessWrapper process)
    {
        log.debug("unSchedule process, process_scheduler: " + hashCode());
        GregorianCalendar previous_time = (GregorianCalendar)processes.remove(process);
        if (previous_time != null)
        {
            FilterableArrayList list = (FilterableArrayList)process_times.get(previous_time);
            if (!list.remove(process))
            {
                throw new QueujException("processes and process_times out of sync! This should not be possible.");
            }

            // Wake the thread if it's waiting on this time
            if (process_times.firstKey().equals(previous_time))
            {
                if (running)
                    interrupt();
                else
                {
                    running = true;
                    start();
                }
            }

            if (list.isEmpty())
                process_times.remove(previous_time);
        }
    }

    boolean notifyAllProcesses(ProcessImplServer ps, final Collection next_runners)
    {
        if (next_runners != null)
        {
            synchronized (unpark_process.run_mutex)
            {
                Object ret = ps.withReadLock(new Callback()
                {
                    @Override
                    protected void doAction()
                    {
                        synchronized (ProcessScheduler.this)
                        {
                            if (processes.isEmpty())
                            {
                                log.debug("No processes to notify, process_scheduler: " + hashCode());
                                _return(Boolean.FALSE);
                            }

                            //else
                            log.debug("Notifying all processes, process_scheduler: " + hashCode());

                            _return(new ArrayList(processes.keySet()));
                        }
                    }
                });

                if (ret != null)
                {
                    if (ret instanceof Collection)
                    {
                        // Outside of syncs
                        unParkProcesses((Collection)ret, next_runners);
                    }

                    if (ret instanceof Boolean)
                        return ((Boolean)ret).booleanValue();
                }

                return true;
            }
        }

        return ((Boolean)ps.withReadLock(new Callback()
        {
            @Override
            protected void doAction()
            {
                synchronized (ProcessScheduler.this)
                {
                    if (processes.isEmpty())
                    {
                        log.debug("No processes to notify, process_scheduler: " + hashCode());
                        _return(Boolean.FALSE);
                    }

                    //else
                    log.debug("Notifying all processes, process_scheduler: " + hashCode());

                    unParkProcesses(processes.keySet(), null);
                    _return(Boolean.TRUE);
                }
            }
        })).booleanValue();
    }

    synchronized void notifyProcess(ProcessWrapper process)
    {
        log.debug("Notifying single process, process_scheduler: " + hashCode());
        if (!processes.containsKey(process))
        {
            //throw new QueujException("Cannot notify process that is not parked");
            return;
        }

        unParkProcess(process);
    }

    synchronized void stopRunning()
    {
        log.debug("Stopping process_scheduler: " + hashCode());
        do_notify = false;
        process_times.clear();
	if (running)
            interrupt();
    }

    @Override
    protected void doRun()
    {
        synchronized (this)
        {
            FilterableArrayList list = null;
            do_notify = false;
            try
            {
                // Check whether we were interrupted while outside of the sync
                if (!interrupted())
                {
                    // If process_times is empty just continue
                    // Use Iterator for performance
                    Iterator i = process_times.keySet().iterator();
                    if (i.hasNext())
                    {
                        // Get the first scheduled time
                        GregorianCalendar  gc_key = (GregorianCalendar)i.next();
                        i = null;

                        // Calculate how long we need to wait if at all and wait
                        long when = gc_key.getTimeInMillis();
                        long now;
                        while ((now = (new GregorianCalendar()).getTimeInMillis()) < when)
                        {
                            long wait_time = when - now;
                            if (log.isDebugEnabled())
                                log.debug("Waiting for " + wait_time);
                            wait(wait_time);
                        }

                        // Remove the current scheduled time and all processes
                        // but keep a local copy (list).
                        list = (FilterableArrayList)process_times.remove(gc_key);
                        if (list != null)
                        {
                            do_notify = true;
                            (new IterativeCallback() {
                                @Override
                                protected void nextObject(Object obj)
                                {
                                    processes.put(obj, null);
                                }
                            }).iterate(list);
                        }
                    }
                }
            }
            catch (InterruptedException ie)
            {
                log.debug("process_scheduler thread InterruptedException: " + hashCode());
            }

            if (do_notify)
                unParkProcesses(list, null);
        }
        log.debug("process_scheduler thread stopped: " + hashCode());
    }

    @Override
    protected boolean continueRunning()
    {
        synchronized (this)
        {
            running = process_times.keySet().iterator().hasNext();
            return running;
        }
    }

    /**
     * Override to prevent exception from being thrown
     */
    @Override
    protected void handleException(QueujException e) {}

    private void unParkProcesses(Collection local_processes, Collection next_runners)
    {
        if (log.isDebugEnabled())
            log.debug("process_scheduler unparking " + local_processes.size() + " processes: " + hashCode());
        if (next_runners != null)
            unpark_process.run(local_processes, next_runners);
        else
            unpark_process.queue(local_processes);
    }

    private void unParkProcess(ProcessWrapper process)
    {
        unpark_process.queue(process);
    }

    private class UnParkBackgroundProcess extends BackgroundProcess
    {
        private final TreeSet<ProcessWrapper> queued_processes = new TreeSet<ProcessWrapper>();
        private final HashMap<String,TreeSet<ProcessWrapper>> groupedByQueue =
                new HashMap<String,TreeSet<ProcessWrapper>>();

        private boolean running = false;

        private final Object run_mutex = new Object();

        private UnParkBackgroundProcess()
        {
            super(pool_creator);
        }

        private ProcessWrapper getHead(TreeSet<ProcessWrapper> processes) {
            ProcessWrapper head = null;
            Iterator<ProcessWrapper> i = processes.iterator();
            if (i.hasNext())
                head = i.next();
            return head;
        }

        private synchronized void adjustQueuedProcesses(boolean add, ProcessWrapper process) {
            if (process.getQueue().hasPredictableRestriction()) {
                TreeSet<ProcessWrapper> processes = groupedByQueue.get(process.getQueue().toString());
                if (processes == null) {
                    processes = new TreeSet<ProcessWrapper>();
                    groupedByQueue.put(process.getQueue().toString(), processes);
                }

                ProcessWrapper head = getHead(processes);

                if (add) {
                    processes.add(process);

                    if (head == null)
                        queued_processes.add(process);
                    else if (head != process && head.compareTo(process) > 0) {
                        queued_processes.remove(head);
                        queued_processes.add(process);
                    }
                }
                else {
                    processes.remove(process);

                    if (head == process) {
                        queued_processes.remove(process);
                        head = getHead(processes);
                        if (head != null)
                            queued_processes.add(head);
                    }
                }
            }
            else {
                if (add)
                    queued_processes.add(process);
                else
                    queued_processes.remove(process);
            }
        }

        private synchronized void queue(ProcessWrapper process)
        {
            log.debug("Unparking processes: " + process.runnerHashCode());

            adjustQueuedProcesses(true, process);

            if (!running)
            {
                running = true;
                start();
            }
        }

        private synchronized void unQueue(ProcessWrapper process)
        {
            log.debug("unQueue parking of process: " + process.runnerHashCode());
            adjustQueuedProcesses(false, process);
        }

        private synchronized void queue(Collection<ProcessWrapper> local_processes)
        {
            if (log.isDebugEnabled())
            {
                String process_hashes = "";
                for (Iterator i = local_processes.iterator(); i.hasNext(); )
                    process_hashes += ((ProcessWrapper)i.next()).runnerHashCode() + ", ";
                log.debug("Unparking processes: " + process_hashes);
            }
            for (ProcessWrapper process : local_processes)
                adjustQueuedProcesses(true, process);

            if (!running)
            {
                running = true;
                start();
            }
        }

        private void run(Collection local_processes, Collection next_runners)
        {
            if (log.isDebugEnabled())
            {
                String process_hashes = "";
                for (Iterator i = local_processes.iterator(); i.hasNext(); )
                    process_hashes += ((ProcessWrapper)i.next()).runnerHashCode() + ", ";
                log.debug("Unparking processes: " + process_hashes);
            }

            FilterableArrayList local_next_runners = new FilterableArrayList();
            ProcessWrapper process = null;
            for (Iterator i = local_processes.iterator(); i.hasNext(); )
            {
                process = (ProcessWrapper)i.next();

                if (process != null)
                    process.unPark(local_next_runners);

                if (!local_next_runners.isEmpty())
                    break;
            }
            next_runners.addAll(local_next_runners);
        }

        @Override
        protected void doRun()
        {
            synchronized (run_mutex)
            {
                ProcessWrapper process = null;
                TreeSet<ProcessWrapper> processes = null;
                synchronized (this)
                {
                    Iterator i = queued_processes.iterator();
                    if (i.hasNext())
                    {
                        process = (ProcessWrapper)i.next();
                        i.remove();
                    }

                    if (process != null)
                        processes = groupedByQueue.remove(process.getQueue().toString());
                }

                if (processes == null) {
                    if (process != null)
                        process.unPark(null);
                }
                else {
                    for (ProcessWrapper process0 : processes) {
                        if (process0 != null) {
                            if (!process0.unPark(null))
                                return;
                        }
                    }
                }
            }
        }

        @Override
        protected boolean continueRunning()
        {
            synchronized (this)
            {
                running = queued_processes.iterator().hasNext();
                return running;
            }
        }

        /**
         * Override to prevent exception from being thrown
         */
        @Override
        protected void handleException(QueujException e) {}
    }
}
