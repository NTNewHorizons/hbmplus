package com.ecstasy.hbmplus.Recipes;

import com.hbm.items.machine.ItemICFPellet;

public class ICFLaserMulti {
    public static final float multiplier = 5;

    public static void patch() {
        for (ItemICFPellet.EnumICFFuel fuel : ItemICFPellet.EnumICFFuel.values()) {
            fuel.fusingDifficulty /= multiplier;
        }
    }
}