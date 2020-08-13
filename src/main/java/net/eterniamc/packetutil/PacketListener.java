package net.eterniamc.packetutil;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface PacketListener {

    /**
     * Set to true only if you need to edit variables in the packet or cancel it entirely. Note that methods with this
     * annotation set to true are called on the IO threads so make sure that methods with this option set to true run
     * very fast.
     *
     * @return whether the annotated method should be able to interact with packets
     */
    boolean interactable() default false;
}
