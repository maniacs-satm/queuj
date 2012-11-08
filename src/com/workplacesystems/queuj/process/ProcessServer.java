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

import com.workplacesystems.queuj.QueueOwner;
import com.workplacesystems.utilsj.Callback;

/**
 *
 * @author dave
 */
public interface ProcessServer extends com.workplacesystems.queuj.ProcessServer {
    
    public ProcessWrapper getNewProcess(QueueOwner queueOwner, boolean is_persistent);

    public void submitProcess(ProcessWrapper process);

    boolean contains(ProcessWrapper process);

    public void delete(ProcessWrapper process);

    public <T> T withReadLock(Callback<T> callback);

    public Object getMutex();

    public ProcessScheduler getProcessScheduler();

    public boolean addProcessToIndex(ProcessWrapper process);

    public boolean removeProcessFromIndex(ProcessWrapper process);
}
