package dev.kyluua.utilitiesscarce.util;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Runs one {@link Sequence} per owner, sharing a per-tick budget of
 * packet-producing steps between them.
 */
public final class ActionScheduler {
	private final Map<String, Sequence> sequences = new LinkedHashMap<>();

	/**
	 * Starts a sequence for {@code owner}, aborting whatever that owner had
	 * running so a half-finished swap still gets its slot restored.
	 */
	public void submit(String owner, Sequence sequence) {
		cancel(owner);
		sequences.put(owner, sequence);
	}

	public void cancel(String owner) {
		Sequence existing = sequences.remove(owner);

		if (existing != null) {
			existing.abort();
		}
	}

	public boolean isRunning(String owner) {
		return sequences.containsKey(owner);
	}

	/** Aborts everything, e.g. when leaving a world. */
	public void clear() {
		for (Sequence sequence : sequences.values()) {
			sequence.abort();
		}

		sequences.clear();
	}

	public void tick(int budgetPerTick) {
		if (sequences.isEmpty()) {
			return;
		}

		int budget = Math.max(1, budgetPerTick);
		Iterator<Map.Entry<String, Sequence>> iterator = sequences.entrySet().iterator();

		while (iterator.hasNext()) {
			Sequence sequence = iterator.next().getValue();
			budget = sequence.tick(budget);

			if (sequence.isFinished()) {
				iterator.remove();
			}
		}
	}
}
