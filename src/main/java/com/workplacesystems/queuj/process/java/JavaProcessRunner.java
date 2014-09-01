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

import com.workplacesystems.queuj.process.ProcessOutputable;
import java.io.Serializable;
import java.util.Locale;


/**
 *
 * @author dave
 */
public class JavaProcessRunner implements Serializable
{
    // Increase the number when an incompatible change is made
    private static final long serialVersionUID = JavaProcessRunner.class.getName().hashCode();

    private transient JavaProcessSession java_process_session;

    private transient ProcessOutputable output;
    
    /** Creates a new instance of JavaProcessRunner */
    public JavaProcessRunner()
    {
    }

    protected void setDetails(JavaProcessRunner java_process_runner)
    {
        this.java_process_session = java_process_runner.java_process_session;
    }

    // must be called with an anonymous doRun so as to implement the process...
    public void setDetails(JavaProcessSession java_process_session, ProcessOutputable output)
    {
        this.java_process_session = java_process_session;
        this.output = output;
    }

    protected final ProcessOutputable getProcessOutputable()
    {
        return output;
    }

    /** overridden by IntegrationRunner */
    public Locale getLocale()
    {
        return Locale.getDefault();
    }

    public final void zapSavedValues ()
    {
        java_process_session.zapSavedValues();
    }

    public Object putValue(String key, Serializable o)
    {
        return java_process_session.putValue(key, o);
    }

    public Object removeValue(String key)
    {
        return java_process_session.removeValue(key);
    }

    public Object getValue(String key)
    {
        return java_process_session.getValue(key);
    }

    /** default to normal (sequential) sections */
    protected boolean useRunnerControlledSections ()
    {
        return false;
    }

    /** default to sequential increment, but runner could override */
    protected int incrementCurrentSection (int current_section)
    {
        return current_section + 1;
    }

    /** construct any additional process sections required
     * - different runner classes have different sections, so they have to handle this
     *   rather than JavaProcessBuilder / IntegrationProcessBuilder
     * - defaults to none */
    public void setupAdditionalProcessSections(JavaProcessSession java_process_session) {}
}
