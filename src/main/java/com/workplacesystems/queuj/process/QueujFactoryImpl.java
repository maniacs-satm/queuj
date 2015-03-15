/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.workplacesystems.queuj.process;

import com.workplacesystems.queuj.ProcessBuilder;
import com.workplacesystems.queuj.process.jpa.ProcessDAO;
import com.workplacesystems.queuj.process.jpa.ProcessImpl;
import com.workplacesystems.queuj.utils.QueujException;
import com.workplacesystems.utilsj.Callback;
import java.io.Serializable;
import java.util.GregorianCalendar;
import java.util.Map;

/**
 *
 * @author dave
 */
public class QueujFactoryImpl extends QueujFactory<Integer> {
    
    protected void init() {}

    protected void setDefaultImplOptions0(ProcessBuilder processBuilder, Map<String, Object> implementation_options) {}

    protected ProcessServer getProcessServer0(String queueOwner, Map<String, Object> server_options) {
        return ProcessImplServer.newInstance(queueOwner);
    }

    protected QueujTransaction<Integer> getTransaction0() {
        return new QueujTransaction<Integer>() {

            public <T> T doTransaction(ProcessWrapper<Integer> process, Callback<T> callback, boolean doStart) {
                return doTransaction(process.getQueueOwner(), process.isPersistent(), callback, doStart);
            }

            public <T> T doTransaction(ProcessWrapper<Integer> process, Callback<T> callback, Callback<Void> commitCallback, boolean doStart) {
                return doTransaction(process.getQueueOwner(), process.isPersistent(), callback, commitCallback, doStart);
            }

            public <T> T doTransaction(String queueOwner, boolean persistent, Callback<T> callback, boolean doStart) {
                return doTransaction(queueOwner, persistent, callback, null, doStart);
            }

            public <T> T doTransaction(String queueOwner, boolean persistent, Callback<T> callback, Callback<Void> commitCallback, boolean doStart) {
                if (persistent)
                    throw new QueujException("No persistence has been enabled.");

                T result = callback.action();

                if (result instanceof ProcessWrapper) {
                    ProcessWrapper process = (ProcessWrapper)result;
                    ((ProcessImplServer)process.getContainingServer()).commit();
                }

                if (commitCallback != null) {
                    try {
                        commitCallback.action();
                    }
                    catch (Exception e) {
                        new QueujException(e);
                    }
                }

                if (result instanceof ProcessWrapper && doStart) {
                    ProcessWrapper process = (ProcessWrapper)result;
                    if (process.rescheduleRequired(false))
                        process.interruptRunner();
                    else
                        process.start();
                }

                if (result instanceof ProcessWrapper) {
                    ProcessWrapper process = (ProcessWrapper)result;
                    process.callListeners();
                }

                return result;
            }
        };
    }

    protected ProcessPersistence<ProcessEntity<Integer>,Integer> getPersistence0(String queueOwner, Map<String, Object> server_options) {
        return null;
    }

    protected ProcessDAO getProcessDAO0() {
        return null;
    }

    public <T> Callback<T> getAsyncCallback0(Callback<T> callback) {
        return callback;
    }

    protected ProcessEntity<Integer> getNewProcessEntity0(String queueOwner, boolean isPersistent, Map<String, Object> server_options) {
        return new ProcessImpl();
    }

    protected ProcessRunner getProcessRunner0(ProcessWrapper process, GregorianCalendar runTime, boolean isFailed) {
        return new ProcessRunnerImpl(process, runTime, isFailed);
    }
}
