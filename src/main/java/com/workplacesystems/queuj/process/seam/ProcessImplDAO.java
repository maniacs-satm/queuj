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

package com.workplacesystems.queuj.process.seam;

import com.workplacesystems.queuj.process.jpa.ProcessImpl;
import com.workplacesystems.queuj.process.jpa.ProcessDAO;
import java.io.Serializable;
import java.util.List;
import javax.persistence.EntityManager;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;

/**
 *
 * @author dave
 */
@Name("processImplDAO")
@AutoCreate
public class ProcessImplDAO extends com.workplacesystems.queuj.process.jpa.ProcessImplDAO implements ProcessDAO, Serializable {

    @In
    protected EntityManager entityManager;

    public ProcessImplDAO() {
        super(null);
    }

    @Override
    public List<String> findQueueOwners() {
        return super.findQueueOwners(entityManager);
    }

    @Override
    public List<ProcessImpl> findProcesses(String queueOwner) {
        return super.findProcesses(entityManager, queueOwner);
    }
}
