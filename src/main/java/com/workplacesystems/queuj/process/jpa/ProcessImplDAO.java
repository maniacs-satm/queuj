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

package com.workplacesystems.queuj.process.jpa;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;

/**
 *
 * @author dave
 */
public class ProcessImplDAO implements ProcessDAO {

    private final EntityManager em;

    protected ProcessImplDAO(EntityManager em) {
        this.em = em;
    }

    public List<String> findQueueOwners() {
        return findQueueOwners(em);
    }

    protected List<String> findQueueOwners(EntityManager em) {
        try {
            return em
                    .createQuery("select distinct p.queueOwnerId from ProcessImpl p")
                    .getResultList();
        } catch (EntityNotFoundException ex) {
        } catch (NoResultException ex) {
        }
        return null;
    }

    public List<ProcessImpl> findProcesses(String queueOwner) {
        return findProcesses(em, queueOwner);
    }

    protected List<ProcessImpl> findProcesses(EntityManager em, String queueOwner) {
        try {
            if (queueOwner == null) {
                return em
                        .createQuery("select p from ProcessImpl p where p.queueOwnerId is null order by p.creationTimestamp")
                        .getResultList();
            }
            else {
                return em
                        .createQuery("select p from ProcessImpl p where p.queueOwnerId=:queueOwnerKey order by p.creationTimestamp")
                        .setParameter("queueOwnerKey", queueOwner)
                        .getResultList();
            }
        } catch (EntityNotFoundException ex) {
        } catch (NoResultException ex) {
        }
        return null;
    }
}
