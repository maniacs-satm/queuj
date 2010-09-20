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

import com.workplacesystems.queuj.occurrence.ScheduleFactory;

/**
 *
 * @author dave
 */
public class ScheduleFactoryImpl extends ScheduleFactory
{
    private static final ScheduleFactoryImpl instance = new ScheduleFactoryImpl();

    /**
     * Creates a new instance of ScheduleFactoryImpl
     */
    private ScheduleFactoryImpl()
    {
    }

    public static ScheduleFactory getInstance()
    {
        return instance;
    }

    @Override
    protected AbsoluteScheduleBuilder newAbsoluteScheduleBuilder(ScheduleSetter schedule_setter)
    {
        return new AbsoluteScheduleBuilder(schedule_setter);
    }

    @Override
    protected RelativeScheduleBuilder newRelativeScheduleBuilder(ScheduleSetter schedule_setter)
    {
        return new RelativeScheduleBuilder(schedule_setter);
    }

    @Override
    protected MinutelyScheduleBuilder newMinutelyScheduleBuilder(ScheduleSetter schedule_setter)
    {
        return new MinutelyScheduleBuilder(schedule_setter);
    }

    @Override
    protected HourlyScheduleBuilder newHourlyScheduleBuilder(ScheduleSetter schedule_setter)
    {
        return new HourlyScheduleBuilder(schedule_setter);
    }

    @Override
    protected DailyScheduleBuilder newDailyScheduleBuilder(ScheduleSetter schedule_setter)
    {
        return new DailyScheduleBuilder(schedule_setter);
    }

    @Override
    protected WeeklyScheduleBuilder newWeeklyScheduleBuilder(ScheduleSetter schedule_setter)
    {
        return new WeeklyScheduleBuilder(schedule_setter);
    }

    @Override
    protected MonthlyScheduleBuilder newMonthlyScheduleBuilder(ScheduleSetter schedule_setter)
    {
        return new MonthlyScheduleBuilder(schedule_setter);
    }

    @Override
    protected YearlyScheduleBuilder newYearlyScheduleBuilder(ScheduleSetter schedule_setter)
    {
        return new YearlyScheduleBuilder(schedule_setter);
    }
}
