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

import com.workplacesystems.queuj.Process;
import com.workplacesystems.queuj.ProcessBuilder;
import com.workplacesystems.queuj.Queue;
import com.workplacesystems.queuj.process.java.JavaProcessServer;
import com.workplacesystems.queuj.process.java.JavaProcessSession;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 *
 * @author dave
 */
public class SeamProcessBuilder extends ProcessBuilder {

    private JavaProcessSession<SeamProcessSection> java_process_session = new JavaProcessSession<SeamProcessSection>();

    private HashMap<String,Serializable> parameters = new HashMap<String, Serializable>();

    public SeamProcessBuilder(Queue queue, Locale locale)
    {
        super(queue, locale);
    }

    public void setProcessDetails(Class run_class, String run_method_str, Class[] param_types, Object[] params)
    {
        java_process_session = new JavaProcessSession();
        addProcessSection(run_class, run_method_str, param_types, params);
    }

    public void addProcessSection(Class run_class, String run_method_str, Class[] param_types, Object[] params)
    {
        java_process_session.addSection(new SeamProcessSection(run_class, run_method_str, param_types, params));
    }

    public void setFailureSection(Class run_class, String run_method_str, Class[] param_types, Object[] params)
    {
        java_process_session.setFailureSection(new SeamProcessSection(run_class, run_method_str, param_types, params));
    }

    public Serializable setParameter(String key, Serializable value) {
        return parameters.put(key, value);
    }

    @Override
    protected <K extends Serializable & Comparable> void setupProcess(Process<K> process)
    {
        process.setParameter(JavaProcessServer.JAVA_PROCESS_SESSION, java_process_session);
        for (Map.Entry<String,Serializable> entry : parameters.entrySet())
            process.setParameter(entry.getKey(), entry.getValue());
    }
}
