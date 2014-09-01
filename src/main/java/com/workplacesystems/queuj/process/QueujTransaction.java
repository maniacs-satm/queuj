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

import com.workplacesystems.utilsj.Callback;
import java.io.Serializable;

/**
 *
 * @author dave
 */
public interface QueujTransaction<K extends Serializable & Comparable> {

    public <T> T doTransaction(ProcessWrapper<K> process, Callback<T> callback, boolean doStart);

    public <T> T doTransaction(ProcessWrapper<K> process, Callback<T> callback, Callback<Void> commitCallback, boolean doStart);

    public <T> T doTransaction(String queueOwner, boolean persistent, Callback<T> callback, boolean doStart);

    public <T> T doTransaction(String queueOwner, boolean persistent, Callback<T> callback, Callback<Void> commitCallback, boolean doStart);
}
