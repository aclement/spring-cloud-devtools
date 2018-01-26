/**
 * Copyright (c) 2018 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.devtools;

import io.jmnarloch.spring.cloud.ribbon.predicate.DiscoveryEnabledPredicate;
import io.jmnarloch.spring.cloud.ribbon.predicate.MetadataAwarePredicate;
import io.jmnarloch.spring.cloud.ribbon.rule.DiscoveryEnabledRule;

/**
 * A baggage aware {@link DiscoveryEnabledRule} implementation.
 *
 * @author Andy Clement
 * @see DiscoveryEnabledRule
 * @see MetadataAwarePredicate
 */
public class BaggageAwareRule extends DiscoveryEnabledRule {

	public BaggageAwareRule() {
        this(new BaggageAwarePredicate());
    }

    /**
     * Creates new instance of {@link BaggageAwareRule} with specific predicate.
     *
     * @param predicate the predicate, can't be {@code null}
     * @throws IllegalArgumentException if predicate is {@code null}
     */
    public BaggageAwareRule(DiscoveryEnabledPredicate predicate) {
        super(predicate);
    }

}
