/**
 * Copyright (c) 2017-2018 the original author or authors
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

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.devtools.DeveloperRoutingDescriptor.RouteConstraint;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.niws.loadbalancer.DiscoveryEnabledServer;

import io.jmnarloch.spring.cloud.ribbon.api.RibbonFilterContext;
import io.jmnarloch.spring.cloud.ribbon.predicate.DiscoveryEnabledPredicate;
import io.jmnarloch.spring.cloud.ribbon.support.RibbonFilterContextHolder;

/**
 * Use the developer routing object to work out whether servers should match.
 *
 * @author Andy Clement
 * @see DiscoveryEnabledPredicate
 */
public class BaggageAwarePredicate extends DiscoveryEnabledPredicate {

	private static Logger log = LoggerFactory.getLogger(BaggageAwarePredicate.class);
	
	public BaggageAwarePredicate() {
	}

	// Examples:
	// server.toString = 192.168.1.34:61547 
	// server.getInstanceInfo().getAppName() == B
	// server.getInstanceInfo().getMetadata() == {foo=bar}
	// RibbonFilterContextHolder.getCurrentContext().getAttributes() == 
	//     [devtools={"id":"Andy","routeConfigs":[{"serviceRegex":"c","keyValueMetadataRegex":"foo=bar"}]}]

	@Override
    protected boolean apply(DiscoveryEnabledServer server) {
        RibbonFilterContext context = RibbonFilterContextHolder.getCurrentContext();
        String jsonDevInfo = context.getAttributes().get("devtools");
        if (jsonDevInfo == null) {
	        	// no constraints, accept server
        		final Map<String, String> metadata = server.getInstanceInfo().getMetadata();
        		if (Boolean.valueOf(metadata.getOrDefault("needsExplicitRouting", "false")) == true) {
				log.info("ignoring server {} since it specifies the need for explicit routing: {}", server,metadata);
				return false;
			}
	        	return true;
        } else {
	        	// Deserialize constraints
	        	DeveloperRoutingDescriptor drd = null;
			try {
	        		ObjectMapper mapper = new ObjectMapper();
				drd = mapper.readValue(jsonDevInfo, DeveloperRoutingDescriptor.class);
			} catch (IOException e) {
				log.error("Unable to deserialize developer info", e);
				return true;
			}
			log.info("verifying server constraints: {}",drd);
			
			String appname = server.getInstanceInfo().getAppName();
			// Is there a route constraint for this service
			String serviceName = server.getInstanceInfo().getAppName();
	        final Map<String, String> metadata = server.getInstanceInfo().getMetadata();
	        log.info("server for app {} metadata is {}",appname, metadata);
	        // System.out.println("BAP: Service metadata "+metadata);
			for (RouteConstraint rc: drd.getRouteConstraints()) {
				String serviceNameConstraint = rc.getServiceName();
				if (serviceNameConstraint == null || serviceNameConstraint.equalsIgnoreCase(serviceName)) {
					// Constraint applies to this service, let's check it
			        final Set<Map.Entry<String, String>> attributes = Collections.unmodifiableSet(rc.getMetadataConstraints().entrySet());
					if (metadata.entrySet().containsAll(attributes)) {
						log.info("server for app {} accepted {} - contraints matched", appname, server);
						return true;
					} else {
						log.info("server for app {} rejected {} - contraints not matched", appname, server);
						return false;
					}
				}
			}
			// If there are no route constraints, ignore anything marked as requiring explicit selection
			if (drd.getRouteConstraints().length == 0) {
				if (Boolean.valueOf(metadata.getOrDefault("needsExplicitRouting", "false")) == true) {
					log.info("ignoring server {} since specifies the need for explicit routing", server);
					return false;
				}
			}
			// By default, accept this service
			return true;
        }		
    }
}
