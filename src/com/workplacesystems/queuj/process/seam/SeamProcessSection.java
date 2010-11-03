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

import com.workplacesystems.queuj.process.ProcessParameters;
import com.workplacesystems.queuj.process.java.JavaProcessRunner;
import com.workplacesystems.queuj.process.java.JavaProcessSession;
import com.workplacesystems.queuj.process.java.ProcessSection;
import com.workplacesystems.utilsj.collections.IterativeCallback;
import java.io.Serializable;
import java.util.Map.Entry;
import org.jboss.seam.Component;
import org.jboss.seam.contexts.Context;
import org.jboss.seam.contexts.Contexts;

/**
 *
 * @author dave
 */
public class SeamProcessSection extends ProcessSection<Class,Object> {

    protected SeamProcessSection(Class run_object, String run_method_str, Class[] param_types, Object[] params)
    {
        super(run_object, run_method_str, param_types, params);
    }

    @Override
    protected Object getRunner(Class run_object) {
        return Component.getInstance(run_object);
    }

    protected Integer invokeSection(final JavaProcessSession session, ProcessParameters parameters) {
        final Context context = Contexts.getEventContext();
        parameters.iterateValues(new IterativeCallback<Entry<String, Serializable>, Void>() {
            @Override
            protected void nextObject(Entry<String, Serializable> entry) {
                context.set(entry.getKey(), entry.getValue());
            }
        });
        session.iterateValues(new IterativeCallback<Entry<String, Serializable>, Void>() {
            @Override
            protected void nextObject(Entry<String, Serializable> entry) {
                context.set(entry.getKey(), entry.getValue());
            }
        });
        JavaProcessRunner runner = new JavaProcessRunner();
        runner.setDetails(session);
        context.set("PROCESS_RUNNER", runner);

        return super.invokeSection();
    }
}
