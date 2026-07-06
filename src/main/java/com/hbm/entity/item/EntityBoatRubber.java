package com.hbm.entity.item;

import com.hbm.interfaces.AutoRegister;
import com.hbm.items.ModItems;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.item.Item;
import net.minecraft.world.World;

@AutoRegister(name = "entity_boat_rubber")
public class EntityBoatRubber extends EntityBoat {

    public EntityBoatRubber(World world) {
        super(world);
    }

    public EntityBoatRubber(World world, double x, double y, double z) {
        super(world, x, y, z);
    }

    @Override
    public Item getItemBoat() {
        return ModItems.boat_rubber;
    }
}