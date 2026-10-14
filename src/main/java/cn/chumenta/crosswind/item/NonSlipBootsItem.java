package cn.chumenta.crosswind.item;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;

public class NonSlipBootsItem extends ArmorItem {
    public NonSlipBootsItem(final Item.Properties properties) {
        super(cn.chumenta.crosswind.registry.CrosswindRegistry.NON_SLIP_ARMOR_MATERIAL,
                ArmorItem.Type.BOOTS, properties);
    }

    @Override
    public void appendHoverText(final net.minecraft.world.item.ItemStack stack,
                                final net.minecraft.world.item.Item.TooltipContext context,
                                final java.util.List<net.minecraft.network.chat.Component> tooltip,
                                final net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(net.minecraft.network.chat.Component
                .translatable("item.crosswind.non_slip_boots.tooltip")
                .withStyle(net.minecraft.ChatFormatting.GRAY));
    }

    public static boolean isWornBy(final Entity entity) {
        return entity instanceof final LivingEntity living
                && living.getItemBySlot(EquipmentSlot.FEET).getItem()
                        instanceof NonSlipBootsItem;
    }
}
