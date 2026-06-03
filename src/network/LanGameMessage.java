package network;

public final class LanGameMessage {
    private final int senderId;
    private final String senderName;
    private final String type;
    private final String payload;

    // Stores one relayed table message from a network player.
    public LanGameMessage(int senderId, String senderName, String type, String payload) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.type = type;
        this.payload = payload;
    }

    // Returns the room id of the sender.
    public int getSenderId() {
        return senderId;
    }

    // Returns the sender display name.
    public String getSenderName() {
        return senderName;
    }

    // Returns the table message type.
    public String getType() {
        return type;
    }

    // Returns the encoded table payload.
    public String getPayload() {
        return payload;
    }
}
