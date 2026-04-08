package com.militopia.data;

public class StructureData {
    public int x;
    public int y;
    public int owner;
    public int level;
    public float currentBaseXP;
    public String baseName;
    public String baseOrdinal;
    public int xpGain;
    public String chosenSuperUnit;
    public int parentBaseX = -1;
    public int parentBaseY = -1;
    public String unitTypeKey;

    // Default constructor for JSON
    public StructureData() {
    }

    public StructureData(int x, int y, int owner, int level, float currentBaseXP, String baseName, String baseOrdinal,
            int xpGain, String chosenSuperUnit) {
        this.x = x;
        this.y = y;
        this.owner = owner;
        this.level = level;
        this.currentBaseXP = currentBaseXP;
        this.baseName = baseName;
        this.baseOrdinal = baseOrdinal;
        this.xpGain = xpGain;
        this.chosenSuperUnit = chosenSuperUnit;
    }
}
