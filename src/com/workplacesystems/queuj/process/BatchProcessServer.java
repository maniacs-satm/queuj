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
import java.util.HashMap;

/**
 *
 * @author Dave
 */
public abstract class BatchProcessServer {

    /** return code for success */
    public final static Integer SUCCESS = new Integer (0);

    /** return code for standard failure */
    public final static Integer FAILURE = new Integer (1);

    private transient HashMap<String,Serializable> local_parameters;

    protected boolean hasMoreSections(ProcessWrapper process, boolean firstSection) { return firstSection; }

    protected boolean hasFailureSection(ProcessWrapper process) { return false; }

    protected void resetSection(ProcessWrapper process, boolean previousRunFailed) {}

    protected abstract Integer runProcess(ProcessWrapper process, boolean failureRun);

    protected Serializable getParameter(ProcessWrapper process, String parameter_name)
    {
        if (local_parameters == null)
            local_parameters = new HashMap<String,Serializable>();

        Serializable parameter_value = local_parameters.get(parameter_name);
        if (parameter_value == null)
        {
            parameter_value = process.getParameter(parameter_name);
            local_parameters.put(parameter_name, parameter_value);
            return parameter_value;
        }

        Serializable previous_value = process.setParameter(parameter_name, parameter_value);
        return parameter_value;
    }
}
