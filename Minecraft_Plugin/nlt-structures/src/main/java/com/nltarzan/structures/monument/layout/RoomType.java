package com.nltarzan.structures.monument.layout;

/**
 * Every room the generator can place, with its vanilla name, size and structure template
 * (under structures/monument/maze/). Sizes are in units, X x Z x Y like the template names;
 * the 2-wide rooms can also be rotated to run along Z.
 */
public enum RoomType {
    CORE('C', "room/2x2x2_core"),
    ENTRY('E', "room/1x1x1_start"),
    /** Vanilla DXY: two hollow pillars. */
    BIG_PIPES('P', "room/2x1x2_pipes"),
    /** Vanilla DYZ: 2x2 dark prismarine pillar. */
    BIG_DARK('D', "room/2x1x2_dark"),
    /** Vanilla DX: U-shaped. */
    HALL_U('U', "room/2x1x1_u"),
    /** Vanilla DZ: open center with sea lanterns. */
    HALL_LANTERN('L', "room/2x1x1_lantern"),
    /** Vanilla DY. */
    TALL('T', "room/1x1x2"),
    SPONGE('S', "room/1x1x1_sponge"),
    /** Plus-shaped small room. */
    SMALL_LANTERNS('l', "room/1x1x1_lanterns"),
    /** Four-pillar small room, can get a center pillar. */
    SMALL_PILLARS('i', "room/1x1x1_pillars"),
    /** Dark prismarine small room, can get a center pillar. */
    SMALL_DARK('d', "room/1x1x1_dark");

    private final char label;
    private final String template;

    RoomType(char label, String template) {
        this.label = label;
        this.template = template;
    }

    /** One-letter label used by {@link LayoutPrinter}. */
    public char label() {
        return label;
    }

    public String template() {
        return template;
    }
}
