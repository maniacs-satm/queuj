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

import java.util.GregorianCalendar;

import com.workplacesystems.queuj.Schedule;

/**
 *
 * @author dave
 */
public class DelayedSchedule extends Schedule
{
    // Increase the number when an incompatible change is made
    private static final long serialVersionUID = DelayedSchedule.class.getName().hashCode() + 1;

    private final int calendar_type;
    private final int quantity;

    /**
     * Creates a new instance of DelayedSchedule
     */
    DelayedSchedule(int calendar_type, int quantity)
    {
        this.calendar_type = calendar_type;
        this.quantity = quantity;
    }

    public int getCalendarType()
    {
        return calendar_type;
    }

    public int getQuantity()
    {
        return quantity;
    }

    @Override
    protected GregorianCalendar getNextRunTime(GregorianCalendar schedule_start)
    {
        GregorianCalendar next_run = (GregorianCalendar)schedule_start.clone();
        next_run.add(calendar_type, quantity);
        return next_run;
    }

    @Override
    protected String getSelfString()
    {
        return ", calendar_type = " + String.valueOf(calendar_type) +
            ", quantity = " + String.valueOf(quantity);
    }
}
