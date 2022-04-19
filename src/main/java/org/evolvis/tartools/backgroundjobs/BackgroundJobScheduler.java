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

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RunnableFuture;

import org.evolvis.tartools.backgroundjobs.BackgroundJobInfo.State;

public class BackgroundJobScheduler implements Runnable {
	private final class MyThread implements BackgroundThread  {
		private final Thread t;
		boolean started = false;

		private MyThread(Thread t) {
			this.t = t;
		}

		@Override
		public void start() {
			t.start();
			started = true;
		}

		@Override
		public void join(long timeout) throws InterruptedException {
			synchronized (BackgroundJobScheduler.this) {
				while (t.isAlive() || !started) {
					BackgroundJobScheduler.this.wait(100);
				}
			}
			t.join(timeout);
		}
	}

	int jobsToKeep = 5;
	final LinkedHashMap<String, BackgroundJobStatus<?>> scheduledJobs = new LinkedHashMap<String, BackgroundJobStatus<?>>();
	final LinkedList<String> oldIds = new LinkedList<String>();
	final LinkedBlockingQueue<BackgroundJobStatus<?>> queue = new LinkedBlockingQueue<BackgroundJobStatus<?>>();
	private FutureFactory futureFactory = new FutureFactory() {

		@Override
		public <V> FutureTask<V> createFutureTask(Callable<V> callable) {
			return new FutureTask<V>(callable);
		}
	};
	private BackgroundThreadFactory threadFactory = new BackgroundThreadFactory() {

		@Override
		public BackgroundThread createThread(RunnableFuture<?> f) {

			final Thread t = new Thread(f) {
				public void run() {
					super.run();
					BackgroundJobScheduler lock = BackgroundJobScheduler.this;
					synchronized(lock){
						lock.notifyAll();
					}
				};
			};

			return new MyThread(t);
		}
	};

	private BackgroundJobLogFactory jobLogFactory = new BackgroundJobLogFactory() {

		@Override
		public BackgroundJobLog createJobLog(final String id) {

			return new BackgroundJobLog() {

				@Override
				public void log(org.evolvis.tartools.backgroundjobs.BackgroundJobMonitor.Severity severity,
						Object message) {
					System.err.println(severity + ":" + id + ": " + message);

				}

				@Override
				public void close() {
					;
				}
			};
		}
	};

	public BackgroundJobLogFactory getJobLogFactory() {
		return jobLogFactory;
	}

	public void setJobLogFactory(BackgroundJobLogFactory jobLogFactory) {
		this.jobLogFactory = jobLogFactory;
	}

	private Thread thread;
	private Object threadLock = new Object();
	private boolean idle = true;

	public <T> BackgroundJobStatus<T> schedule(BackgroundJob<T> job) {
		ScheduledJob<T> scheduledJob = new ScheduledJob<T>(job, threadFactory, futureFactory, jobLogFactory);
		schedule(scheduledJob);
		return scheduledJob;
	}

	public <T> BackgroundJobStatus<T> schedule(String id, BackgroundJob<T> job) {
		ScheduledJob<T> scheduledJob = new ScheduledJob<T>(id, job, threadFactory, futureFactory, jobLogFactory);
		schedule(scheduledJob);
		return scheduledJob;
	}

	@Deprecated
	public <T> BackgroundJobStatus<T> scheduldeImmediately(BackgroundJob<T> job) {
		// typo fix (kept for API)
		return scheduleImmediately(job);
	}

	public <T> BackgroundJobStatus<T> scheduleImmediately(BackgroundJob<T> job) {
		ScheduledJob<T> scheduledJob = null;
		synchronized (this) {
			if (isIdle()) {
				scheduledJob = new ScheduledJob<T>(job, threadFactory, futureFactory, jobLogFactory);
				schedule(scheduledJob);
			}
		}

		return scheduledJob;
	}

	private synchronized <T> void schedule(ScheduledJob<T> scheduledJob) {
		scheduledJob.addJobListener(new JobListener() {

			@Override
			public void stateChanged(JobEvent e) {
				jobStateChanged(e);
			}

			@Override
			public void progressInfoUpdated(JobEvent e) {
				;
			}
		});
		synchronized (this) {
			idle = false;
			queue.add(scheduledJob);
			scheduledJobs.put(scheduledJob.getId(), scheduledJob);
			notifyAll();
		}

	}

	public Collection<BackgroundJobStatus<?>> getJobs() {
		synchronized (this) {
			return scheduledJobs.values();
		}

	}

	public synchronized boolean isIdle() {
		return idle;
	}

	public void ensureStarted() {
		synchronized (threadLock) {
			if (thread == null || !thread.isAlive()) {
				thread = new Thread(this, "BackgroundJob Scheduler");
				thread.start();
			}
		}
	}

	public void stop() throws InterruptedException {
		synchronized (threadLock) {
			if (thread == null || thread.isAlive()) {
				thread.interrupt();
				thread.join();
			}
			// abort all remaining jobs
			while (!queue.isEmpty()) {
				final ScheduledJob<?> job = (ScheduledJob<?>) queue.take();
				job.abort();
			}
		}
	}

	public BackgroundJobStatus<?> getJob(String id) {
		synchronized (this) {
			return scheduledJobs.get(id);
		}

	}

	protected void jobStateChanged(JobEvent e) {
		if (e.getOldState() == State.SCHEDULED) {
			synchronized (this) {
				queue.remove(e.getSource());
				oldIds.addLast(e.getSource().getId());
				if (oldIds.size() > jobsToKeep) {
					String id = oldIds.removeFirst();
					scheduledJobs.remove(id);
				}
			}
		}
		if(Arrays.asList(State.ABORTED,State.FAILED,State.SUCCEEDED).contains(e.getNewState())){
			idle = queue.isEmpty();
		}

	}

	@Override
	public void run() {
		try {
			while (!Thread.interrupted()) {
				executeNext();
			}
		} catch (InterruptedException e) {
			;
		}
	}

	protected synchronized BackgroundJobStatus<?> executeNext() throws InterruptedException {
		final ScheduledJob<?> job;
		while (queue.isEmpty()) {
			wait(1000);
		}
		job = (ScheduledJob<?>) queue.take();
		this.idle = false;
		job.execute();

		job.join(0);

		return job;
	}

	public BackgroundThreadFactory getThreadFactory() {
		return threadFactory;
	}

	public void setThreadFactory(BackgroundThreadFactory threadFactory) {
		this.threadFactory = threadFactory;
	}

	public FutureFactory getFutureFactory() {
		return futureFactory;
	}

	public void setFutureFactory(FutureFactory futureFactory) {
		this.futureFactory = futureFactory;
	}

	public int getJobsToKeep() {
		return jobsToKeep;
	}

	public void setJobsToKeep(int jobsToKeep) {
		this.jobsToKeep = jobsToKeep;
	}
}
