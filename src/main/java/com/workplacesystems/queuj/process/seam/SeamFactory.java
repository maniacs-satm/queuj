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

import com.workplacesystems.queuj.process.ProcessEntity;
import com.workplacesystems.queuj.process.ProcessImplServer;
import com.workplacesystems.queuj.process.ProcessPersistence;
import com.workplacesystems.queuj.process.QueujFactory;
import com.workplacesystems.queuj.process.QueujFactoryImpl;
import com.workplacesystems.queuj.process.QueujTransaction;
import com.workplacesystems.queuj.process.jpa.ProcessDAO;
import com.workplacesystems.utilsj.Callback;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import org.jboss.seam.Component;
import org.jboss.seam.async.Asynchronous;

/**
 *
 * @author dave
 */
public class SeamFactory extends QueujFactoryImpl {

    @Override
    protected void init() {
        List<String> queueOwners = getProcessDAO().findQueueOwners();
        for (String queueOwner : queueOwners) {
            ((ProcessImplServer)getProcessServer0(queueOwner, null)).init();
        }
    }

    @Override
    protected ProcessDAO getProcessDAO0() {
        return (ProcessDAO)Component.getInstance(ProcessImplDAO.class, true);
    }

    @Override
    protected QueujTransaction<Integer> getTransaction0() {
        return (QueujTransaction)Component.getInstance(SeamTransaction.class, true);
    }

    @Override
    protected ProcessPersistence<ProcessEntity<Integer>,Integer> getPersistence0(String queueOwner, Map<String, Object> server_options) {
        return (ProcessPersistence)Component.getInstance(ProcessImplHome.class, true);
    }

    @Override
    public <T> Callback<T> getAsyncCallback0(final Callback<T> callback) {
        return new Callback<T>() {
            private Asynchronous async = (new Asynchronous() {

                @Override
                public void execute(Object timer) {
                    (new ContextualAsynchronousRequest(timer) {

                        @Override
                        protected void process() {
                            callback.action();
                        }
                    }).run();
                }

                @Override
                protected void handleException(Exception exception, Object timer) {}

            });

            @Override
            protected void doAction() {
                async.execute(null);
            }
        };
    }

}
