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

import com.workplacesystems.queuj.process.BatchProcessServer;
import com.workplacesystems.queuj.process.ForceProcessComplete;
import com.workplacesystems.queuj.process.ForceRescheduleException;
import com.workplacesystems.queuj.utils.QueujException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 *
 * @author dave
 */
public abstract class ProcessSection<T,S> implements Serializable
{
    // Increase the number when an incompatible change is made
    private static final long serialVersionUID = ProcessSection.class.getName().hashCode() + 1;

    // parameters for full reflective method call
    private String run_method_str;
    private Class[] param_types;
    private Object[] params;

    private T run_object;
    private transient Method run_method;

    /** constructor with all content */
    protected ProcessSection(T run_object, String run_method_str, Class[] param_types, Object[] params)
    {
        this.run_method_str = run_method_str;
        this.param_types = param_types;
        this.params = params;

        this.run_object = run_object;
    }

    protected ProcessSection(T run_object, String run_method_str)
    {
        this(run_object, run_method_str, new Class [0], new Object [0]);
    }

    // retrieve from list within outer class if missing (since transient)
    public T getRunObject()
    {
        return run_object;
    }

    protected S getRunner(T run_object) {
        return (S)run_object;
    }

    protected int incrementCurrentSection(int current_section) {
        return current_section;
    }

    /** reflectively runs the relevant method */
    protected Integer invokeSection()
    {
        try
        {
            // run object is a JavaProcessRunner
            S runner = getRunner(getRunObject());

            if (run_method_str == null)
                return null;

            createRunMethod(param_types, runner);

            Object[] local_params = (Object[])params.clone();

            Integer result_code = (Integer) run_method.invoke(runner, local_params);

            return result_code;
        }
        // Allow ProcessImpl exceptions to be rethrown
        catch (ForceProcessComplete fpc)
        {
            throw fpc;
        }
        catch (ForceRescheduleException fre)
        {
            throw fre;
        }
        catch (InvocationTargetException ite)
        {
            Throwable cause = ite.getCause();
            if (cause instanceof ForceProcessComplete|| cause instanceof ForceRescheduleException)
                throw (RuntimeException)cause;

            new QueujException(ite);
            return BatchProcessServer.FAILURE;
        }
        catch (Exception e)
        {
            new QueujException(e);
            return BatchProcessServer.FAILURE;
        }
    }

    private void createRunMethod(Class[] param_types, Object local_run_object)
    throws NoSuchMethodException
    {
        if (run_method != null)
            return;

        Class run_class = local_run_object.getClass();
        run_method = run_class.getMethod(run_method_str, param_types);
    }
}
