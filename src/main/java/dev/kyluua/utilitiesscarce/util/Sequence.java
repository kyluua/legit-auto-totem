package dev.kyluua.utilitiesscarce.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * An ordered list of steps spread over client ticks.
 *
 * <p>Steps that produce a packet are marked as costing budget; the scheduler
 * refuses to run more of those per tick than the configured limit, which is what
 * keeps a burst like "charge anchor, then swap" from going out all at once.
 * Steps with no delay and no cost run in the same tick as the previous one, so a
 * swap-and-hit still lands immediately.
 */
public final class Sequence {
	private record Step(int delay, BooleanSupplier gate, int gateTimeout, Runnable action, boolean costsBudget) {
	}

	private final List<Step> steps = new ArrayList<>();
	private BooleanSupplier precondition;
	private Runnable abortAction;

	private int index;
	private int pendingDelay;
	private int gateElapsed;
	private boolean started;
	private boolean finished;

	/**
	 * A condition re-checked before every step. When it stops holding the
	 * sequence aborts, which runs {@link #onAbort(Runnable)}.
	 */
	public Sequence require(BooleanSupplier condition) {
		this.precondition = condition;
		return this;
	}

	/** Runs when the sequence is cancelled or its precondition fails. */
	public Sequence onAbort(Runnable action) {
		this.abortAction = action;
		return this;
	}

	/** An action that sends something to the server, after an optional delay. */
	public Sequence run(int delayTicks, Runnable action) {
		steps.add(new Step(Math.max(0, delayTicks), null, 0, action, true));
		return this;
	}

	/** A local-only action such as selecting a hotbar slot; costs no budget. */
	public Sequence runLocal(int delayTicks, Runnable action) {
		steps.add(new Step(Math.max(0, delayTicks), null, 0, action, false));
		return this;
	}

	/** Idles until {@code gate} holds, giving up after {@code timeoutTicks}. */
	public Sequence waitUntil(BooleanSupplier gate, int timeoutTicks) {
		steps.add(new Step(0, gate, Math.max(0, timeoutTicks), null, false));
		return this;
	}

	/** Idles for a fixed number of ticks. */
	public Sequence waitTicks(int ticks) {
		steps.add(new Step(Math.max(0, ticks), null, 0, null, false));
		return this;
	}

	public boolean isFinished() {
		return finished;
	}

	/** Ends the sequence early and runs its abort action. */
	public void abort() {
		if (finished) {
			return;
		}

		finished = true;

		if (abortAction != null) {
			abortAction.run();
		}
	}

	/**
	 * Advances the sequence as far as this tick's budget allows.
	 *
	 * @param budget remaining packet-producing steps for this tick
	 * @return the budget left over
	 */
	int tick(int budget) {
		if (finished) {
			return budget;
		}

		if (!started) {
			started = true;
			pendingDelay = steps.isEmpty() ? 0 : steps.get(0).delay();
		}

		while (true) {
			if (index >= steps.size()) {
				finished = true;
				return budget;
			}

			if (precondition != null && !precondition.getAsBoolean()) {
				abort();
				return budget;
			}

			if (pendingDelay > 0) {
				pendingDelay--;
				return budget;
			}

			Step step = steps.get(index);

			if (step.gate() != null && !step.gate().getAsBoolean()) {
				gateElapsed++;

				if (gateElapsed > step.gateTimeout()) {
					abort();
				}

				return budget;
			}

			if (step.costsBudget()) {
				if (budget <= 0) {
					return 0;
				}

				budget--;
			}

			gateElapsed = 0;

			if (step.action() != null) {
				step.action().run();
			}

			index++;

			if (index < steps.size()) {
				pendingDelay = steps.get(index).delay();
			}
		}
	}
}
