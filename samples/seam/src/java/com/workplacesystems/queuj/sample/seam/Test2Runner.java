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

import com.workplacesystems.queuj.Process;
import com.workplacesystems.queuj.Queue;
import com.workplacesystems.queuj.occurrence.RunOnce;
import com.workplacesystems.queuj.process.java.JavaProcessRunner;
import com.workplacesystems.queuj.process.seam.SeamProcessBuilder;
import com.workplacesystems.queuj.schedule.RelativeScheduleBuilder;
import com.workplacesystems.queuj.utils.QueujException;
import java.util.ArrayList;
import java.util.Locale;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;

/**
 *
 * @author dave
 */
@Name("test2Runner")
public class Test2Runner {

    @In
    public JavaProcessRunner PROCESS_RUNNER;

    @In
    public Queue<SeamProcessBuilder> SAMPLE_QUEUE;

    @In
    private int count;

    @In(required=false)
    private ArrayList<Process> processes;

    public void run() {
        System.out.println("Creating " + count + " jobs");
        SeamProcessBuilder pb = SAMPLE_QUEUE.newProcessBuilder(Locale.getDefault());
        pb.setProcessName("Test 2 part 2");
        pb.setProcessKeepCompleted(true);

        processes = new ArrayList<Process>();
        PROCESS_RUNNER.putValue("processes", processes);
        for (int i = 0; i<=count; i++) {
            pb.setProcessDescription("Test 5 part 2: " + i);
            pb.setProcessDetails(TestRunner.class, "run", new Class[] {}, new Object[] {});
            pb.setParameter("random", Math.random());

            RunOnce occurrence = new RunOnce();
            RelativeScheduleBuilder rsb = occurrence.newRelativeScheduleBuilder();
            rsb.setRunDelaySeconds((int)(Math.random() * 60));
            rsb.createSchedule();
            pb.setProcessOccurrence(occurrence);

            processes.add(pb.newProcess());
        }
        System.out.println("Created " + count + " jobs");
    }

    public void waitForProcess() {
        boolean failed_process = false;
        System.out.println("Waiting for " + processes.size() + " jobs");
        for (Process process : processes) {
            do
            {
                process.attach();
            } while (process.isFailed() && process.getNextRunTime() != null);

            if (process.isFailed())
                failed_process = true;

            System.out.println("Deleteing process " + process.getProcessKey());
            process.delete();
        }

        System.out.println("Finished waiting for " + processes.size() + " jobs");

        if (failed_process)
            throw new QueujException("Test2Runner failed.");
    }
}
