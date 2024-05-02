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
 * Information about a scheduled {@link BackgroundJob}. <br>
 * <br>
 *
 * This interface provides runtime information on a scheduled Job.
 *
 * @author lukas
 *
 */
public interface BackgroundJobInfo {
    enum State {
        /**
         * The job is scheduled but has not started yet. If aborted now, it will
         * go directly into state ABORTED, otherwise it will eventually enter
         * STARTING.
         */
        SCHEDULED,
        /**
         * The job is about to enter RUNNING. This is a somewhat delicate phase
         * since there is no way to know whether the jvm thread executing the
         * job has already been created or not. Once the thread is up, it will
         * trigger the transition to RUNNING, right before executing the actual
         * job code.
         *
         * If the job is aborted before that, it will transit to
         * STARTING_ABORTING.
         */
        STARTING,

        /**
         * The job is currently executing. Any uncauhgt exception will put it
         * into state FAILED. If it completes normally, it will move to
         * SUCCEEDED. Aborting it will put it into state ABORTING.
         */
        RUNNING,

        /**
         * The job enteres this stage when it is aborted during STARTING. No
         * attempt will be made to stop the creation and execution of the
         * job. Once the thread is up, it will trigger the transition to
         * ABORTING, right before executing the actual job code.
         */
        ABORTING_STARTING,

        /**
         * The cancellation of the job has been requested, but the
         * implementation has not yet acted upon this request. The state will
         * eventually change to either ABORTED or FAILED once the executing thread exits.
         */
        ABORTING,

        /**
         * The job was aborted before it could complete its work.
         */
        ABORTED,

        /**
         * The work is complete.
         */
        SUCCEEDED,

        /**
         * The execution of the job raised an uncaught exception.
         */
        FAILED
    }

    /**
     * @return the current {@link State} of the job.
     */
    abstract State getState();

    /**
     * Get the amount of work that is already done. Returns the number of
     * "units of work" that have been completed by the job so far. Use {@link #getWorkTotal()} to put this into relation.
     *
     * @return number of completed "units of work"
     */
    abstract int getWorkDone();

    /**
     * Get the total amount of work. <br>
     * <br>
     * Returns the total number of "units of work" comprising this job,
     * including the part that is already done. {@link BackgroundJob} -implementations will typically provide a value in an early stage of
     * processing. They are however not obliged to do so, so the expected total
     * amount may not be known yet. In particular, {@link BackgroundJob} are
     * allowed to to update this value if new information about the total amount
     * of work to be done becomes available.
     *
     * @return the expected total amount of work in "units of work".
     */
    abstract int getWorkTotal();

    /**
     * The unique identifier assigned to this job by the scheduler.
     *
     * @return the id.
     */
    abstract String getId();

    /**
     * The human-readable description for this job.
     *
     * @return the description.
     */
    abstract String getDescription();

    /**
     * The class implementing the actual work. This will typically be an
     * Implementation of {@link BackgroundJob}, but Schedulers may choose a
     * different approach.
     *
     * @return the implementation of the job.
     */
    abstract Class<?> getJobClass();

    /**
     *
     * @return the last time at which progress info or state where updated.
     *         (Milliseconds since epoch.)
     */
    abstract long getLastModified();

    abstract long getStartTime();

    /**
     * @see BackgroundJob#isVisible()
     * @since 1.21
     * 
     * @return a <code>boolean</code> indicating whether this job should be visible to the end user.
     */
    abstract boolean isVisible();
}
