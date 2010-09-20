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

package com.workplacesystems.queuj.process.java;

import java.util.Locale;

import com.workplacesystems.queuj.Process;
import com.workplacesystems.queuj.ProcessBuilder;
import com.workplacesystems.queuj.Queue;

/**
 *
 * @author dave
 */
public class JavaProcessBuilder extends ProcessBuilder
{
    private JavaProcessSession java_process_session = new JavaProcessSession<JavaProcessSection>();

    /**
     * Creates a new instance of JavaProcessBuilder
     */
    public JavaProcessBuilder(Queue queue, Locale locale)
    {
        super(queue, locale);
    }

    public void setProcessDetails(JavaProcessRunner run_object, String run_method_str, Class[] param_types, Object[] params)
    {
        java_process_session = new JavaProcessSession();
        addProcessSection(run_object, run_method_str, param_types, params);
    }

    public void addProcessSection(JavaProcessRunner run_object, String run_method_str, Class[] param_types, Object[] params)
    {
        java_process_session.addSection(new JavaProcessSection(run_object, run_method_str, param_types, params));
    }

    public void setFailureSection(JavaProcessRunner run_object, String run_method_str, Class[] param_types, Object[] params)
    {
        java_process_session.setFailureSection(new JavaProcessSection(run_object, run_method_str, param_types, params));
    }

    @Override
    protected void setupProcess(Process process)
    {
        process.setParameter(JavaProcessServer.JAVA_PROCESS_SESSION, java_process_session);
    }

    /** used when sections are controlled by the runner (so dynamically, looped etc) rather than in a predetermined sequence */
    public void setRunnerControlledSections (JavaProcessRunner runner)
    {
        java_process_session.setRunnerControlledSections (runner);
        runner.setupAdditionalProcessSections(java_process_session);
    }
}
