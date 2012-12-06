/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.workplacesystems.queuj.process;

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

    protected void setDefaultImplOptions0(Map<String, Object> implementation_options) {}

    protected ProcessServer getProcessServer0(String queueOwner, Map<String, Object> server_options) {
        return ProcessImplServer.newInstance(queueOwner);
    }

    protected QueujTransaction<Integer> getTransaction0() {
        return new QueujTransaction<Integer>() {

            public <T> T doTransaction(ProcessWrapper<Integer> process, Callback<T> callback, boolean doStart) {
                return doTransaction(process.isPersistent(), callback, doStart);
            }

            public <T> T doTransaction(boolean persistent, Callback<T> callback, boolean doStart) {
                if (persistent)
                    throw new QueujException("No persistence has been enabled.");

                T result = callback.action();

                if (result instanceof ProcessWrapper) {
                    ProcessWrapper process = (ProcessWrapper)result;
                    ((ProcessImplServer)process.getContainingServer()).commit();
                    if (doStart) {
                        if (process.rescheduleRequired(false))
                            process.interruptRunner();
                        else
                            process.start();
                    }
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
