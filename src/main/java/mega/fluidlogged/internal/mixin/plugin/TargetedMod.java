/*
 * This file is part of FluidLogged.
 * Copyright (C) 2025 The MEGA Team, FalsePattern
 * All Rights Reserved
 * The above copyright notice, this permission notice and the word "MEGA"
 * shall be included in all copies or substantial portions of the Software.
 * FluidLogged is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, only version 3 of the License.
 * FluidLogged is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with FluidLogged. If not, see <https://www.gnu.org/licenses/>.
 */

package mega.fluidlogged.internal.mixin.plugin;

import static com.falsepattern.lib.mixin.ITargetedMod.PredicateHelpers.contains;

import java.util.function.Predicate;

import com.falsepattern.lib.mixin.ITargetedMod;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum TargetedMod implements ITargetedMod {

    // Angelica 2.2.45+ renders FluidLogged fluids natively in the Celeritas mesher, so the
    // legacy renderer mixin must not run there or the stored fluid is drawn twice.
    ANGELICA_LEGACY("Angelica 2.1-2.2.44", false,
        name -> name.matches("angelica-2\\.1\\..*|angelica-2\\.2\\.(?:[0-9]|[1-3][0-9]|4[0-4])(?:[.-].*)?")),
    ANGELICA_2_2_40("Angelica 2.2.40", false, name -> name.matches("angelica-2\\.2\\.40(?:[.-].*)?")),
    COFHCORE("CoFH Core", false, contains("cofhcore")),;

    @Getter
    private final String modName;
    @Getter
    private final boolean loadInDevelopment;
    @Getter
    private final Predicate<String> condition;
}
