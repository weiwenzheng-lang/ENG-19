package network;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

public final class LanGameProtocol {
    public static final String HELLO = "HELLO";
    public static final String WELCOME = "WELCOME";
    public static final String PLAYERS = "PLAYERS";
    public static final String ROOM_STATE = "ROOM_STATE";
    public static final String INFO = "INFO";
    public static final String CHAT = "CHAT";
    public static final String READY = "READY";
    public static final String START_GAME = "START_GAME";
    public static final String GAME_ACTION = "GAME_ACTION";
    public static final String GAME_STATE = "GAME_STATE";
    public static final String PING = "PING";
    public static final String PONG = "PONG";
    public static final String GOODBYE = "GOODBYE";
    public static final String ERROR = "ERROR";

    // Prevents construction of this protocol utility class.
    private LanGameProtocol() {
    }

    // Encodes one tab-delimited protocol frame.
    public static String line(String command, String... fields) {
        StringBuilder builder = new StringBuilder(command);
        for (String field : fields) {
            builder.append('\t').append(encode(field == null ? "" : field));
        }
        return builder.toString();
    }

    // Parses one protocol frame and decodes all payload fields.
    public static Message parse(String line) {
        if (line == null || line.trim().isEmpty()) {
            throw new IllegalArgumentException("Empty network message");
        }

        String[] parts = line.split("\t", -1);
        List<String> fields = new ArrayList<>();
        for (int i = 1; i < parts.length; i++) {
            fields.add(decode(parts[i]));
        }
        return new Message(parts[0], fields);
    }

    // Base64 keeps tabs and newlines out of payload fields.
    private static String encode(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        return Base64.getEncoder().encodeToString(bytes);
    }

    // Decodes one Base64 UTF-8 payload field.
    private static String decode(String value) {
        byte[] bytes = Base64.getDecoder().decode(value);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    // Immutable parsed protocol message.
    public static final class Message {
        private final String command;
        private final List<String> fields;

        // Stores the command and decoded payload fields.
        private Message(String command, List<String> fields) {
            this.command = command;
            this.fields = Collections.unmodifiableList(fields);
        }

        // Returns the protocol command name.
        public String getCommand() {
            return command;
        }

        // Returns decoded fields in message order.
        public List<String> getFields() {
            return fields;
        }

        // Returns one decoded field by index.
        public String field(int index) {
            return fields.get(index);
        }
    }
}
