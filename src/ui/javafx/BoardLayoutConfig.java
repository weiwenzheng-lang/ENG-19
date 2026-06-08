package ui.javafx;

// Keeps board-frame coordinates separate from controller behavior.
final class BoardLayoutConfig {
    private static final int FIVE_PLAYER_LAYOUT = 5;
    private static final int THREE_PLAYER_LAYOUT = 3;

    private static final ZoneSpec[][] OPPONENT_SPECS = new ZoneSpec[][]{
            null,
            null,
            {
                    new ZoneSpec(488, 145, 705, 164, 0, 770, 93, 160, 44)
            },
            {
                    new ZoneSpec(158, 162, 470, 220, -28, 226, 154, 186, 45),
                    new ZoneSpec(1044, 162, 470, 220, 31, 1342, 154, 188, 45)
            },
            {
                    new ZoneSpec(130, 215, 410, 224, -32, 226, 200, 150, 44),
                    new ZoneSpec(512, 142, 640, 150, 0, 800, 86, 150, 44),
                    new ZoneSpec(1140, 218, 410, 224, 35, 1420, 213, 152, 44)
            },
            {
                    new ZoneSpec(110, 230, 380, 212, -28, 258, 195, 148, 44),
                    new ZoneSpec(485, 174, 360, 122, -8, 640, 120, 138, 44),
                    new ZoneSpec(865, 174, 320, 122, 8, 1085, 120, 142, 44),
                    new ZoneSpec(1182, 230, 380, 212, 28, 1454, 220, 132, 44)
            }
    };

    private static final ZoneSpec[] OWN_TABLE_SPECS = new ZoneSpec[]{
            null,
            null,
            area(410, 555, 852, 98, 0),
            area(421, 548, 828, 96, 0),
            area(389, 582, 893, 96, 0),
            area(478, 570, 730, 98, 0)
    };

    private static final ZoneSpec[] HAND_SPECS = new ZoneSpec[]{
            null,
            null,
            area(145, 700, 1385, 176, 0),
            area(260, 700, 1155, 178, 0),
            area(252, 700, 1203, 180, 0),
            area(255, 700, 1175, 160, 0)
    };

    private static final ZoneSpec[] OWN_NAME_SPECS = new ZoneSpec[]{
            null,
            null,
            area(206, 628, 162, 48, 0),
            area(264, 642, 150, 48, 0),
            area(210, 685, 150, 48, 0),
            area(286, 660, 150, 48, 0)
    };

    private static final ZoneSpec[] CENTER_TURN_SPECS = new ZoneSpec[]{
            null,
            null,
            area(686, 360, 300, 150, 0), // 2 players
            area(689, 330, 300, 150, 0), // 3 players
            area(690, 360, 300, 150, 0), // 4 players
            area(699, 360, 300, 150, 0)  // 5 players
    };

    // Prevents construction of this static layout holder.
    private BoardLayoutConfig() {
    }

    // Returns opponent table and name-frame coordinates for each player count.
    static ZoneSpec[] opponentSpecs(int count) {
        return specsFor(OPPONENT_SPECS, count, FIVE_PLAYER_LAYOUT);
    }

    // Returns the local player's table-frame coordinates.
    static ZoneSpec ownTableSpec(int count) {
        return specFor(OWN_TABLE_SPECS, count, FIVE_PLAYER_LAYOUT);
    }

    // Returns the local hand-frame coordinates.
    static ZoneSpec handSpec(int count) {
        return specFor(HAND_SPECS, count, FIVE_PLAYER_LAYOUT);
    }

    // Returns the local name and stats-frame coordinates.
    static ZoneSpec ownNameSpec(int count) {
        return specFor(OWN_NAME_SPECS, count, THREE_PLAYER_LAYOUT);
    }

    // Returns the centered frame for the turn owner and action counter text.
    static ZoneSpec centerTurnSpec(int count) {
        return specFor(CENTER_TURN_SPECS, count, THREE_PLAYER_LAYOUT);
    }

    // Creates a simple zone spec without a name frame.
    private static ZoneSpec area(double x, double y, double width, double height, double rotate) {
        return new ZoneSpec(x, y, width, height, rotate, 0, 0, 0, 0);
    }

    // Returns the configured specs while preserving the legacy fallback layout.
    private static ZoneSpec[] specsFor(ZoneSpec[][] specs, int count, int fallback) {
        int index = validIndex(specs, count) ? count : fallback;
        return specs[index].clone();
    }

    // Returns one configured area while preserving the legacy fallback layout.
    private static ZoneSpec specFor(ZoneSpec[] specs, int count, int fallback) {
        int index = validIndex(specs, count) ? count : fallback;
        return specs[index];
    }

    // Checks that a player count has an explicit layout entry.
    private static boolean validIndex(Object[] specs, int count) {
        return count >= 0 && count < specs.length && specs[count] != null;
    }

    // Immutable coordinates for one card zone and its optional name frame.
    static final class ZoneSpec {
        final double x;
        final double y;
        final double width;
        final double height;
        final double rotate;
        final double nameX;
        final double nameY;
        final double nameWidth;
        final double nameHeight;

        // Stores frame coordinates, dimensions, rotation, and optional name-frame bounds.
        ZoneSpec(double x, double y, double width, double height, double rotate,
                 double nameX, double nameY, double nameWidth, double nameHeight) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.rotate = rotate;
            this.nameX = nameX;
            this.nameY = nameY;
            this.nameWidth = nameWidth;
            this.nameHeight = nameHeight;
        }
    }
}
