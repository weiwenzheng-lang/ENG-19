package ui.javafx;

import cards.Card;
import core.GameManager;
import network.LanGameMessage;
import player.Player;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

// Replays LAN actions against the local game state.
final class NetworkActionApplier {
    private final GameManager game;
    private final Runnable endTurnAction;
    private final Runnable renderCallback;
    private final Consumer<String> eventSink;
    private final IntSupplier localPlayerIndexGetter;
    private final IntConsumer localPlayerIndexSetter;
    private final IntConsumer playerCountSetter;
    private final Runnable recreateContent;

    // Stores callbacks that keep network replay independent of the JavaFX controller.
    NetworkActionApplier(GameManager game,
                         Runnable endTurnAction,
                         Runnable renderCallback,
                         Consumer<String> eventSink,
                         IntSupplier localPlayerIndexGetter,
                         IntConsumer localPlayerIndexSetter,
                         IntConsumer playerCountSetter,
                         Runnable recreateContent) {
        this.game = game;
        this.endTurnAction = endTurnAction;
        this.renderCallback = renderCallback;
        this.eventSink = eventSink == null ? message -> { } : eventSink;
        this.localPlayerIndexGetter = localPlayerIndexGetter;
        this.localPlayerIndexSetter = localPlayerIndexSetter;
        this.playerCountSetter = playerCountSetter;
        this.recreateContent = recreateContent;
    }

    // Applies one decoded network message to the current local game state.
    void apply(LanGameMessage message) {
        String type = message.getType();
        Map<String, String> payload = NetworkActionCodec.parsePayload(message.getPayload());
        if ("END_TURN".equals(type)) {
            endTurnAction.run();
        } else if ("BANK".equals(type)) {
            applyRemoteBank(payload);
        } else if ("DISCARD".equals(type)) {
            applyRemoteDiscard(payload);
        } else if ("PLAY".equals(type)) {
            applyRemotePlay(payload);
        } else if ("DOUBLE_RENT".equals(type)) {
            applyRemoteDoubleRent(payload);
        } else if ("JUST_SAY_NO".equals(type)) {
            applyRemoteJustSayNo(payload);
        } else if ("RESOLVE_PENDING".equals(type)) {
            game.resolvePendingAction();
            renderCallback.run();
        } else if ("LEAVE_MATCH".equals(type)) {
            applyRemoteLeave(payload, message.getSenderName());
        }
    }

    // Replays a remote bank deposit.
    private void applyRemoteBank(Map<String, String> payload) {
        int index = findCardIndex(game.getCurrentPlayer(), payload, "index", "card");
        game.depositCardToBank(index);
        renderCallback.run();
    }

    // Replays a remote discard.
    private void applyRemoteDiscard(Map<String, String> payload) {
        int index = findCardIndex(game.getCurrentPlayer(), payload, "index", "card");
        game.discardCard(index);
        renderCallback.run();
    }

    // Replays a normal remote card play.
    private void applyRemotePlay(Map<String, String> payload) {
        Player current = game.getCurrentPlayer();
        int cardIndex = findCardIndex(current, payload, "index", "card");
        Card card = current.getHand().getCard(cardIndex);
        if (card == null) {
            eventSink.accept("[Network] Remote card not found: " + payload.getOrDefault("card", ""));
            return;
        }
        NetworkActionCodec.applyCardState(card, payload);
        game.executePlayerAction(cardIndex,
                NetworkActionCodec.buildTargetInfo(payload, game.getActivePlayers()));
        renderCallback.run();
    }

    // Replays a remote Double The Rent combo.
    private void applyRemoteDoubleRent(Map<String, String> payload) {
        Player current = game.getCurrentPlayer();
        int doubleIndex = findCardIndex(current, payload, "doubleIndex", "double");
        int rentIndex = findCardIndex(current, payload, "rentIndex", "rent");
        Card rent = current.getHand().getCard(rentIndex);
        NetworkActionCodec.applyCardState(rent, payload);
        game.executeDoubleRentAction(doubleIndex, rentIndex,
                NetworkActionCodec.buildTargetInfo(payload, game.getActivePlayers()));
        renderCallback.run();
    }

    // Replays a remote Just Say No counter.
    private void applyRemoteJustSayNo(Map<String, String> payload) {
        Player victim = game.getPendingVictim();
        int index = findCardIndex(victim, payload, "index", "card");
        game.counterAttackWithJustSayNo(index);
        renderCallback.run();
    }

    // Removes a remote player who intentionally left the active match.
    private void applyRemoteLeave(Map<String, String> payload, String senderName) {
        int leavingIndex = NetworkActionCodec.readInt(payload, "index", -1);
        if (leavingIndex < 0 || leavingIndex >= game.getActivePlayers().size()) {
            leavingIndex = findPlayerIndexByName(payload.getOrDefault("name", senderName));
        }
        if (leavingIndex < 0) {
            eventSink.accept("[Network] " + senderName + " left, but their seat could not be matched.");
            return;
        }

        String name = game.getActivePlayers().get(leavingIndex).getPlayerName();
        if (!game.removePlayerAt(leavingIndex)) {
            return;
        }
        if (leavingIndex < localPlayerIndexGetter.getAsInt()) {
            localPlayerIndexSetter.accept(localPlayerIndexGetter.getAsInt() - 1);
        }
        int playerCount = game.getActivePlayers().size();
        playerCountSetter.accept(playerCount);
        eventSink.accept("[Network] " + name + " left the match.");
        if (playerCount >= GameManager.MIN_PLAYERS) {
            recreateContent.run();
        } else {
            renderCallback.run();
        }
    }

    // Finds a current player by display name for resilient leave messages.
    private int findPlayerIndexByName(String name) {
        for (int i = 0; i < game.getActivePlayers().size(); i++) {
            if (game.getActivePlayers().get(i).getPlayerName().equals(name)) {
                return i;
            }
        }
        return -1;
    }

    // Locates a card by original index, then by name for duplicate-safe replay.
    private int findCardIndex(Player player, Map<String, String> payload, String indexKey, String nameKey) {
        if (player == null || payload == null) {
            return -1;
        }
        List<Card> hand = player.getHand().getCards();
        int index = NetworkActionCodec.readInt(payload, indexKey, -1);
        String cardName = payload.getOrDefault(nameKey, "");
        if (index >= 0 && index < hand.size()
                && (cardName.isEmpty() || cardName.equals(hand.get(index).getCardName()))) {
            return index;
        }
        for (int i = 0; i < hand.size(); i++) {
            if (cardName.equals(hand.get(i).getCardName())) {
                return i;
            }
        }
        return -1;
    }
}
