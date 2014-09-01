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

package com.workplacesystems.queuj;

import com.workplacesystems.queuj.Process;
import com.workplacesystems.queuj.occurrence.RunFiniteTimes;
import com.workplacesystems.queuj.occurrence.RunOnce;
import com.workplacesystems.queuj.process.QueujFactory;
import com.workplacesystems.queuj.process.java.JavaProcessBuilder;
import com.workplacesystems.queuj.process.java.JavaProcessRunner;
import com.workplacesystems.queuj.resilience.RunOnlyOnce;
import com.workplacesystems.queuj.schedule.RelativeScheduleBuilder;
import java.util.Locale;
import static junit.framework.Assert.assertEquals;
import junit.framework.TestCase;

/**
 *
 * @author dave
 */
public class UnexpectedFailureTest extends TestCase {

    private Queue<JavaProcessBuilder> RESTRICTION_FAILURE_QUEUE;

    private Queue<JavaProcessBuilder> RESTRICTION_FAIL_ONCE_QUEUE;

    private RunOnce runOnceOccurrence;

    private Resilience runOnlyOnce;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // Init the queue
        QueujFactory.getProcessServer((String)null, null);

        QueueBuilder<JavaProcessBuilder> qb = QueueFactory.DEFAULT_QUEUE.newQueueBuilder();
        qb.setQueueRestriction(new FailQueueRestriction());
        RESTRICTION_FAILURE_QUEUE = qb.newQueue();

        qb = QueueFactory.DEFAULT_QUEUE.newQueueBuilder();
        qb.setQueueRestriction(new FailOnceRestriction());
        RESTRICTION_FAIL_ONCE_QUEUE = qb.newQueue();

        runOnceOccurrence = new RunOnce();
        RelativeScheduleBuilder rsb = runOnceOccurrence.newRelativeScheduleBuilder();
        rsb.setRunImmediately();
        rsb.createSchedule();

        RunFiniteTimes failureOccurrence = new RunFiniteTimes(1);
        RelativeScheduleBuilder failureBuilder = failureOccurrence.newRelativeScheduleBuilder();
        failureBuilder.setRunImmediately();
        failureBuilder.createSchedule();

        runOnlyOnce = new RunOnlyOnce();
        runOnlyOnce.setFailureSchedule(failureOccurrence);
    }

    public static class FailQueueRestriction extends QueueRestriction {

        @Override
        protected boolean isPredictable() {
            return true;
        }

        @Override
        protected boolean canRun(final Queue queue, Process process) {
            throw new NullPointerException();
        }
    }

    public static class FailOnceRestriction extends QueueRestriction {

        @Override
        protected boolean isPredictable() {
            return true;
        }

        @Override
        protected boolean canRun(final Queue queue, Process process) {
            if (process.getAttempt() < 1)
                throw new NullPointerException();
            return true;
        }
    }

    public static class EmptyRunner extends JavaProcessRunner {

        public void run() {}
    }

    public void testRestrictionFailure() {
        JavaProcessBuilder pb = RESTRICTION_FAILURE_QUEUE.newProcessBuilder(Locale.getDefault());
        pb.setProcessName("FailTest");
        pb.setProcessDescription("Restriction Failure Test");
        pb.setProcessPersistence(false);

        pb.setProcessOccurrence(runOnceOccurrence);

        pb.setProcessDetails(new EmptyRunner(), "run", new Class[] {}, new Object[] {});

        Process process = pb.newProcess();

        process.attach();

        assertEquals(process.isFailed(), true);
        assertEquals(process.getAttempt(), 1);

        pb.setProcessResilience(runOnlyOnce);

        process = pb.newProcess();

        do {
            process.attach();
        } while (process.isFailed() && process.getNextRunTime() != null);

        assertEquals(process.isFailed(), true);
        assertEquals(process.getAttempt(), 2);

        pb = RESTRICTION_FAIL_ONCE_QUEUE.newProcessBuilder(Locale.getDefault());
        pb.setProcessName("FailTest2");
        pb.setProcessDescription("Restriction Failure Test2");
        pb.setProcessPersistence(false);

        pb.setProcessOccurrence(runOnceOccurrence);

        pb.setProcessDetails(new EmptyRunner(), "run", new Class[] {}, new Object[] {});

        pb.setProcessResilience(runOnlyOnce);

        process = pb.newProcess();

        do {
            process.attach();
        } while (process.isFailed() && process.getNextRunTime() != null);

        assertEquals(process.isComplete(), true);
        assertEquals(process.getAttempt(), 0);
    }
}
