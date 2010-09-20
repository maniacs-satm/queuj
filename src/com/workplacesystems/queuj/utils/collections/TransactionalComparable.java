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

package com.workplacesystems.queuj.utils.collections;


/**
 *
 * @author dave
 */
public interface TransactionalComparable<T> extends Comparable<T>
{
    /**
     * Commit status flags. Matches TransactionalBidiTreeMap.Node:
     * NO_CHANGE, DELETED and ADDED values.
     */
    public static final int NORMAL = 0;
    public static final int COMMITTED = 1;
    public static final int PENDING = 2;

    public int compareTo(int this_transaction_status, T other_obj, int other_transaction_status);
}
