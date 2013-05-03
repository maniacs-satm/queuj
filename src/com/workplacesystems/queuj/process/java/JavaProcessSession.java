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

import com.workplacesystems.utilsj.collections.IterativeCallback;
import java.io.Serializable;
import java.util.HashMap;

import com.workplacesystems.utilsj.collections.FilterableArrayList;
import java.util.Map.Entry;
import java.util.UUID;

/**
 *
 * @author dave
 */
public final class JavaProcessSession<S extends ProcessSection> implements Serializable
{
    // Increase the number when an incompatible change is made
    private static final long serialVersionUID = -2093320559L;

    private FilterableArrayList<S> sections = new FilterableArrayList();
    private int current_section = 0;
    private S failure_section = null;

    // HashMap of values stored by process
    private HashMap<String, Serializable> saved_values = new HashMap();
 
    /** flag as to whether sections are normal, or controlled by runner */
    private boolean runner_controlled_sections = false;
  
    private UUID uuid;

    /** Creates a new instance of JavaProcessSession */
    public JavaProcessSession()
    {
        uuid = UUID.randomUUID();
    }

    /** method to set up a JavaProcessSection (inner class here) + all parameters */
    public void addSection(S section)
    {
        sections.add(section);
    }

    public void setFailureSection(S section)
    {
        failure_section = section;
    }

    public S getCurrentSection()
    {
        return sections.get(current_section);
    }
    
    public S getSection(int index)
    {
        return sections.get(index);
    }

    public S getFailureSection()
    {
        return failure_section;
    }
    
    /** set by runner as to whether uses runner-controlled sections */
    public void setRunnerControlledSections (JavaProcessRunner runner)
    {
        runner_controlled_sections = runner.useRunnerControlledSections();
    }
    
    /** normally just increments, but for Integration, defers to the run object */
    public void incrementCurrentSection()
    {
        if (runner_controlled_sections) 
        {
            // assume the current run object is at least a JavaProcessRunner!
            current_section = getCurrentSection().incrementCurrentSection(current_section);
        }
        else
        {
            // as previously 
            current_section++;
        }
    }
    
    /** simply counts vs number of sections,
     *  but for Integration, the current section may have been fiddled previously */
    public boolean hasMoreSections()
    {
        return current_section >= 0 & current_section < sections.size();
    }

    public void resetCurrentSection()
    {
        current_section = 0;
        zapSavedValues();
    }

    public boolean hasFailureSection()
    {
        return failure_section != null;
    }
    
    public void zoomToEndOfProcess()
    {
        current_section = -99;
    }

    public <R> R iterateValues(IterativeCallback<Entry<String,Serializable>,R> ic) {
        return ic.iterate(new FilterableArrayList<Entry<String,Serializable>>(saved_values.entrySet()));
    }

    /** replace values with a new map*/
    public void zapSavedValues ()
    {
        uuid = UUID.randomUUID();
        saved_values = new HashMap<String,Serializable>();
    }
 
    public Serializable putValue(String o1, Serializable o2)
    {
        uuid = UUID.randomUUID();
        return saved_values.put(o1, o2);
    }
    
    public Serializable removeValue(String o)
    {
        uuid = UUID.randomUUID();
        return saved_values.remove(o);
    }
    
    public Serializable getValue(String o)
    {
        return saved_values.get(o);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final JavaProcessSession other = (JavaProcessSession) obj;
        if (this.current_section != other.current_section) {
            return false;
        }
        if (this.uuid != other.uuid && (this.uuid == null || !this.uuid.equals(other.uuid))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + this.current_section;
        hash = 37 * hash + (this.uuid != null ? this.uuid.hashCode() : 0);
        return hash;
    }
}
