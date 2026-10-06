package com.mrkoszos.chromatica.model;

/**
 * =========================
 *      BACKGROUND PATTERN
 * =========================
 * Ezek az egyetlen helyek, ahol a minta (elrendezés) hard-code-olva van.
 * Minden menü ugyanezt a négyet használja; a tényleges itemeket
 * (primary/secondary material + név) a config.yml adja.
 * <p>
 * Minden inventory 9 oszlop széles soroknak tekintendő
 * (row = slot / 9, col = slot % 9).
 */
public enum BackgroundPattern {

    /**
     * Minden üres slot primary item lesz. (Ez volt az eredeti, legegyszerűbb
     * viselkedés, amit pl. a Color / Formatting menü használt.)
     */
    SOLID {
        @Override
        public boolean isPrimary(int row, int col, int rows) {
            return true;
        }
    },

    /**
     * Sakktábla minta: soronként és oszloponként váltakozva primary/secondary.
     */
    CHECKERBOARD {
        @Override
        public boolean isPrimary(int row, int col, int rows) {
            return (row + col) % 2 == 0;
        }
    },

    /**
     * Egyszerű szegély: a legszélső sorok/oszlopok primary,
     * minden más (belső rész) secondary.
     */
    BORDER {
        @Override
        public boolean isPrimary(int row, int col, int rows) {
            return row == 0 || row == rows - 1 || col == 0 || col == LAST_COL;
        }
    },

    /**
     * Az eredeti, dekoratív minta, amit a Chromatica/Gradient/Preset menük
     * kézzel, sorról-sorra lekódolva használtak. Általánosítva tetszőleges
     * sorszámra: a legszélső sorpár egy mintát ad, az attól befelé eső
     * sorpár egy másikat, minden ennél beljebb eső sor pedig csak a két
     * szélén (0. és 8. oszlop) primary.
     */
    CLASSIC {
        @Override
        public boolean isPrimary(int row, int col, int rows) {

            int distanceFromEdge = Math.min(row, rows - 1 - row);

            if (distanceFromEdge == 0) {
                return EDGE_ROW[col];
            }

            if (distanceFromEdge == 1) {
                return INNER_ROW[col];
            }

            return col == 0 || col == LAST_COL;
        }
    };

    private static final int LAST_COL = 8;

    // P, S, S, P, P, P, S, S, P
    private static final boolean[] EDGE_ROW = {
            true, false, false, true, true, true, false, false, true
    };

    // S, P, P, S, S, S, P, P, S
    private static final boolean[] INNER_ROW = {
            false, true, true, false, false, false, true, true, false
    };

    /**
     * @return true, ha az adott (sor, oszlop) pozícióra a primary itemet
     * kell helyezni, false, ha a secondary-t.
     */
    public abstract boolean isPrimary(int row, int col, int rows);
}
