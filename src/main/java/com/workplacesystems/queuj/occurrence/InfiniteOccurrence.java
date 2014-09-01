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

import com.workplacesystems.queuj.Occurrence;
import com.workplacesystems.queuj.Schedule;
import com.workplacesystems.queuj.schedule.ScheduleSetter;

/**
 *
 * @author dave
 */
abstract class InfiniteOccurrence extends Occurrence
{
    // Increase the number when an incompatible change is made
    private static final long serialVersionUID = InfiniteOccurrence.class.getName().hashCode() + 1;

    private Schedule schedule;

    /** Creates a new instance of InfiniteOccurrence */
    InfiniteOccurrence()
    {
    }

    @Override
    public final Schedule getSchedule(int run_count)
    {
        return schedule;
    }

    @Override
    public Schedule[] getSchedules()
    {
        return new Schedule[] {schedule};
    }

    protected class InfiniteScheduleSetter implements ScheduleSetter
    {
        public void setSchedule(Schedule schedule)
        {
            InfiniteOccurrence.this.schedule = schedule;
        }
    }

    private final static String new_line = System.getProperty("line.separator");

    @Override
    protected String getSelfString()
    {
        return new_line +
            INDENT + "  {" + new_line +
            INDENT + "    " + schedule.toString() + new_line +
            INDENT + "  }" + new_line;
    }
}
