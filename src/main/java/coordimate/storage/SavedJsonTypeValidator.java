package coordimate.storage;

import java.io.IOException;
import java.util.function.Predicate;

import com.fasterxml.jackson.databind.JsonNode;

import coordimate.commons.exceptions.JsonTypeMismatchException;

/** Checks saved JSON types before Jackson converts values into Java fields. */
final class SavedJsonTypeValidator {
    private SavedJsonTypeValidator() {}

    static void validateCoordiMate(JsonNode root) throws IOException {
        requireType(root, "root", JsonNode::isObject, "an object");
        // The member ID fields already validate their JSON nodes with more specific error messages.
        checkRecords(root, "persons", "persons");
        checkRecords(root, "events", "events");
        checkStrings(root, "tags", "tags");
    }

    static void validatePreferences(JsonNode root) throws IOException {
        requireType(root, "root", JsonNode::isObject, "an object");
        checkField(root, "hasSeenTutorial", "hasSeenTutorial", JsonNode::isBoolean, "a boolean", false);
        JsonNode settings = root.get("guiSettings");
        if (settings == null) {
            return;
        }
        requireType(settings, "guiSettings", JsonNode::isObject, "an object");
        checkField(settings, "windowWidth", "guiSettings.windowWidth", JsonNode::isNumber, "a number", false);
        checkField(settings, "windowHeight", "guiSettings.windowHeight", JsonNode::isNumber, "a number", false);
        checkField(settings, "windowMaximized", "guiSettings.windowMaximized", JsonNode::isBoolean,
                "a boolean", false);
        JsonNode coordinates = settings.get("windowCoordinates");
        if (coordinates != null && !coordinates.isNull()) {
            requireType(coordinates, "guiSettings.windowCoordinates", JsonNode::isObject, "an object");
            checkField(coordinates, "x", "guiSettings.windowCoordinates.x", SavedJsonTypeValidator::isInt,
                    "an integer", false);
            checkField(coordinates, "y", "guiSettings.windowCoordinates.y", SavedJsonTypeValidator::isInt,
                    "an integer", false);
        }
    }

    private static void checkRecords(JsonNode root, String field, String path) throws IOException {
        JsonNode records = root.get(field);
        if (records == null || records.isNull()) {
            return; // Existing model conversion handles missing or null lists.
        }
        requireType(records, path, JsonNode::isArray, "an array");
        for (int index = 0; index < records.size(); index++) {
            JsonNode record = records.get(index);
            if (record.isNull()) {
                continue; // Existing model conversion reports null entries.
            }
            String recordPath = path + "[" + index + "]";
            requireType(record, recordPath, JsonNode::isObject, "an object");
            if (field.equals("persons")) {
                checkPerson(record, recordPath);
            } else {
                checkEvent(record, recordPath);
            }
        }
    }

    private static void checkPerson(JsonNode person, String path) throws IOException {
        for (String field : new String[] {"name", "phone", "email", "address", "role", "birthday",
            "organisation", "note", "leaveUntil"}) {
            checkField(person, field, path + "." + field, JsonNode::isTextual, "a string", true);
        }
        // The archive field has its own check, which identifies the contact by name.
        checkStrings(person, "tags", path + ".tags");
    }

    private static void checkEvent(JsonNode event, String path) throws IOException {
        for (String field : new String[] {"name", "startTime", "endTime"}) {
            checkField(event, field, path + "." + field, JsonNode::isTextual, "a string", true);
        }
        // The archive field has its own check, which identifies the event by name.
        checkStrings(event, "members", path + ".members");
        checkStrings(event, "dismissedReminderStarts", path + ".dismissedReminderStarts");
        JsonNode attendance = event.get("attendance");
        if (attendance != null && !attendance.isNull()) {
            requireType(attendance, path + ".attendance", JsonNode::isObject, "an object");
            var entries = attendance.fields();
            while (entries.hasNext()) {
                var entry = entries.next();
                requireType(entry.getValue(), path + ".attendance." + entry.getKey(),
                        node -> node.isNull() || node.isTextual(), "a string");
            }
        }
    }

    private static void checkStrings(JsonNode parent, String field, String path) throws IOException {
        JsonNode values = parent.get(field);
        if (values == null || values.isNull()) {
            return;
        }
        requireType(values, path, JsonNode::isArray, "an array");
        for (int index = 0; index < values.size(); index++) {
            requireType(values.get(index), path + "[" + index + "]",
                    node -> node.isNull() || node.isTextual(), "a string");
        }
    }

    private static void checkField(JsonNode parent, String field, String path, Predicate<JsonNode> type,
            String expected, boolean allowNull) throws IOException {
        JsonNode value = parent.get(field);
        if (value != null && (!value.isNull() || !allowNull)) {
            requireType(value, path, type, expected);
        }
    }

    private static void requireType(JsonNode value, String path, Predicate<JsonNode> type, String expected)
            throws IOException {
        if (!type.test(value)) {
            throw new JsonTypeMismatchException(path, expected);
        }
    }

    private static boolean isInt(JsonNode value) {
        return value.isIntegralNumber() && value.canConvertToInt();
    }
}
