/*
 * Copyright 2015 Workplace Systems PLC (http://www.workplacesystems.com/).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.workplacesystems.queuj.process;

import com.workplacesystems.queuj.QueueFactory;
import com.workplacesystems.queuj.occurrence.RunFiniteTimes;
import com.workplacesystems.queuj.occurrence.RunOnce;
import com.workplacesystems.queuj.process.java.JavaProcessSection;
import com.workplacesystems.queuj.process.java.JavaProcessServer;
import com.workplacesystems.queuj.process.java.JavaProcessSession;
import com.workplacesystems.queuj.process.jpa.ProcessDAO;
import com.workplacesystems.queuj.process.jpa.ProcessImpl;
import com.workplacesystems.queuj.resilience.RunOnlyOnce;
import com.workplacesystems.queuj.schedule.RelativeScheduleBuilder;
import com.workplacesystems.utilsj.Callback;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import junit.framework.TestCase;

/**
 *
 * @author dave
 */
public class QueueInitialisationTest extends TestCase {

    private final static int ID = 1337;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        System.setProperty("com.workplacesystems.queuj.QueujFactory", TestQueujFactory.class.getName());
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        System.clearProperty("com.workplacesystems.queuj.QueujFactory");
        QueujFactory.setInstance();
    }

    public void testNotRunProcess() {
        ProcessImpl pi = setUpProcess();
        pi.setStatus(ProcessEntity.Status.NOT_RUN);
        TestQueujFactory.setProcess(pi);

        QueujFactory.setInstance();
        ProcessImplServer<Integer> ps = (ProcessImplServer<Integer>)QueujFactory.getProcessServer((String)null, null);

        ProcessWrapper<Integer> process = ps.get(ID);
        process.attach();

        process.delete();

        // Job should be scheduled by initialisation therefore the job should be run and complete
        assertTrue(process.isComplete());
    }

    public void testFailedProcess() {
        ProcessImpl pi = setUpProcess();
        pi.setStatus(ProcessEntity.Status.RUN_ERROR);
        pi.setAttempt(1);
        TestQueujFactory.setProcess(pi);

        QueujFactory.setInstance();
        ProcessImplServer<Integer> ps = (ProcessImplServer<Integer>)QueujFactory.getProcessServer((String)null, null);

        ProcessWrapper<Integer> process = ps.get(ID);
        process.attach();

        process.delete();

        // No failure schedule exists so the job should still be failed
        assertTrue(process.isFailed());
    }

    public void testFailedProcessWithFailureSchedule() {
        ProcessImpl pi = setUpProcess();
        pi.setStatus(ProcessEntity.Status.RUN_ERROR);
        pi.setAttempt(1);
        setRunImmediatelyFailureSchedule(pi);

        TestQueujFactory.setProcess(pi);

        QueujFactory.setInstance();
        ProcessImplServer<Integer> ps = (ProcessImplServer<Integer>)QueujFactory.getProcessServer((String)null, null);

        ProcessWrapper<Integer> process = ps.get(ID);
        do {
            process.attach();
        } while (process.isFailed() && process.getNextRunTime() != null);

        process.delete();

        // Failure schedule exists so the job should run and complete
        assertTrue(process.isComplete());
    }

    public void testRestartedProcess() {
        ProcessImpl pi = setUpProcess();
        pi.setStatus(ProcessEntity.Status.RESTARTED);
        TestQueujFactory.setProcess(pi);

        QueujFactory.setInstance();
        ProcessImplServer<Integer> ps = (ProcessImplServer<Integer>)QueujFactory.getProcessServer((String)null, null);

        ProcessWrapper<Integer> process = ps.get(ID);
        process.attach();

        process.delete();

        // Restart should be honoured by the initialisation and the job should be run and complete
        assertTrue(process.isComplete());
    }

    public void testInvalidAttemptProcess() {
        ProcessImpl pi = setUpProcess();
        pi.setStatus(ProcessEntity.Status.RUN_ERROR);
        pi.setAttempt(0);
        TestQueujFactory.setProcess(pi);

        QueujFactory.setInstance();
        ProcessImplServer<Integer> ps = (ProcessImplServer<Integer>)QueujFactory.getProcessServer((String)null, null);

        ProcessWrapper<Integer> process = ps.get(ID);
        process.attach();

        process.delete();

        // No exception should be thrown due to the invalid attempt count
        // As run attempt is invalid job should be treated as if not run and therefore run and complete
        assertTrue(process.isComplete());
    }

    public void testRunningProcess() {
        ProcessImpl pi = setUpProcess();
        pi.setStatus(ProcessEntity.Status.RUNNING);
        TestQueujFactory.setProcess(pi);

        QueujFactory.setInstance();
        ProcessImplServer<Integer> ps = (ProcessImplServer<Integer>)QueujFactory.getProcessServer((String)null, null);

        ProcessWrapper<Integer> process = ps.get(ID);
        process.attach();

        process.delete();

        // No failure schedule exists so the job should not be run and set to failed
        assertTrue(process.isFailed());
    }

    public void testRunningProcessWithFailureSchedule() {
        ProcessImpl pi = setUpProcess();
        pi.setStatus(ProcessEntity.Status.RUNNING);
        setRunImmediatelyFailureSchedule(pi);

        TestQueujFactory.setProcess(pi);

        QueujFactory.setInstance();
        ProcessImplServer<Integer> ps = (ProcessImplServer<Integer>)QueujFactory.getProcessServer((String)null, null);

        ProcessWrapper<Integer> process = ps.get(ID);
        do {
            process.attach();
        } while (process.isFailed() && process.getNextRunTime() != null);

        process.delete();

        // Failure schedule exists so the job should be re-run and complete
        assertTrue(process.isComplete());
    }

    private ProcessImpl setUpProcess() {
        ProcessImpl pi = new ProcessImpl();
        pi.setProcessId(ID);
        pi.setCreationTimestamp(new Date());
        pi.setProcessName("Test");
        pi.setQueue(QueueFactory.DEFAULT_QUEUE);
        pi.setResilience(new RunOnlyOnce());

        RunOnce occurrence = new RunOnce();
        RelativeScheduleBuilder rsb = occurrence.newRelativeScheduleBuilder();
        rsb.setRunImmediately();
        rsb.createSchedule();

        pi.setOccurrence(occurrence);
        pi.setParameters(new ProcessParameters());
        pi.getParameters().setValue(JavaProcessServer.JAVA_PROCESS_SESSION, new JavaProcessSession<JavaProcessSection>());
        return pi;
    }

    private void setRunImmediatelyFailureSchedule(ProcessImpl pi) {
        RunFiniteTimes failureOccurrence = new RunFiniteTimes(1);
        RelativeScheduleBuilder failureBuilder = failureOccurrence.newRelativeScheduleBuilder();
        failureBuilder.setRunImmediately();
        failureBuilder.createSchedule();
        pi.getResilience().setFailureSchedule(failureOccurrence);
    }

    private static class TestQueujFactory extends QueujFactoryImpl {

        private static ProcessImpl process;

        static void setProcess(ProcessImpl _process) {
            process = _process;
        }

        public TestQueujFactory() {
        }

        @Override
        protected void init() {
            super.init();

            ((ProcessImplServer)getProcessServer0(null, null)).init();
        }

        @Override
        protected ProcessDAO getProcessDAO0() {
            return new ProcessDAO() {

                @Override
                public List<String> findQueueOwners() {
                    return new ArrayList<String>();
                }

                @Override
                public List<ProcessImpl> findProcesses(String queueOwner) {
                    if (process == null)
                        return new ArrayList<ProcessImpl>();
                    return Arrays.asList(process);
                }
            };
        }

        @Override
        protected QueujTransaction<Integer> getTransaction0() {
            return new DefaultTransaction() {

                @Override
                public <T> T doTransaction(ProcessWrapper<Integer> process, Callback<T> callback, boolean doStart) {
                    return doTransaction(process.getQueueOwner(), false, callback, doStart);
                }

                @Override
                public <T> T doTransaction(ProcessWrapper<Integer> process, Callback<T> callback, Callback<Void> commitCallback, boolean doStart) {
                    return doTransaction(process.getQueueOwner(), false, callback, commitCallback, doStart);
                }
            };
        }

        @Override
        protected ProcessPersistence<ProcessEntity<Integer>, Integer> getPersistence0(String queueOwner, Map<String, Object> server_options) {
            return new ProcessPersistence<ProcessEntity<Integer>, Integer>() {

                @Override
                public void clearInstance() {
                }

                @Override
                public ProcessEntity<Integer> getInstance() {
                    return process;
                }

                @Override
                public void setId(Integer id) {
                }

                @Override
                public String persist() {
                    return null;
                }

                @Override
                public String update() {
                    return null;
                }

                @Override
                public String remove() {
                    return null;
                }
            };
        }
    }
}
