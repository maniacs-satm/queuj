/*
 * Copyright 2015 Workplace Systems PLC (http://www.workplacesystems.com/).
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

package com.workplacesystems.queuj;

import com.workplacesystems.queuj.occurrence.RunOnce;
import com.workplacesystems.queuj.process.QueujFactory;
import com.workplacesystems.queuj.process.SequencedProcess;
import com.workplacesystems.queuj.process.java.JavaProcessBuilder;
import com.workplacesystems.queuj.schedule.RelativeScheduleBuilder;
import java.util.Locale;
import junit.framework.TestCase;

/**
 *
 * @author dave
 */
public class SequencedProcessesTest extends TestCase {

    private RunOnce runOnceOccurrence;

    private Resilience runOnlyOnce;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // Init the queue
        QueujFactory.getProcessServer((String)null, null);

        runOnceOccurrence = new RunOnce();
        RelativeScheduleBuilder rsb = runOnceOccurrence.newRelativeScheduleBuilder();
        rsb.setRunImmediately();
        rsb.createSchedule();
    }

    public void testPreProcess() {
        JavaProcessBuilder pb = QueueFactory.DEFAULT_QUEUE.newProcessBuilder(Locale.getDefault());
        pb.setProcessName("PreProcessTest");
        pb.setProcessDescription("PreProcess Test");
        pb.setProcessPersistence(false);

        pb.setProcessOccurrence(runOnceOccurrence);

        TestSequencedProcess preProcess = new TestSequencedProcess();
        pb.setPreProcess(preProcess);

        pb.setProcessDetails(new EmptyRunner(), "run", new Class[] {}, new Object[] {});

        Process process = pb.newProcess();

        process.attach();

        assertTrue(preProcess.hasRun());
    }

    public void testPostProcess() {
        JavaProcessBuilder pb = QueueFactory.DEFAULT_QUEUE.newProcessBuilder(Locale.getDefault());
        pb.setProcessName("PreProcessTest");
        pb.setProcessDescription("PreProcess Test");
        pb.setProcessPersistence(false);

        pb.setProcessOccurrence(runOnceOccurrence);

        TestSequencedProcess postProcess = new TestSequencedProcess();
        pb.setPostProcess(postProcess);

        pb.setProcessDetails(new EmptyRunner(), "run", new Class[] {}, new Object[] {});

        Process process = pb.newProcess();

        process.attach();

        assertTrue(postProcess.hasRun());
    }

    public static class TestSequencedProcess implements SequencedProcess {

        private boolean hasRun = false;

        @Override
        public boolean needsTransaction() {
            return false;
        }

        @Override
        public void action(Process parent_process, String userId, boolean previous_failed) {
            hasRun = true;
        }

        public boolean hasRun() {
            return hasRun;
        }
    }
}
