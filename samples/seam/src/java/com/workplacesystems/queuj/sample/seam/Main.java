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

import com.workplacesystems.queuj.Queue;
import com.workplacesystems.queuj.occurrence.RunOnce;
import com.workplacesystems.queuj.process.seam.SeamProcessBuilder;
import com.workplacesystems.queuj.schedule.RelativeScheduleBuilder;
import java.util.Locale;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;

/**
 *
 * @author dave
 */
@Name("main")
public class Main {

    @In
    public Queue<SeamProcessBuilder> SAMPLE_QUEUE;

    public void runJob1() {

        SeamProcessBuilder pb = SAMPLE_QUEUE.newProcessBuilder(Locale.getDefault());
        pb.setProcessName("Test 1");
        pb.setProcessDescription("Test 1");
        pb.setProcessDetails(TestRunner.class, "run", new Class[] {}, new Object[] {});
        pb.setProcessPersistence(false);
        pb.setParameter("random", Math.random());
        pb.newProcess();
    }

    public void runJob2() {

        SeamProcessBuilder pb = SAMPLE_QUEUE.newProcessBuilder(Locale.getDefault());
        pb.setProcessName("Test 2");
        pb.setProcessPersistence(false);

        for (int i = 0; i<=200; i++) {
            pb.setProcessDescription("Test 2");
            pb.setProcessDetails(TestRunner.class, "run", new Class[] {}, new Object[] {});
            pb.setParameter("random", Math.random());

            RunOnce occurrence = new RunOnce();
            RelativeScheduleBuilder rsb = occurrence.newRelativeScheduleBuilder();
            rsb.setRunDelaySeconds((int)(Math.random() * 60));
            rsb.createSchedule();
            pb.setProcessOccurrence(occurrence);

            pb.newProcess();
        }
    }

    public void runJob3() {
        SeamProcessBuilder pb = SAMPLE_QUEUE.newProcessBuilder(Locale.getDefault());
        pb.setProcessName("Test 3");
        pb.setProcessDescription("Test 3");
        pb.setProcessDetails(TestRunner.class, "run", new Class[] {}, new Object[] {});
        pb.setProcessPersistence(true);
        pb.setParameter("random", Math.random());
        pb.newProcess();
    }

    public void runJob4() {
        SeamProcessBuilder pb = SAMPLE_QUEUE.newProcessBuilder(Locale.getDefault());
        pb.setProcessName("Test 4");

        for (int i = 0; i<=200; i++) {
            pb.setProcessDescription("Test 4");
            pb.setProcessDetails(TestRunner.class, "run", new Class[] {}, new Object[] {});
            pb.setParameter("random", Math.random());

            RunOnce occurrence = new RunOnce();
            RelativeScheduleBuilder rsb = occurrence.newRelativeScheduleBuilder();
            rsb.setRunDelaySeconds((int)(Math.random() * 60));
            rsb.createSchedule();
            pb.setProcessOccurrence(occurrence);

            pb.newProcess();
        }
    }

    public void runJob5() {
        SeamProcessBuilder pb = SAMPLE_QUEUE.newProcessBuilder(Locale.getDefault());
        pb.setProcessName("Test 5");
        pb.setProcessDescription("Test 5");
        pb.setProcessDetails(Test2Runner.class, "run", new Class[] {}, new Object[] {});
        pb.addProcessSection(Test2Runner.class, "waitForProcess", new Class[] {}, new Object[] {});
        pb.setProcessPersistence(true);
        pb.setParameter("count", 200);
        pb.newProcess();
    }
}
