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
public class WeeklySchedule extends Schedule
{
    // Increase the number when an incompatible change is made
    private static final long serialVersionUID = WeeklySchedule.class.getName().hashCode() + 1;

    private final int interval_weeks;
    private final int scheduled_day;
    private final int scheduled_hour;
    private final int scheduled_minute;

    /** Creates a new instance of WeeklySchedule */
    WeeklySchedule(int interval_weeks, int scheduled_day, int scheduled_hour, int scheduled_minute)
    {
        this.interval_weeks = interval_weeks;
        this.scheduled_day = scheduled_day;
        this.scheduled_hour = scheduled_hour;
        this.scheduled_minute = scheduled_minute;
    }

    public int getIntervalWeeks()
    {
        return interval_weeks;
    }

    public int getScheduledDay()
    {
        return scheduled_day;
    }

    public int getScheduledHour()
    {
        return scheduled_hour;
    }

    public int getScheduledMinute()
    {
        return scheduled_minute;
    }

    @Override
    protected GregorianCalendar getNextRunTime(GregorianCalendar schedule_start)
    {
        GregorianCalendar next_run = (GregorianCalendar)schedule_start.clone();
        next_run.set(Calendar.DAY_OF_WEEK, scheduled_day);
        next_run.set(Calendar.HOUR_OF_DAY, scheduled_hour);
        next_run.set(Calendar.MINUTE, scheduled_minute);
        next_run.set(Calendar.SECOND, 0);
        next_run.set(Calendar.MILLISECOND, 0);
        if (!next_run.after(schedule_start))
            next_run.add(Calendar.WEEK_OF_YEAR, interval_weeks);
        else
            next_run.add(Calendar.WEEK_OF_YEAR, interval_weeks - 1);
        return next_run;
    }

    @Override
    protected String getSelfString()
    {
        return ", interval_weeks = " + String.valueOf(interval_weeks) +
            ", scheduled_day = " + String.valueOf(scheduled_day) +
            ", scheduled_hour = " + String.valueOf(scheduled_hour) +
            ", scheduled_minute = " + String.valueOf(scheduled_minute);
    }
}
