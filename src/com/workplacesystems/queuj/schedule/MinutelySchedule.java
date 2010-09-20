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

package com.workplacesystems.queuj.schedule;

import java.util.Calendar;
import java.util.GregorianCalendar;

import com.workplacesystems.queuj.Schedule;

/**
 *
 * @author dave
 */
public class MinutelySchedule extends Schedule
{
    // Increase the number when an incompatible change is made
    private static final long serialVersionUID = MinutelySchedule.class.getName().hashCode() + 1;

    private final int interval_minutes;

    /** Creates a new instance of MinutelySchedule */
    MinutelySchedule(int interval_minutes)
    {
        this.interval_minutes = interval_minutes;
    }

    public int getIntervalMinutes()
    {
        return interval_minutes;
    }

    @Override
    protected GregorianCalendar getNextRunTime(GregorianCalendar schedule_start)
    {
        GregorianCalendar next_run = (GregorianCalendar)schedule_start.clone();
        next_run.set(Calendar.SECOND, 0);
        next_run.set(Calendar.MILLISECOND, 0);
        if (!next_run.after(schedule_start))
            next_run.add(Calendar.MINUTE, interval_minutes);
        else
            next_run.add(Calendar.MINUTE, interval_minutes - 1);
        return next_run;
    }

    @Override
    protected String getSelfString()
    {
        return ", interval_minutes = " + String.valueOf(interval_minutes);
    }
}
