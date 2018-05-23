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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import org.evolvis.tartools.backgroundjobs.BackgroundJobInfo.State;

@RunWith(MockitoJUnitRunner.class)
public class ScheduledJobTest {
	enum Steps {
		START, BEFORE_CALL, WORKING, AFTER_CALL
	}

	Sequencer<Steps> seq = Sequencer.forFixedSteps(Steps.values());
	ScheduledJob<String> scheduledJob;
	@Mock
	private BackgroundJob<String> job;
	@Mock
	private BackgroundThreadFactory threadFactory;
	@Mock
	private FutureFactory futureFactory;
	protected CallableWrapper callableWrapper;
	protected Thread thread;

	@Mock
	private BackgroundJobLogFactory jobLogFactory;
	@Mock
	private BackgroundJobLog jobLog;

	private class CallableWrapper implements Callable<String> {

		final Callable<String> delegatee;

		public CallableWrapper(Callable<String> delegatee) {
			this.delegatee = delegatee;
		}

		@Override
		public String call() throws Exception {
			try {
				System.err.println("Callable.call()");
				seq.checkpoint(Steps.BEFORE_CALL);
				return delegatee.call();
			} finally {
				seq.checkpoint(Steps.AFTER_CALL);
			}
		}

	}

	@SuppressWarnings("unchecked")
	@Before
	public void setup() {

		when(jobLogFactory.createJobLog(anyString())).thenReturn(jobLog);

		when(futureFactory.createFutureTask((Callable<String>) any()))
				.thenAnswer(new Answer<FutureTask<String>>() {

					@Override
					public FutureTask<String> answer(InvocationOnMock invocation)
							throws Throwable {
						Callable<String> callable = (Callable<String>) invocation
								.getArguments()[0];
						callableWrapper = new CallableWrapper(callable);
						return new FutureTask<String>(callableWrapper);
					}

				});

		when(threadFactory.createThread((FutureTask<String>) any()))
				.thenAnswer(new Answer<BackgroundThread>() {

					@Override
					public BackgroundThread answer(InvocationOnMock invocation)
							throws Throwable {
						FutureTask<String> future = (FutureTask<String>) invocation
								.getArguments()[0];
						thread = new Thread(future, "testwork");
						return new BackgroundThread() {

							@Override
							public void start() {
								System.err.println("Thread.start()");
								thread.start();
							}

							@Override
							public void join(long timeout)
									throws InterruptedException {
								System.err.println("Thread.join() ENTER");
								thread.join(timeout);
								System.err.println("Thread.join() EXIT");
							}
						};
					}
				});
		scheduledJob = new ScheduledJob<String>(job, threadFactory,
				futureFactory, jobLogFactory);
	}

	@Test
	public void test() throws Exception {
		when(job.work(any(BackgroundJobMonitor.class))).thenReturn("result");
		scheduledJob.execute();
		assertEquals("result", scheduledJob.result());

	}

	@Test
	public void testSuccessSequence() throws Throwable {

		when(job.work(any(BackgroundJobMonitor.class))).thenAnswer(
				new Answer<String>() {

					@Override
					public String answer(InvocationOnMock invocation)
							throws Throwable {
						seq.checkpoint(Steps.WORKING);
						return "result";
					}

				});
		assertEquals(State.SCHEDULED, scheduledJob.getState());
		assertStateAtStep(State.STARTING, Steps.BEFORE_CALL);
		assertStateAtStep(State.RUNNING, Steps.WORKING);
		assertStateAtStep(State.SUCCEEDED, Steps.AFTER_CALL);
		scheduledJob.execute();
		assertEquals("result", scheduledJob.result());
		seq.throwAnyError();

	}

	@Test
	public void testFailureSequence() throws Throwable {

		when(job.work(any(BackgroundJobMonitor.class))).thenAnswer(
				new Answer<String>() {

					@Override
					public String answer(InvocationOnMock invocation)
							throws Throwable {
						seq.checkpoint(Steps.WORKING);
						throw new RuntimeException();
					}

				});
		assertEquals(State.SCHEDULED, scheduledJob.getState());
		assertStateAtStep(State.STARTING, Steps.BEFORE_CALL);
		assertStateAtStep(State.RUNNING, Steps.WORKING);
		assertStateAtStep(State.FAILED, Steps.AFTER_CALL);
		scheduledJob.execute();
		try {
			scheduledJob.result();
			fail("expected an exception.");
		} catch (ExecutionException e) {
			;// expected.
		}
		assertEquals(State.FAILED, scheduledJob.getState());
		seq.throwAnyError();

	}

	@Test
	public void testAbortSequence() throws Throwable {

		when(job.work(any(BackgroundJobMonitor.class))).thenAnswer(
				new Answer<String>() {

					@Override
					public String answer(InvocationOnMock invocation)
							throws Throwable {
						try {
							seq.checkpoint(Steps.WORKING);
						} catch (InterruptedException e) {
							return "interrupted";
						}
						return "result";
					}

				});
		assertEquals(State.SCHEDULED, scheduledJob.getState());
		assertStateAtStep(State.STARTING, Steps.BEFORE_CALL);
		assertStateAtStep(State.ABORTED, Steps.AFTER_CALL);
		seq.addAction(Steps.WORKING, new Runnable() {

			@Override
			public void run() {
				assertEquals(State.RUNNING, scheduledJob.getState());
				scheduledJob.abort();
				State state = scheduledJob.getState();
				if (!state.equals(State.ABORTED)
						&& !state.equals(State.ABORTING)) {
					fail();
				}
			}
		});

		scheduledJob.execute();
		try {
			scheduledJob.result();
			fail("expected an exception.");
		} catch (CancellationException e) {
			;// expected.
		}
		scheduledJob.join(0);
		assertEquals(State.ABORTED, scheduledJob.getState());
		seq.throwAnyError();

	}

	private void assertStateAtStep(final State scheduled, final Steps start) {
		seq.addAction(start, new Runnable() {

			@Override
			public void run() {

				assertEquals(scheduled, scheduledJob.getState());
			}
		});
	}

	@Test
	public void testDeadlockWhenAbortingDuringStarting() throws Throwable {
		when(job.work(any(BackgroundJobMonitor.class))).thenAnswer(
				new Answer<String>() {

					@Override
					public String answer(InvocationOnMock invocation)
							throws Throwable {
						try {
							seq.checkpoint(Steps.WORKING);
						} catch (InterruptedException e) {
							return "interrupted";
						}
						return "result";
					}

				});
		scheduledJob.addJobListener(new JobListener() {

			@Override
			public void stateChanged(JobEvent e) {
				System.err.println(e.getOldState()+" --> "+e.getNewState());
				if(State.STARTING.equals(e.getNewState())){
					try {
						seq.checkpoint(Steps.START);
					} catch (InterruptedException e1) {
						throw new RuntimeException();
					}
				}

			}

			@Override
			public void progressInfoUpdated(JobEvent e) {

			}
		});
		seq.addAction(Steps.START, new Runnable() {

			@Override
			public void run() {
				assertEquals(State.STARTING, scheduledJob.getState());
				scheduledJob.abort();
				System.err.println("aborting");
				assertEquals(State.ABORTING_STARTING, scheduledJob.getState());
			}
		});
		scheduledJob.execute();
		scheduledJob.join(0);
		assertEquals(State.ABORTED,scheduledJob.getState());
		seq.throwAnyError();
	}
}
