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

import com.workplacesystems.queuj.Process;
import com.workplacesystems.queuj.QueueListener;
import com.workplacesystems.queuj.QueueOwner;
import com.workplacesystems.queuj.process.jpa.ProcessDAO;
import com.workplacesystems.queuj.process.jpa.ProcessImpl;
import com.workplacesystems.utilsj.Callback;
import com.workplacesystems.utilsj.collections.FilterableArrayList;
import com.workplacesystems.utilsj.collections.FilterableCollection;
import com.workplacesystems.utilsj.collections.FilterableList;
import com.workplacesystems.utilsj.collections.IterativeCallback;
import com.workplacesystems.utilsj.collections.SyncUtils;
import com.workplacesystems.utilsj.collections.TransactionalBidiTreeMap;
import com.workplacesystems.utilsj.collections.TransactionalSortedFilterableBidiMap;
import com.workplacesystems.utilsj.collections.decorators.SynchronizedFilterableCollection;
import com.workplacesystems.utilsj.collections.decorators.SynchronizedTransactionalSortedFilterableBidiMap;
import java.io.Serializable;
import java.util.GregorianCalendar;
import java.util.HashMap;


public class ProcessImplServer<K extends Serializable & Comparable> implements ProcessServer<K>, Serializable {

    private final static HashMap<String,ProcessImplServer> instances = new HashMap<String, ProcessImplServer>();

    private final static ProcessImplServer instance = new ProcessImplServer(null);

    final ProcessIndexesImpl indexes = new ProcessIndexesImpl();

    final Object mutex = new Object();

    private final String queueOwner;

    private final TransactionalSortedFilterableBidiMap<K,ProcessWrapper<K>> processes;

    private final ProcessScheduler processScheduler = new ProcessScheduler();

    private final FilterableCollection<QueueListener> queueListeners = SynchronizedFilterableCollection.decorate(new FilterableArrayList<QueueListener>());

    static synchronized ProcessImplServer newInstance(String queueOwner) {
        if (queueOwner == null)
            return instance;

        if (!instances.containsKey(queueOwner))
            instances.put(queueOwner, new ProcessImplServer(queueOwner));

        return instances.get(queueOwner);
    }

    private ProcessImplServer(String queueOwner) {
        this.queueOwner = queueOwner;
        processes = SynchronizedTransactionalSortedFilterableBidiMap.decorate(
                new TransactionalBidiTreeMap<K,ProcessWrapper<K>>());
        processes.setAutoCommit(false);
    }

    public Object getMutex() {
        return mutex;
    }

    public void submitProcess(ProcessWrapper<K> process) {
        processes.put(process.getProcessKey(), process);
        indexes.addProcessToIndex(process);
    }

    public ProcessScheduler getProcessScheduler() {
        return processScheduler;
    }

    public void init() {
        writeLocked(new Callback<Void>() {

            @Override
            protected void doAction() {
                try {
                    ProcessDAO processDAO = QueujFactory.getProcessDAO();
                    if (processDAO != null) {
                        for (ProcessImpl process : processDAO.findProcesses(queueOwner)) {
                            ProcessWrapper processWrapper = ProcessWrapper.getNewInstance(queueOwner, process, true);
                            submitProcess(processWrapper);
                        }
                        commit();
                    }
                }
                catch (RuntimeException re) {
                    rollback();
                    throw re;
                }

                (new IterativeCallback<ProcessWrapper,Void>() {

                    @Override
                    protected void nextObject(ProcessWrapper process) {
                        if (process.isFailed())
                            process.updateRunErrorAndRestart();
                        else
                            process.start();
                    }
                }).iterate(processes.valuesByValue());
            }
        });
    }

    public Boolean notifyQueue()
    {
        return getProcessScheduler().notifyAllProcesses(this, null) ? Boolean.TRUE : Boolean.FALSE;
    }

    void notifyProcess(ProcessWrapper process) {
        getProcessScheduler().notifyProcess(process);
    }

    public void delete(ProcessWrapper process) {
        try {
            processes.remove(process.getProcessKey());
            indexes.removeProcessFromIndex(process);

            commit();
        }
        catch (RuntimeException re) {
            rollback();
            throw re;
        }
    }

    public void commit() {
        processes.commit();
        indexes.finalizeIndexes(true);

        (new IterativeCallback<QueueListener,Void>() {

            @Override
            protected void nextObject(QueueListener l) {
                l.queueUpdated();
            }
        }).iterate(queueListeners);
    }

    public void rollback() {
        processes.rollback();
        indexes.finalizeIndexes(false);
    }

    public int size() {
        return processes.size();
    }

    public FilterableList<Process> subList(final int fromIndex, final int toIndex) {
        final FilterableArrayList<Process> subList = new FilterableArrayList<Process>();
        (new IterativeCallback<ProcessWrapper<K>,Void>() {

            private int index = 0;

            @Override
            protected void nextObject(ProcessWrapper<K> obj) {
                if (index >= fromIndex)
                    subList.add(new Process<K>(obj));
                if (++index >= toIndex)
                    _break();
            }
        }).iterate(processes.valuesByValue());
        return subList;
    }

    public ProcessWrapper<K> get(K key) {
        return processes.get(key);
    }

    public <T> T readLocked(Callback<T> callback) {
        return SyncUtils.synchronizeRead(processes, callback);
    }

    public <T> T writeLocked(Callback<T> callback) {
        return SyncUtils.synchronizeWrite(processes, callback);
    }

    public void registerListener(QueueListener listener) {
        queueListeners.add(listener);
    }

    public void removeListener(QueueListener listener) {
        queueListeners.remove(listener);
    }

    public boolean contains(ProcessWrapper process) {
        return processes.containsValue(process);
    }

    public boolean scheduleOverride(ProcessWrapper process, GregorianCalendar nextRun) {
        return false;
    }

    public boolean addProcessToIndex(ProcessWrapper process)
    {
        return indexes.addProcessToIndex(process);
    }

    public boolean removeProcessFromIndex(ProcessWrapper process)
    {
        return indexes.removeProcessFromIndex(process);
    }

    public <R> R indexesWithReadLock(final ProcessIndexesCallback<R> indexesCallback) {
        return indexes.readLocked(new Callback<R>() {
            @Override
            protected void doAction() {
                _return(indexesCallback.readIndexes(indexes));
            }
        });
    }
}