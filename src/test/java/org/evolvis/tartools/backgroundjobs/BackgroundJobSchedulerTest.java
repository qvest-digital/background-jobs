package org.evolvis.tartools.backgroundjobs;

/*-
 * Background-Jobs is Copyright
 *  © 2012 Атанас Александров (a.alexandrov@tarent.de)
 *  © 2012, 2013, 2014, 2015 Lukas Degener (l.degener@qvest-digital.com)
 *  © 2015, 2016 Jens Oberender (j.oberender@tarent.de)
 * Licensor: Qvest Digital AG, Bonn, Germany
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

import org.evolvis.tartools.backgroundjobs.BackgroundJobInfo.State;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BackgroundJobSchedulerTest {
    enum Steps {
        START,
        BEFORE_CALL,
        WORKING,
        AFTER_CALL
    }

    private interface BackgroundJobForObject extends BackgroundJob<Object> {
    }

    @SuppressWarnings("unused")
    Sequencer<Steps> seq = Sequencer.forFixedSteps(Steps.values());
    private BackgroundJobScheduler scheduler;

    @Before
    public void setup() {
        scheduler = new BackgroundJobScheduler();
    }

    @Test
    public void testALotOfTimes() throws InterruptedException,
      ExecutionException {
        for (int i = 0; i < 1000; i++) {
            testEarlyAbort();
        }
    }

    private static class DummyJob implements BackgroundJob<Object> {
        boolean shouldRun = true;

        @SuppressWarnings("unused")
        public void stop() {
            shouldRun = false;
        }

        @Override
        public Object work(BackgroundJobMonitor monitor) throws Exception {
            while (shouldRun) {
                synchronized (this) {
                    wait(50);
                }
            }
            return "fertig!";
        }

        @Override
        public String getDescription() {
            return "Ich bin ein Dummy-Job";
        }
    }

    @Test
    public void testScheduleImmediately() {
        scheduler.ensureStarted();

        DummyJob dummyJob = new DummyJob();
        scheduler.schedule(dummyJob);

        BackgroundJob<Object> job = mock(BackgroundJobForObject.class);
        BackgroundJobStatus<Object> result = scheduler.scheduleImmediately(job);
        assertNull(result);
    }

    @Test
    public void testScheduleImmediately1() throws InterruptedException {
        scheduler.ensureStarted();

        BackgroundJob<Object> job = mock(BackgroundJobForObject.class);
        BackgroundJobStatus<Object> result = scheduler.scheduleImmediately(job);
        assertNotNull(result);
        result.join(1000);
        assertEquals(State.SUCCEEDED, result.getState());
    }

    @Test
    public void testEarlyAbort() throws InterruptedException,
      ExecutionException {
        BackgroundJob<String> jobToBeAborted = new BackgroundJob<String>() {
            @Override
            public String work(BackgroundJobMonitor monitor) {
                throw new RuntimeException("this should never execute");
            }

            @Override
            public String getDescription() {
                return "Abort me!";
            }
        };

        BackgroundJob<String> jobToBeExecuted = new BackgroundJob<String>() {
            @Override
            public String getDescription() {
                return "Go ahead... make my day.";
            }

            @Override
            public String work(BackgroundJobMonitor monitor) {
                return "cool.";
            }
        };
        BackgroundJobStatus<String> abortedJob = scheduler.schedule(jobToBeAborted);
        BackgroundJobStatus<String> executedJob = scheduler.schedule(jobToBeExecuted);
        abortedJob.abort();
        assertEquals(executedJob, scheduler.executeNext());
        assertEquals(State.ABORTED, abortedJob.getState());
        assertEquals("cool.", executedJob.result());
        if (executedJob.getState() != State.SUCCEEDED) {
            System.out.println("break");
        }
        assertEquals(State.SUCCEEDED, executedJob.getState());
    }
}
