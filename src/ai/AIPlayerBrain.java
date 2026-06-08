package ai;

import cards.*;
import core.GameManager;
import core.TargetInfo;
import enums.PropertyColor;
import player.Player;
import player.Rentable;

import java.util.*;
import java.util.function.Predicate;

public class AIPlayerBrain implements AIActionStrategy {

    // Picks the next legal AI action by strategy priority.
    @Override
    public AIAction decideNextAction(Player ai, GameManager game) {
        int actions = game.getActionsRemaining();
        List<Card> hand = ai.getHand().getCards();

        if (ai.getHand().requiresDiscard() && actions == 0) {
            return discardWorstCard(ai, hand);
        }

        if (actions <= 0) {
            return new AIAction(AIAction.Type.END_TURN);
        }

        AIAction priorityAction = tryPriorityAction(ai, hand, game, actions);
        if (priorityAction != null) return priorityAction;

        AIAction buildAction = tryBuildOrBankAction(ai, hand, game);
        if (buildAction != null) return buildAction;

        if (ai.getHand().requiresDiscard()) {
            return discardWorstCard(ai, hand);
        }
        return new AIAction(AIAction.Type.END_TURN);
    }

    // Tries high-impact action cards before building.
    private AIAction tryPriorityAction(Player ai, List<Card> hand, GameManager game, int actions) {
        if (actions >= 2) {
            AIAction doubleRent = tryDoubleRentCombo(ai, hand, game);
            if (doubleRent != null) return doubleRent;
        }

        AIAction dealBreaker = tryPlayCardType(ai, hand, game, DealBreakerCard.class);
        if (dealBreaker != null) return dealBreaker;

        AIAction passGo = tryPlayCardType(ai, hand, game, PassGoCard.class);
        if (passGo != null) return passGo;

        AIAction slyDeal = tryPlayCardType(ai, hand, game, SlyDealCard.class);
        if (slyDeal != null) return slyDeal;

        AIAction forceDeal = tryPlayCardType(ai, hand, game, ForceDealCard.class);
        if (forceDeal != null) return forceDeal;

        AIAction debtCollector = tryPlayCardType(ai, hand, game, DebtCollectorCard.class);
        if (debtCollector != null) return debtCollector;

        AIAction wildRent = tryWildRent(ai, hand, game);
        if (wildRent != null) return wildRent;

        AIAction rent = tryRentCard(ai, hand, game);
        if (rent != null) return rent;

        AIAction birthday = tryPlayCardType(ai, hand, game, BirthdayCard.class);
        if (birthday != null) return birthday;
        return null;
    }

    // Tries property building and fallback banking.
    private AIAction tryBuildOrBankAction(Player ai, List<Card> hand, GameManager game) {
        AIAction wildProp = tryWildProperty(ai, hand);
        if (wildProp != null) return wildProp;

        AIAction property = tryPropertyCard(ai, hand);
        if (property != null) return property;

        AIAction house = tryImprovementCard(ai, hand, game, HouseCard.class);
        if (house != null) return house;

        AIAction hotel = tryImprovementCard(ai, hand, game, HotelCard.class);
        if (hotel != null) return hotel;

        AIAction bankDeposit = tryBankDeposit(ai, hand);
        if (bankDeposit != null) return bankDeposit;
        return null;
    }

    // Chooses the best color for a wild property or rent card.
    public PropertyColor chooseColor(Player ai, PropertyColor[] options, Card contextCard) {
        if (options == null || options.length == 0) return null;
        if (options.length == 1) return options[0];

        if (contextCard instanceof RentCard || contextCard instanceof WildRentCard) {
            PropertyColor best = chooseBestRentColor(ai, options);
            if (best != null) return best;
        }

        PropertyColor best = chooseBestProgressColor(ai, options);
        return best == null ? chooseSmallestSetColor(options) : best;
    }

    // Chooses the complete color with the highest rent.
    private PropertyColor chooseBestRentColor(Player ai, PropertyColor[] options) {
        PropertyColor best = null;
        int bestRent = -1;
        for (PropertyColor color : options) {
            Rentable set = ai.getPropertyArea().getPropertySet(color);
            if (set != null && set.isComplete() && set.calculateRent() > bestRent) {
                bestRent = set.calculateRent();
                best = color;
            }
        }
        return best;
    }

    // Chooses the color closest to completion.
    private PropertyColor chooseBestProgressColor(Player ai, PropertyColor[] options) {
        PropertyColor best = options[0];
        double bestProgress = 0;
        for (PropertyColor color : options) {
            double progress = getSetCompletionProgress(ai, color);
            if (progress > bestProgress) {
                bestProgress = progress;
                best = color;
            }
        }
        return bestProgress == 0 ? null : best;
    }

    // Chooses the smallest set when no progress exists.
    private PropertyColor chooseSmallestSetColor(PropertyColor[] options) {
        PropertyColor best = options[0];
        for (PropertyColor color : options) {
            if (best == null || color.getRequiredCount() < best.getRequiredCount()) {
                best = color;
            }
        }
        return best;
    }

    // Chooses a target for attack cards that need one.
    public TargetInfo chooseTarget(Player ai, Card card, GameManager game) {
        List<Player> opponents = game.getOpponents(ai);
        if (opponents.isEmpty()) return null;

        if (card instanceof SlyDealCard) {
            return chooseSlyDealTarget(ai, opponents);
        } else if (card instanceof ForceDealCard) {
            return chooseForceDealTarget(ai, opponents);
        } else if (card instanceof DealBreakerCard) {
            return chooseDealBreakerTarget(opponents);
        } else if (card instanceof DebtCollectorCard) {
            return new TargetInfo(pickRichestOpponent(opponents));
        } else if (card instanceof WildRentCard) {
            return new TargetInfo(pickRichestOpponent(opponents));
        }
        return null;
    }

    // Chooses payment cards while preserving valuable property progress.
    public List<Card> choosePaymentCards(Player payer, Player payee, int amount,
                                         List<Card> bankCards, List<PropertyCard> propertyCards) {
        List<Card> result = new ArrayList<>();

        List<Card> bankSelection = selectOptimalPayment(bankCards, amount);
        int bankTotal = bankSelection.stream().mapToInt(Card::getMonetaryValue).sum();

        if (bankTotal >= amount) {
            return bankSelection;
        }

        result.addAll(bankCards);
        int stillOwe = amount - bankCards.stream().mapToInt(Card::getMonetaryValue).sum();

        if (stillOwe > 0) {
            List<PropertyCard> sortedProps = new ArrayList<>(propertyCards);
            sortedProps.sort(Comparator.comparingInt((PropertyCard c) -> {
                double progress = getSetCompletionProgress(payer, c.getColorGroup());
                int penalty = progress >= 0.8 ? 100 : 0;
                return c.getMonetaryValue() + penalty;
            }));

            int raised = 0;
            for (PropertyCard card : sortedProps) {
                if (raised >= stillOwe) break;
                result.add(card);
                raised += card.getMonetaryValue();
            }
        }

        return result;
    }

    // Decides whether an AI should answer an attack with Just Say No.
    @Override
    public boolean shouldCounterWithJustSayNo(Player victim, GameManager game) {
        List<Card> hand = victim.getHand().getCards();
        boolean hasJSN = hand.stream().anyMatch(c -> c.getCardName().equals("Just Say No"));
        if (!hasJSN) return false;


        return true;
    }

    // Tries to play a card of the requested type.
    private AIAction tryPlayCardType(Player ai, List<Card> hand, GameManager game,
                                      Class<? extends Card> cardType) {
        for (int i = 0; i < hand.size(); i++) {
            Card card = hand.get(i);
            if (!cardType.isInstance(card)) continue;

            if (card instanceof JustSayNoCard || card instanceof DoubleTheRentCard) continue;

            if (card.requiresTarget()) {
                if (!hasPlayableTarget(ai, card, game)) continue;
                TargetInfo target = chooseTarget(ai, card, game);
                if (target == null || target.getTargetPlayer() == null) continue;
                return new AIAction(AIAction.Type.PLAY_CARD, i, target);
            }

            return new AIAction(AIAction.Type.PLAY_CARD, i);
        }
        return null;
    }

    // Checks whether an action card has a valid target before selecting it.
    private boolean hasPlayableTarget(Player ai, Card card, GameManager game) {
        if (card instanceof DealBreakerCard) {
            return hasOpponentMatching(ai, game,
                    opponent -> opponent.getPropertyArea().countCompletedSets() > 0);
        }
        if (card instanceof SlyDealCard) {
            return hasOpponentMatching(ai, game,
                    opponent -> !opponent.getPropertyArea().getStealableIncompleteColors().isEmpty());
        }
        if (card instanceof ForceDealCard) {
            return !ai.getPropertyArea().getPropertyColorsWithCards().isEmpty()
                    && hasOpponentMatching(ai, game,
                    opponent -> !opponent.getPropertyArea().getPropertyColorsWithCards().isEmpty());
        }
        return true;
    }

    // Reports whether any opponent satisfies a target predicate.
    private boolean hasOpponentMatching(Player ai, GameManager game, Predicate<Player> predicate) {
        for (Player opponent : game.getOpponents(ai)) {
            if (predicate.test(opponent)) {
                return true;
            }
        }
        return false;
    }

    // Tries to pair Double The Rent with the best rent card.
    private AIAction tryDoubleRentCombo(Player ai, List<Card> hand, GameManager game) {
        int doubleIdx = findDoubleRentIndex(hand);
        if (doubleIdx < 0) return null;

        DoubleRentCandidate candidate = findBestDoubleRentCandidate(ai, hand, game, doubleIdx);
        if (!candidate.isPresent()) return null;
        return new AIAction(AIAction.Type.PLAY_DOUBLE_RENT,
                doubleIdx,
                candidate.rentIndex,
                candidate.target,
                candidate.color);
    }

    // Finds the first Double The Rent card in hand.
    private int findDoubleRentIndex(List<Card> hand) {
        for (int i = 0; i < hand.size(); i++) {
            if (hand.get(i) instanceof DoubleTheRentCard) {
                return i;
            }
        }
        return -1;
    }

    // Finds the highest-value rent card that can be doubled.
    private DoubleRentCandidate findBestDoubleRentCandidate(Player ai, List<Card> hand,
                                                            GameManager game, int doubleIdx) {
        DoubleRentCandidate candidate = new DoubleRentCandidate();
        for (int i = 0; i < hand.size(); i++) {
            if (i == doubleIdx) continue;
            Card card = hand.get(i);
            if (card instanceof RentCard) {
                considerRegularRent(ai, (RentCard) card, i, candidate);
            } else if (card instanceof WildRentCard) {
                considerWildRent(ai, game, i, candidate);
            }
        }
        return candidate;
    }

    // Considers a regular rent card for a doubled group rent.
    private void considerRegularRent(Player ai, RentCard rentCard, int index, DoubleRentCandidate candidate) {
        for (PropertyColor color : rentCard.getColorOptions()) {
            Rentable set = ai.getPropertyArea().getPropertySet(color);
            if (set != null && set.isComplete() && set.calculateRent() >= 3) {
                candidate.update(set.calculateRent() * 2, index, color, null);
            }
        }
    }

    // Considers a wild rent card for a doubled single-target rent.
    private void considerWildRent(Player ai, GameManager game, int index, DoubleRentCandidate candidate) {
        Player target = pickRichestOpponent(game.getOpponents(ai));
        if (target == null) return;
        for (PropertyColor color : PropertyColor.values()) {
            if (color == PropertyColor.WILD) continue;
            Rentable set = ai.getPropertyArea().getPropertySet(color);
            if (set != null && set.isComplete() && set.calculateRent() >= 3) {
                candidate.update(set.calculateRent() * 2, index, color, new TargetInfo(target));
            }
        }
    }

    // Stores the best Double The Rent pairing found so far.
    private static final class DoubleRentCandidate {
        private int rentIndex = -1;
        private PropertyColor color;
        private int rent = -1;
        private TargetInfo target;

        // Replaces the candidate when the rent value is higher.
        private void update(int rent, int rentIndex, PropertyColor color, TargetInfo target) {
            if (rent > this.rent) {
                this.rent = rent;
                this.rentIndex = rentIndex;
                this.color = color;
                this.target = target;
            }
        }

        // Reports whether a usable pairing was found.
        private boolean isPresent() {
            return rentIndex >= 0 && color != null;
        }
    }

    // Tries to play a wild rent card against one opponent.
    private AIAction tryWildRent(Player ai, List<Card> hand, GameManager game) {
        for (int i = 0; i < hand.size(); i++) {
            Card card = hand.get(i);
            if (!(card instanceof WildRentCard)) continue;

            PropertyColor bestColor = null;
            int bestRent = -1;
            for (PropertyColor color : ((WildRentCard) card).getAvailableColors()) {
                Rentable set = ai.getPropertyArea().getPropertySet(color);
                if (set != null && set.calculateRent() > 0) {
                    int rent = set.calculateRent();
                    if (rent > bestRent) {
                        bestRent = rent;
                        bestColor = color;
                    }
                }
            }
            if (bestColor == null) continue;

            Player target = pickRichestOpponent(game.getOpponents(ai));
            if (target == null) continue;

            return new AIAction(AIAction.Type.PLAY_CARD, i, new TargetInfo(target), bestColor);
        }
        return null;
    }
    // Tries to play a regular two-color rent card.
    private AIAction tryRentCard(Player ai, List<Card> hand, GameManager game) {
        for (int i = 0; i < hand.size(); i++) {
            Card card = hand.get(i);
            if (!(card instanceof RentCard)) continue;

            RentCard rentCard = (RentCard) card;
            PropertyColor bestColor = null;
            int bestRent = -1;
            for (PropertyColor color : rentCard.getColorOptions()) {
                Rentable set = ai.getPropertyArea().getPropertySet(color);
                if (set != null && set.calculateRent() > 0) {
                    int rent = set.calculateRent();
                    if (rent > bestRent) {
                        bestRent = rent;
                        bestColor = color;
                    }
                }
            }
            if (bestColor == null) continue;

            return new AIAction(AIAction.Type.PLAY_CARD, i, null, bestColor);
        }
        return null;
    }
    // Tries to play a property wild card.
    private AIAction tryWildProperty(Player ai, List<Card> hand) {
        for (int i = 0; i < hand.size(); i++) {
            Card card = hand.get(i);
            if (!(card instanceof SuperWildCard) && !(card instanceof PropertyWildCard)) continue;

            PropertyColor[] options;
            if (card instanceof SuperWildCard) {
                options = ((SuperWildCard) card).getAvailableColors();
            } else {
                options = ((PropertyWildCard) card).getAvailableColors();
            }
            PropertyColor chosen = chooseColor(ai, options, card);
            if (chosen == null) continue;

            return new AIAction(AIAction.Type.PLAY_CARD, i, null, chosen);
        }
        return null;
    }
    // Tries to play a standard property card.
    private AIAction tryPropertyCard(Player ai, List<Card> hand) {
        for (int i = 0; i < hand.size(); i++) {
            Card card = hand.get(i);
            if (!(card instanceof PropertyCard)) continue;
            if (card instanceof SuperWildCard || card instanceof PropertyWildCard) continue;
            return new AIAction(AIAction.Type.PLAY_CARD, i);
        }
        return null;
    }
    // Tries to add a house or hotel to the best eligible set.
    private AIAction tryImprovementCard(Player ai, List<Card> hand, GameManager game,
                                         Class<? extends Card> cardType) {
        for (int i = 0; i < hand.size(); i++) {
            Card card = hand.get(i);
            if (!cardType.isInstance(card)) continue;

            List<PropertyColor> eligible;
            if (card instanceof HouseCard) {
                eligible = ai.getPropertyArea().getHouseEligibleColors();
            } else {
                eligible = ai.getPropertyArea().getHotelEligibleColors();
            }
            if (eligible.isEmpty()) continue;

            PropertyColor best = null;
            int bestRent = -1;
            for (PropertyColor color : eligible) {
                Rentable set = ai.getPropertyArea().getPropertySet(color);
                if (set != null && set.calculateRent() > bestRent) {
                    bestRent = set.calculateRent();
                    best = color;
                }
            }
            if (best == null) continue;

            return new AIAction(AIAction.Type.PLAY_CARD, i, TargetInfo.forImprovement(best));
        }
        return null;
    }
    // Tries to bank the least important money card.
    private AIAction tryBankDeposit(Player ai, List<Card> hand) {
        int bestIdx = -1;
        int bestValue = Integer.MAX_VALUE;
        for (int i = 0; i < hand.size(); i++) {
            Card card = hand.get(i);
            if (card instanceof MoneyCard) {
                int val = card.getMonetaryValue();
                if (val < bestValue) {
                    bestValue = val;
                    bestIdx = i;
                }
            }
        }
        if (bestIdx < 0) return null;
        return new AIAction(AIAction.Type.DEPOSIT_TO_BANK, bestIdx);
    }
    // Chooses the lowest priority card to discard.
    private AIAction discardWorstCard(Player ai, List<Card> hand) {
        int worstIdx = 0;
        int worstScore = Integer.MAX_VALUE;
        for (int i = 0; i < hand.size(); i++) {
            Card card = hand.get(i);
            int score = (card instanceof MoneyCard ? 0 : 100) + card.getMonetaryValue();
            if (score < worstScore) {
                worstScore = score;
                worstIdx = i;
            }
        }
        return new AIAction(AIAction.Type.DISCARD, worstIdx);
    }

    // Chooses an opponent property for Sly Deal.
    private TargetInfo chooseSlyDealTarget(Player ai, List<Player> opponents) {
        for (Player opponent : opponents) {
            List<PropertyColor> stealable = opponent.getPropertyArea().getStealableIncompleteColors();
            for (PropertyColor color : stealable) {
                Rentable mySet = ai.getPropertyArea().getPropertySet(color);
                if (mySet != null && !mySet.isComplete()) {
                    return new TargetInfo(opponent, color, 0);
                }
            }
        }
        for (Player opponent : opponents) {
            List<PropertyColor> stealable = opponent.getPropertyArea().getStealableIncompleteColors();
            if (!stealable.isEmpty()) {
                return new TargetInfo(opponent, stealable.get(0), 0);
            }
        }
        return null;
    }
    // Chooses both sides of a Forced Deal exchange.
    private TargetInfo chooseForceDealTarget(Player ai, List<Player> opponents) {
        List<PropertyColor> myColors = ai.getPropertyArea().getPropertyColorsWithCards();
        if (myColors.isEmpty()) return null;

        PropertyColor myGiveColor = myColors.get(0);
        int myGiveIdx = 0;

        for (PropertyColor color : myColors) {
            List<PropertyCard> cards = ai.getPropertyArea().getCards(color, false);
            if (cards.size() > 1) {
                myGiveColor = color;
                myGiveIdx = cards.size() - 1;
                break;
            }
        }

        for (Player opponent : opponents) {
            List<PropertyColor> theirColors = opponent.getPropertyArea().getPropertyColorsWithCards();
            if (theirColors.isEmpty()) continue;

            PropertyColor theirColor = theirColors.get(0);
            for (PropertyColor color : theirColors) {
                Rentable mySet = ai.getPropertyArea().getPropertySet(color);
                if (mySet != null && !mySet.isComplete()) {
                    theirColor = color;
                    break;
                }
            }

            return new TargetInfo(opponent, myGiveColor, myGiveIdx, theirColor, 0);
        }
        return null;
    }
    // Chooses the best completed set to steal with Deal Breaker.
    private TargetInfo chooseDealBreakerTarget(List<Player> opponents) {
        Player bestTarget = null;
        int mostSets = -1;
        PropertyColor bestColor = null;

        for (Player opponent : opponents) {
            int sets = opponent.getPropertyArea().countCompletedSets();
            if (sets > mostSets) {
                mostSets = sets;
                bestTarget = opponent;
                bestColor = pickHighestRentCompletedColor(opponent);
            }
        }

        if (bestTarget == null || bestColor == null) return null;
        return TargetInfo.forImprovement(bestColor).withTarget(bestTarget);
    }

    // Finds the opponent with the most banked money.
    private Player pickRichestOpponent(List<Player> opponents) {
        Player richest = null;
        int maxFunds = -1;
        for (Player p : opponents) {
            int funds = p.getBankArea().calculateTotalFunds();
            if (funds > maxFunds) {
                maxFunds = funds;
                richest = p;
            }
        }
        return richest;
    }
    // Finds the completed color set with the highest rent.
    private PropertyColor pickHighestRentCompletedColor(Player player) {
        PropertyColor best = null;
        int bestRent = -1;
        for (PropertyColor color : player.getPropertyArea().getCompletedColors()) {
            Rentable set = player.getPropertyArea().getPropertySet(color);
            if (set != null && set.calculateRent() > bestRent) {
                bestRent = set.calculateRent();
                best = color;
            }
        }
        return best;
    }
    // Calculates progress toward completing one color set.
    private double getSetCompletionProgress(Player player, PropertyColor color) {
        Rentable set = player.getPropertyArea().getPropertySet(color);
        if (set == null) return 0.0;

        int count = 0;
        if (set instanceof player.SetDecorator) {
            player.PropertySet root = ((player.SetDecorator) set).getRootSet();
            count = root != null ? root.getCardsCount() : 0;
        } else if (set instanceof player.PropertySet) {
            count = ((player.PropertySet) set).getCardsCount();
        }

        return Math.min(1.0, (double) count / color.getRequiredCount());
    }
    // Selects bank cards that meet the payment with minimal overpay.
    private List<Card> selectOptimalPayment(List<Card> candidates, int amount) {
        if (candidates.isEmpty()) return Collections.emptyList();

        List<Card> sorted = new ArrayList<>(candidates);
        BestResult best = new BestResult();
        search(0, amount, 0, new ArrayList<>(), sorted, best);

        if (best.cards != null && best.sum >= amount) {
            return best.cards;
        }
        return new ArrayList<>(candidates);
    }
    // Backtracks through payment candidates to find the best subset.
    private void search(int idx, int required, int curSum, List<Card> current,
                        List<Card> allCards, BestResult best) {
        if (curSum >= required) {
            if (best.cards == null || curSum < best.sum
                    || (curSum == best.sum && current.size() < best.count)) {
                best.sum = curSum;
                best.count = current.size();
                best.cards = new ArrayList<>(current);
            }
            return;
        }
        if (idx == allCards.size()) return;

        search(idx + 1, required, curSum, current, allCards, best);
        Card card = allCards.get(idx);
        current.add(card);
        search(idx + 1, required, curSum + card.getMonetaryValue(), current, allCards, best);
        current.remove(current.size() - 1);
    }

    private static class BestResult {
        List<Card> cards;
        int sum = 0;
        int count = 0;
    }
}
