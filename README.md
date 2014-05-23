# QueuJ

QueuJ is a Java job queue library. Its goal is to provide a simple, flexible and extensible batch processing library that fulfils the scalability, performance and resilience requirements of applications.

## Features

 * **Concurrency** -- provides powerful but simple concurrency controls to limit the number of jobs running concurrently.

 * **Scheduling** -- provides flexible scheduling options to control how often and when a job is run.

 * **Resilience** -- provides flexible resilience options to control what happens for missed scheduled runs and failed runs.

 * **Optional persistence** -- jobs can optionally be persisted to the database to survive JVM restarts/downtime.

 * **Transactional Sections** -- jobs can consist of multiple sections with managed transactions and will restart from the currently running section after a JVM restart/failure.

 * **User access** -- provides user access options so users can be allowed or denied access to restarting, deleting or viewing jobs.

## Getting Started

**Simple Example**

Here is a simple Hello World example. This will create a job that runs immediately and outputs 'Hello World' to System.out.

```java
JavaProcessBuilder pb = QueueFactory.DEFAULT_QUEUE.newProcessBuilder(Locale.getDefault());
pb.setProcessName("Test");
pb.setProcessDescription("Test Job");
pb.setProcessDetails(new TestRunner(), "run", new Class[] {String.class}, new Object[] {"Hello World"});
pb.newProcess();
```

```java
public class TestRunner extends JavaProcessRunner {

    public void run(String message) {
        System.out.println(message);
    }
}
```

**Persistence**

By default jobs are non-persistent. To use JPA persistence you must setup the following parameters before using QueuJ. Once setup jobs will default to being persistent unless overridden.

```java
System.setProperty("com.workplacesystems.queuj.QueujFactory", "com.workplacesystems.queuj.process.jpa.JPAFactory");
System.setProperty("com.workplacesystems.jpa.persistenceUnitName", "qjDatabase");
```

ProcessImpl class nodes must also be added to persistence.xml.

```xml
   <persistence-unit name="qjDatabase" transaction-type="RESOURCE_LOCAL">
      <class>com.workplacesystems.queuj.process.jpa.ProcessImpl</class>
```

**Sections**

A job can be split into multiple sections with transactions managed by the library. If an exception is thrown during the section the transaction will be rolled back and the section will be reattempted based on the supplied failure schedule. Serializable Objects can be stored in one section and used in another as class variables of the runners instance or using the JavaProcessRunner putValue/getValue methods. To create sections.

```java
JavaProcessBuilder pb = QueueFactory.DEFAULT_QUEUE.newProcessBuilder(Locale.getDefault());
pb.setProcessName("Test");
pb.setProcessDescription("Test Job");
TestRunner runner = new TestRunner();
pb.setProcessDetails(runner, "sectionOne", new Class[] {String.class}, new Object[] {"Hello World"});
pb.addProcessSection(runner, "sectionTwo", new Class[] {}, new Object[] {});
pb.newProcess();
```

```java
public class TestRunner extends JavaProcessRunner {

    private String message;

    public void sectionOne(String message) {
        this.message = message;
    }
    public void sectionTwo() {
        System.out.println(message);
    }
}
```


**Queues**

Queues can be created to provided to define a default set of properties for jobs. Queues are hierarchical and inherit properties from the parent Queue. Most properties can be overridden by the ProcessBuilder but some (i.e. QueueRestriction) can only be specified against the Queue. To create a new Queue.

```java
QueueBuilder<JavaProcessBuilder> qb = QueueFactory.DEFAULT_QUEUE.newQueueBuilder();
Queue sampleQueue = qb.newQueue();
```

**Concurrency**

Concurrency restrictions are set up against the Queue.

```java
QueueBuilder<JavaProcessBuilder> qb = QueueFactory.DEFAULT_QUEUE.newQueueBuilder();
qb.setQueueRestriction(new TestQueueRestriction());
Queue sampleQueue = qb.newQueue();
```

This example restriction class will limit the number of jobs that run concurrently to 10. It counts the jobs that are running (iterateRunningProcesses) and jobs that have already passed concurrency checks but are not yet running (iterateWaitingToRunProcesses) and will return false if they total to over 10. This code uses HasLessThan which stops counting any jobs above the passed in limit to improve scalability.

```java
public class TestQueueRestriction extends QueueRestriction {

    @Override
    protected boolean canRun(final Queue queue, Process process) {
        return process.getContainingServer().indexesWithReadLock(new ProcessIndexesCallback<Boolean>() {

            public Boolean readIndexes(ProcessIndexes pi) {
                HasLessThan<ProcessWrapper> hasLessThen = new HasLessThan<ProcessWrapper>(10);
                hasLessThen = pi.iterateRunningProcesses(queue, hasLessThen);
                pi.iterateWaitingToRunProcesses(queue, hasLessThen);
                return hasLessThen.hasLess();
            }
        });
    }
}
```
So any jobs now created with the queue sampleQueue will be limited to a concurrency of 10.

The QueueRestriction maybe called many times and on a critical path. Therefore the QueueRestriction must be coded to be as high performance as possible. The ProcessIndexes class returned by the getProcessIndexes method provides various high performance ways of analysing the current state of the queues. Custom indexes can be added to the Queue to allow for high performance concurrency requirements. For instance this code will force a concurrency of 2 per user:

```java
QueueBuilder<UserIdProcessBuilder> qb = QueueFactory.DEFAULT_QUEUE.newQueueBuilder(UserIdProcessBuilder.class);
qb.setQueueRestriction(new UserIdQueueRestriction());
qb.setIndex(new UserIdIndex());
Queue<UserIdProcessBuilder> sampleQueue = qb.newQueue();
UserIdProcessBuilder uipb = sampleQueue.newProcessBuilder(Locale.getDefault());
uipb.newProcess();

public class UserIdIndex extends Index {
    @Override
    protected Object getKey(Process process) {
        return process.getParameter("user_id");
    }
}
public class UserIdQueueRestriction extends QueueRestriction {
    @Override
    protected boolean canRun(final Queue queue, Process process) {
        return process.getContainingServer().indexesWithReadLock(new ProcessIndexesCallback<Boolean>() {

            public Boolean readIndexes(ProcessIndexes pi) {
                HasLessThan<ProcessWrapper> hasLessThen = new HasLessThan<ProcessWrapper>(2);
                hasLessThen = pi.iterateRunningProcesses(queue, queue.getIndexKey(process), hasLessThen);
                pi.iterateWaitingToRunProcesses(queue, queue.getIndexKey(process), hasLessThen);
                return hasLessThen.hasLess();
            }
        });
    }
}
public class UserIdProcessBuilder extends JavaProcessBuilder {
    public UserIdProcessBuilder(Queue queue, Locale locale) {
        super(queue, locale);
    }
    @Override
    protected void setupProcess(Process process)
    {
        super.setupProcess(process);
        process.setParameter("user_id", getUser().getUserId());
    }
}
```

**Scheduling**

By default a job will run when the currently active transaction commits or when ProcessBuilder.newProcess is called if there is no transaction. This sample code will create a job that runs in 2 hours from now.

```java
RunOnce occurrence = new RunOnce();
RelativeScheduleBuilder rsb = occurrence.newRelativeScheduleBuilder();
rsb.setRunDelayHours(2);
rsb.createSchedule();
pb.setProcessOccurrence(occurrence);
```

The Occurrence can be set on the QueueBuilder instead of the ProcessBuilder so that all jobs created with that queue get that schedule unless overridden on the ProcessBuilder.

```java
qb.setDefaultOccurrence(occurrence);
```

An AbsoluteScheduleBuilder can be used to schedule a job for a fixed point in time rather than relative to the current time.

```java
GregorianCalendar gc = new GregorianCalendar();
gc.set(Calendar.HOUR, 9);
RunOnce occurrence = new RunOnce();
AbsoluteScheduleBuilder asb = occurrence.newAbsoluteScheduleBuilder();
asb.setDate(gc);
asb.createSchedule();
pb.setProcessOccurrence(occurrence);
```

Other occurrences can be used to run multiple times. For instance this code schedules the job to run every day at 9.

```java
RunDaily occurrence = new RunDaily();
DailyScheduleBuilder dsb = occurrence.newSchedulerBuilder();
dsb.setScheduledHour(9);
dsb.createSchedule();
pb.setProcessOccurrence(occurrence);
```

The RunFiniteTimes occurrence can be used to schedule a job to run a fixed number of times. This code schedules the job to run 2 times, first in 30 minutes and then again 2 hours after that.

```java
RunFiniteTimes occurrence = new RunFiniteTimes(2);
RelativeScheduleBuilder rsb = occurrence.newRelativeScheduleBuilder();
rsb.setRunDelayMinutes(30);
rsb.createSchedule();
rsb = occurrence.newRelativeScheduleBuilder();
rsb.setRunDelayHours(2);
rsb.createSchedule();
pb.setProcessOccurrence(occurrence);
```

**Resilience**

The Resilience class controls what happens when scheduled runs are missed and when a run fails. There are currently 3 subclasses of Resilience: ForgetMissed, RunOnlyOnce and CatchUp. ForgetMissed ignores any missed scheduled runs; RunOnlyOnce runs immediately once for any number of missed runs and CatchUp runs all missed scheduled runs immediately and serially.

```java
pb.setProcessResilience(new RunOnlyOnce());
```

The Resilience can be set against the QueueBuilder to apply to all jobs created with that Queue unless overridden on the ProcessBuilder.

```java
qb.setDefaultResilience(new RunOnlyOnce());
```

The Resilience class is also used to control failure retries by setting a failure schedule. The failure schedule is created in the same way as normal occurrences other than infinite occurrences are not supported. The failure schedule is then set in the Resilience. This code creates a resilience that runs only once for any missed runs and a failure schedule that reattempts the job after 10 minutes and then, if that run also fails, after another 2 hours.

```java
Resilience resilience = new RunOnlyOnce();
RunFiniteTimes failureOccurrence = new RunFiniteTimes(2);
resilience.setFailureSchedule(failureOccurrence);
RelativeScheduleBuilder rsb = failureOccurrence.newRelativeScheduleBuilder(); 
rsb.setRunDelayMinutes(10);
rsb.createSchedule();
rsb = failureOccurrence.newRelativeScheduleBuilder();
rsb.setRunDelayHours(2);
rsb.createSchedule();
pb.setProcessResilience(resilience);
```

**JBoss Seam 3 Integration**

QueuJ has been added to Seam Cron [http://seamframework.org/Seam3/CronModule] as a provider and Seam Cron is currently being enhanced to support more of the functionality provided by QueuJ. This will be in a future release of Seam Cron.

**JBoss Seam 2 Integration**

When the QueuJ jar file is included on the classpath for JBoss Seam applications QueuJ will automatically initialise and provide DEFAULT_QUEUE in the application context. Application scope beans can be created for initialising custom queues at startup. For example this Application scope bean injects the DEFAULT_QUEUE and outjects SAMPLE_QUEUE.

```java
@Name("initialiser")
@Startup(depends={"queujInitialiser"})
@Scope(ScopeType.APPLICATION)
public class Initialiser {

    @In
    private Queue<SeamProcessBuilder> DEFAULT_QUEUE;

    @Out
    private Queue<SeamProcessBuilder> SAMPLE_QUEUE;

    @Create
    public void init() {
        QueueBuilder<SeamProcessBuilder> qb = DEFAULT_QUEUE.newQueueBuilder();
        SAMPLE_QUEUE = qb.newQueue();
    }
```

The SeamProcessBuilder differs from the standard JavaProcessBuilder in that it takes a class for the setProcessDetails/addProcessSection methods rather than an instance. QueuJ will then use Component.getInstance to create the runner.

```java
@In
private Queue<SeamProcessBuilder> SAMPLE_QUEUE;

public void runJob1() {
    SeamProcessBuilder pb = SAMPLE_QUEUE.newProcessBuilder(Locale.getDefault());
    pb.setProcessName("Test 1");
    pb.setProcessDescription("Test 1");
    pb.setProcessDetails(TestRunner.class, "run", new Class[] {String.class}, new Object[] {"Hello World"});
    pb.newProcess();
}
```

Runners therefore need to be setup as Seam components and instead of extending JavaProcessRunner should inject the PROCESS_RUNNER component if required instead.

```java
@Name("testRunner")
public class TestRunner {

    @In
    private JavaProcessRunner PROCESS_RUNNER;

    public void run(String message) {
        System.out.println(message);
    }
}
```

Because the runner is now a component values passed between sections should no longer be saved as class variables. They need to be saved in the PROCESS_RUNNER using the putValue method. The value can be retrieved either using the PROCESS_RUNNER getValue method or injecting the variable.

```java
@Name("testRunner")
public class TestRunner {

    @In
    private JavaProcessRunner PROCESS_RUNNER;

    @In(required=false)
    private String message;

    public void run1(String message) {
        PROCESS_RUNNER.putValue("message", message);
    }

    public void run2() {
        System.out.println(message);
    }
}
```

**Common Patterns**

Sometimes you may want a process to have different concurrency limits at different points throughout the run. For instance you may want an initialisation phase with concurrency of 1; middle phase with a concurrency of 10 and a finalisation phase with concurrency of 1 again. This can be achieved by having a job create other jobs in a section and waiting for them to complete in the following section.

Firstly set up two queues with the two different concurrency limits.

```java
QueueBuilder<JavaProcessBuilder> qb = QueueFactory.DEFAULT_QUEUE.newQueueBuilder();
qb.setQueueRestriction(new MainJobQueueRestriction());
Queue MAIN_JOB_QUEUE = qb.newQueue();
qb = QueueFactory.DEFAULT_QUEUE.newQueueBuilder();
qb.setQueueRestriction(new SubJobQueueRestriction());
Queue SUB_JOB_QUEUE = qb.newQueue();
```

```java
public class MainJobQueueRestriction extends QueueRestriction {
    @Override
    protected boolean canRun(final Queue queue, Process process) {
        return process.getContainingServer().indexesWithReadLock(new ProcessIndexesCallback<Boolean>() {

            public Boolean readIndexes(ProcessIndexes pi) {
                boolean blocked = pi.iterateRunningProcesses(queue, new NotEmptyIterativeCallback<ProcessWrapper>());
                if (!blocked)
                    blocked = pi.iterateWaitingToRunProcesses(queue, new NotEmptyIterativeCallback<ProcessWrapper>());
                return !blocked;
            }
        });
    }
}
public class SubJobQueueRestriction extends QueueRestriction {
    @Override
    protected boolean canRun(final Queue queue, Process process) {
        return process.getContainingServer().indexesWithReadLock(new ProcessIndexesCallback<Boolean>() {

            public Boolean readIndexes(ProcessIndexes pi) {
                HasLessThan<ProcessWrapper> hasLessThen = new HasLessThan<ProcessWrapper>(10);
                hasLessThen = pi.iterateRunningProcesses(queue, hasLessThen);
                pi.iterateWaitingToRunProcesses(queue, hasLessThen);
                return hasLessThen.hasLess();
            }
        });
    }
}
```

Create the main job with 2 sections

```java
JavaProcessBuilder pb = MAIN_JOB_QUEUE.newProcessBuilder(Locale.getDefault());
pb.setProcessName("MainJob");
pb.setProcessDescription("Main Job");
MainJobRunner runner = new MainJobRunner();
pb.setProcessDetails(runner, "initPhase", new Class[] {}, new Object[] {});
pb.addProcessSection(runner, "finalPhase", new Class[] {}, new Object[] {});
pb.newProcess();
```

In the initPhase method of the runner do any initialisation and create as many sub jobs as required. The sub jobs must be setup to keep completed so that the finalPhase of the runner can wait for the sub jobs to complete and delete them. This is so that the job isn't deleted before being attached to and so that saved values from the sub jobs can be retrieved in the main job. The loop around the process.attach will only exit for a failed job if no further scheduled runs exist. This allows failure schedules to be set up against the sub jobs and the main job will wait for all failure attempts.

```java
public class MainJobRunner extends JavaProcessRunner {

    private ArrayList<Process> subJobs = new ArrayList<Process>();

    public void initPhase() {
        System.out.println("Initialisation phase with concurrency of 1");

        JavaProcessBuilder pb = SUB_JOB_QUEUE.newProcessBuilder(Locale.getDefault());
        pb.setProcessName("SubJob");
        pb.setProcessKeepCompleted(true);
        for (int i =1; i<=100; i++) {
            pb.setProcessDescription("Sub Job" + i);
            SubJobRunner runner = new SubJobRunner();
            pb.setProcessDetails(runner, "run", new Class[] {}, new Object[] {});
            subJobs.add(pb.newProcess());
        }
    }
    public void finalPhase() {
        boolean failedProcess = false;
        for (Process process : subJobs) {
            do {
                process.attach();
            } while (process.isFailed() && process.getNextRunTime() != null);

            if (process.isFailed())
                failed_process = true;

            try {
                JavaProcessSession session = (JavaProcessSession)process.getParameter(JavaProcessServer.JAVA_PROCESS_SESSION);
                String subJobResult = (String)session.getValue("SUB_JOB_RESULT");
            }
            finally {
                process.delete();
            }
        }
        if (failedProcess)
            throw new QueujException("SubJob failed.");

        System.out.println("Finalisation phase with concurrency of 1");
    }
}
public class SubJobRunner extends JavaProcessRunner {
    public void run() {
        System.out.println("SubJob phase with concurrency of 10");
        putValue("SUB_JOB_RESULT", "Hello World");
    }
}
```
