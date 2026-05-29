package network;

public final class LanGameMessage {
    private final int senderId;
    private final String senderName;
    private final String type;
    private final String payload;

    public LanGameMessage(int senderId, String senderName, String type, String payload) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.type = type;
        this.payload = payload;
    }

    public int getSenderId() {
        return senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getType() {
        return type;
    }

    public String getPayload() {
        return payload;
    }
}
