package com.hbm.render.item.weapon.sedna;

import com.hbm.interfaces.AutoRegister;
import com.hbm.main.ResourceManager;

@AutoRegister(item = "gun_minigun_sedna")
public class ItemRenderMinigunSedna extends ItemRenderMinigun {

	public ItemRenderMinigunSedna() {
		super(ResourceManager.minigun_tex);
	}
}