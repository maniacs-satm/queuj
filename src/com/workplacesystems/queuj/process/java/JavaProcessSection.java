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

/**
 *
 * @author dave
 */
public class JavaProcessSection extends ProcessSection<JavaProcessRunner,JavaProcessRunner> {

    // Increase the number when an incompatible change is made
    private static final long serialVersionUID = JavaProcessSection.class.getName().hashCode() + 1;

    protected JavaProcessSection(JavaProcessRunner run_object, String run_method_str, Class[] param_types, Object[] params)
    {
        super(run_object, run_method_str, param_types, params);
    }

    @Override
    protected int incrementCurrentSection(int current_section) {
        return getRunObject().incrementCurrentSection(current_section);
    }

    protected Integer invokeSection(JavaProcessSession session) {
        // run object is a JavaProcessRunner
        getRunObject().setDetails(session);
        return super.invokeSection();
    }
}
