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

import com.workplacesystems.queuj.ProcessBuilder;
import com.workplacesystems.queuj.QueueOwner;
import com.workplacesystems.queuj.Version;
import com.workplacesystems.queuj.process.jpa.ProcessDAO;
import com.workplacesystems.queuj.process.jpa.ProcessImpl;
import com.workplacesystems.queuj.utils.QueujException;
import com.workplacesystems.utilsj.Callback;
import java.io.Serializable;
import java.util.GregorianCalendar;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author dave
 */
public abstract class QueujFactory<K extends Serializable & Comparable> {

    private final static Log log = LogFactory.getLog(QueujFactory.class);

    private static QueujFactory<?> instance;

    static {
        setInstance();
    }

    static void setInstance() {
        try {
            QueujFactory<?> instance0 = null;

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
                instance0 = new QueujFactoryImpl();

            instance = instance0;

            log.info("Initialising Queuj " + Version.getVersionMini());
            log.info("Utilsj " + com.workplacesystems.utilsj.Version.getVersionMini());

            instance.init();
        }
        catch (Exception e) {
            throw new QueujException(e);
        }
    }

    protected QueujFactory() {}

    public static final void setDefaultImplOptions(ProcessBuilder processBuilder, Map<String, Object> implementation_options) {
        instance.setDefaultImplOptions0(processBuilder, implementation_options);
    }

    public static final ProcessServer<?> getProcessServer(QueueOwner queueOwner, Map<String, Object> server_options) {
        return instance.getProcessServer0(queueOwner == null ? null : queueOwner.getQueueOwnerKey(), server_options);
    }

    public static final ProcessServer<?> getProcessServer(String queueOwner, Map<String, Object> server_options) {
        return instance.getProcessServer0(queueOwner, server_options);
    }

    public static final QueujTransaction<?> getTransaction() {
        return instance.getTransaction0();
    }

    public static final ProcessPersistence<?,?> getPersistence(String queueOwner, Map<String, Object> server_options) {
        return instance.getPersistence0(queueOwner, server_options);
    }

    public static final ProcessDAO getProcessDAO() {
        return instance.getProcessDAO0();
    }

    public static final <T> Callback<T> getAsyncCallback(Callback<T> callback) {
        return instance.getAsyncCallback0(callback);
    }

    static final ProcessEntity<?> getNewProcessEntity(String queueOwner, boolean isPersistent, Map<String, Object> server_options) {
        return instance.getNewProcessEntity0(queueOwner, isPersistent, server_options);
    }

    static final ProcessRunner getProcessRunner(ProcessWrapper process, GregorianCalendar runTime, boolean isFailed) {
        return instance.getProcessRunner0(process, runTime, isFailed);
    }
    protected abstract void init();

    protected abstract void setDefaultImplOptions0(ProcessBuilder processBuilder, Map<String, Object> implementation_options);

    protected abstract ProcessServer<K> getProcessServer0(String queueOwner, Map<String, Object> server_options);

    protected abstract QueujTransaction<K> getTransaction0();

    protected abstract ProcessPersistence<ProcessEntity<K>,K> getPersistence0(String queueOwner, Map<String, Object> server_options);

    protected abstract ProcessDAO getProcessDAO0();

    public abstract <T> Callback<T> getAsyncCallback0(Callback<T> callback);

    protected abstract ProcessEntity<K> getNewProcessEntity0(String queueOwner, boolean isPersistent, Map<String, Object> server_options);

    protected abstract ProcessRunner getProcessRunner0(ProcessWrapper process, GregorianCalendar runTime, boolean isFailed);
}
