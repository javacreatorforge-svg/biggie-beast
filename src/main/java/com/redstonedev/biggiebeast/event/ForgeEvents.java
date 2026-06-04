package com.redstonedev.biggiebeast.event;

import com.redstonedev.biggiebeast.init.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;
import java.util.Random;

public class ForgeEvents {
    private static final Random RNG = new Random();
    private int tickCounter = 0;

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer() == null) return;
        tickCounter++;
        if (tickCounter % 100 != 0) return; // ~5s
        for (ServerLevel level : event.getServer().getAllLevels()) trySpawn(level);
    }

    private boolean caseohExists(ServerLevel level) {
        return !level.getEntities(ModEntities.CASEOH.get(), e -> e.isAlive()).isEmpty();
    }

    private void trySpawn(ServerLevel level) {
        List<? extends ServerPlayer> players = level.players();
        if (players.isEmpty() || caseohExists(level)) return;
        for (ServerPlayer player : players) {
            boolean day = level.isDay();
            // Rare. Often in day, sometimes at night.
            int chance = day ? 900 : 1800;
            if (RNG.nextInt(chance) != 0) continue;
            BlockPos pos = pickSpawnPos(level, player);
            if (pos == null) continue;
            CaseOhSpawn(level, pos);
            return;
        }
    }

    private void CaseOhSpawn(ServerLevel level, BlockPos pos) {
        net.minecraft.world.entity.Mob mob = ModEntities.CASEOH.get().create(level);
        if (mob == null) return;
        mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360F, 0);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
        level.addFreshEntity(mob);
    }

    private BlockPos pickSpawnPos(ServerLevel level, ServerPlayer player) {
        BlockPos origin = player.blockPosition();
        for (int attempt = 0; attempt < 24; attempt++) {
            int x = origin.getX() + (RNG.nextInt(40) - 20);
            int z = origin.getZ() + (RNG.nextInt(40) - 20);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            BlockPos c = new BlockPos(x, y, z);
            boolean floor = !level.getBlockState(c.below()).getCollisionShape(level, c.below()).isEmpty();
            if (floor && level.getBlockState(c).getCollisionShape(level, c).isEmpty()) {
                double d = c.distSqr(origin);
                if (d > 144 && d < 2500) return c; // 12-50 blocks away
            }
        }
        return null;
    }
}
