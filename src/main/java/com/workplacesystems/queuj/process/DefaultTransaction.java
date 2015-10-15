/*
 * Copyright 2015 Workplace Systems PLC (http://www.workplacesystems.com/).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.workplacesystems.queuj.process;

import com.workplacesystems.queuj.utils.QueujException;
import com.workplacesystems.utilsj.Callback;

/**
 *
 * @author dave
 */
public class DefaultTransaction implements QueujTransaction<Integer> {

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
}
