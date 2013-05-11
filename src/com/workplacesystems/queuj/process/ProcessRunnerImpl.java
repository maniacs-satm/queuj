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

import com.workplacesystems.queuj.utils.BackgroundProcess;
import com.workplacesystems.queuj.utils.QueujException;
import com.workplacesystems.utilsj.Callback;
import java.util.Collection;
import java.util.GregorianCalendar;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author dave
 */
public class ProcessRunnerImpl extends BackgroundProcess implements ProcessRunner {

    private final static Log log = LogFactory.getLog(ProcessRunnerImpl.class);

    protected final ProcessWrapper process;
    private final GregorianCalendar runTime;
    private final boolean failed;
    private boolean used = false;

    private volatile boolean started = false;

    private volatile boolean parked = false;

    private volatile boolean interrupted = false;

    private GregorianCalendar nextRun = null;

    protected ProcessRunnerImpl(ProcessWrapper process, GregorianCalendar runTime, boolean failed) {
        this.process = process;
        this.runTime = runTime;
        this.failed = failed;
        initialise();
    }

    protected final ProcessWrapper getProcess() {
        return process;
    }

    public boolean isSleeping()
    {
        return parked;
    }

    public void doStart() {
        park();
        doNotify();
    }

    /**
     * Currently does nothing and can't safely stop the thread (See http://docs.oracle.com/javase/7/docs/api/java/lang/Thread.html#stop()).
     * But custom implementors may want to override this method to clear resources or set a 'stopping' flag for the thread to check.
     */
    public void stop() {}

    public synchronized void doNotify() {
        if (parked)
            process.getContainingServer().getProcessScheduler().notifyProcess(process);
        else
            notify();
    }

    public synchronized void doInterrupt() {
        if (!interrupted)
        {
            log.debug("Interrupting runner: " + hashCode());
            interrupted = true;
            doNotify();
        }
    }

    private void park() {
        log.debug("Parking runner: " + hashCode());
        parked = true;
        process.getContainingServer().getProcessScheduler().scheduleProcess(process, runTime);
    }

    boolean unPark(Collection<ProcessRunner> next_runners) {
        try {
            initialiseUnpark();

            if (used)
                return false;

            if (!parked) {
                new QueujException("Can't unPark a runner that is not parked?!?! ProcessId: "  + process.getProcessKey());
                return false;
            }

            if (interrupted) {
                // Get another thread to deal with this if next_runners != null
                if (next_runners != null) {
                    doNotify();
                    return false;
                }

                log.debug("Runner interrupted in unPark(): " + hashCode());
                process.getContainingServer().getProcessScheduler().unScheduleProcess(process);
                doFinally();
            }
            else {
                if (!process.isNotRun() && !process.isFailed() && !process.isDeleted()) {
                    try {
                        attemptProcessPessimisticLock();
                        if (!process.isDeleted()) // Double check
                            process.updateNotRun();
                    }
                    catch (Exception e) {} // Ignore
                    finally {
                        releaseProcessPessimisticLock();
                    }
                }

                try {
                    preStart();

                    ProcessServer ps = process.getContainingServer();
                    synchronized (ps.getMutex()) {
                        log.debug("Checking unpark for runner: " + hashCode());
                        boolean can_unpark = canRunProcess();
                        boolean pending_delete = process.isDeleted();

                        if (can_unpark || pending_delete) {
                            log.debug("UnParking runner (pending_delete==" + (pending_delete ? "true" : "false") + "): " + hashCode());
                            ps.getProcessScheduler().unScheduleProcess(process);
                            if (!pending_delete) {
                                used = true;
                                if (next_runners == null)
                                    start();
                                else
                                    next_runners.add(this);

                                return true;
                            }
                            else {
                                interrupted = false;
                                doFinally();
                            }
                        }
                        else {
                            if (!process.isNotRun() && !process.isFailed()) // Because we couldn't get the lock
                                doNotify();
                        }
                    }
                }
                catch (Exception e) {
                    new QueujException(e);
                    try {
                        process.getContainingServer().getProcessScheduler().unScheduleProcess(process);
                        process.updateRunError();
                        nextRun = process.getNextRunTime();
                    }
                    finally {
                        doFinally();
                    }
                }
            }
            return false;
        }
        finally {
            finaliseUnpark();
        }
    }

    protected void initialise() {}
    protected void initialiseRun() {}
    protected void initialiseUnpark() {}
    protected void finaliseUnpark() {}
    protected void waitForProcessPessimisticLock() {}
    protected void waitForProcessExclusivePessimisticLock() {}
    protected void waitForOtherPessimisticLocks() {}
    protected void attemptProcessPessimisticLock() {}
    protected void releaseProcessPessimisticLock() {}
    protected void releasePessimisticLocks() {}
    protected void preStart() {}
    protected void postStart() {}
    protected void postFinish() {}

    protected void setStarted(boolean started) {
        this.started = started;
        if (!started)
            process.getContainingServer().notifyQueue();
    }

    protected boolean isStarted() {
        return started;
    }

    @Override
    protected void doRun() {
        log.debug("Starting runner: " + hashCode());

        initialiseRun();

        // Firstly take a local copy of the parked status for use in if statements
        boolean local_parked = parked;
        try
        {
            if (local_parked)
            {
                // Update class and local parked status
                parked = false;
                local_parked = false;
            }
            else
                throw new QueujException("start called on LocalProcessRunner directly.");

            if (!process.isDeleted())
            {
                waitForProcessPessimisticLock();

                if (interrupted)
                {
                    log.debug("Runner interrupted when about to run: " + hashCode());

                    if (!process.isDeleted())
                        process.updateNotRun();
                    return;
                }

                process.setupOutputFile();

                waitForOtherPessimisticLocks();

                process.updateRunning(runTime);

                postStart();

                process.runProcess(this, failed);

                postFinish();

                // If job didn't fail update its status to complete and delete from the queue
                if (process.isFailed()) {
                    nextRun = process.getNextRunTime();
                }
                else {
                    QueujTransaction transaction = QueujFactory.getTransaction();
                    transaction.doTransaction(process, new Callback<Void>() {

                        @Override
                        protected void doAction() {
                            // Must update as other threads may have a reference to it
                            process.updateComplete();

                            nextRun = process.getNextRunTime();

                            // if ok, remove it from queue and continue
                            // if failed, leave in queue (so can be displayed) but suspend the controller
                            if (nextRun == null && !process.keepCompleted()) {
                                waitForProcessExclusivePessimisticLock();
                                process.getContainingServer().writeLocked(new Callback() {
                                    @Override
                                    protected void doAction() {
                                        synchronized (ProcessRunnerImpl.this) {
                                            process.delete();
                                        }
                                    }
                                });
                            }
                        }
                    }, false);
                }
            }
        }
        catch (final ForceRescheduleException fre)
        {
            process.updateOccurrence(fre.getNewOccurence(process.getProcessEntity().getOccurrence()));
            nextRun = process.getNextRunTime();
        }
        catch (Exception e)
        {
            new QueujException(e);
        }
        finally
        {
            releasePessimisticLocks();

            // Only check local parked status in case a second thread is started concurrently
            if (local_parked)
            {
                log.debug("Don't call finally as local_parked==true for runner: " + hashCode());
            }
            else
                doFinally();
            log.debug("Stopping runner: " + hashCode());
        }
    }

    protected void doFinally() {
        synchronized (process.mutex)
        {
            log.debug("Clearing runner: " + hashCode());
            process.processRunner = null;

            process.mutex.notifyAll();
        }

        // If another run is due because of failure or schedule then start it again
        if (nextRun != null || interrupted)
        {
            if (log.isDebugEnabled())
                log.debug("Runner about to call ProcessImpl.start() (interrupted==" + (interrupted ? "true" : "false") + "): " + hashCode());
            process.start();
        }

        // Sync'ed just in case multiple threads call doFinally. Should never happen!
        synchronized (this) {
            setStarted(false);
        }
    }

    private boolean canRunProcess()
    {
        synchronized (this) {
            if (isStarted())
                return true;
        }

        GregorianCalendar currentTime = new GregorianCalendar();
        boolean can_run = process.canRun(currentTime) && !runTime.after(currentTime);
        if (!can_run || process.isDeleted())
            return false;

        synchronized (this) {
            setStarted(true);

            if (!isStarted())
                return false;
        }

        waitForProcessExclusivePessimisticLock();
        process.updateLocked();
        releasePessimisticLocks();

        return true;
    }
}
