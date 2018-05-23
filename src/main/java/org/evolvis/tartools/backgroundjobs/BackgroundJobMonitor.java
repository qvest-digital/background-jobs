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

/**
 * Used by {@link BackgroundJob} to communicate with the scheduler.
 *
 * The {@link BackgroundJobScheduler} passes an instance of this type to
 * {@link BackgroundJob#work(BackgroundJobMonitor)}, effectively providing the
 * job with a feedback channel to report progress information.
 *
 * The {@link BackgroundJob} also should use this monitor for all logging and to
 * periodically poll for cancellation requests. <br>
 * <br>
 * The scheduler creates an instance of this type <b>exclusively</b> for one
 * particular job. It is not shared between jobs, and it is not reused.
 *
 * @author lukas
 *
 */
public interface BackgroundJobMonitor {
	public enum Severity {
		INFO, WARNING, ERROR
	}

	/**
	 * write to the (job-specific) log.
	 *
	 * @param severity
	 * @param message
	 */
	public abstract void log(Severity severity, Object message);

	/**
	 * Whether the cancellation of the job was requested. <br>
	 * <br>
	 * When true is returned, the scheduler has already tried set the interrupt
	 * flag on the thread executing the job. {@link BackgroundJob}s should poll
	 * this method in regular intervals and exit as quickly as possible if true
	 * is returned.
	 *
	 * @return true, if the job should abort its execution.
	 */
	public boolean isAborting();

	/**
	 * Get the identifier assigned to the {@link BackgroundJob} by the
	 * scheduler. <br>
	 * This ID uniquely identifies the job instance among all jobs managed by
	 * the scheduler. Since a single job instance is never executed more than
	 * once, it equivalently identifies that particular execution.
	 *
	 * @return the id.
	 */
	public String getScheduledJobId();

	/**
	 * Announce the total amount of work to be done. <br>
	 * <br>
	 * The {@link BackgroundJob} should call this method at the beginning of its
	 * execution, as soon as it knows what lies ahead. It is ok to call this
	 * method multiple times with different values to update an initial
	 * assessment as new/more detailed information about the work to be done
	 * becomes available.
	 *
	 * @param totalItems the total units of work to be done.
	 */
	public void announceTotal(int totalItems);

	/**
	 * Report units of work completed since the last progress report.
	 * <br><br>
	 * The {@link BackgroundJob} can use this method to report relative increments.
	 *
	 * @param items number of units of work completed since the last call.
	 */
	public void reportProgressIncrement(int items);

	/**
	 * Report units of work completed since the begining of the job.
	 * <br><br>
	 * The {@link BackgroundJob} can use this method to report the absolute amount of work
	 * that was tackled since the execution started.
	 *
	 * @param items number of units of work completed since execution started.
	 */
	public void reportProgressAbsolute(int items);
}
