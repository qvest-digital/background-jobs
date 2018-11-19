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

import java.util.concurrent.ExecutionException;

/**
 * A handle on a scheduled {@link BackgroundJob}.
 * <br>
 * <br>
 * This interface extends the {@link BackgroundJobInfo} contract by adding
 * methods to {@link #abort()} or {@link #join(long)} a scheduled job, and to
 * obtain its {@link #result()} once it is done.
 *
 * @author lukas
 *
 * @param <T>
 */
public interface BackgroundJobStatus<T> extends BackgroundJobInfo {
    /**
     * Request the cancellation of a job.
     * <br>
     * <br>
     * This will try to interrupt the thread executing the job and it will make {@link BackgroundJobMonitor#isAborting()} return false.
     * Nevertheless it may take some time until the job actually stops whatever it is doing.
     */
    public void abort();

    /**
     * Join the thread executing the job.
     * 
     * @param timeout
     *            the maximum number of milliseconds to wait for the job.
     * @throws InterruptedException
     */
    public void join(long timeout) throws InterruptedException;

    /**
     * Obtain the result produced by the job, waiting for its completion if necessary.
     * 
     * @return the result.
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public T result() throws InterruptedException, ExecutionException;

    /**
     * Whether this job can be canceled.<br>
     * <br>
     *
     * @see BackgroundJob#isCancellationSupported()
     * 
     * @return a <code>boolean</code> indicating whether this job can be canceled once its execution has started.
     * @since 1.21
     */
    public boolean isCancellationSupported();

    void addJobListener(JobListener l);

    void removeJobListener(JobListener l);
}
