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

package com.workplacesystems.queuj.process.seam;

import com.workplacesystems.queuj.ProcessServer;
import com.workplacesystems.queuj.process.ProcessImplServer;
import com.workplacesystems.queuj.process.ProcessWrapper;
import com.workplacesystems.queuj.process.QueujTransaction;
import com.workplacesystems.utilsj.Callback;
import com.workplacesystems.utilsj.collections.FilterableArrayList;
import java.util.ArrayList;
import javax.persistence.EntityManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.TransactionPropagationType;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.core.Events;

/**
 *
 * @author dave
 */
@Name("seamTransactions")
@Scope(ScopeType.STATELESS)
public class SeamTransaction implements QueujTransaction<Integer> {

    private final static Log log = LogFactory.getLog(ProcessWrapper.class);

    @In
    private ThreadLocal<TransactionContext> transactionContext;

    @Factory(value="transactionContext",autoCreate=true,scope=ScopeType.APPLICATION)
    public ThreadLocal<TransactionContext> createTransactionContext() {
        return new ThreadLocal<TransactionContext>() {

            @Override
            protected TransactionContext initialValue() {
                TransactionContext context = new TransactionContext();
                log.debug("Starting transaction " + context.transactionId);
                return context;
            }
        };
    }

    @Transactional(TransactionPropagationType.REQUIRED)
    public <T> T doTransaction(ProcessWrapper<Integer> process, Callback<T> callback, boolean doStart) {
        return doTransaction(process.getQueueOwner(), process.isPersistent(), callback, doStart);
    }

    @Transactional(TransactionPropagationType.REQUIRED)
    public <T> T doTransaction(String queueOwner, boolean persistent, Callback<T> callback, boolean doStart) {
        TransactionContext context = transactionContext.get();
        if (context.obsolete) {
            transactionContext.remove();
            context = transactionContext.get();
        }

        if (!context.callbacksSet) {
            Events events = (Events)Component.getInstance(Events.class);
            events.raiseTransactionSuccessEvent("processTransactionSuccess", context);
            events.raiseTransactionCompletionEvent("processTransactionComplete", context);
            context.callbacksSet = true;
        }
        T result = callback.action();
        if (result instanceof ProcessWrapper) {
            log.debug("Adding process " + ((ProcessWrapper)result).getProcessKey() + "(" +
                    ((ProcessWrapper)result).getProcessVersion() + ") to transaction " + context.transactionId);
            context.processes.add((ProcessWrapper<Integer>)result);
            if  (doStart)
                context.startProcesses.add((ProcessWrapper<Integer>)result);
        }
        return result;
    }

    @Observer("processTransactionSuccess")
    public void transactionSuccess(TransactionContext context) {
        if (context.obsolete)
            return;
        context.obsolete = true;

        log.debug("Commiting transaction " + context.transactionId);
        FilterableArrayList<ProcessServer> processServers = new FilterableArrayList<ProcessServer>();
        for (ProcessWrapper process : context.processes) {
            ProcessServer ps = process.getContainingServer();
            if (!processServers.contains(ps))
                processServers.add(ps);
        }
        for (ProcessServer ps : processServers)
            ((ProcessImplServer)ps).commit();

        for (ProcessWrapper process : context.startProcesses) {
            if (process.rescheduleRequired(false))
                process.interruptRunner();
            else
                process.start();
        }

        EntityManager em = (EntityManager)Component.getInstance("entityManager");
        em.clear();
    }

    @Observer("processTransactionComplete")
    public void transactionComplete(TransactionContext context) {
        if (context.obsolete)
            return;
        context.obsolete = true;

        log.debug("Rolling back transaction " + context.transactionId);
        FilterableArrayList<ProcessServer> processServers = new FilterableArrayList<ProcessServer>();
        for (ProcessWrapper process : context.processes) {
            ProcessServer ps = process.getContainingServer();
            if (!processServers.contains(ps))
                processServers.add(ps);
        }
        for (ProcessServer ps : processServers)
            ((ProcessImplServer)ps).rollback();

        EntityManager em = (EntityManager)Component.getInstance("entityManager");
        em.clear();
    }

    private static volatile int transactionCount = 0;

    private class TransactionContext {
        private int transactionId = ++transactionCount;
        private boolean obsolete = false;
        private boolean callbacksSet = false;
        private ArrayList<ProcessWrapper<Integer>> processes = new ArrayList<ProcessWrapper<Integer>>();
        private ArrayList<ProcessWrapper<Integer>> startProcesses = new ArrayList<ProcessWrapper<Integer>>();
    }
}
