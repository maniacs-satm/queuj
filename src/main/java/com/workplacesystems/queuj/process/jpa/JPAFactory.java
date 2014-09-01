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

package com.workplacesystems.queuj.process.jpa;

import com.workplacesystems.queuj.ProcessServer;
import com.workplacesystems.queuj.process.ProcessEntity;
import com.workplacesystems.queuj.process.ProcessImplServer;
import com.workplacesystems.queuj.process.ProcessPersistence;
import com.workplacesystems.queuj.process.ProcessWrapper;
import com.workplacesystems.queuj.process.QueujFactory;
import com.workplacesystems.queuj.process.QueujFactoryImpl;
import com.workplacesystems.queuj.process.QueujTransaction;
import com.workplacesystems.queuj.process.jpa.ProcessImpl;
import com.workplacesystems.queuj.utils.QueujException;
import com.workplacesystems.utilsj.Callback;
import com.workplacesystems.utilsj.collections.FilterableArrayList;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

/**
 *
 * @author dave
 */
public class JPAFactory extends QueujFactoryImpl {

    private EntityManagerFactory emf;

    private final ThreadLocal<EntityManager> tlEm = new ThreadLocal<EntityManager>();

    private final ThreadLocal<ArrayList<ProcessWrapper>> tlProcesses = new ThreadLocal<ArrayList<ProcessWrapper>>() {

        @Override
        protected ArrayList<ProcessWrapper> initialValue() {
            return new FilterableArrayList<ProcessWrapper>();
        }
    };

    private final ThreadLocal<ArrayList<ProcessWrapper>> tlStartProcesses = new ThreadLocal<ArrayList<ProcessWrapper>>() {

        @Override
        protected ArrayList<ProcessWrapper> initialValue() {
            return new FilterableArrayList<ProcessWrapper>();
        }
    };

    private final ThreadLocal<ArrayList<Callback<Void>>> tlCommitCallbacks = new ThreadLocal<ArrayList<Callback<Void>>>() {

        @Override
        protected ArrayList<Callback<Void>> initialValue() {
            return new FilterableArrayList<Callback<Void>>();
        }
    };

    public JPAFactory() {}

    @Override
    protected void init() {
        String persistenceUnitName = System.getProperty("com.workplacesystems.queuj.jpa.persistenceUnitName");
        if (persistenceUnitName == null)
            persistenceUnitName = System.getProperty("com.workplacesystems.jpa.persistenceUnitName");
        emf = Persistence.createEntityManagerFactory(persistenceUnitName);

        tlEm.set(emf.createEntityManager());
        List<String> queueOwners = getProcessDAO().findQueueOwners();
        for (String queueOwner : queueOwners) {
            ((ProcessImplServer)getProcessServer0(queueOwner, null)).init();
        }
        tlEm.remove();
    }

    @Override
    protected ProcessDAO getProcessDAO0() {
        EntityManager em = tlEm.get();
        if (em == null)
            em = emf.createEntityManager();
        return new ProcessImplDAO(em);
    }

    @Override
    protected QueujTransaction<Integer> getTransaction0() {
        return new QueujTransaction<Integer>() {

            private boolean emIsLocal = false;
            private boolean transactionIsLocal = false;

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
                boolean committed = false;
                try {
                    if (tlEm.get() == null) {
                        tlEm.set(emf.createEntityManager());
                        emIsLocal = true;
                    }
                    if (!tlEm.get().getTransaction().isActive()) {
                        tlEm.get().getTransaction().begin();
                        transactionIsLocal = true;
                    }

                    T result = callback.action();

                    if (result instanceof ProcessWrapper) {
                        tlProcesses.get().add((ProcessWrapper)result);
                        if (doStart)
                            tlStartProcesses.get().add((ProcessWrapper)result);
                    }
                    if (commitCallback != null)
                        tlCommitCallbacks.get().add(commitCallback);

                    if (transactionIsLocal) {
                        tlEm.get().getTransaction().commit();

                        FilterableArrayList<ProcessServer> processServers = new FilterableArrayList<ProcessServer>();
                        for (ProcessWrapper process : tlProcesses.get()) {
                            ProcessServer ps = process.getContainingServer();
                            if (!processServers.contains(ps))
                                processServers.add(ps);
                        }
                        for (ProcessServer ps : processServers)
                            ((ProcessImplServer)ps).commit();

                        for (Callback<Void> commitCallback0 : tlCommitCallbacks.get()) {
                            try {
                                commitCallback0.action();
                            }
                            catch (Exception e) {
                                new QueujException(e);
                            }
                        }

                        for (ProcessWrapper process : tlStartProcesses.get()) {
                            if (process.rescheduleRequired(false))
                                process.interruptRunner();
                            else
                                process.start();
                        }

                        committed = true;
                    }
                    return result;
                }
                finally {
                    if (transactionIsLocal) {
                        if (!committed)
                            tlEm.get().getTransaction().rollback();

                        FilterableArrayList<ProcessServer> processServers = new FilterableArrayList<ProcessServer>();
                        for (ProcessWrapper process : tlProcesses.get()) {
                            ProcessServer ps = process.getContainingServer();
                            if (!processServers.contains(ps))
                                processServers.add(ps);
                        }
                        for (ProcessServer ps : processServers)
                            ((ProcessImplServer)ps).rollback();

                        tlProcesses.remove();
                        tlStartProcesses.remove();
                        tlCommitCallbacks.remove();
                    }
                    if (emIsLocal)
                        tlEm.remove();
                }
            }
        };
    }

    @Override
    protected ProcessPersistence<ProcessEntity<Integer>,Integer> getPersistence0(final String queueOwner, Map<String, Object> server_options) {
        return new ProcessPersistence<ProcessEntity<Integer>,Integer>() {

            private ProcessImpl instance = null;

            public void clearInstance() {
                instance = null;
            }

            public ProcessImpl getInstance() {
                if (instance == null)
                    instance = (ProcessImpl)getNewProcessEntity0(queueOwner, true, null);
                return instance;
            }

            public void setId(Integer id) {
                if (tlEm.get() == null)
                    throw new QueujException("No transaction.");
                if (id == null)
                    return;
                instance = tlEm.get().find(ProcessImpl.class, id);
                tlEm.get().refresh(instance);
            }

            public String persist() {
                if (tlEm.get() == null)
                    throw new QueujException("No transaction.");

                tlEm.get().persist(instance);

                return null;
            }

            public String update() {
                if (tlEm.get() == null)
                    throw new QueujException("No transaction.");

                tlEm.get().flush();

                return null;
            }

            public String remove() {
                if (tlEm.get() == null)
                    throw new QueujException("No transaction.");

                tlEm.get().remove(instance);

                return null;
            }
        };
    }
}
