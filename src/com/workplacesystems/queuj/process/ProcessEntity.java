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

package com.workplacesystems.queuj.process;

import com.workplacesystems.queuj.Access;
import com.workplacesystems.queuj.Occurrence;
import com.workplacesystems.queuj.Output;
import com.workplacesystems.queuj.Queue;
import com.workplacesystems.queuj.Resilience;
import com.workplacesystems.queuj.Visibility;
import java.io.Serializable;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

/**
 *
 * @author dave
 */
public interface ProcessEntity {

    public enum Status {
        RUN_OK(0, "qj.enum.ProcessImplStatus.RunOK"),
        NOT_RUN(1, "qj.enum.ProcessImplStatus.NotRun"),
        LOCKED(2, "qj.enum.ProcessImplStatus.Locked"),
        RUNNING(3, "qj.enum.ProcessImplStatus.Running"),
        RESTARTED(4, "qj.enum.ProcessImplStatus.Restarted"),
        RUN_ERROR(5, "qj.enum.ProcessImplStatus.RunError"),
        DELETED(6, "qj.enum.ProcessImplStatus.Deleted");

        private final int status;
        private final String description;

        Status(int status, String description) {
            this.status = status;
            this.description = description;
        }

        public int getValue() {
            return status;
        }

        public String getDescription() {
            return description;
        }
    };

    public Integer getProcessId();
    public void setProcessId(Integer processId);

    public Integer getVersion();

    public String getQueueOwnerId();
    public void setQueueOwnerId(String queueOwnerId);

    public String getUUID();
    public void setUUID(String uuid);

    public void setProcessWrapper(ProcessWrapper processWrapper);

    public void setImplementationOptions(Map<String, Serializable> implementation_options);

    public Map<String,Serializable> getServerOptions();

    public String getProcessName();
    public void setProcessName(String processName);

    public Date getCreationTimestamp();
    public void setCreationTimestamp(Date creationTimestamp);

    public String getDescription();
    public void setDescription(String description);

    public Date getScheduledTimestamp();
    public void setScheduledTimestamp(Date scheduledTimestamp);

    public Date getStartedTimestamp();
    public void setStartedTimestamp(Date startedTimestamp);

    public ProcessParameters getParameters();
    public void setParameters(ProcessParameters parameters);

    public Queue getQueue();
    public void setQueue(Queue queue);

    public String getUserId();
    public void setUserId(String userId);

    public Locale getLocale();
    public void setLocale(Locale locale);

    public Status getStatus();
    public void setStatus(Status status);

    public Occurrence getOccurrence();
    public void setOccurrence(Occurrence occurrence);

    public Access getAccess();
    public void setAccess(Access access);

    public Visibility getVisibility();
    public void setVisibility(Visibility visibility);

    public Resilience getResilience();
    public void setResilience(Resilience resilience);

    public Output getOutput();
    public void setOutput(Output output);

    public int getAttempt();
    public void setAttempt(int attempt);

    public int getRunCount();
    public void setRunCount(int runCount);

    public int getResultCode();
    public void setResultCode(int resultCode);

    public boolean isAssociatedReport();
    public void setAssociatedReport(boolean associatedReport);

    public boolean isKeepCompleted();
    public void setKeepCompleted(boolean keepCompleted);
}
