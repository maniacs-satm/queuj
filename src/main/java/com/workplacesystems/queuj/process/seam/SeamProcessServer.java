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

import com.workplacesystems.queuj.process.BatchProcessServer;
import com.workplacesystems.queuj.process.ForceProcessComplete;
import com.workplacesystems.queuj.process.ForceRescheduleException;
import com.workplacesystems.queuj.process.ProcessOutputable;
import com.workplacesystems.queuj.process.ProcessWrapper;
import com.workplacesystems.queuj.process.java.JavaProcessServer;
import com.workplacesystems.queuj.process.java.JavaProcessSession;
import com.workplacesystems.queuj.utils.QueujException;

/**
 *
 * @author dave
 */
public class SeamProcessServer extends JavaProcessServer {

    @Override
    protected Integer runProcess(ProcessWrapper process, boolean failureRun) {
        try
        {
            JavaProcessSession<SeamProcessSection> jps = getJavaProcessSession(process);

            ProcessOutputable output = process.getProcessOutputable();

            if (failureRun)
                jps.getFailureSection().invokeSection(jps, output, process.getParameters());
            else
            {
                Integer result_code = jps.getCurrentSection().invokeSection(jps, output, process.getParameters());

                // If section completed succesfully so increment current section
                if (result_code == null || result_code.equals(BatchProcessServer.SUCCESS))
                    incrementCurrentSection(jps);

                // if a non-null result was returned, use that (ignore the cache thing below!)
                if (result_code != null)
                    return result_code;
            }
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
        catch (Exception e)
        {
            // any exception puts into error state
            new QueujException(e);
            return new Integer(1);
        }

        // default to ok
        return new Integer(0);
    }

    protected void incrementCurrentSection(JavaProcessSession jps) {
        // by default, simple increment in JavaProcessSession
        jps.incrementCurrentSection();
    }

    private JavaProcessSession<SeamProcessSection> getJavaProcessSession(ProcessWrapper process) {
        return (JavaProcessSession<SeamProcessSection>)getParameter(process, JAVA_PROCESS_SESSION);
    }
}
