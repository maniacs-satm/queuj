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

import com.workplacesystems.queuj.Queue;
import com.workplacesystems.queuj.QueueBuilder;
import com.workplacesystems.queuj.QueueFactory;
import com.workplacesystems.queuj.occurrence.RunOnce;
import com.workplacesystems.queuj.process.QueujFactory;
import com.workplacesystems.queuj.resilience.RunOnlyOnce;
import com.workplacesystems.queuj.restriction.JavaQueueRestriction;
import com.workplacesystems.queuj.schedule.RelativeScheduleBuilder;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;
import org.jboss.seam.contexts.Contexts;

/**
 *
 * @author dave
 */
@Name("queujInitialiser")
@Startup(depends={"entityManager"})
@Scope(ScopeType.APPLICATION)
public class QueujInitialiser {

    @Create
    public void init() {
        System.setProperty("com.workplacesystems.queuj.QueujFactory", "com.workplacesystems.queuj.process.seam.SeamFactory");

        RunOnce occurrence = new RunOnce();
        RelativeScheduleBuilder rsb = occurrence.newRelativeScheduleBuilder();
        rsb.setRunImmediately();
        rsb.createSchedule();

        QueueBuilder<SeamProcessBuilder> qb = QueueFactory.ROOT_QUEUE.newQueueBuilder(SeamProcessBuilder.class);
        qb.setQueueRestriction(new JavaQueueRestriction());
        qb.setBatchProcessServerClass(SeamProcessServer.class);
        qb.setDefaultOccurrence(occurrence);
        qb.setDefaultResilience(new RunOnlyOnce());
        Queue<SeamProcessBuilder> seamQueue = qb.newQueue();
        Contexts.getApplicationContext().set("DEFAULT_QUEUE", seamQueue);

        QueujFactory.getProcessServer((String)null);
    }
}
