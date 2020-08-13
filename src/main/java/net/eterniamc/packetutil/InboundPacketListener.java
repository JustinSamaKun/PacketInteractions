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
 *
 * @param <T> The type of the packet (Packet or IMessage)
 */
public class InboundPacketListener<T> extends SimpleChannelInboundHandler<T> {

    public InboundPacketListener() {
        super(false);
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected void channelRead0(ChannelHandlerContext ctx, T msg) {
        if (PacketInteractionController.INSTANCE.interactableEvents.containsKey(msg.getClass())) {
            for (Predicate predicate : PacketInteractionController.INSTANCE.interactableEvents.get(msg.getClass())) {
                if (predicate.test(msg)) {
                    return;
                }
            }
        } else if (PacketInteractionController.INSTANCE.observedEvents.containsKey(msg.getClass())) {
            FMLCommonHandler.instance().getMinecraftServerInstance().addScheduledTask(() ->
                PacketInteractionController.INSTANCE.observedEvents.get(msg.getClass()).forEach(task -> task.accept(msg))
            );
        }
        ctx.fireChannelRead(msg);
    }
}
