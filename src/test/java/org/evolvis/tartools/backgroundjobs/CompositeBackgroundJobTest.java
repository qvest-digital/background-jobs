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

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;

public class CompositeBackgroundJobTest {

    private final class ZeroItemsJob implements BackgroundJob<String> {
        @Override
        public String work(final BackgroundJobMonitor monitor) throws Exception {
            monitor.announceTotal(0);

            monitor.reportProgressAbsolute(0);
            return "done";
        }

        @Override
        public String getDescription() {
            return "description";
        }
    }

    ArrayList<String> log;
    private BackgroundJobMonitor monitor;

    @Before
    public void setUp() {
        log = new ArrayList<String>();
        monitor = new BackgroundJobMonitor() {

            @Override
            public void log(final Severity severity, final Object message) {
                log.add("log " + severity + ": " + message);
            }

            @Override
            public boolean isAborting() {
                return false;
            }

            @Override
            public String getScheduledJobId() {
                return null;
            }

            @Override
            public void announceTotal(final int totalItems) {
                log.add("announceTotal: " + totalItems);
            }

            @Override
            public void reportProgressIncrement(final int items) {
                log.add("reportProgressIncrement: " + items);

            }

            @Override
            public void reportProgressAbsolute(final int items) {
                log.add("reportProgressAbsolute: " + items);

            }

        };

    }

    @Test
    /**
     * This is ment to catch a regression that hit us when rolling out 15.2 on production.
     * [#2060] was configured such that all PEO/PEMU/PEUs were blacklisted to recreate the situation
     * in the previous release. This triggers problems via the extractor which reports the
     * number of rows in the result set as total work items. After that it initializes the progress
     * with a call to BackgroundJobMonior.reportProgressAbsolute(0), which triggers a divide by zero
     * error in the sub-job monitor.
     */
    public void subjobWithZeroWorkItems() throws Exception {
        final CompositeBackgroundJob job = new CompositeBackgroundJob("composite", new ZeroItemsJob());
        job.work(monitor);
        assertEquals(2, log.size());
        assertEquals("announceTotal: 100", log.get(0));
        assertEquals("reportProgressAbsolute: 0", log.get(1));
    }

}
