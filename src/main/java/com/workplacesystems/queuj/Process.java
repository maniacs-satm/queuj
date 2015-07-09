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

import com.workplacesystems.queuj.process.ProcessEntity;
import com.workplacesystems.queuj.process.ProcessImplServer;
import com.workplacesystems.queuj.process.ProcessOutputable;
import com.workplacesystems.queuj.process.ProcessWrapper;
import com.workplacesystems.queuj.process.QueujFactory;
import java.io.Serializable;

import com.workplacesystems.queuj.utils.User;
import java.util.GregorianCalendar;
import java.util.Map;

/**
 * This is the public interface to ProcessImpl and PartitionedProcessImpl.
 *
 * @author dave
 */
public final class Process<K extends Serializable & Comparable> implements Serializable, Comparable {

    private String queueOwner;

    private K processKey;

    private Map<String,Object> server_options;

    private transient ProcessWrapper<K> process;

    public Process(ProcessWrapper<K> process) {
        this.process = process;
        this.queueOwner = process.getQueueOwner();
        this.processKey = process.getProcessKey();
        this.server_options = process.getServerOptions();
    }

    private synchronized ProcessWrapper getProcess() {
        if (process == null) {
            ProcessServer server = QueujFactory.getProcessServer(queueOwner, server_options);
            process = ((ProcessImplServer)server).get(processKey);
        }
        return process;
    }

    public boolean exists() {
        ProcessWrapper pw = getProcess();
        return pw != null && !pw.isDeleted();
    }

    public String getQueueOwner() {
        return queueOwner;
    }

    public K getProcessKey() {
        return processKey;
    }

    public int getAttempt() {
        return getProcess().getAttempt();
    }

    public ProcessEntity getProcessEntity() {
        return getProcess().getProcessEntity();
    }

    public Serializable setParameter(String key, Serializable value) {
        return getProcess().setParameter(key, value);
    }

    public Object getParameter(String key) {
        return getProcess().getParameter(key);
    }

    public Queue getQueue() {
        return getProcess().getQueue();
    }

    public String getUserId() {
        return getProcess().getUserId();
    }

    public ProcessServer getContainingServer() {
        return getProcess().getContainingServer();
    }

    public void attach() {
        getProcess().attach();
    }

    public boolean attach(long timeout) {
        return getProcess().attach(timeout);
    }

    public boolean isRunning() {
        return getProcess().isRunning();
    }

    public boolean isWaitingToRun() {
        return getProcess().isWaitingToRun();
    }

    public boolean isNotRun() {
        return getProcess().isNotRun();
    }

    public boolean isFailed() {
        return getProcess().isFailed();
    }

    public boolean isComplete() {
        return getProcess().isComplete();
    }

    public boolean isRunError() {
        return getProcess().isRunError();
    }

    public boolean createReport()
    {
        return getProcess().createReport();
    }

    public ProcessOutputable getProcessOutputable()
    {
        return getProcess().getProcessOutputable();
    }

    public GregorianCalendar getNextRunTime() {
        return getProcess().getNextRunTime();
    }

    public void updateOccurrence(final Occurrence occurrence) {
        getProcess().updateOccurrence(occurrence);
    }

    public void startNow() {
        getProcess().startNow();
    }

    public boolean restart(User user, QueueOwner activeQueueOwner) {
        return getProcess().restart(user, activeQueueOwner);
    }

    public boolean restart() {
        return getProcess().restart();
    }

    public boolean delete(User user, QueueOwner activeQueueOwner) {
        return getProcess().delete(user, activeQueueOwner);
    }

    public boolean delete() {
        return getProcess().delete();
    }

    public void notifySelf() {
        getProcess().notifySelf();
    }

    public String getProcessName() {
        return getProcess().getProcessName();
    }

    public String getDescription() {
        return getProcess().getDescription();
    }

    public String getStatusDescription() {
        return getProcess().getStatusDescription();
    }

    public String getStartedTimestamp() {
        return getProcess().getStartedTimestamp();
    }

    public String getStatusImg() {
        return getProcess().getStatusImg();
    }

    public int compareTo(Object o) {
        if (o instanceof Process)
            return getProcess().compareTo(((Process)o).getProcess());
        else if (o instanceof ProcessWrapper)
            return getProcess().compareTo((ProcessWrapper)o);

        throw new IllegalArgumentException("Parameter for compareTo must be Process or ProcessWrapper");
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Process other = (Process) obj;
        if (this.getProcess() != other.getProcess() && (this.getProcess() == null || !this.getProcess().equals(other.getProcess()))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + (this.getProcess() != null ? this.getProcess().hashCode() : 0);
        return hash;
    }
}
