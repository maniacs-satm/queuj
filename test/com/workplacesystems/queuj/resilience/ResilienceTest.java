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

package com.workplacesystems.queuj.resilience;

import com.workplacesystems.queuj.occurrence.RunFiniteTimes;
import com.workplacesystems.queuj.occurrence.RunOnce;
import com.workplacesystems.queuj.occurrence.RunWeekly;
import com.workplacesystems.queuj.schedule.RelativeScheduleBuilder;
import com.workplacesystems.queuj.schedule.WeeklyScheduleBuilder;
import java.util.GregorianCalendar;
import junit.framework.TestCase;

/**
 *
 * @author dave
 */
public class ResilienceTest extends TestCase {

    private RunOnce runOnce;

    private RunWeekly runWeekly;

    private RunFiniteTimes failureOccurrence;

    private GregorianCalendar scheduleStart;

    private GregorianCalendar now;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        runOnce = new RunOnce();
        RelativeScheduleBuilder rsb = runOnce.newRelativeScheduleBuilder();
        rsb.setRunImmediately();
        rsb.createSchedule();

        runWeekly = new RunWeekly();
        WeeklyScheduleBuilder weeklyScheduleBuilder = runWeekly.newSchedulerBuilder();
        weeklyScheduleBuilder.setScheduledDay(GregorianCalendar.MONDAY);
        weeklyScheduleBuilder.setScheduledHour(9);
        weeklyScheduleBuilder.setScheduledMinute(30);
        weeklyScheduleBuilder.createSchedule();

        failureOccurrence = new RunFiniteTimes(3);
        RelativeScheduleBuilder failureBuilder = failureOccurrence.newRelativeScheduleBuilder();
        failureBuilder.setRunDelayHours(1);
        failureBuilder.createSchedule();
        failureBuilder.setRunDelayDays(1);
        failureBuilder.createSchedule();
        failureBuilder.setRunDelayWeeks(1);
        failureBuilder.createSchedule();

        scheduleStart = new GregorianCalendar(2012, 0, 1, 12, 0, 0);
        now = new GregorianCalendar();
    }

    public void testRunOnlyOnce() {
        RunOnlyOnce roo = new RunOnlyOnce();

        GregorianCalendar when = roo.getNextRunTime(runOnce, false, 0, scheduleStart, 0);
        assertTrue(scheduleStart.equals(when));

        when = roo.getNextRunTime(runOnce, true, 1, scheduleStart, 0);
        assertNull(when);

        roo.setFailureSchedule(failureOccurrence);
        when = roo.getNextRunTime(runOnce, false, 0, scheduleStart, 0);
        assertTrue(scheduleStart.equals(when));

        when = roo.getNextRunTime(runOnce, true, 1, now, 0);
        GregorianCalendar when2 = (GregorianCalendar)now.clone();
        when2.add(GregorianCalendar.HOUR_OF_DAY, 1);
        assertTrue(when.equals(when2));

        when = roo.getNextRunTime(runOnce, true, 2, now, 0);
        when2 = (GregorianCalendar)now.clone();
        when2.add(GregorianCalendar.DAY_OF_YEAR, 1);
        assertTrue(when.equals(when2));

        when = roo.getNextRunTime(runOnce, true, 3, now, 0);
        when2 = (GregorianCalendar)now.clone();
        when2.add(GregorianCalendar.WEEK_OF_YEAR, 1);
        assertTrue(when.equals(when2));

        when = roo.getNextRunTime(runOnce, true, 4, now, 0);
        assertNull(when);

        roo = new RunOnlyOnce();
        when = roo.getNextRunTime(runWeekly, false, 0, scheduleStart, 0);
        when2 = (GregorianCalendar)scheduleStart.clone();
        when2.set(GregorianCalendar.DAY_OF_WEEK, GregorianCalendar.MONDAY);
        when2.set(GregorianCalendar.HOUR_OF_DAY, 9);
        when2.set(GregorianCalendar.MINUTE, 30);
        when2.set(GregorianCalendar.SECOND, 0);
        when2.set(GregorianCalendar.MILLISECOND, 0);
        when2.set(GregorianCalendar.YEAR, now.get(GregorianCalendar.YEAR));
        when2.set(GregorianCalendar.WEEK_OF_YEAR, now.get(GregorianCalendar.WEEK_OF_YEAR));
        if (when2.after(now))
            when2.add(GregorianCalendar.WEEK_OF_YEAR, -1);
        assertTrue(when.equals(when2));

        roo.setFailureSchedule(failureOccurrence);
        when = roo.getNextRunTime(runWeekly, false, 0, scheduleStart, 0);
        assertTrue(when.equals(when2));

        when = roo.getNextRunTime(runWeekly, true, 1, now, 0);
        when2 = (GregorianCalendar)now.clone();
        when2.add(GregorianCalendar.HOUR_OF_DAY, 1);
        assertTrue(when.equals(when2));

        when = roo.getNextRunTime(runWeekly, true, 2, now, 0);
        when2 = (GregorianCalendar)now.clone();
        when2.add(GregorianCalendar.DAY_OF_YEAR, 1);
        assertTrue(when.equals(when2));

        when = roo.getNextRunTime(runWeekly, true, 3, now, 0);
        when2 = (GregorianCalendar)now.clone();
        when2.add(GregorianCalendar.WEEK_OF_YEAR, 1);
        assertTrue(when.equals(when2));

        when = roo.getNextRunTime(runWeekly, true, 4, now, 0);
        when2 = (GregorianCalendar)now.clone();
        when2.set(GregorianCalendar.DAY_OF_WEEK, GregorianCalendar.MONDAY);
        when2.set(GregorianCalendar.HOUR_OF_DAY, 9);
        when2.set(GregorianCalendar.MINUTE, 30);
        when2.set(GregorianCalendar.SECOND, 0);
        when2.set(GregorianCalendar.MILLISECOND, 0);
        if (when2.before(now))
            when2.add(GregorianCalendar.WEEK_OF_YEAR, 1);
        assertTrue(when.equals(when2));
    }

    public void testCatchUp() {
        CatchUp cu = new CatchUp();

        GregorianCalendar when = cu.getNextRunTime(runOnce, false, 0, scheduleStart, 0);
        GregorianCalendar when2 = (GregorianCalendar)scheduleStart.clone();
        assertTrue(when.equals(when2));

        when = cu.getNextRunTime(runOnce, true, 1, scheduleStart, 0);
        assertNull(when);

        cu.setFailureSchedule(failureOccurrence);
        when = cu.getNextRunTime(runOnce, false, 0, scheduleStart, 0);
        assertTrue(scheduleStart.equals(when));

        when = cu.getNextRunTime(runOnce, true, 1, now, 0);
        when2 = (GregorianCalendar)now.clone();
        when2.add(GregorianCalendar.HOUR_OF_DAY, 1);
        assertTrue(when.equals(when2));

        when = cu.getNextRunTime(runOnce, true, 2, now, 0);
        when2 = (GregorianCalendar)now.clone();
        when2.add(GregorianCalendar.DAY_OF_YEAR, 1);
        assertTrue(when.equals(when2));

        when = cu.getNextRunTime(runOnce, true, 3, now, 0);
        when2 = (GregorianCalendar)now.clone();
        when2.add(GregorianCalendar.WEEK_OF_YEAR, 1);
        assertTrue(when.equals(when2));

        when = cu.getNextRunTime(runOnce, true, 4, now, 0);
        assertNull(when);

        cu = new CatchUp();
        when = cu.getNextRunTime(runWeekly, false, 0, scheduleStart, 0);
        when2 = (GregorianCalendar)scheduleStart.clone();
        when2.set(GregorianCalendar.DAY_OF_WEEK, GregorianCalendar.MONDAY);
        when2.set(GregorianCalendar.HOUR_OF_DAY, 9);
        when2.set(GregorianCalendar.MINUTE, 30);
        when2.set(GregorianCalendar.SECOND, 0);
        when2.set(GregorianCalendar.MILLISECOND, 0);
        if (when2.before(scheduleStart))
            when2.add(GregorianCalendar.WEEK_OF_YEAR, 1);
        assertTrue(when.equals(when2));

        cu.setFailureSchedule(failureOccurrence);
        when = cu.getNextRunTime(runWeekly, false, 0, scheduleStart, 0);
        assertTrue(when.equals(when2));

        when = cu.getNextRunTime(runWeekly, true, 1, now, 0);
        when2 = (GregorianCalendar)now.clone();
        when2.add(GregorianCalendar.HOUR_OF_DAY, 1);
        assertTrue(when.equals(when2));

        when = cu.getNextRunTime(runWeekly, true, 2, now, 0);
        when2 = (GregorianCalendar)now.clone();
        when2.add(GregorianCalendar.DAY_OF_YEAR, 1);
        assertTrue(when.equals(when2));

        when = cu.getNextRunTime(runWeekly, true, 3, now, 0);
        when2 = (GregorianCalendar)now.clone();
        when2.add(GregorianCalendar.WEEK_OF_YEAR, 1);
        assertTrue(when.equals(when2));

        when = cu.getNextRunTime(runWeekly, true, 4, now, 0);
        when2 = (GregorianCalendar)now.clone();
        when2.set(GregorianCalendar.DAY_OF_WEEK, GregorianCalendar.MONDAY);
        when2.set(GregorianCalendar.HOUR_OF_DAY, 9);
        when2.set(GregorianCalendar.MINUTE, 30);
        when2.set(GregorianCalendar.SECOND, 0);
        when2.set(GregorianCalendar.MILLISECOND, 0);
        if (when2.before(now))
            when2.add(GregorianCalendar.WEEK_OF_YEAR, 1);
        assertTrue(when.equals(when2));
    }

    public void testForgetMissed() {
        ForgetMissed fm = new ForgetMissed();

        GregorianCalendar when = fm.getNextRunTime(runOnce, false, 0, scheduleStart, 0);
        assertTrue(scheduleStart.equals(when));

        when = fm.getNextRunTime(runOnce, true, 1, scheduleStart, 0);
        assertNull(when);

        fm.setFailureSchedule(failureOccurrence);
        when = fm.getNextRunTime(runOnce, false, 0, scheduleStart, 0);
        assertTrue(scheduleStart.equals(when));

        when = fm.getNextRunTime(runOnce, true, 1, now, 0);
        GregorianCalendar when2 = (GregorianCalendar)now.clone();
        when2.add(GregorianCalendar.HOUR_OF_DAY, 1);
        assertTrue(when.equals(when2));

        when = fm.getNextRunTime(runOnce, true, 2, now, 0);
        when2 = (GregorianCalendar)now.clone();
        when2.add(GregorianCalendar.DAY_OF_YEAR, 1);
        assertTrue(when.equals(when2));

        when = fm.getNextRunTime(runOnce, true, 3, now, 0);
        when2 = (GregorianCalendar)now.clone();
        when2.add(GregorianCalendar.WEEK_OF_YEAR, 1);
        assertTrue(when.equals(when2));

        when = fm.getNextRunTime(runOnce, true, 4, now, 0);
        assertNull(when);

        fm = new ForgetMissed();
        when = fm.getNextRunTime(runWeekly, false, 0, scheduleStart, 0);
        when2 = (GregorianCalendar)scheduleStart.clone();
        when2.set(GregorianCalendar.DAY_OF_WEEK, GregorianCalendar.MONDAY);
        when2.set(GregorianCalendar.HOUR_OF_DAY, 9);
        when2.set(GregorianCalendar.MINUTE, 30);
        when2.set(GregorianCalendar.SECOND, 0);
        when2.set(GregorianCalendar.MILLISECOND, 0);
        when2.set(GregorianCalendar.YEAR, now.get(GregorianCalendar.YEAR));
        when2.set(GregorianCalendar.WEEK_OF_YEAR, now.get(GregorianCalendar.WEEK_OF_YEAR));
        if (when2.before(now))
            when2.add(GregorianCalendar.WEEK_OF_YEAR, 1);
        assertTrue(when.equals(when2));

        fm.setFailureSchedule(failureOccurrence);
        when = fm.getNextRunTime(runWeekly, false, 0, scheduleStart, 0);
        assertTrue(when.equals(when2));

        when = fm.getNextRunTime(runWeekly, true, 1, now, 0);
        when2 = (GregorianCalendar)now.clone();
        when2.add(GregorianCalendar.HOUR_OF_DAY, 1);
        assertTrue(when.equals(when2));

        when = fm.getNextRunTime(runWeekly, true, 2, now, 0);
        when2 = (GregorianCalendar)now.clone();
        when2.add(GregorianCalendar.DAY_OF_YEAR, 1);
        assertTrue(when.equals(when2));

        when = fm.getNextRunTime(runWeekly, true, 3, now, 0);
        when2 = (GregorianCalendar)now.clone();
        when2.add(GregorianCalendar.WEEK_OF_YEAR, 1);
        assertTrue(when.equals(when2));

        when = fm.getNextRunTime(runWeekly, true, 4, now, 0);
        when2 = (GregorianCalendar)now.clone();
        when2.set(GregorianCalendar.DAY_OF_WEEK, GregorianCalendar.MONDAY);
        when2.set(GregorianCalendar.HOUR_OF_DAY, 9);
        when2.set(GregorianCalendar.MINUTE, 30);
        when2.set(GregorianCalendar.SECOND, 0);
        when2.set(GregorianCalendar.MILLISECOND, 0);
        if (when2.before(now))
            when2.add(GregorianCalendar.WEEK_OF_YEAR, 1);
        assertTrue(when.equals(when2));
    }
}
