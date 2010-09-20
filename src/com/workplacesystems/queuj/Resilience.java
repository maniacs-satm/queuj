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

import java.io.Serializable;
import java.util.GregorianCalendar;

import com.workplacesystems.queuj.occurrence.RunFiniteTimes;
import com.workplacesystems.queuj.resilience.RunOnlyOnce;
import com.workplacesystems.queuj.schedule.RelativeScheduleBuilder;

/**
 * The Resilience class provides the resilience of a Process.
 * It does this in 2 ways. Firstly it specifies what happens when
 * a Process misses its scheduled run time and secondly provides
 * a schedule for automatically running failed Processes.
 *
 * If a Process misses 1 or more of its scheduled runs it can either
 * forget about the missed runs and will therefore run at its next scheduled
 * time or it can run once for any number of missed runs or 'catch up' by
 * running every missed run.
 *
 * If a Process fails this class provides a schedule for automatically
 * reattempting runs. For instance it could try 15 minutes later, then
 * 4 hours later and then 24 hours later.
 *
 * @author dave
 */
public abstract class Resilience implements Serializable
{
    // Increase the number when an incompatible change is made
    private static final long serialVersionUID = Resilience.class.getName().hashCode() + 1;

    /** The failure schedule. */
    private RunFiniteTimes failure_schedule;

    /** Creates a new instance of Resilience */
    public Resilience()
    {
    }
    
    /** create a simple Resilience object so process can restart - runs once, with retries as parameters  */
    public static Resilience createRunOnceResilience(int retry_count, int retry_interval)
    {
        Resilience resilience = new RunOnlyOnce();
        RunFiniteTimes failure_occurrence = new RunFiniteTimes(retry_count);
        resilience.setFailureSchedule(failure_occurrence);

        for (int i=0 ; i < retry_count ; i++)
        {
            RelativeScheduleBuilder rsb = failure_occurrence.newRelativeScheduleBuilder(); 
            rsb.setRunDelayMinutes(retry_interval * (i+1));
            rsb.createSchedule();
        }
        
        return resilience;
    }

    /**
     * Set the failure Occurence.
     */
    public void setFailureSchedule(RunFiniteTimes failure_schedule)
    {
        this.failure_schedule = failure_schedule;
        failure_schedule.INDENT = "  ";
    }

    public boolean hasFailureSchedule()
    {
        return failure_schedule != null;
    }

    private final static String new_line = System.getProperty("line.separator");

    /**
     * Generates a unique String for this Resilience object.
     */
    @Override
    public String toString()
    {
        String ts = getClass().getName();
        if (failure_schedule != null)
        {
            ts += "  {" + new_line;
            ts += "    occurrence: " + failure_schedule.toString() + new_line;
            ts += "  }";
        }
        return ts;
    }

    /**
     * Get the next run time/date for this Resilience.
     */
    public final GregorianCalendar getNextRunTime(Occurrence occurrence, boolean is_failed, int attempt_count, GregorianCalendar schedule_start, int run_count)
    {
        if (is_failed)
        {
            if (failure_schedule == null)
                return null;

            return failure_schedule.getNextRunTime(schedule_start, --attempt_count);
        }

        return getAdjustedNextRunTime(occurrence, schedule_start, run_count, new GregorianCalendar());
    }

    protected abstract GregorianCalendar getAdjustedNextRunTime(Occurrence occurrence, GregorianCalendar schedule_start, int run_count, GregorianCalendar now);
}
