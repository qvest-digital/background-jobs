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

public abstract class BackgroundJobStatusDecorator<T> implements
BackgroundJobStatus<T> {

    protected final BackgroundJobStatus<T> delegatee;

    public BackgroundJobStatusDecorator(final BackgroundJobStatus<T> delegatee) {
        this.delegatee = delegatee;
    }

    @Override
    public org.evolvis.tartools.backgroundjobs.BackgroundJobStatus.State getState() {
        return getDelegatee().getState();
    }

    @Override
    public int getWorkDone() {
        return getDelegatee().getWorkDone();
    }

    @Override
    public int getWorkTotal() {
        return getDelegatee().getWorkTotal();
    }

    @Override
    public void abort() {
        getDelegatee().abort();
    }

    @Override
    public String getId() {
        return getDelegatee().getId();
    }

    @Override
    public void join(final long timeout) throws InterruptedException {
        getDelegatee().join(timeout);
    }

    @Override
    public String getDescription() {
        return getDelegatee().getDescription();
    }

    @Override
    public T result() throws InterruptedException, ExecutionException {
        return getDelegatee().result();
    }

    @Override
    public Class<?> getJobClass() {
        return getDelegatee().getJobClass();
    }

    @Override
    public void addJobListener(final JobListener l) {
        getDelegatee().addJobListener(l);

    }

    @Override
    public void removeJobListener(final JobListener l) {
        getDelegatee().removeJobListener(l);

    }

    @Override
    public long getLastModified() {
        return getDelegatee().getLastModified();
    };

    @Override
    public long getStartTime() {
        return getDelegatee().getStartTime();
    }

    protected BackgroundJobStatus<T> getDelegatee() {
        return delegatee;
    }

}
