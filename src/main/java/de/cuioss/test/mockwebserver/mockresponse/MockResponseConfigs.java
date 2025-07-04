/**
 * Copyright © 2025 CUI-OpenSource-Software (info@cuioss.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.cuioss.test.mockwebserver.mockresponse;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container annotation for {@link MockResponseConfig} to support repeatable annotations.
 * <p>
 * This annotation is automatically used by the Java compiler when multiple {@link MockResponseConfig}
 * annotations are applied to the same element.
 *
 * @author Oliver Wolff
 * @since 1.1
 * @see MockResponseConfig
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Documented
public @interface MockResponseConfigs {

    /**
     * The contained {@link MockResponseConfig} annotations.
     *
     * @return an array of {@link MockResponseConfig} annotations
     */
    MockResponseConfig[] value();
}
