/*
 * Copyright 2012 Workplace Systems PLC (http://www.workplacesystems.com/).
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

package com.workplacesystems.queuj.test;

import com.workplacesystems.queuj.Process;
import com.workplacesystems.queuj.Queue;
import com.workplacesystems.queuj.QueueBuilder;
import com.workplacesystems.queuj.QueueFactory;
import com.workplacesystems.queuj.QueueRestriction;
import com.workplacesystems.queuj.occurrence.RunOnce;
import com.workplacesystems.queuj.process.ProcessIndexes;
import com.workplacesystems.queuj.process.ProcessIndexesCallback;
import com.workplacesystems.queuj.process.ProcessWrapper;
import com.workplacesystems.queuj.process.QueujFactory;
import com.workplacesystems.queuj.process.java.JavaProcessBuilder;
import com.workplacesystems.queuj.process.java.JavaProcessRunner;
import com.workplacesystems.queuj.schedule.RelativeScheduleBuilder;
import com.workplacesystems.utilsj.collections.helpers.HasLessThan;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Locale;

/**
 *
 * @author dave
 */
public class QueuJPerfTest {

    private final static Queue<JavaProcessBuilder> PERF_QUEUE;

    static {
        QueueBuilder<JavaProcessBuilder> qb = QueueFactory.DEFAULT_QUEUE.newQueueBuilder();
        qb.setQueueRestriction(new PerfQueueRestriction());
        PERF_QUEUE = qb.newQueue();
    }

    public static class PerfQueueRestriction extends QueueRestriction {

        @Override
        protected boolean isPredictable() {
            return true;
        }

        @Override
        protected boolean canRun(final Queue queue, com.workplacesystems.queuj.Process process) {
            return process.getContainingServer().indexesWithReadLock(new ProcessIndexesCallback<Boolean>() {

                public Boolean readIndexes(ProcessIndexes pi) {
                    HasLessThan<ProcessWrapper> hasLessThen = new HasLessThan<ProcessWrapper>(1);
                    hasLessThen = pi.iterateRunningProcesses(queue, hasLessThen);
                    pi.iterateWaitingToRunProcesses(queue, hasLessThen);
                    return hasLessThen.hasLess();
                }
            });
        }
    }

    public QueuJPerfTest() {}

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        // Init the queue
        QueujFactory.getProcessServer((String)null, null);

        QueuJPerfTest test = new QueuJPerfTest();
        System.out.println(test.run(false));
    }

    public String run() {
        return run(true);
    }

    private String run(boolean persistent) {
        StringBuffer buffer = new StringBuffer();

        JavaProcessBuilder pb = PERF_QUEUE.newProcessBuilder(Locale.getDefault());
        pb.setProcessName("Test 2");
        pb.setProcessDescription("Perf Test");
        pb.setProcessPersistence(persistent);

        RunOnce occurrence = new RunOnce();
        RelativeScheduleBuilder rsb = occurrence.newRelativeScheduleBuilder();
        rsb.setRunImmediately();
        rsb.createSchedule();
        pb.setProcessOccurrence(occurrence);

        pb.setProcessDetails(new PerfRunner(), "run", new Class[] {}, new Object[] {});

        ArrayList<Process> processes = new ArrayList<Process>();

        GregorianCalendar start = new GregorianCalendar();

        for (int i = 0; i<1000; i++) {
            processes.add(pb.newProcess());
        }

        for (Process process : processes)
            process.attach();

        GregorianCalendar finish = new GregorianCalendar();

        buffer.append("Time taken was ");
        buffer.append(finish.getTimeInMillis() - start.getTimeInMillis());
        buffer.append("ms");

        return buffer.toString();
    }

    public static class PerfRunner extends JavaProcessRunner {

        public void run() {} // Do nothing so only overhead is the queue.
    }
}
