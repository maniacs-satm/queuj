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

/**
 * The Schedule class provides the appropriate schedule for the Processes
 * Occurrence. There are 2 virtual types of schedule: Absolute and Relative.
 *
 * For instance the RunOnce Occurrence can have either an Absolute or a
 * Relative Schedule and can either run at a specific date and time or in
 * a certain amount of time.
 * 
 * The inifinte Occurences such as RunDaily can only have an Absolute
 * Schedule. For instance RunDaily can run at a specified time.
 *
 * @author dave
 */
public abstract class Schedule implements Serializable
{
    // Increase the number when an incompatible change is made
    private static final long serialVersionUID = Schedule.class.getName().hashCode() + 1;

    protected abstract GregorianCalendar getNextRunTime(GregorianCalendar schedule_start);

    /**
     * Generate a unique String for this Schedule.
     */
    @Override
    public String toString()
    {
        return getClass().getName() + getSelfString();
    }

    /**
     * Gets the unique String from subclasses for this Schedule.
     */
    protected abstract String getSelfString();
}
