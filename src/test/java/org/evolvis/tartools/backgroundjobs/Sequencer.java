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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * Synchronisationswerkzeug zum Testen nebenläufiger Prozesse.
 *
 * @author lukas
 *
 * @param <T>
 */
public class Sequencer<T> {
	private T maxStep = null;

	private class CheckpointAction {
		public CheckpointAction(T step, Thread thread, Runnable action) {
			this.step = step;
			this.thread = thread;
			this.action = action;
		}

		final T step;
		final Thread thread;
		final Runnable action;
	}

	HashMap<Thread, T> whereIsEverybody = new HashMap<Thread, T>();

	final private ArrayList<CheckpointAction> checkpointActions = new ArrayList<Sequencer<T>.CheckpointAction>();
	final private Comparator<T> comparator;

	private Throwable error;

	public static <C extends Comparable<C>> Sequencer<C> forComparableSteps() {
		final Comparator<C> cc = new Comparator<C>() {

			@Override
			public int compare(C o1, C o2) {
				return o1.compareTo(o2);
			}
		};
		return new Sequencer<C>(cc);
	}

	public static <C> Sequencer<C> forFixedSteps(C... steps) {
		final HashMap<C, Integer> positions = new HashMap<C, Integer>();
		for (int i = 0; i < steps.length; i++) {
			positions.put(steps[i], i);
		}
		final Comparator<C> cc = new Comparator<C>() {

			@Override
			public int compare(C o1, C o2) {
				return positions.get(o1) - positions.get(o2);
			}
		};
		return new Sequencer<C>(cc);
	}

	public Sequencer(Comparator<T> comparator) {
		this.comparator = comparator;
	}

	public synchronized void resume() {
		resumeUntil(null);
	}

	public synchronized void resumeUntil(T step) {

		this.maxStep = step;
		notifyAll();
	}

	public synchronized void checkpoint(final T step)
			throws InterruptedException {
		final Thread thread = Thread.currentThread();
		threadArrived(thread, step);
		while (error == null && maxStep != null
				&& comparator.compare(this.maxStep, step) < 0) {
			wait();
		}
		if (error == null) {
			Thread actionsRunner = new Thread("checkpointActions") {
				public void run() {
					runCheckpointActions(step, thread);
				};
			};
			actionsRunner.run();
		}
		threadPassed(thread, step);
	}

	public void runCheckpointActions(T step, final Thread thread) {
		List<Runnable> actions = getCheckpointActions(thread, step);
		try {
			for (Runnable runnable : actions) {

				runnable.run();

			}
		} catch (Throwable t) {
			synchronized (this) {
				if (error == null) {
					error = t;
					notifyAll();
				}
			}
		}
	}

	public void throwAnyError() throws Throwable {
		if (error != null) {
			throw error;
		}
	}

	private List<Runnable> getCheckpointActions(Thread thread, T step) {
		ArrayList<Runnable> runnables = new ArrayList<Runnable>();
		for (CheckpointAction cpa : checkpointActions) {
			if ((cpa.step == null || step.equals(cpa.step)
					&& (cpa.thread == null || thread.equals(cpa.thread)))) {
				runnables.add(cpa.action);
			}
		}
		return runnables;
	}

	private synchronized void threadPassed(Thread thread, T step) {
		whereIsEverybody.remove(thread);
		notifyAll();
	}

	private synchronized void threadArrived(Thread thread, T step) {
		whereIsEverybody.put(thread, step);
		notifyAll();
	}

	public synchronized void join(Thread thread, T step)
			throws InterruptedException {
		while (error == null && whereIsEverybody.get(thread) != step) {
			wait();
		}
	}

	public synchronized void addAction(T step, Runnable action) {
		addAction(null, step, action);
	}

	public synchronized void addAction(Thread thread, T step, Runnable action) {
		checkpointActions.add(new CheckpointAction(step, thread, action));
	}
}
