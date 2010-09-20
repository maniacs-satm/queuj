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
public class AbsoluteSchedule extends Schedule
{    
    // Increase the number when an incompatible change is made
    private static final long serialVersionUID = AbsoluteSchedule.class.getName().hashCode() + 1;

    private final GregorianCalendar date;
    private final int scheduled_day;
    private final int scheduled_hour;
    private final int scheduled_minute;

    /** Creates a new instance of AbsoluteSchedule */
    AbsoluteSchedule(GregorianCalendar date, int scheduled_day, int scheduled_hour, int scheduled_minute)
    {
        this.date = date;
        this.scheduled_day = scheduled_day;
        this.scheduled_hour = scheduled_hour;
        this.scheduled_minute = scheduled_minute;
    }

    public GregorianCalendar getDate()
    {
        return (GregorianCalendar)date.clone();
    }

    @Override
    protected GregorianCalendar getNextRunTime(GregorianCalendar schedule_start)
    {
        GregorianCalendar next_run = getDate();
        next_run.set(Calendar.HOUR_OF_DAY, scheduled_hour);
        next_run.set(Calendar.MINUTE, scheduled_minute);
        next_run.set(Calendar.SECOND, 0);
        next_run.set(Calendar.MILLISECOND, 0);

        return next_run;
    }

    @Override
    protected String getSelfString()
    {
        return ", date = " + date.toString() +
            ", scheduled_day = " + String.valueOf(scheduled_day) +
            ", scheduled_hour = " + String.valueOf(scheduled_hour) +
            ", scheduled_minute = " + String.valueOf(scheduled_minute);
    }
}
