package org.evolvis.tartools.backgroundjobs;

/*-
 * Background-Jobs is Copyright
 *  © 2012 Атанас Александров (a.alexandrov@tarent.de)
 *  © 2012, 2013, 2014, 2015 Lukas Degener (l.degener@tarent.de)
 *  © 2015, 2016 Jens Oberender (j.oberender@tarent.de)
 * Licensor is tarent solutions GmbH, http://www.tarent.de/
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import org.evolvis.tartools.backgroundjobs.BackgroundJobMonitor.Severity;

class ScheduledJob<T> implements BackgroundJobStatus<T> {
    private final BackgroundThread thread;
    private final FutureTask<T> future;
    private int workDone;
    private int workTotal;
    private State state = State.SCHEDULED;
    private final ArrayList<JobListener> listeners = new ArrayList<JobListener>();
    private final Object stateLock = new Object();
    final private String description;
    final private Class<?> jobClass;
    final private String id;
    private boolean aborting;
    private long startTime;
    private long lastModified;
    final private boolean visible;
    final private boolean cancellationSupported;

    public ScheduledJob(final BackgroundJob<T> job,
            final BackgroundThreadFactory threadFactory,
            final FutureFactory futureFactory, final BackgroundJobLogFactory jobLogFactory) {
        this(UUID.randomUUID().toString(), job, threadFactory, futureFactory,
                jobLogFactory);
    }

    private class Monitor implements BackgroundJobMonitor, BackgroundJobLog {

        private final BackgroundJobLog jobLog;

        public Monitor(final BackgroundJobLog jobLog) {
            this.jobLog = jobLog;
        }

        @Override
        public String getScheduledJobId() {
            return getId();
        }

        @Override
        public boolean isAborting() {
            return aborting;
        }

        @Override
        public void log(final Severity severity, final Object message) {
            jobLog.log(severity, message);
        }

        @Override
        public void announceTotal(final int totalItems) {
            workTotal = totalItems;
            fireProgressInfoUpdated();
        }

        @Override
        public void reportProgressIncrement(final int items) {
            workDone += items;
            fireProgressInfoUpdated();
        }

        @Override
        public void reportProgressAbsolute(final int items) {
            workDone = items;
            fireProgressInfoUpdated();
        }

        @Override
        public void close() {
            jobLog.close();
        }

    }

    public ScheduledJob(final String id, final BackgroundJob<T> job,
            final BackgroundThreadFactory threadFactory,
            final FutureFactory futureFactory,
            final BackgroundJobLogFactory jobLogFactory) {
        this.id = id;
        this.description = job.getDescription();
        this.visible = job.isVisible();
        this.cancellationSupported = job.isCancellationSupported();
        this.jobClass = job.getClass();
        startTime = System.currentTimeMillis();
        final Callable<T> callable;
        callable = new Callable<T>() {

            @Override
            public T call() throws Exception {
                jobRunning();
                final BackgroundJobLog jobLog = jobLogFactory.createJobLog(id);
                final Monitor monitor = new Monitor(jobLog);
                try {
                    final T result = job.work(monitor);
                    jobReturned();
                    return result;
                } catch (final Throwable e) {
                    jobRaisedException();
                    final StringWriter stringWriter = new StringWriter();
                    final PrintWriter printWriter = new PrintWriter(stringWriter);
                    e.printStackTrace(printWriter);
                    monitor.log(Severity.ERROR,
                            "Job raised an uncaught exception:" + stringWriter);
                    if (e instanceof Exception) {
                        throw (Exception) e;
                    }
                    throw new RuntimeException(e);

                } finally {
                    monitor.close();
                }

            }

        };

        future = futureFactory.createFutureTask(callable);
        thread = threadFactory.createThread(future);

    }

    public void execute() {
        jobStarted();
        thread.start();
    }

    private void jobStarted() {
        setState(State.STARTING);
    }

    private void jobRunning() {
        synchronized (stateLock) {
            if (getState() == State.ABORTING_STARTING) {
                setState(State.ABORTING);
                future.cancel(true);
            } else {
                setState(State.RUNNING);
            }
        }

    }

    protected void jobRaisedException() {
        setState(State.FAILED);
    }

    protected void jobReturned() {
        synchronized (stateLock) {
            setState(getState() == State.ABORTING ? State.ABORTED
                    : State.SUCCEEDED);
        }
    }

    @Override
    public void abort() {
        synchronized (stateLock) {
            switch (getState()) {
                case SCHEDULED:
                    setState(State.ABORTED);
                    future.cancel(true);
                    break;
                case STARTING:
                    setState(State.ABORTING_STARTING);
                    break;
                default:
                    setState(State.ABORTING);
                    future.cancel(true);
                    break;
            }
        }
    }

    @Override
    public void join(final long timeout) throws InterruptedException {
        thread.join(timeout);
    }

    @Override
    public T result() throws InterruptedException, ExecutionException {

        return future.get();
    }

    @Override
    public State getState() {
        synchronized (stateLock) {
            return state;
        }

    }

    @Override
    public int getWorkDone() {

        return workDone;
    }

    @Override
    public int getWorkTotal() {
        return workTotal;
    }

    public void setState(final State newState) {
        final State oldState;
        synchronized (stateLock) {
            oldState = this.state;
            if (newState == oldState) {
                return;
            }
            // copy this information, so the job does not need to
            // obtain stateLock for calling isAborting()
            if (newState == State.ABORTING) {
                aborting = true;
            }
            this.state = newState;

        }
        fireStateChanged(oldState, newState);

    }

    @Override
    public void addJobListener(final JobListener l) {
        synchronized (listeners) {
            if (!listeners.contains(l)) {
                listeners.add(l);
            }
        }
    }

    @Override
    public void removeJobListener(final JobListener l) {
        synchronized (listeners) {
            if (listeners.contains(l)) {
                listeners.remove(l);
            }
        }
    }

    private void fireStateChanged(final State oldState, final State newState) {
        lastModified = System.currentTimeMillis();
        final ArrayList<JobListener> clonedListeners;
        final JobEvent e;
        synchronized (listeners) {
            clonedListeners = new ArrayList<JobListener>(listeners);
            e = new JobEvent(this, oldState, newState);
        }
        for (final JobListener jobListener : clonedListeners) {
            jobListener.stateChanged(e);
        }
    }

    private void fireProgressInfoUpdated() {
        lastModified = System.currentTimeMillis();
        final ArrayList<JobListener> clonedListeners;
        final JobEvent e;
        synchronized (listeners) {
            clonedListeners = new ArrayList<JobListener>(listeners);
            e = new JobEvent(this);
        }
        for (final JobListener jobListener : clonedListeners) {
            jobListener.stateChanged(e);
        }

    }

    @Override
    public String getId() {

        return id;
    }

    @Override
    public String getDescription() {

        return description;
    }

    @Override
    public Class<?> getJobClass() {

        return jobClass;
    }

    @Override
    public long getLastModified() {
        return lastModified;
    }

    @Override
    public long getStartTime() {
        return startTime;
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public boolean isCancellationSupported() {
        return cancellationSupported;
    }
}
