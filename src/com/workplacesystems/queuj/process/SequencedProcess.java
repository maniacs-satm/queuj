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

import java.io.Serializable;

import com.workplacesystems.queuj.Process;
/**
 * An action that can be run as part of a sequence.  The iterator which runs a sequence of such processes
 * should pass the success/fail status of each one into the next.  Implementors can choose to handle a previous failure 
 * and do some further processing, or simply propagate the failure forwards by throwing an exception.  
 */
public interface SequencedProcess extends Serializable
{
    /** Check if this process needs to be provided with a transaction - determines which of the actions will be invoked */
    public boolean needsTransaction();
    
    public void action(Process parent_process, String userId, boolean previous_failed);
}