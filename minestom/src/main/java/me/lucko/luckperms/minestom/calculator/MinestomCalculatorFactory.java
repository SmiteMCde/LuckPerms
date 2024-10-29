package me.lucko.luckperms.minestom.calculator;

import me.lucko.luckperms.common.cacheddata.CacheMetadata;
import me.lucko.luckperms.common.calculator.CalculatorFactory;
import me.lucko.luckperms.common.calculator.PermissionCalculator;
import me.lucko.luckperms.common.calculator.processor.*;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.config.LuckPermsConfiguration;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import net.luckperms.api.query.QueryOptions;

import java.util.ArrayList;
import java.util.List;

public final class MinestomCalculatorFactory implements CalculatorFactory {

    private final LuckPermsPlugin plugin;
    private final LuckPermsConfiguration configuration;

    public MinestomCalculatorFactory(LuckPermsPlugin plugin, LuckPermsConfiguration configuration) {
        this.plugin = plugin;
        this.configuration = configuration;
    }

    @Override
    public PermissionCalculator build(QueryOptions queryOptions, CacheMetadata metadata) {
        List<PermissionProcessor> processors = new ArrayList<>(4); // todo: add initial capacity

        processors.add(new DirectProcessor());
        if (this.configuration.get(ConfigKeys.APPLYING_REGEX)) processors.add(new RegexProcessor());
        if (this.configuration.get(ConfigKeys.APPLYING_WILDCARDS)) processors.add(new WildcardProcessor());
        if (this.configuration.get(ConfigKeys.APPLYING_WILDCARDS_SPONGE)) processors.add(new SpongeWildcardProcessor());

        return new PermissionCalculator(this.plugin, metadata, processors);
    }
}
