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
import java.io.Serializable;
import java.util.GregorianCalendar;

/**
 *
 * @author dave
 */
public interface ProcessServer<K extends Serializable & Comparable> extends com.workplacesystems.queuj.ProcessServer {
    
    public void submitProcess(ProcessWrapper<K> process);

    boolean contains(ProcessWrapper<K> process);

    public void delete(ProcessWrapper<K> process);

    public <T> T readLocked(Callback<T> callback);

    public <T> T writeLocked(Callback<T> callback);

    public Object getMutex();

    public ProcessScheduler getProcessScheduler();

    public boolean addProcessToIndex(ProcessWrapper<K> process);

    public boolean removeProcessFromIndex(ProcessWrapper<K> process);

    public boolean scheduleOverride(ProcessWrapper<K> process, GregorianCalendar nextRun);
}
