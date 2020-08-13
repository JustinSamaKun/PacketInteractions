package net.eterniamc.packetutil;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.util.function.Predicate;

/**
 * Created by Justin
 *
 * Used to quickly intercept packets and send them to listeners while passing on the message to other handlers if it
 * was not cancelled.
 */
@SuppressWarnings("rawtypes")
public class InboundPacketListener extends SimpleChannelInboundHandler {

    public InboundPacketListener() {
        super(false);
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        for (Class<?> type : PacketInteractionController.INSTANCE.interactableEvents.keySet()) {
            if (type.isAssignableFrom(msg.getClass())) {
                for (Predicate predicate : PacketInteractionController.INSTANCE.interactableEvents.get(type)) {
                    if (predicate.test(msg)) {
                        return;
                    }
                }
            }
        }
        FMLCommonHandler.instance().getMinecraftServerInstance().addScheduledTask(() ->
                PacketInteractionController.INSTANCE.observedEvents.keySet().stream()
                        .filter(c -> c.isAssignableFrom(msg.getClass()))
                        .flatMap(c -> PacketInteractionController.INSTANCE.observedEvents.get(msg.getClass()).stream())
                        .forEach(task -> task.accept(msg))
        );
        ctx.fireChannelRead(msg);
    }
}
