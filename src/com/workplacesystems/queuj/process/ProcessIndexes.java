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

import com.workplacesystems.queuj.Queue;
import com.workplacesystems.utilsj.collections.IterativeCallback;

/**
 *
 * @author dave
 */
public interface ProcessIndexes {

    public int countOfNotRunProcesses(Queue queue);

    public int countOfNotRunProcesses(Queue queue, Object index);

    public int countOfRunningProcesses(Queue queue);

    public int countOfRunningProcesses(Queue queue, Object index);

    public int countOfWaitingToRunProcesses(Queue queue);

    public int countOfWaitingToRunProcesses(Queue queue, Object index);

    public int countOfFailedProcesses(Queue queue);

    public int countOfFailedProcesses(Queue queue, Object index);

    public <R> R iterateNotRunProcesses(Queue queue, IterativeCallback<ProcessWrapper,R> ic);

    public <R> R iterateNotRunProcesses(Queue queue, Object key, IterativeCallback<ProcessWrapper,R> ic);

    public <R> R iterateRunningProcesses(Queue queue, IterativeCallback<ProcessWrapper,R> ic);

    public <R> R iterateRunningProcesses(Queue queue, Object key, IterativeCallback<ProcessWrapper,R> ic);

    public <R> R iterateWaitingToRunProcesses(Queue queue, IterativeCallback<ProcessWrapper,R> ic);

    public <R> R iterateWaitingToRunProcesses(Queue queue, Object key, IterativeCallback<ProcessWrapper,R> ic);

    public <R> R iterateFailedProcesses(Queue queue, IterativeCallback<ProcessWrapper,R> ic);

    public <R> R iterateFailedProcesses(Queue queue, Object key, IterativeCallback<ProcessWrapper,R> ic);
}
