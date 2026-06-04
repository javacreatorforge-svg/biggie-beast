package com.redstonedev.biggiebeast.entity;

import net.minecraft.world.damagesource.DamageSource;

public final class ModDamage {
    private ModDamage() {}
    public static final DamageSource CASEOH_EAT =
            new DamageSource("caseoh_eat") {}.bypassArmor().bypassMagic().bypassInvul();
}
