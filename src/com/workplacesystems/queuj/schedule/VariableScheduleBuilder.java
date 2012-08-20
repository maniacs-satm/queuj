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
import com.workplacesystems.queuj.utils.QueujException;

/**
 *
 * @author dave
 */
public class VariableScheduleBuilder extends ScheduleBuilder
{
    private Class<VariableSchedule> schedule_class;

    /** Creates a new instance of VariableScheduleBuilder */
    VariableScheduleBuilder(ScheduleSetter schedule_setter)
    {
        super(schedule_setter);
    }

    public void setVariableSchedule(Class<VariableSchedule> schedule_class)
    {
        this.schedule_class = schedule_class;
    }

    @Override
    Schedule newSchedule()
    {
        try {
            return schedule_class.newInstance();
        } catch (Exception e) {
            throw new QueujException(e);
        }
    }
}
