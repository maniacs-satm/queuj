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

import com.workplacesystems.queuj.Schedule;

/**
 *
 * @author dave
 */
public class RelativeScheduleBuilder extends ScheduleBuilder
{
    private int calendar_type = 0;
    private int quantity = 0;

    /** Creates a new instance of RelativeScheduleBuilder */
    RelativeScheduleBuilder(ScheduleSetter schedule_setter)
    {
        super(schedule_setter);
    }

    public void setRunImmediately()
    {
        setRunDelayMinutes(0);
    }

    public void setRunDelayWeeks(int weeks)
    {
        this.calendar_type = Calendar.WEEK_OF_YEAR;
        this.quantity = weeks;
    }

    public void setRunDelayDays(int days)
    {
        this.calendar_type = Calendar.DAY_OF_YEAR;
        this.quantity = days;
    }

    public void setRunDelayHours(int hours)
    {
        this.calendar_type = Calendar.HOUR;
        this.quantity = hours;
    }

    public void setRunDelayMinutes(int minutes)
    {
        this.calendar_type = Calendar.MINUTE;
        this.quantity = minutes;
    }

    public void setRunDelaySeconds(int seconds)
    {
        this.calendar_type = Calendar.SECOND;
        this.quantity = seconds;
    }

    @Override
    Schedule newSchedule()
    {
        return new DelayedSchedule(calendar_type, quantity);
    }
}
