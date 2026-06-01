package ai;

import cards.*;
import core.GameManager;
import core.TargetInfo;
import enums.PropertyColor;
import player.Player;
import player.Rentable;

import java.util.*;

/**
 * AI 策略引擎 —— 所有 AI 决策逻辑的集中地。
 * 方法均为纯函数：读取游戏状态，返回决策，不修改任何状态。
 */
public class AIPlayerBrain {

    // ==================== 主决策：下一步动作 ====================

    /**
     * 为 AI 玩家决定当前回合的下一步动作。
     * 按策略优先级依次评估，返回第一个可行的动作。
     */
    public AIAction decideNextAction(Player ai, GameManager game) {
        int actions = game.getActionsRemaining();
        List<Card> hand = ai.getHand().getCards();

        // 手牌超限时优先弃牌（不消耗行动）
        if (ai.getHand().requiresDiscard() && actions == 0) {
            return discardWorstCard(ai, hand);
        }

        // 无剩余行动则结束回合
        if (actions <= 0) {
            return new AIAction(AIAction.Type.END_TURN);
        }

        // 策略 1: DoubleTheRent + 租金卡组合 (需 2 行动且手牌中有两者)
        if (actions >= 2) {
            AIAction doubleRent = tryDoubleRentCombo(ai, hand, game);
            if (doubleRent != null) return doubleRent;
        }

        // 策略 2: DealBreaker
        AIAction dealBreaker = tryPlayCardType(ai, hand, game, DealBreakerCard.class);
        if (dealBreaker != null) return dealBreaker;

        // 策略 3: PassGo (1 行动换 2 张牌，总是值得)
        AIAction passGo = tryPlayCardType(ai, hand, game, PassGoCard.class);
        if (passGo != null) return passGo;

        // 策略 4: SlyDeal
        AIAction slyDeal = tryPlayCardType(ai, hand, game, SlyDealCard.class);
        if (slyDeal != null) return slyDeal;

        // 策略 5: ForceDeal
        AIAction forceDeal = tryPlayCardType(ai, hand, game, ForceDealCard.class);
        if (forceDeal != null) return forceDeal;

        // 策略 6: DebtCollector
        AIAction debtCollector = tryPlayCardType(ai, hand, game, DebtCollectorCard.class);
        if (debtCollector != null) return debtCollector;

        // 策略 7: WildRent (需要拥有可收租的房产套装)
        AIAction wildRent = tryWildRent(ai, hand, game);
        if (wildRent != null) return wildRent;

        // 策略 8: 普通租金卡
        AIAction rent = tryRentCard(ai, hand, game);
        if (rent != null) return rent;

        // 策略 9: Birthday
        AIAction birthday = tryPlayCardType(ai, hand, game, BirthdayCard.class);
        if (birthday != null) return birthday;

        // 策略 10: 万用房产卡
        AIAction wildProp = tryWildProperty(ai, hand);
        if (wildProp != null) return wildProp;

        // 策略 11: 普通房产卡
        AIAction property = tryPropertyCard(ai, hand);
        if (property != null) return property;

        // 策略 12: House
        AIAction house = tryImprovementCard(ai, hand, game, HouseCard.class);
        if (house != null) return house;

        // 策略 13: Hotel
        AIAction hotel = tryImprovementCard(ai, hand, game, HotelCard.class);
        if (hotel != null) return hotel;

        // 策略 14: 货币卡存入银行（保留高面额用于支付）
        AIAction bankDeposit = tryBankDeposit(ai, hand);
        if (bankDeposit != null) return bankDeposit;

        // 策略 15: 没有更好的选择了
        // 在结束回合之前检查是否需要先弃牌
        if (ai.getHand().requiresDiscard()) {
            return discardWorstCard(ai, hand);
        }
        return new AIAction(AIAction.Type.END_TURN);
    }

    // ==================== 颜色选择 ====================

    /**
     * 为万用卡/租金卡选择最有利的颜色。
     * 优先选择拥有已完成套装中租金最高的颜色，其次选择最接近完成的颜色。
     */
    public PropertyColor chooseColor(Player ai, PropertyColor[] options, Card contextCard) {
        if (options == null || options.length == 0) return null;
        if (options.length == 1) return options[0];

        // 如果上下文是一张租金卡，优先选租金最高的已完成套装颜色
        if (contextCard instanceof RentCard || contextCard instanceof WildRentCard) {
            PropertyColor best = null;
            int bestRent = -1;
            for (PropertyColor color : options) {
                Rentable set = ai.getPropertyArea().getPropertySet(color);
                if (set != null && set.isComplete()) {
                    int rent = set.calculateRent();
                    if (rent > bestRent) {
                        bestRent = rent;
                        best = color;
                    }
                }
            }
            if (best != null) return best;
        }

        // 对于房产万用卡，选择最接近完成套装的颜色
        PropertyColor best = options[0];
        double bestProgress = 0;
        for (PropertyColor color : options) {
            double progress = getSetCompletionProgress(ai, color);
            if (progress > bestProgress) {
                bestProgress = progress;
                best = color;
            }
        }

        // 如果进度相同，选择要求卡牌数少（更容易完成）的颜色
        if (bestProgress == 0) {
            for (PropertyColor color : options) {
                if (best == null || color.getRequiredCount() < best.getRequiredCount()) {
                    best = color;
                }
            }
        }

        return best;
    }

    // ==================== 目标选择 ====================

    /**
     * 为攻击卡选择目标对手，返回包含完整目标信息的 TargetInfo。
     */
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

    // ==================== 支付选择 ====================

    /**
     * 选择用于支付的卡牌组合。
     * 优先使用银行资金，不足时使用房产卡（但不拆散接近完成的套装）。
     */
    public List<Card> choosePaymentCards(Player payer, Player payee, int amount,
                                         List<Card> bankCards, List<PropertyCard> propertyCards) {
        List<Card> result = new ArrayList<>();

        // 第一步：尝试仅用银行资金支付（子集求和，最小化超额）
        List<Card> bankSelection = selectOptimalPayment(bankCards, amount);
        int bankTotal = bankSelection.stream().mapToInt(Card::getMonetaryValue).sum();

        if (bankTotal >= amount) {
            return bankSelection;
        }

        // 第二步：银行资金不够，使用全部银行资金
        result.addAll(bankCards);
        int stillOwe = amount - bankCards.stream().mapToInt(Card::getMonetaryValue).sum();

        // 第三步：需要变卖房产
        if (stillOwe > 0) {
            // 按价值从低到高排序房产（优先卖掉低价值、不拆散近完成套装的）
            List<PropertyCard> sortedProps = new ArrayList<>(propertyCards);
            sortedProps.sort(Comparator.comparingInt((PropertyCard c) -> {
                // 优先卖低价值卡，但接近完成套装的卡排在后面
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

    // ==================== Just Say No 决策 ====================

    /**
     * 决定是否使用 Just Say No 反击。
     */
    public boolean shouldCounterWithJustSayNo(Player victim, GameManager game) {
        // 找到手牌中的 Just Say No
        List<Card> hand = victim.getHand().getCards();
        boolean hasJSN = hand.stream().anyMatch(c -> c.getCardName().equals("Just Say No"));
        if (!hasJSN) return false;

        // 判断威胁等级：通过检查 pending 状态中的信息
        // 总是反击 DealBreaker（失去完整套装是最糟糕的）
        // 对于 SlyDeal / ForceDeal：仅当目标房产属于接近完成的套装时反击
        // 对于 DebtCollector：仅当银行余额 > 7M 时反击
        // 不反击 Birthday / Rent / WildRent

        // 由于 GameManager 不直接暴露 pending action 类型，
        // 我们采用保守策略：总是反击（因为 JSN 在手牌中不用也会被弃掉）
        // 更好的策略：如果有多个 JSN 或者手牌接近上限，就使用它
        return true;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 尝试打出指定类型的卡牌（需要目标选择的卡牌类型）。
     */
    private AIAction tryPlayCardType(Player ai, List<Card> hand, GameManager game,
                                      Class<? extends Card> cardType) {
        GameManager gm = GameManager.getInstance();
        for (int i = 0; i < hand.size(); i++) {
            Card card = hand.get(i);
            if (!cardType.isInstance(card)) continue;

            // 跳过 JustSayNo 和 DoubleTheRent（它们不能直接打出）
            if (card instanceof JustSayNoCard || card instanceof DoubleTheRentCard) continue;

            // 需要目标的卡牌
            if (card.requiresTarget()) {
                // 特殊处理 DealBreaker
                if (card instanceof DealBreakerCard) {
                    List<Player> opponents = gm.getOpponents(ai);
                    opponents.removeIf(p -> p.getPropertyArea().countCompletedSets() == 0);
                    if (opponents.isEmpty()) continue;
                }
                // 特殊处理 SlyDeal
                if (card instanceof SlyDealCard) {
                    List<Player> opponents = gm.getOpponents(ai);
                    opponents.removeIf(p -> p.getPropertyArea().getStealableIncompleteColors().isEmpty());
                    if (opponents.isEmpty()) continue;
                }
                // 特殊处理 ForceDeal
                if (card instanceof ForceDealCard) {
                    if (ai.getPropertyArea().getPropertyColorsWithCards().isEmpty()) continue;
                    List<Player> opponents = gm.getOpponents(ai);
                    opponents.removeIf(p -> p.getPropertyArea().getPropertyColorsWithCards().isEmpty());
                    if (opponents.isEmpty()) continue;
                }

                TargetInfo target = chooseTarget(ai, card, gm);
                if (target == null || target.getTargetPlayer() == null) continue;
                return new AIAction(AIAction.Type.PLAY_CARD, i, target);
            }

            // 不需要目标的卡牌（PassGo, Birthday 等）
            return new AIAction(AIAction.Type.PLAY_CARD, i);
        }
        return null;
    }

    /**
     * 尝试 DoubleTheRent 组合。
     */
    private AIAction tryDoubleRentCombo(Player ai, List<Card> hand, GameManager game) {
        int doubleIdx = -1;
        for (int i = 0; i < hand.size(); i++) {
            if (hand.get(i) instanceof DoubleTheRentCard) {
                doubleIdx = i;
                break;
            }
        }
        if (doubleIdx < 0) return null;

        // 找租金卡并评估最佳组合
        int bestRentIdx = -1;
        PropertyColor bestColor = null;
        int bestRent = -1;
        TargetInfo bestTarget = null;

        for (int i = 0; i < hand.size(); i++) {
            if (i == doubleIdx) continue;
            Card card = hand.get(i);

            if (card instanceof RentCard) {
                RentCard rentCard = (RentCard) card;
                for (PropertyColor color : rentCard.getColorOptions()) {
                    Rentable set = ai.getPropertyArea().getPropertySet(color);
                    if (set != null && set.isComplete() && set.calculateRent() >= 3) {
                        int rent = set.calculateRent() * 2; // doubled
                        if (rent > bestRent) {
                            bestRent = rent;
                            bestRentIdx = i;
                            bestColor = color;
                            bestTarget = null; // group attack, no target
                        }
                    }
                }
            } else if (card instanceof WildRentCard) {
                // WildRent 需要选颜色和目标
                for (PropertyColor color : PropertyColor.values()) {
                    if (color == PropertyColor.WILD) continue;
                    Rentable set = ai.getPropertyArea().getPropertySet(color);
                    if (set != null && set.isComplete() && set.calculateRent() >= 3) {
                        int rent = set.calculateRent() * 2;
                        if (rent > bestRent) {
                            Player target = pickRichestOpponent(game.getOpponents(ai));
                            if (target != null) {
                                bestRent = rent;
                                bestRentIdx = i;
                                bestColor = color;
                                bestTarget = new TargetInfo(target);
                            }
                        }
                    }
                }
            }
        }

        if (bestRentIdx < 0 || bestColor == null) return null;
        return new AIAction(AIAction.Type.PLAY_DOUBLE_RENT, doubleIdx, bestRentIdx, bestTarget, bestColor);
    }

    /**
     * 尝试打出 WildRent（单目标租金卡）。
     */
    private AIAction tryWildRent(Player ai, List<Card> hand, GameManager game) {
        for (int i = 0; i < hand.size(); i++) {
            Card card = hand.get(i);
            if (!(card instanceof WildRentCard)) continue;

            // 选择最高租金的已完成套装颜色
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

    /**
     * 尝试打出普通租金卡（多目标）。
     */
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

    /**
     * 尝试打出万用房产卡。
     * 选择最接近完成套装的颜色。
     */
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

    /**
     * 尝试打出普通房产卡。
     */
    private AIAction tryPropertyCard(Player ai, List<Card> hand) {
        for (int i = 0; i < hand.size(); i++) {
            Card card = hand.get(i);
            if (!(card instanceof PropertyCard)) continue;
            if (card instanceof SuperWildCard || card instanceof PropertyWildCard) continue;
            // 普通房产直接打出
            return new AIAction(AIAction.Type.PLAY_CARD, i);
        }
        return null;
    }

    /**
     * 尝试打出 House 或 Hotel。
     */
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

            // 选择租金最高的完成套装
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

    /**
     * 尝试将货币卡存入银行。优先存低面额卡，保留高面额用于支付。
     */
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

    /**
     * 弃掉手牌中价值最低的卡牌。
     */
    private AIAction discardWorstCard(Player ai, List<Card> hand) {
        int worstIdx = 0;
        int worstScore = Integer.MAX_VALUE;
        for (int i = 0; i < hand.size(); i++) {
            Card card = hand.get(i);
            // 货币卡优先丢弃（保留行动卡），同类型中丢弃价值最低的
            int score = (card instanceof MoneyCard ? 0 : 100) + card.getMonetaryValue();
            if (score < worstScore) {
                worstScore = score;
                worstIdx = i;
            }
        }
        return new AIAction(AIAction.Type.DISCARD, worstIdx);
    }

    // ---- 目标选择子方法 ----

    private TargetInfo chooseSlyDealTarget(Player ai, List<Player> opponents) {
        // 优先选择能补全自己未完成套装的房产
        for (Player opponent : opponents) {
            List<PropertyColor> stealable = opponent.getPropertyArea().getStealableIncompleteColors();
            for (PropertyColor color : stealable) {
                // 检查自己是否有同色的未完成套装
                Rentable mySet = ai.getPropertyArea().getPropertySet(color);
                if (mySet != null && !mySet.isComplete()) {
                    // 偷对手该颜色的第一张卡
                    return new TargetInfo(opponent, color, 0);
                }
            }
        }
        // 没有能补全的，偷对手第一个可偷的房产
        for (Player opponent : opponents) {
            List<PropertyColor> stealable = opponent.getPropertyArea().getStealableIncompleteColors();
            if (!stealable.isEmpty()) {
                return new TargetInfo(opponent, stealable.get(0), 0);
            }
        }
        return null;
    }

    private TargetInfo chooseForceDealTarget(Player ai, List<Player> opponents) {
        // 找到自己最不重要的房产（未完成套装中多余的卡）
        List<PropertyColor> myColors = ai.getPropertyArea().getPropertyColorsWithCards();
        if (myColors.isEmpty()) return null;

        // 选择自己最不重要的房产颜色和对手最有价值的房产颜色
        PropertyColor myGiveColor = myColors.get(0);
        int myGiveIdx = 0;

        // 优先选自己有多个同色卡的颜色
        for (PropertyColor color : myColors) {
            List<PropertyCard> cards = ai.getPropertyArea().getCards(color, false);
            if (cards.size() > 1) {
                myGiveColor = color;
                myGiveIdx = cards.size() - 1; // 最后一张
                break;
            }
        }

        for (Player opponent : opponents) {
            List<PropertyColor> theirColors = opponent.getPropertyArea().getPropertyColorsWithCards();
            if (theirColors.isEmpty()) continue;

            // 优先选对手的高价值颜色
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

    private TargetInfo chooseDealBreakerTarget(List<Player> opponents) {
        // 选择完成套装最多的对手
        Player bestTarget = null;
        int mostSets = -1;
        PropertyColor bestColor = null;

        for (Player opponent : opponents) {
            int sets = opponent.getPropertyArea().countCompletedSets();
            if (sets > mostSets) {
                mostSets = sets;
                bestTarget = opponent;
                // 选择该对手租金最高的完成套装
                bestColor = pickHighestRentCompletedColor(opponent);
            }
        }

        if (bestTarget == null || bestColor == null) return null;
        return TargetInfo.forImprovement(bestColor).withTarget(bestTarget);
    }

    // ---- 通用辅助方法 ----

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

    /**
     * 计算某颜色套装的完成进度 (0.0 ~ 1.0)。
     */
    private double getSetCompletionProgress(Player player, PropertyColor color) {
        Rentable set = player.getPropertyArea().getPropertySet(color);
        if (set == null) return 0.0;

        // 获取底层 PropertySet 中的卡牌数量
        int count = 0;
        if (set instanceof player.SetDecorator) {
            player.PropertySet root = ((player.SetDecorator) set).getRootSet();
            count = root != null ? root.getCardsCount() : 0;
        } else if (set instanceof player.PropertySet) {
            count = ((player.PropertySet) set).getCardsCount();
        }

        return Math.min(1.0, (double) count / color.getRequiredCount());
    }

    /**
     * 子集求和：从候选卡牌中选择总价值 >= amount 且超额最小的组合。
     * 使用回溯搜索，与 BankArea.selectOptimalCardsForPayment 算法相同。
     */
    private List<Card> selectOptimalPayment(List<Card> candidates, int amount) {
        if (candidates.isEmpty()) return Collections.emptyList();

        List<Card> sorted = new ArrayList<>(candidates);
        BestResult best = new BestResult();
        search(0, amount, 0, new ArrayList<>(), sorted, best);

        if (best.cards != null && best.sum >= amount) {
            return best.cards;
        }
        // 找不到足够资金，返回所有卡牌
        return new ArrayList<>(candidates);
    }

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

        // 不选当前卡
        search(idx + 1, required, curSum, current, allCards, best);
        // 选当前卡
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
