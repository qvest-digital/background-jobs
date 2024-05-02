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

/**
 * A unit of work that may be processed asynchronously.
 *
 * An instance of this class represents a potentially long-running task that may
 * be scheduled and processed asynchronously by a {@link BackgroundJobScheduler}.
 *
 * This interface models the “inside” part of the contract that is to be
 * fulfilled by the code implementing a particular job.
 *
 * To schedule a job, use {@link BackgroundJobScheduler#schedule(BackgroundJob)}
 * or {@link BackgroundJobScheduler#schedule(String, BackgroundJob)}.
 *
 * See also: {@link BackgroundJobInfo}, {@link BackgroundJobScheduler},
 * {@link BackgroundJobStatus}
 *
 * @author lukas
 *
 * @param <V>
 */
public interface BackgroundJob<V> {
    /**
     * Execute the Job.
     *
     * This method is typically called by some sort of scheduler.
     *
     * <b>This method should never be called twice on the same instance!</b>
     *
     * While executing, implementations should use the
     * {@link BackgroundJobMonitor} to communicate with the scheduler:
     * <ul>
     * <li>Progress should be reported to the {@link BackgroundJobMonitor}
     * through the {@link BackgroundJobMonitor#announceTotal(int)},
     * {@link BackgroundJobMonitor#reportProgressAbsolute(int)} and
     * {@link BackgroundJobMonitor#reportProgressIncrement(int)} methods.</li>
     * <li>All logging should use the
     * {@link BackgroundJobMonitor#log(org.evolvis.tartools.backgroundjobs.BackgroundJobMonitor.Severity, Object)}
     * Method.</li>
     * <li>Long-running operations should periodically poll the state of the
     * {@link BackgroundJobMonitor#isAborting()} property, to check whether a
     * cancellation was requested. If this is the case, this method should
     * return as quickly as possible.</li>
     * <li>Implementations can obtain a unique identifier for the current
     * execution of this job using the
     * {@link BackgroundJobMonitor#getScheduledJobId()} Method.</li>
     * </ul>
     *
     * @param monitor
     *            The job monitor that should be used to communicate with the
     *            scheduler.
     * @return the result produced by this job. Schedulers should ignore the
     *         returned value if the job has been aborted before finishing
     *         normally.
     * @throws Exception
     *             The scheduler should be prepared to deal with any kind of
     *             exception by marking the job as failed.
     */
    public V work(BackgroundJobMonitor monitor) throws Exception;

    /**
     * Describe the job.
     *
     * @return a human-readable Description of the job.
     */
    public String getDescription();

    /**
     * Whether this job can be canceled.<br>
     * <br>
     *
     * While most jobs should allow cancellation by the user, there will be cases
     * where it is just not feasible or even dangerous to abort a job once it is running (i.e.
     * once the {@link #work(BackgroundJobMonitor)} method is executing).
     * Those jobs usually simply ignore the value of {@link BackgroundJobMonitor#isAborting()} and
     * just keep on running until they are done.
     * <br>
     * If a job just cannot be cancelled, the UI should communicate this fact to the user (e.g. by not
     * showing a Cancel-Button in the first place). Implementations should override this method and return
     * <code>false</code>.
     * <br>
     * <b>NOTE:</b> While in state {@link BackgroundJobInfo.State#SCHEDULED}, a job can always be canceled,
     * regardless of what this method returns.
     * 
     * @return a <code>boolean</code> indicating whether this job can be canceled once its execution has started.
     * @since 1.21
     */
    public default boolean isCancellationSupported() {
        return true;
    }

    /**
     * Whether this job should be visible to the end user. <br>
     * <br>
     * 
     * In applications that provide some kind of manegment UI for scheduled/running jobs
     * it might make sense to hide certain jobs from the user. Examples would be jobs
     * that are started automatically, have a very short runtime and are not intended to be
     * interacted with in any way.
     * <br>
     * <br>
     * Implementation can override this method and return <code>false</code> to indicate that
     * this job should not be shown in the UI.
     * 
     * @return a <code>boolean</code> indicating whether this job should be visible to the
     *         end user.
     * @since 1.21
     */
    public default boolean isVisible() {
        return true;
    }
}
