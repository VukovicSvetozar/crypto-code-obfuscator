package utility;

import java.nio.file.*;
import java.nio.file.WatchEvent.Kind;

import static controller.DekripcijaDatoteke.mojeBrisanjePoruke;

import static java.nio.file.StandardWatchEventKinds.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.Executor;

import javafx.application.Platform;

public class FileWatcher {

	private final WatchService watcher;
	private final Map<WatchKey, Path> keys;
	private boolean trace = false;

	@SuppressWarnings("unchecked")
	static <T> WatchEvent<T> cast(WatchEvent<?> event) {
		return (WatchEvent<T>) event;
	}

	// Executor for notifications: by default just run on the current thread
	@SuppressWarnings("unused")
	private Executor notificationExecutor = Runnable::run;

	public void setNotificationExecutor(Executor notificationExecutor) {
		this.notificationExecutor = notificationExecutor;
	}

	/**
	 * Register the given directory with the WatchService
	 */
	private void register(Path dir) throws IOException {
		WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
		if (trace) {
			Path prev = keys.get(key);
			if (prev == null) {
				System.out.format("register: %s\n", dir);
			} else {
				if (!dir.equals(prev)) {
					System.out.format("update: %s -> %s\n", prev, dir);
				}
			}
		}
		keys.put(key, dir);
	}

	/**
	 * Creates a WatchService and registers the given directory
	 */
	public FileWatcher(Path dir) throws IOException {
		this.watcher = FileSystems.getDefault().newWatchService();
		this.keys = new HashMap<WatchKey, Path>();

		register(dir);

		// enable trace after initial registration
		this.trace = true;
	}

	/**
	 * Process all events for keys queued to the watcher
	 */
	public void processEvents() {
		for (;;) {

			// wait for key to be signalled
			WatchKey key;
			try {
				key = watcher.take();
			} catch (InterruptedException x) {
				return;
			}

			Path dir = keys.get(key);
			if (dir == null) {
				System.err.println("WatchKey not recognized!!");
				continue;
			}

			for (WatchEvent<?> event : key.pollEvents()) {
				Kind<?> kind = event.kind();

				// TBD - provide example of how OVERFLOW event is handled
				if (kind == OVERFLOW) {
					continue;
				}

				// Context for directory entry event is the file name of entry
				WatchEvent<Path> ev = cast(event);
				@SuppressWarnings("unused")
				Path name = ev.context();

				if (!mojeBrisanjePoruke && kind.equals(ENTRY_DELETE)) {
					Platform.runLater(() -> {
						String upozorenje = "Poruka je izvan aplikacije obrisana sa fajl sistema!";
						Dijalog.showWarningDialog("Upozorenje", "Ovo je upozorenje!", upozorenje);
					});
				}
			}

			// Reset key and remove from set if directory no longer accessible
			boolean valid = key.reset();
			if (!valid) {
				keys.remove(key);

				// All directories are inaccessible
				if (keys.isEmpty())
					break;
			}
		}
	}

}
