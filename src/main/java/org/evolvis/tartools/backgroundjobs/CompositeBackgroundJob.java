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

import java.util.ArrayList;
import java.util.List;

public class CompositeBackgroundJob implements BackgroundJob<List<?>> {

    public class SubJobMonitor implements BackgroundJobMonitor {

        private final BackgroundJobMonitor monitor;
        private final int anteilVomGanzen;
        private int subTotal;

        private int itemCount = 0;

        public SubJobMonitor(final BackgroundJobMonitor monitor, final int anteilVomGanzen) {
            this.monitor = monitor;
            this.anteilVomGanzen = anteilVomGanzen;
        }

        @Override
        public String getScheduledJobId() {
            return monitor.getScheduledJobId();
        }

        @Override
        public void log(final BackgroundJobMonitor.Severity severity, final Object message) {
            monitor.log(severity, message);
        }

        @Override
        public void announceTotal(final int totalItems) {
            subTotal = totalItems;
        }

        @Override
        public void reportProgressIncrement(final int items) {
            itemCount += items;
            reportAbsolute();
        }

        @Override
        public void reportProgressAbsolute(final int items) {
            itemCount = items;
            reportAbsolute();
        }

        private void reportAbsolute() {
            if (subTotal == 0) {
                monitor.reportProgressAbsolute(progressOffset);
            } else {
                monitor.reportProgressAbsolute(progressOffset + itemCount
                        * anteilVomGanzen / subTotal);
            }
        }

        @Override
        public boolean isAborting() {
            return monitor.isAborting();
        }

    }

    private final BackgroundJob<?>[] steps;
    private BackgroundJob<?> currentStep = null;
    private final String description;
    private int progressOffset = 0;

    public CompositeBackgroundJob(final String description, final BackgroundJob<?>... steps) {
        this.description = description;
        this.steps = steps;
    }

    @Override
    public List<?> work(final BackgroundJobMonitor monitor) throws Exception {
        final ArrayList<Object> arrayList = new ArrayList<Object>();
        int total = 0;
        for (final BackgroundJob<?> step : steps) {
            total += getWeight(step);
        }

        monitor.announceTotal(total * 100);
        for (final BackgroundJob<?> step : steps) {
            if (monitor.isAborting()) {
                break;
            }
            currentStep = step;
            final int anteilVomGanzen = 100 * getWeight(step);
            arrayList.add(step
                    .work(new SubJobMonitor(monitor, anteilVomGanzen)));
            progressOffset += anteilVomGanzen;
        }

        return arrayList;
    }

    private int getWeight(final BackgroundJob<?> step) {
        return step instanceof WeightedBackgroundJob ? ((WeightedBackgroundJob<?>) step)
                .getWeight() : 1;
    }

    @Override
    public String getDescription() {
        return currentStep == null ? description : description + " / "
                + currentStep.getDescription();
    }

}
