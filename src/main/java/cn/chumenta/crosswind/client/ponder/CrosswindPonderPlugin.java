package cn.chumenta.crosswind.client.ponder;

import cn.chumenta.crosswind.Crosswind;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

public final class CrosswindPonderPlugin implements PonderPlugin {
    @Override
    public String getModId() {
        return Crosswind.MOD_ID;
    }

    @Override
    public void registerScenes(final PonderSceneRegistrationHelper<ResourceLocation> helper) {
        CrosswindPonderScenes.register(helper);
    }

    @Override
    public void registerTags(final PonderTagRegistrationHelper<ResourceLocation> helper) {
        CrosswindPonderTags.register(helper);
    }
}
