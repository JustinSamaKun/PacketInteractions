package net.eterniamc.packetutil;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.Packet;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.network.FMLEmbeddedChannel;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.relauncher.Side;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

public enum PacketInteractionController {
    INSTANCE;

    @SuppressWarnings("rawtypes")
    protected final Map<Class<?>, List<Predicate>> interactableEvents = Maps.newConcurrentMap();
    @SuppressWarnings("rawtypes")
    protected final Map<Class<?>, List<Consumer>> observedEvents = Maps.newConcurrentMap();

    @SuppressWarnings("unused")
    public void initialize() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        player.connection.netManager.channel().pipeline().addBefore("fml:packet_handler", "packet_listener",  new InboundPacketListener<Packet<?>>());
        for (String name : NetworkRegistry.INSTANCE.channelNamesFor(Side.SERVER)) {
            FMLEmbeddedChannel channel = NetworkRegistry.INSTANCE.getChannel(name, Side.SERVER);
            channel.pipeline().addAfter(channel.pipeline().names().get(1), "message_listener", new InboundPacketListener<IMessage>());
        }
    }

    /** Takes in either a class or an object */
    @SuppressWarnings("unused")
    public void register(Object listener) {
        if (listener instanceof Class) {
            for (Method method : ((Class<?>) listener).getDeclaredMethods()) {
                if (method.isAnnotationPresent(PacketListener.class) && Modifier.isStatic(method.getModifiers())) {
                    method.setAccessible(true);
                    Class<?> type = method.getParameterTypes()[0];
                    PacketListener annotation = method.getDeclaredAnnotation(PacketListener.class);
                    if (annotation.interactable()) {
                        interactableEvents.computeIfAbsent(type, t -> Lists.newArrayList()).add(msg -> {
                            try {
                                return method.invoke(null, msg) == Boolean.TRUE;
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            return false;
                        });
                    } else {
                        observedEvents.computeIfAbsent(type, t -> Lists.newArrayList()).add(msg -> {
                            try {
                                method.invoke(null, msg);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    }
                }
            }
        } else {
            for (Method method : listener.getClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(PacketListener.class)) {
                    method.setAccessible(true);
                    Class<?> type = method.getParameterTypes()[0];
                    PacketListener annotation = method.getDeclaredAnnotation(PacketListener.class);
                    if (annotation.interactable()) {
                        interactableEvents.computeIfAbsent(type, t -> Lists.newArrayList()).add(msg -> {
                            try {
                                return method.invoke(listener, msg) == Boolean.TRUE;
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            return false;
                        });
                    } else {
                        observedEvents.computeIfAbsent(type, t -> Lists.newArrayList()).add(msg -> {
                            try {
                                method.invoke(listener, msg);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    }
                }
            }
        }
    }
}
