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

package com.workplacesystems.queuj.utils;

import com.workplacesystems.queuj.process.QueujFactory;
import com.workplacesystems.queuj.utils.threadpool.ThreadPool;
import com.workplacesystems.queuj.utils.threadpool.ThreadPoolCreator;
import com.workplacesystems.queuj.utils.threadpool.WorkerThread;
import java.util.Date;
import java.util.HashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class is used to run Background processing within an application.
 *
 * @author  Dave
 */
public abstract class BackgroundProcess implements Runnable
{
    private final static Log log = LogFactory.getLog(BackgroundProcess.class);

    /** The thread of the background process */
    private volatile Thread thread;

    /** The parent BackgroundProcess object. This object does all the locking and controls the thread. */
    private BackgroundProcess parent_process = null;
    
    /** Flag a parent process to run in the current thread. */
    private boolean in_process = false;

    /** The background process thread name */
    private String name;

    /** Mutex to wait on when setting/clearing the thread. */
    private final Object thread_set_mutex = new Object();

    /** The ThreadPool to use */
    private ThreadPool thread_pool;

    /** Mutex to wait on when borrowing a pooled thread. */
    private Object pool_borrow_mutex;

    /** Mutex to wait on when using a pooled thread. */
    private Object pool_mutex;

    /** The time at which the BackgroundProcess began running */
    private Date started_date;

    private final Callback async = QueujFactory.getAsyncCallback(new Callback() {

        @Override
        protected void doAction() {
            if (pool_borrow_mutex != null)
            {
                synchronized (pool_borrow_mutex)
                {
                    pool_borrow_mutex.notify();
                }
            }

            try
            {
                do
                {
                    try
                    {
                        doRun();
                    }
                    catch (Exception e)
                    {
                        QueujException ce = new QueujException(e);
                        BackgroundProcess.this.handleException(ce);
                    }
                }
                while (continueRunning());
            }
            finally
            {
                synchronized (thread_set_mutex)
                {
                    if (Thread.currentThread().equals(thread))
                        thread = null;
                }
            }
        }
    });

    /** Creates a new instance of BackgroundProcess */
    protected BackgroundProcess()
    {
        super();
    }
    
    /** Creates a new instance of BackgroundProcess that will run in the current thread. */
    protected BackgroundProcess(boolean in_process)
    {
        this();
        this.in_process = in_process;
    }
    
    /** Creates a new instance of BackgroundProcess with a parent process which will control the thread and locks */
    protected BackgroundProcess(BackgroundProcess parent_process)
    {
        this();
        setParentProcess(parent_process);
    }

    private final static HashMap thread_pools = new HashMap();

    /** Creates a new instance of BackgroundProcess that will run in the current thread. */
    protected BackgroundProcess(ThreadPoolCreator tp_creator)
    {
        this();
        this.in_process = false;
        this.thread_pool = getThreadPool(tp_creator);
        pool_borrow_mutex = new Object();
        pool_mutex = new Object();
    }

    private synchronized static ThreadPool getThreadPool(ThreadPoolCreator tp_creator)
    {
        String tp_name = tp_creator.getThreadPoolName();
        ThreadPool thread_pool = (ThreadPool)thread_pools.get(tp_name);
        if (thread_pool == null)
        {
            thread_pool = new ThreadPool(tp_creator.getThreadObjectFactory(), tp_creator.getThreadPoolConfig());
            thread_pools.put(tp_name, thread_pool);
        }
        return thread_pool;
    }

    protected final void setParentProcess(BackgroundProcess parent_process)
    {
        if (parent_process.parent_process != null)
            throw new QueujException("BackgroundProcess's must not be chained.");

        this.parent_process = parent_process;
    }

    public final BackgroundProcess getParentProcess()
    {
        return parent_process;
    }

    protected final void setName(String name)
    {
        this.name = name;
    }

    /** start is used to start the processing. run() is called directly if this is a child process 
     * or in a separate thread if this is a parent process. */
    public final Object start()
    {
        return start(Thread.NORM_PRIORITY);
    }
    
    /** start is used to start the processing. run() is called directly if this is a child process 
     * or in a separate thread if this is a parent process. */
    public final Object start(int priority)
    {
        started_date = new Date();

        if (parent_process != null || in_process)
        {
            return_object = null;

            doRun();

            return return_object;
        }

        if (thread_pool != null)
        {
            try
            {
                synchronized (thread_set_mutex)
                {
                    thread = (Thread)thread_pool.borrowObject();

                    synchronized (pool_borrow_mutex)
                    {
                        ((WorkerThread)thread).execute(this, pool_mutex);
                        try {
                            pool_borrow_mutex.wait();
                        }
                        catch (InterruptedException ie) {}
                    }
                }

            }
            catch (Exception e)
            {
                throw new QueujException(e);
            }
        }
        else
        {
            // Do not check that the Thread is null first.
            synchronized (thread_set_mutex)
            {
                thread = new Thread(this);
            }
            if (name == null)
                thread.setName("BackgroundProcess - " + thread.getName());
            else
                thread.setName (name);
            thread.setPriority(priority);
            thread.setDaemon(true);
            thread.start();
        }

        return null;
    }

    private Object return_object;

    protected final void _return(Object o)
    {
        return_object = o;
    }

    /**
     * Inactivively waits for the process to finish or until timeout occurs,
     * whichever is earlier. If the process was not running, returns immediatelly.
     * 
     * @param timeout
     */
    public void joinThread(long timeout)
    {
        // take local ref copy, so can reliably check if ref is null,
        Thread local_thread_ref = thread;
        if (local_thread_ref != null)
        {
            try 
            {
                long millis = 0l;
                if (log.isDebugEnabled())
                {
                    log.debug("BackgroundProcess.joinThread() Waiting " + timeout + " millis for " + this + " to finish");
                    millis = System.currentTimeMillis();
                }
                if (thread_pool != null)
                {
                    synchronized (pool_mutex)
                    {
                        pool_mutex.wait(timeout);
                    }
                }
                else
                    local_thread_ref.join(timeout);

                if (log.isDebugEnabled())
                {
                    millis = System.currentTimeMillis() - millis;
                    log.debug("BackgroundProcess.joinThread() Process " + this + " finished in " + millis + " millis");
                }
            } 
            catch (InterruptedException e) 
            {
                if (log.isDebugEnabled())
                    log.debug("BackgroundProcess.joinThread() Timed out waiting " + timeout + " millis for " + this + " to finish");
            }
        }
        else
        {
            if (log.isDebugEnabled())
                log.debug("BackgroundProcess.joinThread() " + name + " was not running, thread was null");
        }
    }

    protected final boolean isRunning()
    {
        return thread != null;
    }

    /** either the standard run() method for a thread, or if in process, just calls it */
    public final void run()
    {
        async.doAction();
    }
    
    /** must be overridden by subclasses to perform the actual processing */
    protected abstract void doRun();

    /**
     * Override this method to loop the run of the thread. It is essential to use
     * this method to continue a thread pool loop as the lock check
     * is done within the loop and cannot therefore be affected by a subsequent
     * thread running.
     * 
     * @return
     */
    protected boolean continueRunning()
    {
        return false;
    }

    /**
     * Override this method to handle the loop ending by way of a
     * QueujException.
     */
    protected void handleException(QueujException e)
    {
        throw e;
    }

    /**
     * wait for a number of seconds (nicer than microseconds!)
     * Be aware that wait relinquishes its monitors
     */
    protected final void doWait(double seconds)
    {
        try
        {
            long ms = (long)(seconds * 1000l);
            synchronized (this)
            {
                wait(ms);
            }
        }
        catch (InterruptedException ie)
        {
            log.debug("BackgroundProcess thread interuptted.");
        }
    }
    
    /**
     * sleep for a number of seconds (nicer than microseconds!)
     * sleep does not relinquish its monitors
     */
    protected final void doSleep(double seconds)
    {
        try
        {
            long ms = (long)(seconds * 1000l);
            Thread.sleep(ms);
        }
        catch (InterruptedException ie)
        {
            log.debug("BackgroundProcess thread interuptted.");
        }
    }

    protected final synchronized void interrupt()
    {
        if (thread != null)
            thread.interrupt();
    }
    
    protected final boolean interrupted()
    {
        return Thread.interrupted();
    }

    protected boolean isInProcess()
    {
        return in_process;
    }
}
