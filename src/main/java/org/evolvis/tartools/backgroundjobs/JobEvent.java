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

import java.util.EventObject;

import org.evolvis.tartools.backgroundjobs.BackgroundJobInfo.State;

public class JobEvent extends EventObject {
	private static final long serialVersionUID = 5647512299253073261L;
	private final int workDone;
	private final int workTotal;
	private final State oldState;
	private final State newState;

	@Override
	public BackgroundJobInfo getSource() {
		return (BackgroundJobInfo) super.getSource();
	}

	public JobEvent(BackgroundJobInfo source) {
		super(source);
		workDone = source.getWorkDone();
		workTotal = source.getWorkTotal();
		oldState = newState = source.getState();
	}

	public JobEvent(BackgroundJobInfo source, State oldState, State newState) {
		super(source);
		this.oldState = oldState;
		this.newState = newState;
		workDone = source.getWorkDone();
		workTotal = source.getWorkTotal();
	}

	public int getWorkDone() {
		return workDone;
	}

	public int getWorkTotal() {
		return workTotal;
	}

	public State getOldState() {
		return oldState;
	}

	public State getNewState() {
		return newState;
	}
}
