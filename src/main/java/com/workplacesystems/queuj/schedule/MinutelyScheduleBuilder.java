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

import com.workplacesystems.queuj.Schedule;

/**
 *
 * @author dave
 */
public class MinutelyScheduleBuilder extends ScheduleBuilder
{
    private int interval_minutes = 1;

    /** Creates a new instance of MinutelyScheduleBuilder */
    MinutelyScheduleBuilder(ScheduleSetter schedule_setter)
    {
        super(schedule_setter);
    }

    public void setIntervalMinutes(int interval_minutes)
    {
        this.interval_minutes = interval_minutes;
    }

    @Override
    Schedule newSchedule()
    {
        return new MinutelySchedule(interval_minutes);
    }
}
