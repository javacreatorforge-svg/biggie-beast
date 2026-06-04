package com.redstonedev.biggiebeast.network;

import com.redstonedev.biggiebeast.BiggieBeast;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.Supplier;

public class PacketHandler {
    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(BiggieBeast.MODID, "main"),
            () -> VERSION, VERSION::equals, VERSION::equals);
    private static int nextId = 0;

    public static void register() {
        CHANNEL.registerMessage(nextId++, ShakePacket.class,
                ShakePacket::encode, ShakePacket::decode, ShakePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    /** Tells the client to shake the screen for a few ticks (CaseOh stomping nearby). */
    public static class ShakePacket {
        public final int ticks;
        public final float intensity;
        public ShakePacket(int ticks, float intensity) { this.ticks = ticks; this.intensity = intensity; }
        public static void encode(ShakePacket p, FriendlyByteBuf b) { b.writeInt(p.ticks); b.writeFloat(p.intensity); }
        public static ShakePacket decode(FriendlyByteBuf b) { return new ShakePacket(b.readInt(), b.readFloat()); }
        public static void handle(ShakePacket p, Supplier<net.minecraftforge.network.NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                    net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> () -> com.redstonedev.biggiebeast.client.ShakeState.add(p.ticks, p.intensity)));
            ctx.get().setPacketHandled(true);
        }
    }
}
