/*
 * Copyright 2015 Palantir Technologies
 *
 * Licensed under the BSD-3 License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.palantir.atlasdb.table.description.render;

import com.palantir.atlasdb.table.description.NameComponentDescription;

class ComponentRenderers {
    private ComponentRenderers() {
        // cannot instantiate
    }

    static String typeName(NameComponentDescription comp) {
        return comp.getType().getJavaClassName();
    }

    static String TypeName(NameComponentDescription comp) {
        return comp.getType().getJavaObjectClassName();
    }

    static String varName(NameComponentDescription comp) {
        return Renderers.camelCase(comp.getComponentName());
    }

    static String VarName(NameComponentDescription comp) {
        return Renderers.CamelCase(comp.getComponentName());
    }
}
