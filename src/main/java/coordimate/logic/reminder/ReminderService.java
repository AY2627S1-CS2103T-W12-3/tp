package coordimate.logic.reminder;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import coordimate.commons.exceptions.DataLoadingException;
import coordimate.model.CoordiMate;
import coordimate.model.Model;
import coordimate.model.event.Event;
import coordimate.model.event.EventTime;
import coordimate.model.reminder.UpcomingEventFinder;
import coordimate.storage.Storage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Maintains current reminders and saves dismissals without changing event data on a failed write.
 */
public class ReminderService {
    private final Model model;
    private final Storage storage;
    private final UpcomingEventFinder finder;
    private final ObservableList<Event> reminders = FXCollections.observableArrayList();
    private final ObservableList<Event> unmodifiableReminders = FXCollections.unmodifiableObservableList(reminders);
    private final Map<String, Set<EventTime>> sessionDismissals = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    /**
     * Uses the validated model as the source of events. Refreshes do not read or write storage.
     */
    public ReminderService(Model model, Storage storage, Clock clock) {
        this.model = requireNonNull(model);
        this.storage = storage;
        finder = new UpcomingEventFinder(clock);
        refreshReminders();
    }

    /**
     * Returns the live, unmodifiable reminder list, refreshed against the current time and model.
     */
    public ObservableList<Event> getReminders() {
        refreshReminders();
        return unmodifiableReminders;
    }

    /**
     * Replaces reminders with the eligible, undismissed events in chronological order.
     * Forgets temporary dismissals when their event is deleted.
     */
    public void refreshReminders() {
        List<Event> events = model.getCoordiMate().getEventList();
        sessionDismissals.keySet().removeIf(name -> events.stream()
                .noneMatch(event -> event.getName().equalsIgnoreCase(name)));
        List<Event> updated = finder.findUpcomingEvents(events).stream()
                .filter(event -> !event.isReminderDismissed())
                .filter(event -> !sessionDismissals.getOrDefault(event.getName(), Set.of())
                        .contains(event.getStartTime()))
                .toList();
        if (!reminders.equals(updated)) {
            reminders.setAll(updated);
        }
    }

    /**
     * Dismisses a currently active reminder and saves its history before updating the model.
     * Repeated dismissals and outdated reminder snapshots have no effect.
     *
     * @throws ReminderDismissalException If saving fails; the reminder stays hidden for this session.
     */
    public void dismissReminder(Event reminder) throws ReminderDismissalException {
        requireNonNull(reminder);
        refreshReminders();
        if (!reminders.contains(reminder)) {
            return;
        }
        Event dismissed = reminder.withReminderDismissed();
        CoordiMate candidate = new CoordiMate(model.getCoordiMate());
        candidate.setEvent(reminder, dismissed);
        try {
            storage.readCoordiMate();
            storage.saveCoordiMate(candidate);
        } catch (DataLoadingException | IOException e) {
            sessionDismissals.computeIfAbsent(reminder.getName(), name -> new HashSet<>())
                    .add(reminder.getStartTime());
            refreshReminders();
            throw new ReminderDismissalException(e);
        }
        model.setEvent(reminder, dismissed);
        refreshReminders();
    }

    /**
     * Moves temporary dismissals after a rename is saved and before the model is updated.
     */
    public void renameEvent(String oldName, String newName) {
        Set<EventTime> history = sessionDismissals.remove(oldName);
        if (history != null) {
            sessionDismissals.put(newName, history);
        }
    }
}
