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

import com.workplacesystems.queuj.ProcessServer;
import com.workplacesystems.queuj.QueueOwner;
import com.workplacesystems.queuj.Version;
import com.workplacesystems.queuj.process.jpa.ProcessDAO;
import com.workplacesystems.queuj.utils.Callback;
import com.workplacesystems.queuj.utils.QueujException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author dave
 */
public class QueujFactory {

    private final static Log log = LogFactory.getLog(QueujFactory.class);

    private static final QueujFactory instance;

    static {
        try {
            QueujFactory instance0 = null;

            String implClazzStr = System.getProperty("com.workplacesystems.queuj.QueujFactory");
            if (implClazzStr != null) {
                try {
                    Class<QueujFactory> implClazz = (Class<QueujFactory>) Class.forName(implClazzStr);
                    instance0 = implClazz.newInstance();
                }
                catch (ClassNotFoundException ex) {
                }
                catch (InstantiationException ex) {
                }
                catch (IllegalAccessException ex) {
                }
            }

            if (instance0 == null)
                instance0 = new QueujFactory();

            instance = instance0;

            log.info("Initialising Queuj " + Version.getVersionMini());

            instance.init();
        }
        catch (Exception e) {
            throw new QueujException(e);
        }
    }

    protected QueujFactory() {}

    public static final ProcessServer getProcessServer(QueueOwner queueOwner) {
        return instance.getProcessServer0(queueOwner == null ? null : queueOwner.getQueueOwnerKey());
    }

    public static final ProcessServer getProcessServer(String queueOwner) {
        return instance.getProcessServer0(queueOwner);
    }

    public static final QueujTransaction getTransaction() {
        return instance.getTransaction0();
    }

    public static final ProcessPersistence getPersistence() {
        return instance.getPersistence0();
    }

    public static final ProcessDAO getProcessDAO() {
        return instance.getProcessDAO0();
    }

    public static final <T> Callback<T> getAsyncCallback(Callback<T> callback) {
        return instance.getAsyncCallback0(callback);
    }

    protected void init() {}

    protected ProcessServer getProcessServer0(String queueOwner) {
        return ProcessImplServer.newInstance(queueOwner);
    }

    protected QueujTransaction getTransaction0() {
        return new QueujTransaction() {

            public <T> T doTransaction(ProcessWrapper process, Callback<T> callback, boolean doStart) {
                return doTransaction(process.isPersistent(), callback, doStart);
            }

            public <T> T doTransaction(boolean persistent, Callback<T> callback, boolean doStart) {
                if (persistent)
                    throw new QueujException("No persistence has been enabled.");

                T result = callback.action();

                if (result instanceof ProcessWrapper) {
                    ProcessWrapper process = (ProcessWrapper)result;
                    ((ProcessImplServer)process.getContainingServer()).commit();
                    if (doStart)
                        process.start();
                }

                return result;
            }
        };
    }

    protected ProcessPersistence getPersistence0() {
        return null;
    }

    protected ProcessDAO getProcessDAO0() {
        return null;
    }

    public <T> Callback<T> getAsyncCallback0(Callback<T> callback) {
        return callback;
    }
}
