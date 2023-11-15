/*
 * CarbonChat
 *
 * Copyright (c) 2023 Josua Parks (Vicarious)
 *                    Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.draycia.carbon.common.messages.placeholders;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.moonshine.placeholder.ConclusionValue;
import net.kyori.moonshine.placeholder.ContinuanceValue;
import net.kyori.moonshine.placeholder.IPlaceholderResolver;
import net.kyori.moonshine.util.Either;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
public class ComponentPlaceholderResolver<R> implements IPlaceholderResolver<R, Component, Tag> {

    @Override
    public @Nullable Map<String, Either<ConclusionValue<? extends Tag>, ContinuanceValue<?>>> resolve(
        final String placeholderName,
        final Component value,
        final R receiver,
        final Type owner,
        final Method method,
        final @Nullable Object[] parameters
    ) {
        return Map.of(placeholderName, Either.left(ConclusionValue.conclusionValue(Tag.selfClosingInserting(value))));
    }

}
