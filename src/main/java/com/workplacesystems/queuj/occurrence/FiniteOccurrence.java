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

import java.util.Iterator;

import com.workplacesystems.queuj.Occurrence;
import com.workplacesystems.queuj.Schedule;
import com.workplacesystems.queuj.schedule.AbsoluteScheduleBuilder;
import com.workplacesystems.queuj.schedule.RelativeScheduleBuilder;
import com.workplacesystems.queuj.schedule.ScheduleFactoryImpl;
import com.workplacesystems.queuj.schedule.ScheduleSetter;
import com.workplacesystems.utilsj.collections.FilterableArrayList;
import com.workplacesystems.utilsj.collections.IterativeCallback;

/**
 *
 * @author dave
 */
abstract class FiniteOccurrence extends Occurrence
{
    // Increase the number when an incompatible change is made
    private static final long serialVersionUID = FiniteOccurrence.class.getName().hashCode() + 1;

    private final int number_of_occurrences;
    private final FilterableArrayList schedules;

    /** Creates a new instance of FiniteOccurrence */
    FiniteOccurrence(int number_of_occurrences)
    {
        super();
        this.number_of_occurrences = number_of_occurrences;
        this.schedules = new FilterableArrayList();
    }

    @Override
    public final Schedule getSchedule(int run_count)
    {
        if (run_count < 0)
            return null;

        if (run_count >= number_of_occurrences)
            return null;

        return (Schedule)schedules.get(run_count);
    }

    @Override
    public Schedule[] getSchedules()
    {
        Schedule[] schedules_array = new Schedule[schedules.size()];
        int i = 0;
        for (Iterator it = schedules.iterator(); it.hasNext(); )
            schedules_array[i++] = (Schedule)it.next();
        return schedules_array;
    }

    public RelativeScheduleBuilder newRelativeScheduleBuilder()
    {
        return ScheduleFactoryImpl.getInstance().newRelativeScheduleBuilder(new FiniteScheduleSetter());
    }

    public AbsoluteScheduleBuilder newAbsoluteScheduleBuilder()
    {
        return ScheduleFactoryImpl.getInstance().newAbsoluteScheduleBuilder(new FiniteScheduleSetter());
    }

    private class FiniteScheduleSetter implements ScheduleSetter
    {
        public void setSchedule(Schedule schedule)
        {
            FiniteOccurrence.this.schedules.add(schedule);
        }
    }

    private final static String new_line = System.getProperty("line.separator");

    @Override
    protected String getSelfString()
    {
        final StringBuffer self = new StringBuffer(getSelfFiniteString() + " {" + new_line);
        (new IterativeCallback() {
            @Override
            protected void nextObject(Object obj)
            {
                Schedule schedule = (Schedule)obj;
                self.append(INDENT + "    " + schedule.toString() + new_line);
            }
        }).iterate(schedules);
        self.append(INDENT + "  }" + new_line);

        return self.toString();
    }

    protected String getSelfFiniteString()
    {
        return ", occurences = " + number_of_occurrences;
    }
}
