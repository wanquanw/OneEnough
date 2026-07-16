package cc.sighs.oneenoughitem.event;

import cc.sighs.oelib.neoforge.data.DataManager;
import cc.sighs.oelib.neoforge.event.DataReloadEvent;
import cc.sighs.oneenoughitem.Oneenoughitem;
import cc.sighs.oneenoughitem.data.Replacements;
import cc.sighs.oneenoughitem.event.base.AbstractReplacementEventHandler;
import cc.sighs.oneenoughitem.init.ItemReplacementCache;
import cc.sighs.oneenoughitem.init.config.OEIConfig;
import cc.sighs.oneenoughitem.util.Utils;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.util.Optional;

@EventBusSubscriber(modid = Oneenoughitem.MODID)
public class ServerEventHandler {

    private static final Handler HANDLER = new Handler();

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        var server = event.getServer();
        var registryLookup = server.registryAccess().lookupOrThrow(Registries.ITEM);
        HANDLER.rebuildReplacementCache("oei-server-start", DataManager.get(Replacements.class), registryLookup);
    }

    @SubscribeEvent
    public static void onDataReload(DataReloadEvent event) {
        if (event.isDataType(Replacements.class)) {
            var server = DataManager.getCurrentServer();
            var registryLookup = server != null ? server.registryAccess().lookupOrThrow(Registries.ITEM) : null;

            HANDLER.rebuildReplacementCache("server-data-reload", DataManager.get(Replacements.class), registryLookup);

            Oneenoughitem.LOGGER.info("Server replacement cache rebuilt due to data reload: {} entries loaded, {} invalid",
                    event.getLoadedCount(), event.getInvalidCount());
            ItemReplacementCache.endReloadOverride();
        }
    }

    private static class Handler extends AbstractReplacementEventHandler<Item> {
        @Override
        protected void clearModuleCache() {
            ItemReplacementCache.clearCache();
        }

        @Override
        protected void putToModuleCache(Replacements r, HolderLookup.RegistryLookup<Item> registryLookup) {
            ItemReplacementCache.putReplacement(buildReplacements(r), registryLookup);
        }

        @Override
        protected boolean tryResolveData(String id, HolderLookup.RegistryLookup<Item> registryLookup) {
            return Utils.getItemById(id) != null;
        }

        @Override
        protected boolean tryResolveTag(String tagId, HolderLookup.RegistryLookup<Item> registryLookup) {
            return Utils.isTagExists(Identifier.parse(tagId), registryLookup);
        }

        @Override
        protected Replacements buildReplacements(Replacements r) {
            var dr = OEIConfig.get();
            if (r.rules().isEmpty() && dr != null) {
                return new Replacements(r.match(), r.result(), Optional.of(dr.defaultRules().toRules()));
            }
            return r;
        }

        @Override
        protected boolean acceptLocation(Identifier location) {
            return "oei".equals(location.getNamespace());
        }
    }
}
