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
import java.util.Iterator;

import com.workplacesystems.utilsj.collections.FilterableArrayList;
import com.workplacesystems.utilsj.collections.IterativeCallback;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
//import org.jboss.seam.contexts.Context;

/**
 * If {@link Object#equals(Object)} is ever implemented properly, then code that
 * relied on the equals-implementation only doing reference comparison and doing
 * a {@link #clone()} to get a different value must be re-arranged to first do the
 * clone and then do the changes to the objects.
 */
public class ProcessParameters implements Serializable {

    // Increase the number when an incompatible change is made
    private static final long serialVersionUID = ProcessParameters.class.getName().hashCode() + 4;

    static final String PRE_PROCESSES = "PRE_PROCESSES";

    static final String POST_PROCESSES = "POST_PROCESSES";

    private UUID uuid;

    private HashMap<String, Serializable> parameters = new HashMap<String, Serializable>();

    ProcessParameters() {
        uuid = UUID.randomUUID();
    }

    void iteratePreProcesses(IterativeCallback<SequencedProcess, Void> ic) {
        FilterableArrayList<SequencedProcess> pre_processes = (FilterableArrayList<SequencedProcess>)getValue(PRE_PROCESSES);
        if (pre_processes != null)
            ic.iterate(pre_processes);
    }

    void iteratePostProcesses(IterativeCallback<SequencedProcess, Void> ic) {
        FilterableArrayList<SequencedProcess> post_processes = (FilterableArrayList<SequencedProcess>)getValue(POST_PROCESSES);
        if (post_processes != null)
            ic.iterate(post_processes);
    }

    Serializable setValue(String key, Serializable value) {
        uuid = UUID.randomUUID();
        return parameters.put(key, value);
    }

    Serializable getValue(String key) {
        return parameters.get(key);
    }

    public <R> R iterateValues(IterativeCallback<Entry<String,Serializable>,R> ic) {
        return ic.iterate(new FilterableArrayList<Entry<String,Serializable>>(parameters.entrySet()));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ProcessParameters other = (ProcessParameters) obj;
        if (this.uuid != other.uuid && (this.uuid == null || !this.uuid.equals(other.uuid))) {
            return false;
        }
        if (this.parameters != other.parameters && (this.parameters == null || !mapsEqual(other.parameters))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + (this.uuid != null ? this.uuid.hashCode() : 0);
        hash = 53 * hash + (this.parameters != null ? this.parameters.hashCode() : 0);
        return hash;
    }

    public boolean mapsEqual(Object o) {
	if (o == parameters)
	    return true;

	if (!(o instanceof Map))
	    return false;
	Map<String, Serializable> m = (Map<String, Serializable>) o;
	if (m.size() != parameters.size())
	    return false;

        try {
            Iterator<Map.Entry<String, Serializable>> i = parameters.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry<String, Serializable> e = i.next();
                String key = e.getKey();
                Serializable value = e.getValue();
                if (value == null) {
                    if (!(m.get(key)==null && m.containsKey(key)))
                        return false;
                } else {
                    if (value instanceof long[]) {
                        if (!Arrays.equals((long[])value, (long[])m.get(key)))
                            return false;
                    } else if (value instanceof int[]) {
                        if (!Arrays.equals((int[])value, (int[])m.get(key)))
                            return false;
                    } else if (value instanceof short[]) {
                        if (!Arrays.equals((short[])value, (short[])m.get(key)))
                            return false;
                    } else if (value instanceof char[]) {
                        if (!Arrays.equals((char[])value, (char[])m.get(key)))
                            return false;
                    } else if (value instanceof byte[]) {
                        if (!Arrays.equals((byte[])value, (byte[])m.get(key)))
                            return false;
                    } else if (value instanceof boolean[]) {
                        if (!Arrays.equals((boolean[])value, (boolean[])m.get(key)))
                            return false;
                    } else if (value instanceof double[]) {
                        if (!Arrays.equals((double[])value, (double[])m.get(key)))
                            return false;
                    } else if (value instanceof float[]) {
                        if (!Arrays.equals((float[])value, (float[])m.get(key)))
                            return false;
                    } else if (value instanceof Object[]) {
                        if (!Arrays.equals((Object[])value, (Object[])m.get(key)))
                            return false;
                    } else
                        if (!value.equals(m.get(key)))
                            return false;
                }
            }
        } catch (ClassCastException unused) {
            return false;
        } catch (NullPointerException unused) {
            return false;
        }

	return true;
    }
}

