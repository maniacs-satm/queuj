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
public class HourlySchedule extends Schedule
{
    // Increase the number when an incompatible change is made
    private static final long serialVersionUID = HourlySchedule.class.getName().hashCode() + 1;

    private final int interval_hours;
    private final int scheduled_minute;

    /** Creates a new instance of HourlySchedule */
    HourlySchedule(int interval_hours, int scheduled_minute)
    {
        this.interval_hours = interval_hours;
        this.scheduled_minute = scheduled_minute;
    }

    public int getIntervalHours()
    {
        return interval_hours;
    }

    public int getScheduledMinute()
    {
        return scheduled_minute;
    }

    @Override
    protected GregorianCalendar getNextRunTime(GregorianCalendar schedule_start)
    {
        GregorianCalendar next_run = (GregorianCalendar)schedule_start.clone();
        next_run.set(Calendar.MINUTE, scheduled_minute);
        next_run.set(Calendar.SECOND, 0);
        next_run.set(Calendar.MILLISECOND, 0);
        if (!next_run.after(schedule_start))
            next_run.add(Calendar.HOUR, interval_hours);
        else
            next_run.add(Calendar.HOUR, interval_hours - 1);
        return next_run;
    }

    @Override
    protected String getSelfString()
    {
        return ", interval_hours = " + String.valueOf(interval_hours) +
            ", scheduled_minute = " + String.valueOf(scheduled_minute);
    }
}
