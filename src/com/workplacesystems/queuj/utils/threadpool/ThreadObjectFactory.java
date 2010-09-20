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

package com.workplacesystems.queuj.utils.threadpool;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.pool.PoolableObjectFactory;

/**
 * Abstract ThreadObjectFactory. Override to initialise newly created threads.
 */
public abstract class ThreadObjectFactory implements PoolableObjectFactory {
   /**
    * Logger for this class
    */
   private final static Log log = LogFactory.getLog(ThreadObjectFactory.class);

   /* (non-Javadoc)
    * @see org.apache.commons.pool.PoolableObjectFactory#makeObject()
    */
   public final Object makeObject() {
      log.debug(" makeObject...");
      WorkerThread thread = new WorkerThread();
      initialiseThread(thread);
      thread.start();
      return thread;
   }

   public abstract void initialiseThread(Thread thread);

   /* (non-Javadoc)
    * @see org.apache.commons.pool.PoolableObjectFactory#destroyObject(java.lang.Object)
    */
   public final void destroyObject(Object obj) {
      log.debug(" !!! destroyObject... !!!" + obj);
      if (obj instanceof WorkerThread) {
         WorkerThread rt = (WorkerThread) obj;
         rt.setStopped();
      }
   }

   /* (non-Javadoc)
    * @see org.apache.commons.pool.PoolableObjectFactory#validateObject(java.lang.Object)
    */
   public final boolean validateObject(Object obj) {
      log.debug(" validateObject..." + obj);
      if (obj instanceof WorkerThread) {
         WorkerThread rt = (WorkerThread) obj;
         if(!rt.isDone()) {  //if the thread is running the previous task, get another one.
            return false;
         }
         if (rt.isRunning()) {
            if (rt.getThreadGroup() == null) {
               return false;
            }
            return true;
         }
      }
      return true;
   }

   /* (non-Javadoc)
    * @see org.apache.commons.pool.PoolableObjectFactory#activateObject(java.lang.Object)
    */
   public final void activateObject(Object obj) {
      log.debug(" activateObject...");
      activateThread((Thread)obj);
   }

   public abstract void activateThread(Thread thread);

   /* (non-Javadoc)
    * @see org.apache.commons.pool.PoolableObjectFactory#passivateObject(java.lang.Object)
    */
   public final void passivateObject(Object obj) {
      log.debug(" passivateObject..." + obj);
      passivateThread((Thread)obj);
   }

   public abstract void passivateThread(Thread thread);

}