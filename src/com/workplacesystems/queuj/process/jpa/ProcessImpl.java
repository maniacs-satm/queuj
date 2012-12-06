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

import com.workplacesystems.queuj.Access;
import com.workplacesystems.queuj.Occurrence;
import com.workplacesystems.queuj.Output;
import com.workplacesystems.queuj.Queue;
import com.workplacesystems.queuj.Resilience;
import com.workplacesystems.queuj.Visibility;
import com.workplacesystems.queuj.process.ProcessEntity;
import com.workplacesystems.queuj.process.ProcessParameters;
import com.workplacesystems.queuj.process.ProcessWrapper;
import java.io.Serializable;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

/**
 *
 * @author dave
 */
@Entity
@Table(name="process", uniqueConstraints={@UniqueConstraint(columnNames={"queue_owner_id","process_id"})})
public class ProcessImpl implements ProcessEntity<Integer>, Serializable {

    private Integer processId;
    private Integer OPTLOCK;
    private String queueOwnerId;
    private UUID uuid;
    private String processName;//" max_length="50" key_field="true" mandatory="true"/>
    private Date creationTimestamp;//" key_field="true" mandatory="true" ordering_field="true"/>
    private String description;//" max_length="50"/>
    private Date scheduledTimestamp;
    private Date startedTimestamp;
    private ProcessParameters parameters;
    private Queue queue;
    private String userId;
    private Locale locale;
    private Status status;
    private Occurrence occurrence;
    private Access access;
    private Visibility visibility;
    private Resilience resilience;
    private Output output;
    private int attempt;
    private int runCount;
    private int resultCode;
    private boolean associatedReport;
    private boolean keepCompleted;

    private static int nextProcessId = Integer.MAX_VALUE;

    @Id
    @GeneratedValue
    @Column(name = "process_id")
    public Integer getProcessId() { return processId; }
    public void setProcessId(Integer processId) { this.processId = processId; }

    @Version
    @Column(name="OPTLOCK",nullable=false)
    public Integer getVersion() { return OPTLOCK; }
    public void setVersion(Integer OPTLOCK) { this.OPTLOCK = OPTLOCK; }

    @Column(name = "queue_owner_id")
    public String getQueueOwnerId() { return queueOwnerId; }
    public void setQueueOwnerId(String queueOwnerId) { this.queueOwnerId = queueOwnerId; }

    @Column(name="uuid",nullable=false,unique=true)
    public String getUUID() { return uuid.toString(); }
    public void setUUID(String uuid) { this.uuid = UUID.fromString(uuid); }

    @Column(name = "process_name")
    public String getProcessName() { return processName; }
    public void setProcessName(String processName) { this.processName = processName; }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "creation_timestamp")
    public Date getCreationTimestamp() { return creationTimestamp; }
    public void setCreationTimestamp(Date creationTimestamp) { this.creationTimestamp = creationTimestamp; }

    @Column(name = "description")
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "scheduled_timestamp")
    public Date getScheduledTimestamp() { return scheduledTimestamp; }
    public void setScheduledTimestamp(Date scheduledTimestamp) { this.scheduledTimestamp = scheduledTimestamp; }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "started_timestamp")
    public Date getStartedTimestamp() { return startedTimestamp; }
    public void setStartedTimestamp(Date startedTimestamp) { this.startedTimestamp = startedTimestamp; }

    @Lob
    @Column(name = "parameters")
    public ProcessParameters getParameters() { return parameters; }
    public void setParameters(ProcessParameters parameters) { this.parameters = parameters; }

    @Lob
    @Column(name = "queue")
    public Queue getQueue() { return queue; }
    public void setQueue(Queue queue) { this.queue = queue; }

    @Column(name = "user_id")
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    @Lob
    @Column(name = "locale")
    public Locale getLocale() { return locale; }
    public void setLocale(Locale locale) { this.locale = locale; }

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status")
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    @Lob
    @Column(name = "occurrence")
    public Occurrence getOccurrence() { return occurrence; }
    public void setOccurrence(Occurrence occurrence) { this.occurrence = occurrence; }

    @Lob
    @Column(name = "access")
    public Access getAccess() { return access; }
    public void setAccess(Access access) { this.access = access; }

    @Lob
    @Column(name = "visibility")
    public Visibility getVisibility() { return visibility; }
    public void setVisibility(Visibility visibility) { this.visibility = visibility; }

    @Lob
    @Column(name = "resilience")
    public Resilience getResilience() { return resilience; }
    public void setResilience(Resilience resilience) { this.resilience = resilience; }

    @Lob
    @Column(name = "output")
    public Output getOutput() { return output; }
    public void setOutput(Output output) { this.output = output; }

    @Column(name = "attempt")
    public int getAttempt() { return attempt; }
    public void setAttempt(int attempt) { this.attempt = attempt; }

    @Column(name = "run_count")
    public int getRunCount() { return runCount; }
    public void setRunCount(int runCount) { this.runCount = runCount; }

    @Column(name = "result_code")
    public int getResultCode() { return resultCode; }
    public void setResultCode(int resultCode) { this.resultCode = resultCode; }

    @Column(name = "associated_report")
    public boolean isAssociatedReport() { return associatedReport; }
    public void setAssociatedReport(boolean associatedReport) { this.associatedReport = associatedReport; }

    @Column(name = "keep_completed")
    public boolean isKeepCompleted() { return keepCompleted; }
    public void setKeepCompleted(boolean keepCompleted) { this.keepCompleted = keepCompleted; }

    // Entity doesn't require a reverse reference to the wrapper for JPA and Seam implementations
    public void setProcessWrapper(ProcessWrapper<Integer> processWrapper) {}

    // Implementation options not currently required for JPA and Seam implementations
    public void setImplementationOptions(Map<String, Object> implementation_options) {}

    @Transient
    public Integer getNextProcessId() {
        return getNextProcessId0();
    }

    @Transient
    private static synchronized int getNextProcessId0() {
        return nextProcessId--;
    }

    // No special options required for retrieval of the server
    @Transient
    public Map<String,Object> getServerOptions() { return null; }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ProcessImpl other = (ProcessImpl) obj;
        if (this.uuid != other.uuid && (this.uuid == null || !this.uuid.equals(other.uuid))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return 67 * 5 + (this.uuid != null ? this.uuid.hashCode() : 0);
    }
}
