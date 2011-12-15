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
import com.workplacesystems.queuj.QueueBuilder;
import com.workplacesystems.queuj.QueueRestriction;
import com.workplacesystems.queuj.process.ProcessIndexes;
import com.workplacesystems.queuj.process.ProcessIndexesCallback;
import com.workplacesystems.queuj.process.ProcessWrapper;
import com.workplacesystems.queuj.process.seam.SeamProcessBuilder;
import com.workplacesystems.utilsj.collections.helpers.HasLessThan;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;

/**
 *
 * @author dave
 */
@Name("initialiser")
@Startup(depends={"queujInitialiser"})
@Scope(ScopeType.APPLICATION)
public class Initialiser {

    @In
    public Queue<SeamProcessBuilder> DEFAULT_QUEUE;

    @Out
    public Queue<SeamProcessBuilder> SAMPLE_QUEUE;

    @Create
    public void init() {
        QueueBuilder<SeamProcessBuilder> qb = DEFAULT_QUEUE.newQueueBuilder();
        qb.setQueueRestriction(new TestQueueRestriction());
        SAMPLE_QUEUE = qb.newQueue();
    }

    public static class TestQueueRestriction extends QueueRestriction {

        @Override
        protected boolean canRun(final Queue queue, Process process) {
            return process.getContainingServer().indexesWithReadLock(new ProcessIndexesCallback<Boolean>() {

                public Boolean readIndexes(ProcessIndexes pi) {
                    HasLessThan<ProcessWrapper> hasLessThen = new HasLessThan<ProcessWrapper>(100);
                    hasLessThen = pi.iterateRunningProcesses(queue, hasLessThen);
                    pi.iterateWaitingToRunProcesses(queue, hasLessThen);
                    return hasLessThen.hasLess();
                }
            });
        }
    }
}
