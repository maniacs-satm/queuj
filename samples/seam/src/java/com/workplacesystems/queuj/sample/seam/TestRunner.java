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

package com.workplacesystems.queuj.sample.seam;

import com.workplacesystems.queuj.process.java.JavaProcessRunner;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;

/**
 *
 * @author dave
 */
@Name("testRunner")
public class TestRunner {

    @In
    public JavaProcessRunner PROCESS_RUNNER;

    @In
    public double random;

    private static final Object mutex = new Object();

    private static int running = 0;
    private static int totalStarted = 0;

    public void run() {
        try {
            long sleepTime = (long)(random * 60000);
            int count;
            synchronized (mutex) {
                count = ++totalStarted;
                running++;
                System.out.println("(" + count + ") Sleeping for " + sleepTime);
            }
            Thread.sleep(sleepTime);
            synchronized (mutex) {
                running--;
                System.out.println("(" + count + ") Finished. " + running + " still running.");
            }
        } catch (InterruptedException ex) {}
    }
}
