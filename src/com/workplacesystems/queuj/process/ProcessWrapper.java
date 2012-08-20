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

import com.workplacesystems.queuj.Access;
import com.workplacesystems.queuj.Occurrence;
import com.workplacesystems.queuj.Output;
import com.workplacesystems.queuj.Process;
import com.workplacesystems.queuj.ProcessServer;
import com.workplacesystems.queuj.Queue;
import com.workplacesystems.queuj.Resilience;
import com.workplacesystems.queuj.Visibility;
import com.workplacesystems.queuj.process.jpa.ProcessImpl;
import com.workplacesystems.queuj.process.jpa.ProcessImpl.Status;
import com.workplacesystems.queuj.process.seam.ProcessImplHome;
import com.workplacesystems.utilsj.Callback;
import com.workplacesystems.queuj.utils.QueujException;
import com.workplacesystems.queuj.utils.User;
import com.workplacesystems.utilsj.collections.FilterableArrayList;
import com.workplacesystems.utilsj.collections.IterativeCallback;
import java.io.Serializable;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.UUID;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author dave
 */
public class ProcessWrapper implements Comparable<ProcessWrapper> {

    private final static Log log = LogFactory.getLog(ProcessWrapper.class);

    private String queueOwner;

    private ProcessImpl process;

    private boolean isPersistent;

    private boolean deleted = false;

    private static int nextProcessId = Integer.MAX_VALUE;

    private ProcessWrapper(String queueOwner, ProcessImpl process, boolean isPersistent) {
        this.queueOwner = queueOwner;
        this.process = process;
        this.isPersistent = isPersistent;
    }

    static ProcessWrapper getNewInstance(String queueOwner, ProcessImpl process, boolean isPersistent) {
        ProcessWrapper wrapper = new ProcessWrapper(queueOwner, process, isPersistent);
        return wrapper;
    }

    static ProcessWrapper getNewInstance(String queueOwner, boolean isPersistent) {
        ProcessWrapper processWrapper;
        if (isPersistent) {
            ProcessPersistence<ProcessImpl> processHome = QueujFactory.getPersistence();
            processHome.clearInstance();
            ProcessImpl process = processHome.getInstance();
            processWrapper = getNewInstance(queueOwner, process, true);
        }
        else {
            ProcessImpl process = new ProcessImpl();
            process.setProcessId(getNextProcessId());
            processWrapper = getNewInstance(queueOwner, process, false);
        }
        return processWrapper;
    }

    private static synchronized int getNextProcessId() {
        return nextProcessId--;
    }

    private ProcessPersistence<ProcessImpl> getProcessPersistence() {
        if (!isPersistent)
            return null;
        ProcessPersistence<ProcessImpl> processHome = QueujFactory.getPersistence();
        processHome.clearInstance();
        processHome.setId(process.getProcessId());
        this.process = processHome.getInstance();
        return processHome;
    }

    public ProcessServer getContainingServer() {
        return QueujFactory.getProcessServer(queueOwner);
    }

    public boolean isAddedToServer() {
        return ((ProcessImplServer)getContainingServer()).contains(this);
    }

    public boolean isPersistent() { return isPersistent; }

    public void setDetails(String process_name, Queue queue, String process_description, User user,
            Occurrence occurrence, Visibility visibility, Access access, Resilience resilience, Output output,
            FilterableArrayList<? extends SequencedProcess> pre_processes, FilterableArrayList<? extends SequencedProcess> post_processes,
            boolean keep_completed, Locale locale) {
        ProcessPersistence<ProcessImpl> processHome = getProcessPersistence();
        process.setProcessName(process_name);
        if (queueOwner != null)
            process.setQueueOwnerId(queueOwner);
        process.setUUID(UUID.randomUUID().toString());
        process.setQueue(queue);
        process.setDescription(process_description);
        if (user != null)
            process.setUserId(user.getUserId());
        process.setOccurrence(occurrence);
        process.setVisibility(visibility);
        process.setAccess(access);
        process.setResilience(resilience);
        process.setOutput(output);
        process.setKeepCompleted(keep_completed);
        process.setLocale(locale);
        process.setStatus(Status.NOT_RUN);

        process.setCreationTimestamp(new Timestamp((new Date()).getTime()));

        ProcessParameters parameters = new ProcessParameters();
        parameters.setValue(ProcessParameters.POST_PROCESSES, post_processes);
        parameters.setValue(ProcessParameters.PRE_PROCESSES, pre_processes);
        process.setParameters(parameters);

        if (isPersistent) processHome.persist();
    }

    public void setReportType(String report_type) {
    }

    public void setSourceName(String source_name) {
    }

    public Serializable setParameter(String parameterName, Serializable parameterValue) {
        ProcessParameters parameters = getParameters();
        Serializable ret = parameters.setValue(parameterName, parameterValue);
        process.setParameters(parameters);
        return ret;
    }

    //-------------------------------------------- Get job details --------------------------------------------

    public Queue getQueue() {
        return process.getQueue();
    }

    public GregorianCalendar getNextRunTime() {
        Date initialStartDate = process.getScheduledTimestamp() == null ? process.getCreationTimestamp() : process.getScheduledTimestamp();
        GregorianCalendar initialStart = new GregorianCalendar();
        initialStart.setTime(initialStartDate);
        return process.getResilience().getNextRunTime(
                process.getOccurrence(), isFailed(), process.getAttempt(), initialStart, process.getRunCount());
    }

    public boolean isNotRun() {
        return process.getStatus().equals(Status.NOT_RUN);
    }

    boolean isRunning() {
        return process.getStatus().equals(Status.RUNNING);
    }

    boolean isWaitingToRun() {
        return process.getStatus().equals(Status.LOCKED);
    }

    private boolean isComplete() {
        return process.getStatus().equals(Status.RUN_OK);
    }

    boolean isRunError() {
        return process.getStatus().equals(Status.RUN_ERROR);
    }

    public boolean isFailed() {
        return process.getResultCode() > 0 || isRunError() ||
            (((isRunning() || isWaitingToRun()) && processRunner == null));
    }

    public boolean isDueImminently(GregorianCalendar dueTime)
    {
        if (isComplete() || isDeleted())
            return false;

        GregorianCalendar nextRun = getNextRunTime();
        if (nextRun == null) // No run is due
            return false;

        if (nextRun.after(dueTime))
            return false;

        return true;
    }

    boolean isDeleted() {
        return deleted;
    }

    Date getCreationTimestamp() {
        return process.getCreationTimestamp();
    }

    public String getProcessName() {
        return process.getProcessName();
    }

    public String getDescription() {
        return process.getDescription();
    }

    public String getStartedTimestamp() {
        return (new SimpleDateFormat("HH:mm:ss EEE, d MMM yyyy")).format(process.getStartedTimestamp());
    }

    public String getStatusDescription() {
        return process.getStatus().getDescription();
    }

    public String getStatusImg() {
        if (isFailed())
            return "job_failed";

        if (isRunning())
            return "job_running";

        return null;
    }

    public String getQueueOwner() {
        return queueOwner;
    }

    public Integer getProcessKey() {
        return process.getProcessId();
    }

    public Integer getProcessVersion() {
        return process.getVersion();
    }

    boolean keepCompleted() {
        return process.isKeepCompleted();
    }

    int getResultCode() {
        return process.getResultCode();
    }

    public ProcessParameters getParameters() {
        return process.getParameters();
    }

    public Serializable getParameter(String parameterName) {
        return getParameters().getValue(parameterName);
    }

    @Override
    public int compareTo(ProcessWrapper otherProcess) {

        if (getCreationTimestamp().before(otherProcess.getCreationTimestamp()))
            return -1;

        if (getCreationTimestamp().after(otherProcess.getCreationTimestamp()))
            return 1;

        int cmp = getProcessName().compareTo(otherProcess.getProcessName());
        if (cmp != 0)
            return cmp;

        Integer id1 = getProcessKey();
        Integer id2 = otherProcess.getProcessKey();
        cmp = id1.getClass().toString().compareTo(id2.getClass().toString());
        if (cmp != 0)
            return cmp;

        return id1.compareTo(id2);
    }

    boolean canRun(GregorianCalendar currentTime) {
        String debug_id = "";
        if (log.isDebugEnabled())
        {
            // Lets get the process details as a readable String
            debug_id = "Process '" + getProcessKey() + ", " + getProcessName() + "'";
            // Log the start of canRun
            log.debug("Checking whether " + debug_id + " can run.");
        }

        // Now ask the Queue if we're ok to run
        if (!getQueue().canRun(new Process(this)))
        {
            if (log.isDebugEnabled())
                log.debug("Queue for " + debug_id + " reports that it cannot run.");
            return false;
        }
        return true;
    }

    public boolean canDelete(User user) {
        return process.getAccess().canDelete(new Process(this), user, queueOwner);
    }

    public boolean canRestart(User user) {
        return process.getAccess().canRestart(new Process(this), user, queueOwner);
    }

    //-------------------------------------------- Update job details/status --------------------------------------------

    void updateRunErrorAndRestart() {
        updateRunError();
        if (updateRestart())
            restart0();
    }

    void updateRunError() {
        doTransaction(new Callback() {

            @Override
            protected void doAction() {
                ((ProcessImplServer)getContainingServer()).removeProcessFromIndex(ProcessWrapper.this);
                ProcessPersistence<ProcessImpl> processHome = getProcessPersistence();
                log.debug("ProcessWrapper.updateRunError called for " + process.getProcessId() + "(" + process.getVersion() + ")");
                process.setStatus(Status.RUN_ERROR);
                process.setAttempt(process.getAttempt() + 1);
                if (isPersistent) processHome.update();
                ((ProcessImplServer)getContainingServer()).addProcessToIndex(ProcessWrapper.this);
                _return(ProcessWrapper.this);
            }
        });
    }

    boolean updateRestart() {
        if (isFailed())
        {
            doTransaction(new Callback() {

                @Override
                protected void doAction() {
                    ((ProcessImplServer)getContainingServer()).removeProcessFromIndex(ProcessWrapper.this);

                    ProcessPersistence<ProcessImpl> processHome = getProcessPersistence();
                    log.debug("ProcessWrapper.updateRestart called for " + process.getProcessId() + "(" + process.getVersion() + ")");
                    process.setStatus(Status.RESTARTED);
                    process.setResultCode(0);
                    if (isPersistent) processHome.update();
                    _return(ProcessWrapper.this);
                }
            });
            return true;
        }
        return false;
    }

    void updateRunning(final GregorianCalendar runTime) {
        doTransaction(new Callback() {

            @Override
            protected void doAction() {
                ((ProcessImplServer)getContainingServer()).removeProcessFromIndex(ProcessWrapper.this);
                ProcessPersistence<ProcessImpl> processHome = getProcessPersistence();
                log.debug("ProcessWrapper.updateRunning called for " + process.getProcessId() + "(" + process.getVersion() + ")");
                process.setStatus(Status.RUNNING);
                process.setResultCode(0);

                process.setStartedTimestamp(new Date());
                process.setScheduledTimestamp(runTime.getTime());
                if (isPersistent) processHome.update();

                ((ProcessImplServer)getContainingServer()).addProcessToIndex(ProcessWrapper.this);
                _return(ProcessWrapper.this);
            }
        });
    }

    void updateNotRun() {
        doTransaction(new Callback() {

            @Override
            protected void doAction() {
                ((ProcessImplServer)getContainingServer()).removeProcessFromIndex(ProcessWrapper.this);
                ProcessPersistence<ProcessImpl> processHome = getProcessPersistence();
                log.debug("ProcessWrapper.updateNotRun called for " + process.getProcessId() + "(" + process.getVersion() + ")");
                process.setStatus(Status.NOT_RUN);
                process.setResultCode(0);
                if (isPersistent) processHome.update();
                ((ProcessImplServer)getContainingServer()).addProcessToIndex(ProcessWrapper.this);
                _return(ProcessWrapper.this);
            }
        });
    }

    void updateLocked() {
        doTransaction(new Callback() {

            @Override
            protected void doAction() {
                ((ProcessImplServer)getContainingServer()).removeProcessFromIndex(ProcessWrapper.this);
                ProcessPersistence<ProcessImpl> processHome = getProcessPersistence();
                log.debug("ProcessWrapper.updateLocked called for " + process.getProcessId() + "(" + process.getVersion() + ")");
                process.setStatus(Status.LOCKED);
                process.setResultCode(0);
                if (isPersistent) processHome.update();
                ((ProcessImplServer)getContainingServer()).addProcessToIndex(ProcessWrapper.this);
                _return(ProcessWrapper.this);
            }
        });
    }

    void updateComplete() {
        doTransaction(new Callback() {

            @Override
            protected void doAction() {
                ((ProcessImplServer)getContainingServer()).removeProcessFromIndex(ProcessWrapper.this);
                ProcessPersistence<ProcessImpl> processHome = getProcessPersistence();
                log.debug("ProcessWrapper.updateComplete called for " + process.getProcessId() + "(" + process.getVersion() + ")");
                process.setStatus(Status.RUN_OK);
                process.setRunCount(process.getRunCount() + 1);
                process.setAttempt(0);
                if (isPersistent) processHome.update();
                _return(ProcessWrapper.this);
            }
        });
    }

    public boolean delete(User user) {
        if (deleted)
            return false;

        if (!canDelete(user))
            return false;

        return delete();
    }

    public boolean delete() {
        if (deleted)
            return false;

        doTransaction(new Callback() {

            @Override
            protected void doAction() {
                ProcessPersistence<ProcessImpl> processHome = getProcessPersistence();
                log.debug("ProcessWrapper.delete called for " + process.getProcessId() + "(" + process.getVersion() + ")");
                if (isPersistent) processHome.remove();
                ((ProcessImplServer)getContainingServer()).delete(ProcessWrapper.this);
                deleted = true;
                _return(ProcessWrapper.this);
            }
        });

        return true;
    }

    public boolean restart(User user) {
        if (deleted)
            return false;

        if (!canRestart(user))
            return false;

        return restart();
    }

    public boolean restart() {
        if (deleted)
            return false;

        return restart0();
    }

    void setupOutputFile() {} // To be implemented

    boolean unPark(FilterableArrayList local_next_runners) {
        ProcessRunner _processRunner = processRunner;
        if (_processRunner != null)
            return _processRunner.unPark(local_next_runners);
        return false;
    }

    private void doTransaction(Callback callback) {
        try {
            QueujTransaction transaction = QueujFactory.getTransaction();
            transaction.doTransaction(this, callback, false);
        }
        catch (ForceRollbackException fre) {} // Catch ForceRollbackException and swallow
    }

    //-------------------------------------------- Running the job --------------------------------------------

    void runProcess(ProcessRunner runner, boolean previousRunFailed) {
        class ProcessRunStatus
        {
            private Integer result_code = new Integer(0);

            // a result code of 0 means normal success
            // a result code of 1 means a serious error, rollback handled here and error routine called
            // a positive result > 1 may to for a 'controlled' error, transaction is rolled back,
            //      but error routine NOT called (eg for validation error on imports)

            private boolean exception_thrown = false;
            private boolean run_error = false;
            private boolean force_complete = false;
        }
        final ProcessRunStatus run_status = new ProcessRunStatus();

        boolean first_section = true;

        final BatchProcessServer bps = getQueue().getBatchProcessServer();
        bps.resetSection(this, previousRunFailed);

        /* Loop round all sections until a run_error occurs or no more sections exist.
         * A run_error consists of a non-zero return code or a thrown exception.
         * On a run_error a rollback occurs otherwise the transaction is committed.
         * If the previous run was a failure (is_failed) then the first section to run
         * is the one that was running when the failure occurred, otherwise the first
         * sction to run is the first section.
         */
        while (bps.hasMoreSections(this, first_section) && !run_status.run_error && !run_status.force_complete)
        {
            try
            {
                doTransaction(new Callback() {

                    @Override
                    protected void doAction() {
                        ProcessPersistence<ProcessImpl> processHome = getProcessPersistence();

                        try {
                            run_status.result_code = bps.runProcess(ProcessWrapper.this, false);

                            if (!run_status.result_code.equals(BatchProcessServer.SUCCESS))
                                throw new ForceRollbackException();
                        }
                        catch (ForceProcessComplete fpc) {
                            run_status.force_complete = true;
                        }

                        if (isPersistent) processHome.update();
                        _return(ProcessWrapper.this);
                    }
                });
            }
            catch (Exception e)
            {
                run_status.exception_thrown = true;
                new QueujException(e);
            }
            finally
            {
                first_section = false;

                // does not set run error if result code > 1
                run_status.run_error = run_status.exception_thrown || run_status.result_code.equals (BatchProcessServer.FAILURE);
            }
        }

        /**
         * If a failure has occurred then run the failure section if one exists.
         * To run the failure section run_error must already be true so no need
         * to set it again if the failure section also fails!
         */
        if (run_status.run_error && bps.hasFailureSection(this))
        {
            try
            {
                doTransaction(new Callback() {

                    @Override
                    protected void doAction() {
                        ProcessPersistence<ProcessImpl> processHome = getProcessPersistence();

                        try {
                            Integer failure_result_code = bps.runProcess(ProcessWrapper.this, true);

                            if (failure_result_code.intValue() != 0)
                                throw new ForceRollbackException();
                        }
                        catch (ForceProcessComplete fpc) {
                            throw new QueujException("Cannot force complete from a failure section.");
                        }

                        if (isPersistent) processHome.update();
                        _return(ProcessWrapper.this);
                    }
                });
            }
            catch (Exception e)
            {
                new QueujException(e);
            }
        }

        /**
         * optional post process operations (e.g. reloading BudgetingLogs after a budgeting run)
         * Post processes run whether the section run has failed or not. A flag is passed in with
         * the state of the section run, but it must be noted that the Process will still report
         * a status of running.
         */
        getParameters().iteratePostProcesses(new IterativeCallback<SequencedProcess,Void>() {
            @Override
            protected void nextObject(final SequencedProcess post_process)
            {
                try
                {
                    doTransaction(new Callback() {

                        @Override
                        protected void doAction() {
                            ProcessPersistence<ProcessImpl> processHome = getProcessPersistence();

                            post_process.action(new Process(ProcessWrapper.this), process.getUserId(), run_status.run_error);

                            if (isPersistent) processHome.update();
                            _return(ProcessWrapper.this);
                        }
                    });
                }
                catch (Exception e)
                {
                    run_status.exception_thrown = true;
                    new QueujException(e);
                }
                finally
                {
                    run_status.run_error = run_status.exception_thrown || run_status.result_code.intValue() != 0;
                }
            }
        });

        /**
         * Finally update the Process with the cummulative status thats been collected as the various parts
         * have run and/or failed.
         */
        try
        {
            doTransaction(new Callback() {

                @Override
                protected void doAction() {
                    ProcessImplServer ps = ((ProcessImplServer)getContainingServer());
                    ps.removeProcessFromIndex(ProcessWrapper.this);

                    ProcessPersistence<ProcessImpl> processHome = getProcessPersistence();
                    process.setResultCode(run_status.result_code.intValue());

                    if (run_status.run_error) {
                        // Increase attempt first to prevent exception in higherPriorityJobExists
                        process.setAttempt(process.getAttempt() + 1);
                        process.setStatus(Status.RUN_ERROR);
                    }

                    ps.addProcessToIndex(ProcessWrapper.this);

                    if (isPersistent) processHome.update();
                    _return(ProcessWrapper.this);
                }
            });
        }
        catch (Exception e)
        {
            new QueujException(e);
        }
    }

    //-------------------------------------------- Starting the job --------------------------------------------

    final Object mutex = new Object();
    ProcessRunner processRunner = null;

    int runnerHashCode() {
        return processRunner.hashCode(); // Throws NPE if not yet running
    }

    public void attach() {
        synchronized (mutex) {
            if (processRunner == null)
                return;

            while (processRunner != null) {
                try {
                    mutex.wait();
                }
                catch (InterruptedException ie) {}
            }
        }
    }

    public void start() {
        // Calculate next run time for the process
        GregorianCalendar nextRun = getNextRunTime();
        if (nextRun == null) // No run is due
            return;

        if (log.isDebugEnabled())
        {
            String desc = getDescription();
            if (desc == null)
                desc = getProcessName();
            if (desc == null)
                desc = "Process: " + getProcessKey();
            desc = desc.trim();

            log.debug("Next run date for " + desc + " is " + nextRun.getTime().toString());
        }

        start(nextRun, isFailed());
    }

    private void startNow() {
        start(new GregorianCalendar(), isFailed());
    }

    private boolean restart0() {
        return start(new GregorianCalendar(), true);
    }

    private boolean start(GregorianCalendar runTime, boolean isFailed) {
        synchronized (mutex) {
            if (processRunner != null)
                return false;

            processRunner = new ProcessRunner(this, runTime, isFailed);
            processRunner.doStart();
            return true;
        }
    }
}
