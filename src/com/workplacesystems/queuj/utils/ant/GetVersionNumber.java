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

package com.workplacesystems.queuj.utils.ant;

import java.lang.reflect.Method;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 *
 * @author dave
 */
public class GetVersionNumber extends Task {

    private String propertyName;
    private String propertyDescName;
    private String versionClass;

    public void setPropertyName(String propertyName) { this.propertyName = propertyName; }
    public void setPropertyDescName(String propertyDescName) { this.propertyDescName = propertyDescName; }
    public void setVersionClass(String versionClass) { this.versionClass = versionClass; }

    @Override
    public void execute() throws BuildException {

        try {
            log("Retrieving version from: " + versionClass);
            Method m = Class.forName(versionClass).getMethod("getVersion", (Class[])null);
            Method m2 = Class.forName(versionClass).getMethod("getVersionDesc", (Class[])null);
            String version = (String)m.invoke((Object)null, (Object[])null);
            String versionDesc = (String)m2.invoke((Object)null, (Object[])null);
            getProject().setNewProperty(propertyName, version);
            getProject().setNewProperty(propertyDescName, versionDesc);
            log("Version: " + versionDesc);
        }
        catch (Exception e) {
            throw new BuildException(e);
        }
    }
}
