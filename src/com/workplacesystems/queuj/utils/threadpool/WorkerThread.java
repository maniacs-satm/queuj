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

/**
 * The actual WorkerThread.
 */
public class WorkerThread extends Thread {

   private final static Log log = LogFactory.getLog(WorkerThread.class);

   public WorkerThread() {
      //default constructor
   }

   /**
    * This thread has been used. used is set to true when the thread is run.
    */
   private boolean used = false;

   /**
    * Manages the thread's state. When the thread is first created, running is
    * set to true. When the thread is assigned a task for the first time, the
    * thread continues to be in the running state until stopped.
    */
   private volatile boolean running = false;

   /**
    * The Runnable to run.
    */
   private Runnable runnable;

   /**
    * The object to synchronize upon for notifying the completion of task.
    */
   private Object syncObject = null;

   /**
    * The pool being used. We use this if we need to return the object back to the
    * pool. If this is not set, we assume that the client will take care of returning
    * the object back to the pool.
    */
   private ThreadPool pool = null;

   /**
    * @param pool The pool to set.
    */
   synchronized void setPool(ThreadPool pool) {
      this.pool = pool;
   }
   /**
    * @return Returns the done.
    */
   synchronized boolean isDone() {
      return runnable == null;
   }
   /**
    * @return Returns the running.
    */
   boolean isRunning() {
      return running;
   }
   /**
    * Sets the thread running
    */
   @Override
public synchronized void start() {
      if (!running && !used) {
         this.running = true;
         this.used = true;
         this.setDaemon(true);
         super.start();
      }
   }
   /**
    * Stop the thread
    */
   synchronized void setStopped() {
      this.running = false;
      notify();
   }
   /**
    * @return Returns the syncObject.
    */
   public synchronized Object getSyncObject() {
      return syncObject;
   }
   /**
    * @param syncObject The syncObject to set.
    */
   public synchronized void setSyncObject(Object syncObject) {
      this.syncObject = syncObject;
   }
   /**
    * execute
    * @param clsName
    * @param methName
    * @param params
    * @param synObj
    */
   public synchronized void execute(Runnable runnable, Object synObj) {
      this.runnable = runnable;
      this.syncObject = synObj;

      if (!running) { //If this is the first time, then kick off the thread.
         start();
      } else { // we already have a thread running so wakeup the waiting thread.
         this.notifyAll();
      }
   }

   /* (non-Javadoc)
    * @see java.lang.Runnable#run()
    */
   @Override
public void run() {
      while (running) {
         Runnable _runnable = null;
         synchronized (this) {
            while (running && runnable == null) {
               try {
                  this.wait();
               } catch (InterruptedException e) {} // Ignore, as interrupts are not used by the pool itself
            }
            _runnable = runnable;
         }

         if (_runnable != null) {
            try {
              _runnable.run();
            } catch (Exception e) {
               log.error("", e);
            } finally {
               synchronized (this) {
                  if (syncObject != null) {
                     synchronized (syncObject) {
                        syncObject.notify();
                     }
                  }
                  reset();
                  returnToPool();
               }
            }
         }
      }
   }

   /**
    * Returns the thread to the pool.
    */
   private void returnToPool() {
      if(pool != null) {
         try {
            pool.returnObject(this);
         } catch (Exception e1) {
            log.error("Exception :", e1);
         }
         this.pool = null;
      }
   }

   /**
    * reset the memebers to service next request.
    */
   private void reset() {
      this.runnable = null;
      this.syncObject = null;
   }
}