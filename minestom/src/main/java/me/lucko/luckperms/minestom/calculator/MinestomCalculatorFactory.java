/*
 * This file is part of LuckPerms, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

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