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

package com.workplacesystems.queuj.occurrence;

import com.workplacesystems.queuj.schedule.AbsoluteScheduleBuilder;
import com.workplacesystems.queuj.schedule.DailyScheduleBuilder;
import com.workplacesystems.queuj.schedule.HourlyScheduleBuilder;
import com.workplacesystems.queuj.schedule.MinutelyScheduleBuilder;
import com.workplacesystems.queuj.schedule.MonthlyScheduleBuilder;
import com.workplacesystems.queuj.schedule.RelativeScheduleBuilder;
import com.workplacesystems.queuj.schedule.ScheduleSetter;
import com.workplacesystems.queuj.schedule.WeeklyScheduleBuilder;
import com.workplacesystems.queuj.schedule.YearlyScheduleBuilder;

/**
 *
 * @author dave
 */
public abstract class ScheduleFactory
{
    /** Creates a new instance of ScheduleFactory */
    protected ScheduleFactory()
    {
    }

    protected abstract AbsoluteScheduleBuilder newAbsoluteScheduleBuilder(ScheduleSetter schedule_setter);

    protected abstract RelativeScheduleBuilder newRelativeScheduleBuilder(ScheduleSetter schedule_setter);

    protected abstract MinutelyScheduleBuilder newMinutelyScheduleBuilder(ScheduleSetter schedule_setter);

    protected abstract HourlyScheduleBuilder newHourlyScheduleBuilder(ScheduleSetter schedule_setter);

    protected abstract DailyScheduleBuilder newDailyScheduleBuilder(ScheduleSetter schedule_setter);

    protected abstract WeeklyScheduleBuilder newWeeklyScheduleBuilder(ScheduleSetter schedule_setter);

    protected abstract MonthlyScheduleBuilder newMonthlyScheduleBuilder(ScheduleSetter schedule_setter);

    protected abstract YearlyScheduleBuilder newYearlyScheduleBuilder(ScheduleSetter schedule_setter);
}
