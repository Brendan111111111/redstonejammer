package com.redstonejammer.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SetSuppressorNamePayload(BlockPos pos, String name) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SetSuppressorNamePayload> TYPE = 
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("redstonejammer", "set_suppressor_name"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetSuppressorNamePayload> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, SetSuppressorNamePayload::pos,
        ByteBufCodecs.STRING_UTF8, SetSuppressorNamePayload::name,
        SetSuppressorNamePayload::new
    );

    @Override
    public CustomPacketPayload.Type<SetSuppressorNamePayload> type() {
        return TYPE;
    }
}
