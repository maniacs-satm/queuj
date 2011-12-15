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

package queujsample;

import com.workplacesystems.queuj.Process;
import com.workplacesystems.queuj.Queue;
import com.workplacesystems.queuj.QueueBuilder;
import com.workplacesystems.queuj.QueueFactory;
import com.workplacesystems.queuj.QueueRestriction;
import com.workplacesystems.queuj.occurrence.RunOnce;
import com.workplacesystems.queuj.process.ProcessIndexes;
import com.workplacesystems.queuj.process.ProcessIndexesCallback;
import com.workplacesystems.queuj.process.ProcessWrapper;
import com.workplacesystems.queuj.process.java.JavaProcessBuilder;
import com.workplacesystems.queuj.process.java.JavaProcessRunner;
import com.workplacesystems.queuj.schedule.RelativeScheduleBuilder;
import com.workplacesystems.queuj.utils.QueujException;
import com.workplacesystems.utilsj.collections.helpers.HasLessThan;
import java.util.List;
import org.apache.log4j.xml.DOMConfigurator;
import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.TaskMonitor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Locale;
import javax.swing.Timer;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JFrame;

/**
 * The application's main frame.
 */
public class QueujSampleView extends FrameView {

    private final static Queue<JavaProcessBuilder> SAMPLE_QUEUE;

    static {
        System.setProperty("com.workplacesystems.queuj.QueujFactory", "com.workplacesystems.queuj.process.jpa.JPAFactory");
        System.setProperty("com.workplacesystems.jpa.persistenceUnitName", "qjDatabase");

        QueueBuilder<JavaProcessBuilder> qb = QueueFactory.DEFAULT_QUEUE.newQueueBuilder();
        qb.setQueueRestriction(new TestQueueRestriction());
        SAMPLE_QUEUE = qb.newQueue();
    }

    public QueujSampleView(SingleFrameApplication app) {
        super(app);

        DOMConfigurator.configure(getClass().getResource("/META-INF/log4j.xml"));

        initComponents();

        // status bar initialization - message timeout, idle icon and busy animation, etc
        ResourceMap resourceMap = getResourceMap();
        int messageTimeout = resourceMap.getInteger("StatusBar.messageTimeout");
        messageTimer = new Timer(messageTimeout, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                statusMessageLabel.setText("");
            }
        });
        messageTimer.setRepeats(false);
        int busyAnimationRate = resourceMap.getInteger("StatusBar.busyAnimationRate");
        for (int i = 0; i < busyIcons.length; i++) {
            busyIcons[i] = resourceMap.getIcon("StatusBar.busyIcons[" + i + "]");
        }
        busyIconTimer = new Timer(busyAnimationRate, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                busyIconIndex = (busyIconIndex + 1) % busyIcons.length;
                statusAnimationLabel.setIcon(busyIcons[busyIconIndex]);
            }
        });
        idleIcon = resourceMap.getIcon("StatusBar.idleIcon");
        statusAnimationLabel.setIcon(idleIcon);
        progressBar.setVisible(false);

        // connecting action tasks to status bar via TaskMonitor
        TaskMonitor taskMonitor = new TaskMonitor(getApplication().getContext());
        taskMonitor.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                String propertyName = evt.getPropertyName();
                if ("started".equals(propertyName)) {
                    if (!busyIconTimer.isRunning()) {
                        statusAnimationLabel.setIcon(busyIcons[0]);
                        busyIconIndex = 0;
                        busyIconTimer.start();
                    }
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(true);
                } else if ("done".equals(propertyName)) {
                    busyIconTimer.stop();
                    statusAnimationLabel.setIcon(idleIcon);
                    progressBar.setVisible(false);
                    progressBar.setValue(0);
                } else if ("message".equals(propertyName)) {
                    String text = (String)(evt.getNewValue());
                    statusMessageLabel.setText((text == null) ? "" : text);
                    messageTimer.restart();
                } else if ("progress".equals(propertyName)) {
                    int value = (Integer)(evt.getNewValue());
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(value);
                }
            }
        });
    }

    @Action
    public void showAboutBox() {
        if (aboutBox == null) {
            JFrame mainFrame = QueujSampleApp.getApplication().getMainFrame();
            aboutBox = new QueujSampleAboutBox(mainFrame);
            aboutBox.setLocationRelativeTo(mainFrame);
        }
        QueujSampleApp.getApplication().show(aboutBox);
    }

    public static class TestRunner extends JavaProcessRunner {

        private static final Object mutex = new Object();

        private static int running = 0;
        private static int totalStarted = 0;

        public void run() {
            try {
                long sleepTime = (long)(Math.random() * 60000);
                int count;
                synchronized (mutex) {
                    count = ++totalStarted;
                    running++;
                    System.out.println("(" + count + ") Sleeping for " + sleepTime);
                }
                Thread.sleep(sleepTime);
                synchronized (mutex) {
                    running--;
                    System.out.println("(" + count + ") Finished. " + running + " still running.");
                }
            } catch (InterruptedException ex) {}
        }
    }

    public static class Test2Runner extends JavaProcessRunner {

        private List<Process> processes;

        public void run(int count) {
            System.out.println("Creating " + count + " jobs");
            JavaProcessBuilder pb = SAMPLE_QUEUE.newProcessBuilder(Locale.getDefault());
            pb.setProcessName("Test 5/2");
            pb.setProcessPersistence(true);
            pb.setProcessKeepCompleted(true);

            processes = new ArrayList<Process>();
            for (int i=0; i<count; i++) {
                pb.setProcessDetails(new TestRunner(), "run", new Class[] {}, new Object[] {});
                pb.setProcessDescription("Test 5 part 2: " + i);
                processes.add(pb.newProcess());
            }
            System.out.println("Created " + count + " jobs");
        }

        public void waitForProcess() {
            boolean failed_process = false;
            System.out.println("Waiting for " + processes.size() + " jobs");
            for (Process process : processes) {
                do
                {
                    process.attach();
                } while (process.isFailed() && process.getNextRunTime() != null);

                if (process.isFailed())
                    failed_process = true;

                process.delete();
            }

            System.out.println("Finished waiting for " + processes.size() + " jobs");

            if (failed_process)
                throw new QueujException("GuildScanRunner failed.");
        }
    }

    public static class TestQueueRestriction extends QueueRestriction {

        @Override
        protected boolean canRun(final Queue queue, Process process) {
            return process.getContainingServer().indexesWithReadLock(new ProcessIndexesCallback<Boolean>() {

                public Boolean readIndexes(ProcessIndexes pi) {
                    HasLessThan<ProcessWrapper> hasLessThen = new HasLessThan<ProcessWrapper>(100);
                    hasLessThen = pi.iterateRunningProcesses(queue, hasLessThen);
                    pi.iterateWaitingToRunProcesses(queue, hasLessThen);
                    return hasLessThen.hasLess();
                }
            });
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainPanel = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        jSpinner1 = new javax.swing.JSpinner();
        jButton5 = new javax.swing.JButton();
        jButton6 = new javax.swing.JButton();
        menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenu helpMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();
        statusPanel = new javax.swing.JPanel();
        javax.swing.JSeparator statusPanelSeparator = new javax.swing.JSeparator();
        statusMessageLabel = new javax.swing.JLabel();
        statusAnimationLabel = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();

        mainPanel.setName("mainPanel"); // NOI18N

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(queujsample.QueujSampleApp.class).getContext().getResourceMap(QueujSampleView.class);
        jButton1.setText(resourceMap.getString("jButton1.text")); // NOI18N
        jButton1.setName("jButton1"); // NOI18N
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton2.setText(resourceMap.getString("jButton2.text")); // NOI18N
        jButton2.setName("jButton2"); // NOI18N
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jButton3.setText(resourceMap.getString("jButton3.text")); // NOI18N
        jButton3.setName("jButton3"); // NOI18N
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        jButton4.setText(resourceMap.getString("jButton4.text")); // NOI18N
        jButton4.setName("jButton4"); // NOI18N
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });

        jSpinner1.setName("jSpinner1"); // NOI18N

        jButton5.setText(resourceMap.getString("jButton5.text")); // NOI18N
        jButton5.setName("jButton5"); // NOI18N
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });

        jButton6.setText(resourceMap.getString("jButton6.text")); // NOI18N
        jButton6.setName("jButton6"); // NOI18N
        jButton6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton6ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addComponent(jButton1)
                        .addGap(55, 55, 55)
                        .addComponent(jSpinner1, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jButton2)
                    .addComponent(jButton3)
                    .addComponent(jButton4)
                    .addComponent(jButton5)
                    .addComponent(jButton6))
                .addContainerGap(187, Short.MAX_VALUE))
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton1)
                    .addComponent(jSpinner1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jButton2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jButton3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jButton4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jButton5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jButton6)
                .addContainerGap(38, Short.MAX_VALUE))
        );

        menuBar.setName("menuBar"); // NOI18N

        fileMenu.setText(resourceMap.getString("fileMenu.text")); // NOI18N
        fileMenu.setName("fileMenu"); // NOI18N

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(queujsample.QueujSampleApp.class).getContext().getActionMap(QueujSampleView.class, this);
        exitMenuItem.setAction(actionMap.get("quit")); // NOI18N
        exitMenuItem.setName("exitMenuItem"); // NOI18N
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        helpMenu.setText(resourceMap.getString("helpMenu.text")); // NOI18N
        helpMenu.setName("helpMenu"); // NOI18N

        aboutMenuItem.setAction(actionMap.get("showAboutBox")); // NOI18N
        aboutMenuItem.setName("aboutMenuItem"); // NOI18N
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        statusPanel.setName("statusPanel"); // NOI18N

        statusPanelSeparator.setName("statusPanelSeparator"); // NOI18N

        statusMessageLabel.setName("statusMessageLabel"); // NOI18N

        statusAnimationLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        statusAnimationLabel.setName("statusAnimationLabel"); // NOI18N

        progressBar.setName("progressBar"); // NOI18N

        javax.swing.GroupLayout statusPanelLayout = new javax.swing.GroupLayout(statusPanel);
        statusPanel.setLayout(statusPanelLayout);
        statusPanelLayout.setHorizontalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(statusPanelSeparator, javax.swing.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(statusMessageLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 216, Short.MAX_VALUE)
                .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(statusAnimationLabel)
                .addContainerGap())
        );
        statusPanelLayout.setVerticalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addComponent(statusPanelSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(statusMessageLabel)
                    .addComponent(statusAnimationLabel)
                    .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(3, 3, 3))
        );

        setComponent(mainPanel);
        setMenuBar(menuBar);
        setStatusBar(statusPanel);
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed

        JavaProcessBuilder pb = SAMPLE_QUEUE.newProcessBuilder(Locale.getDefault());
        pb.setProcessName("Test 1");
        pb.setProcessDescription("Test 1");
        pb.setProcessDetails(new TestRunner(), "run", new Class[] {}, new Object[] {});
        pb.setProcessPersistence(false);
        pb.newProcess();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed

        JavaProcessBuilder pb = SAMPLE_QUEUE.newProcessBuilder(Locale.getDefault());
        pb.setProcessName("Test 2");
        pb.setProcessPersistence(false);

        for (int i = 0; i<=(Integer)jSpinner1.getValue(); i++) {
            pb.setProcessDescription("Test 2");
            pb.setProcessDetails(new TestRunner(), "run", new Class[] {}, new Object[] {});

            RunOnce occurrence = new RunOnce();
            RelativeScheduleBuilder rsb = occurrence.newRelativeScheduleBuilder();
            rsb.setRunDelaySeconds((int)(Math.random() * 60));
            rsb.createSchedule();
            pb.setProcessOccurrence(occurrence);

            pb.newProcess();
        }
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        JavaProcessBuilder pb = SAMPLE_QUEUE.newProcessBuilder(Locale.getDefault());
        pb.setProcessName("Test 3");
        pb.setProcessDescription("Test 3");
        pb.setProcessDetails(new TestRunner(), "run", new Class[] {}, new Object[] {});
        pb.setProcessPersistence(true);
        pb.newProcess();
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        JavaProcessBuilder pb = SAMPLE_QUEUE.newProcessBuilder(Locale.getDefault());
        pb.setProcessName("Test 4");

        for (int i = 0; i<=(Integer)jSpinner1.getValue(); i++) {
            pb.setProcessDescription("Test 4");
            pb.setProcessDetails(new TestRunner(), "run", new Class[] {}, new Object[] {});

            RunOnce occurrence = new RunOnce();
            RelativeScheduleBuilder rsb = occurrence.newRelativeScheduleBuilder();
            rsb.setRunDelaySeconds((int)(Math.random() * 60));
            rsb.createSchedule();
            pb.setProcessOccurrence(occurrence);

            pb.newProcess();
        }
    }//GEN-LAST:event_jButton4ActionPerformed

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
        JavaProcessBuilder pb = SAMPLE_QUEUE.newProcessBuilder(Locale.getDefault());
        pb.setProcessName("Test 5");
        pb.setProcessDescription("Test 5");
        Test2Runner runner = new Test2Runner();
        pb.setProcessDetails(runner, "run", new Class[] {Integer.TYPE}, new Object[] {new Integer(1)});
        pb.addProcessSection(runner, "waitForProcess", new Class[] {}, new Object[] {});
        pb.setProcessPersistence(true);
        pb.newProcess();
    }//GEN-LAST:event_jButton5ActionPerformed

    private void jButton6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton6ActionPerformed
        JavaProcessBuilder pb = SAMPLE_QUEUE.newProcessBuilder(Locale.getDefault());
        pb.setProcessName("Test 5");
        pb.setProcessDescription("Test 5");
        Test2Runner runner = new Test2Runner();
        pb.setProcessDetails(runner, "run", new Class[] {Integer.TYPE}, new Object[] {(Integer)jSpinner1.getValue()});
        pb.addProcessSection(runner, "waitForProcess", new Class[] {}, new Object[] {});
        pb.setProcessPersistence(true);
        pb.newProcess();
    }//GEN-LAST:event_jButton6ActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JSpinner jSpinner1;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JLabel statusAnimationLabel;
    private javax.swing.JLabel statusMessageLabel;
    private javax.swing.JPanel statusPanel;
    // End of variables declaration//GEN-END:variables

    private final Timer messageTimer;
    private final Timer busyIconTimer;
    private final Icon idleIcon;
    private final Icon[] busyIcons = new Icon[15];
    private int busyIconIndex = 0;

    private JDialog aboutBox;
}
