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

import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulate description of a route a call wants to make through a mesh of services. Not all
 * fields are used right now, just exploring the options...
 * 
 * @author Andy Clement
 */
public class DeveloperRoutingDescriptor {

	private String id;
	
	private RouteConstraint[] routeConstraints;
	
	static class RouteConstraint {
		
		public RouteConstraint() {}
		
		public RouteConstraint(String serviceName, Map<String,String> metadataConstraints) {
			this.setServiceName(serviceName);
			this.setMetadataConstraints(metadataConstraints);
		}

		/**
		 * Which service names the metadata constraints apply to. If null contraints apply
		 * to all.
		 */
		private String serviceName;

		/**
		 * Constraints on the metadata on the service registry entry. All must match.
		 */
		private Map<String,String> metadataConstraints;
		
		// Would be nice to even select particular URLs but that data isn't available in
		// current place where decisions are made (ribbon instance selector)
//		private String[] urlPatterns; ??
		
		public String toString() {
			return "RouteConfig:serviceName="+getServiceName()+" metadataConstraints="+getMetadataConstraints();
		}

		public String getServiceName() {
			return serviceName;
		}

		public void setServiceName(String serviceRegex) {
			this.serviceName = serviceRegex;
		}

		public Map<String,String> getMetadataConstraints() {
			return metadataConstraints;
		}

		public void setMetadataConstraints(Map<String,String> metadataConstraints) {
			this.metadataConstraints = metadataConstraints;
		}
	}
	
	public DeveloperRoutingDescriptor() {
	}
	
	public DeveloperRoutingDescriptor(String id) {
		this.id = id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getId() {
		return this.id;
	}
	
	public RouteConstraint[] getRouteConstraints() {
		return this.routeConstraints;
	}
	
	public void setRouteConfigs(RouteConstraint[] routeConstraints) {
		this.routeConstraints = routeConstraints;
	}
	
	/**
	 * Should be able to join up constraint objects - probably report errors on
	 * inconsistencies/incompatibilities.
	 */
	public void merge(DeveloperRoutingDescriptor drd) {
		// assert this.id = drd.id
	}

	/**
	 * Add a route constraint to this routing descriptor.
	 * @param serviceName the specific service to which the constraint applies or null for all
	 * @param keyValueMetadataConstraints "a=b" style list of constraints on service registry metadata for services.
	 */
	public void addRouteConstraint(String serviceName, String... keyValueMetadataConstraints) {
		Map<String,String> map = new HashMap<>();
		for (int i=0;i<keyValueMetadataConstraints.length;i++) {
			String kv = keyValueMetadataConstraints[i];
			int index = kv.indexOf('=');
			if (index != -1) {
				map.put(kv.substring(0,index), kv.substring(index+1));
			}
		}
		addRouteConfig(serviceName,map);
	}

	public void addRouteConfig(String serviceName, Map<String,String> metadataConstraints) {
		RouteConstraint rc = new RouteConstraint(serviceName, metadataConstraints);
		if (routeConstraints == null) {
			routeConstraints = new RouteConstraint[] {rc};
		} else {
			RouteConstraint[] newRouteConfigs = new RouteConstraint[routeConstraints.length+1];
			System.arraycopy(routeConstraints, 0, newRouteConfigs, 0, routeConstraints.length);
			newRouteConfigs[routeConstraints.length] = rc;
			routeConstraints = newRouteConfigs;
		}
	}
	
	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append("DeveloperRoutingDescriptor(#").append(id).append(")");
		for (int i=0;i<routeConstraints.length;i++) {
			s.append("[RC#").append(i).append(":");
			s.append(routeConstraints[i].getServiceName()).append(":").append(routeConstraints[i].getMetadataConstraints());
			s.append("]");
		}
		return s.toString();
	}

}
