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
 * The Occurrence class is an abstract class that can be overridden to
 * provide Process occurences. An occurrence is how often a process will
 * run. Provided subclasses provide minutely, hourly, daily, weekly and
 * monthly occurrences as well as RunOnce and RuneFiniteTimes.
 *
 * Occurrence instances provide a method to retrieve a ScheduleBuilder
 * from which you can build an appropriate schedule for the Occurrence.
 * For instance the RunDaily Occurrence provides a ScheduleBuilder in
 * which you can set the time and the daily interval.
 *
 * @author dave
 */
public abstract class Occurrence implements Serializable
{
    // Increase the number when an incompatible change is made
    private static final long serialVersionUID = Occurrence.class.getName().hashCode() + 1;

    /** INDENT allows the toString implementation to indent when owned by Resilience. */
    protected String INDENT = "";

    /**
     * toString provides a unique String for this Occurence.
     */
    @Override
    public final String toString()
    {
        return getClass().getName() + getSelfString();
    }

    /**
     * Called by toString and overridden by subclasses.
     * Allows subclasses to output their unique String.
     */
    protected abstract String getSelfString();

    /**
     * Get the next run time/date for this Occurrence.
     */
    public final GregorianCalendar getNextRunTime(GregorianCalendar schedule_start, int run_count)
    {
        Schedule schedule = getSchedule(run_count);
        return schedule == null ? null : schedule.getNextRunTime(schedule_start);
    }

    public abstract Schedule getSchedule(int run_count);

    public abstract Schedule[] getSchedules();

    /**
     * Implement equals to use result of toString.
     */
    @Override
    public boolean equals(Object object)
    {
        if (object == null || !(object instanceof Occurrence))
            return false;

        Occurrence another = (Occurrence)object;
        return another.toString().equals(toString());
    }

    /**
     * Implement hashCode to fulfil the contract of Object.
     */
    @Override
    public int hashCode()
    {
        return toString().hashCode();
    }
}
