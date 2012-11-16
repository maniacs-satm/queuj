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

package com.workplacesystems.queuj.output;

import com.workplacesystems.queuj.Output;
import com.workplacesystems.queuj.process.ProcessOutputable;

/** Default Output manager for batch jobs.  This {@link Output} implementation does nothing. */
public class NoOutput extends Output
{
    private static final long serialVersionUID = NoOutput.class.getName().hashCode() + 1;

    public NoOutput()
    {
        super();
    }

    @Override
    public ProcessOutputable getOutputable(String queueOwner, String userId, String process_name, String report_type, String source_name)
    {
        return null;
    }

    @Override
    public Boolean producesOutput() {
        return false;
    }
}