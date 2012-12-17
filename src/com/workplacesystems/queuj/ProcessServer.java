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

import com.workplacesystems.queuj.process.ProcessIndexesCallback;

/**
 * This is the public interface to ProcessImplServer and PartitionedProcessImplServer.
 *
 * @author dave
 */
public interface ProcessServer
{
    public Boolean notifyQueue();

    public <R> R indexesWithReadLock(final ProcessIndexesCallback<R> indexesCallback);

    public void registerListener(QueueListener listener);

    public void removeListener(QueueListener listener);
}
