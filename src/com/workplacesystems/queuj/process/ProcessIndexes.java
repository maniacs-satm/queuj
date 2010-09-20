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

import com.workplacesystems.queuj.Process;
import com.workplacesystems.queuj.Queue;
import com.workplacesystems.queuj.utils.Callback;
import com.workplacesystems.queuj.utils.collections.FilterableArrayList;
import com.workplacesystems.queuj.utils.collections.FilterableCollection;
import com.workplacesystems.queuj.utils.collections.IterativeCallback;
import com.workplacesystems.queuj.utils.collections.SyncUtils;
import com.workplacesystems.queuj.utils.collections.TransactionalBidiTreeMap;
import com.workplacesystems.queuj.utils.collections.TransactionalSortedFilterableBidiMap;
import com.workplacesystems.queuj.utils.collections.decorators.SynchronizedTransactionalSortedFilterableBidiMap;
import com.workplacesystems.queuj.utils.collections.helpers.CounterIterativeCallback;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author dave
 */
public final class ProcessIndexes {

    private final static Log log = LogFactory.getLog(ProcessIndexes.class);

    private final Map not_run_processes = Collections.synchronizedMap(new HashMap());
    private final Map running_processes = Collections.synchronizedMap(new HashMap());
    private final Map failed_processes = Collections.synchronizedMap(new HashMap());
    private final Map locked_processes  = Collections.synchronizedMap(new HashMap());

    private final static String NULL_INDEX_KEY = ".null_custom_index_key";

    ProcessIndexes() {}

    protected final void initialiseStatuses()
    {
        finalizeIndexes(true);
    }

    private void invalidateIndexes()
    {
        not_run_processes.clear();
        running_processes.clear();
        failed_processes.clear();
        locked_processes.clear();
    }

    final boolean addProcessToIndex(ProcessWrapper process)
    {
        return updateIndexes(process, true);
    }

    final boolean  removeProcessFromIndex(ProcessWrapper process)
    {
        return updateIndexes(process, false);
    }

    private boolean updateIndexes(ProcessWrapper process, boolean add)
    {
        if (!process.isAddedToServer())
            return false;

        if (process.isNotRun())
            return updateIndex(not_run_processes, process, add);
        else if (process.isRunning())
            return updateIndex(running_processes, process, add);
        else if (process.isWaitingToRun())
            return updateIndex(locked_processes, process, add);
        else if (process.isRunError() || process.getResultCode() != 0)
            return updateIndex(failed_processes, process, add);
        return false;
    }

    private boolean updateIndex(Map index_map, ProcessWrapper process, boolean add)
    {
        boolean modified = false;
        final Queue queue = process.getQueue();
        if (queue.hasIndex())
        {
            final Map _index_map = index_map;
            index_map = (Map)SyncUtils.synchronizeWrite(index_map, new Callback() {
                @Override
                protected void doAction()
                {
                    Map custom_index_map = (Map)_index_map.get(queue.toString());
                    if (custom_index_map == null)
                    {
                        custom_index_map = Collections.synchronizedMap(new HashMap());
                        _index_map.put(queue.toString(), custom_index_map);
                    }
                    _return(custom_index_map);
                }
            });
            modified = updateIndexMap(index_map, process, add, queue.getIndexKey(new Process(process)));
            updateIndexMap(index_map, process, add, NULL_INDEX_KEY);
        }
        else
            modified = updateIndexMap(index_map, process, add, queue.toString());
        return modified;
    }

    private boolean updateIndexMap(final Map index_map, final ProcessWrapper process, final boolean add, final Object key)
    {
        final TransactionalSortedFilterableBidiMap queue_index_map =
                (TransactionalSortedFilterableBidiMap)SyncUtils.synchronizeWrite(index_map, new Callback() {
            @Override
            protected void doAction()
            {
                TransactionalSortedFilterableBidiMap queue_index_map = (TransactionalSortedFilterableBidiMap)index_map.get(key);
                if (queue_index_map == null)
                {
                    queue_index_map = SynchronizedTransactionalSortedFilterableBidiMap.decorate(new TransactionalBidiTreeMap());
                    queue_index_map.setAutoCommit(false);
                    index_map.put(key, queue_index_map);
                }
                _return(queue_index_map);
            }
        });

        final Object processKey = process.getProcessKey();
        return ((Boolean)SyncUtils.synchronizeWrite(queue_index_map, new Callback() {
            @Override
            protected void doAction()
            {
                if (add)
                {
                    if (queue_index_map.containsKey(processKey))
                    {
                        _return(Boolean.FALSE);
                        return;
                    }
                    queue_index_map.put(processKey, process);
                }
                else
                {
                    if (!queue_index_map.containsKey(processKey))
                    {
                        _return(Boolean.FALSE);
                        return;
                    }
                    queue_index_map.remove(processKey);
                }
                _return(Boolean.TRUE);
            }
        })).booleanValue();
    }

    void finalizeIndexes(boolean commit)
    {
        finalizeIndex(not_run_processes, commit, "Not Run Map", false);
        finalizeIndex(running_processes, commit, "Running Map", false);
        finalizeIndex(failed_processes, commit, "Failed Map", false);
        finalizeIndex(locked_processes, commit, "Waiting to Run Map", false);
    }

    private int[] finalizeIndex(final Map index_map, final boolean commit, final String map_description, final boolean recursed)
    {
        int[] total_sizes = (int[])SyncUtils.synchronizeWrite(index_map, new Callback() {
            @Override
            protected void doAction()
            {
                int total_size = 0;
                int total_indexed_size = 0;
                for (Iterator i = index_map.entrySet().iterator(); i.hasNext(); )
                {
                    Map.Entry entry = (Map.Entry)i.next();
                    Map next_map = (Map)entry.getValue();
                    if (next_map instanceof TransactionalSortedFilterableBidiMap)
                    {
                        TransactionalSortedFilterableBidiMap queue_index_map = (TransactionalSortedFilterableBidiMap)next_map;
                        if (commit)
                            queue_index_map.commit();
                        else
                            queue_index_map.rollback();

                        if (log.isDebugEnabled())
                        {
                            int map_size = queue_index_map.size();
                            if (recursed)
                            {
                                if (entry.getKey().equals(NULL_INDEX_KEY))
                                    total_size += map_size;
                                else
                                    total_indexed_size += map_size;
                            }
                            else
                            {
                                total_size += map_size;
                                total_indexed_size += map_size;
                            }
                        }
                    }
                    else
                    {
                        int[] recursed_sizes = finalizeIndex(next_map, commit, map_description, true);
                        if (log.isDebugEnabled())
                        {
                            total_size += recursed_sizes[0];
                            total_indexed_size += recursed_sizes[1];
                        }
                    }
                }

                if (log.isDebugEnabled())
                    _return(new int[] {total_size, total_indexed_size});
            }
        });

        if (!recursed && log.isDebugEnabled())
            log.debug((commit ? "Committing " : "Rolling back ") + map_description +
                    ", current size is " + total_sizes[0] + ", custom index size is " + total_sizes[1]);

        return total_sizes;
    }

    public int countOfNotRunProcesses(Queue queue)
    {
        return countOfNotRunProcesses(queue, NULL_INDEX_KEY);
    }

    public int countOfNotRunProcesses(Queue queue, Object index)
    {
        return countOfProcesses(not_run_processes, queue, index);
    }

    public int countOfRunningProcesses(Queue queue)
    {
        return countOfRunningProcesses(queue, NULL_INDEX_KEY);
    }

    public int countOfRunningProcesses(Queue queue, Object index)
    {
        return countOfProcesses(running_processes, queue, index);
    }

    public int countOfWaitingToRunProcesses(Queue queue)
    {
        return countOfWaitingToRunProcesses(queue, NULL_INDEX_KEY);
    }

    public int countOfWaitingToRunProcesses(Queue queue, Object index)
    {
        return countOfProcesses(locked_processes, queue, index);
    }

    public int countOfFailedProcesses(Queue queue)
    {
        return countOfFailedProcesses(queue, NULL_INDEX_KEY);
    }

    public int countOfFailedProcesses(Queue queue, Object index)
    {
        return countOfProcesses(failed_processes, queue, index);
    }

    private int countOfProcesses(Map index_map, Queue queue, Object key)
    {
        CounterIterativeCallback cic = new CounterIterativeCallback();
        iterateProcessIndexes(index_map, queue, key, cic);
        return cic.getCount();
    }

    public Object iterateNotRunProcesses(Queue queue, IterativeCallback ic)
    {
        return iterateNotRunProcesses(queue, NULL_INDEX_KEY, ic);
    }

    public Object iterateNotRunProcesses(Queue queue, Object key, IterativeCallback ic)
    {
        return iterateProcessIndexes(not_run_processes, queue, key, ic);
    }

    public Object iterateRunningProcesses(Queue queue, IterativeCallback ic)
    {
        return iterateRunningProcesses(queue, NULL_INDEX_KEY, ic);
    }

    public Object iterateRunningProcesses(Queue queue, Object key, IterativeCallback ic)
    {
        return iterateProcessIndexes(running_processes, queue, key, ic);
    }

    public Object iterateWaitingToRunProcesses(Queue queue, IterativeCallback ic)
    {
        return iterateWaitingToRunProcesses(queue, NULL_INDEX_KEY, ic);
    }

    public Object iterateWaitingToRunProcesses(Queue queue, Object key, IterativeCallback ic)
    {
        return iterateProcessIndexes(locked_processes, queue, key, ic);
    }

    public Object iterateFailedProcesses(Queue queue, IterativeCallback ic)
    {
        return iterateFailedProcesses(queue, NULL_INDEX_KEY, ic);
    }

    public Object iterateFailedProcesses(Queue queue, Object key, IterativeCallback ic)
    {
        return iterateProcessIndexes(failed_processes, queue, key, ic);
    }

    private Object iterateProcessIndexes(Map index_map, Queue queue, Object key, IterativeCallback ic)
    {
        Map queue_index_map = (Map)index_map.get(queue.toString());
        if (queue_index_map != null)
        {
            if (queue.hasIndex())
                queue_index_map = (Map)queue_index_map.get(key);
            if (queue_index_map != null)
                return ic.iterate((FilterableCollection)queue_index_map.values());
        }
        return ic.iterate(new FilterableArrayList());
    }
}
