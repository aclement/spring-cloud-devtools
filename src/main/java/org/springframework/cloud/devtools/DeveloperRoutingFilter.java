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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import io.jmnarloch.spring.cloud.ribbon.support.RibbonFilterContextHolder;

/**
 * 
 * 
 * @author Andy Clement
 */
@Component
public class DeveloperRoutingFilter extends GenericFilterBean {

	private static Logger log = LoggerFactory.getLogger(DeveloperRoutingFilter.class);
	
	public Tracer tracer;
	
	private DeveloperRoutingDescriptor developerRoutingDescriptor;
	
	DeveloperRoutingFilter(Tracer tracer, DeveloperRoutingDescriptor developerRoutingDescriptor) {
		log.info("Filter created, developer routing descriptor = {}",developerRoutingDescriptor);
		this.tracer = tracer;
		this.developerRoutingDescriptor = developerRoutingDescriptor;
	}
	
	private String jsonVersionOfDeveloperRoutingDescriptor(DeveloperRoutingDescriptor drd) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
			String json = mapper.writeValueAsString(drd);
			// {"id":"Andy","routeConstraints":[{"serviceName":"b","metadataConstraints":{"foo":"bar"}}]}
			return json;
		} catch (JsonProcessingException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
			throws IOException, ServletException {
		if (!(servletRequest instanceof HttpServletRequest) || !(servletResponse instanceof HttpServletResponse)) {
			throw new ServletException("Filter just supports HTTP requests");
		}
		DeveloperRoutingDescriptor drd = this.developerRoutingDescriptor;
		HttpServletRequest request = (HttpServletRequest) servletRequest;
		HttpServletResponse response = (HttpServletResponse) servletResponse;
		
		String inputHeaderRoutingConfig = request.getHeader("DEVELOPER-ROUTE"); // service:key=value
		
		Span currentSpan = tracer.getCurrentSpan();
		String devtoolsBaggage = currentSpan.getBaggageItem("devtools");
		log.info("Incoming request has routing info? {}", devtoolsBaggage != null);

		log.info("Current active span: "+currentSpan);

		// If we don't have a developer routing descriptor but there is a header, build one from 
		// the header
		if (drd == null && inputHeaderRoutingConfig !=null) {
			log.info("building developer routing descriptor from incoming header {}",inputHeaderRoutingConfig);
			int colon = inputHeaderRoutingConfig.indexOf(":");
			int equalsPos = inputHeaderRoutingConfig.indexOf("=");
			drd = new DeveloperRoutingDescriptor();
			Map<String,String> m = new HashMap<>();
			m.put(inputHeaderRoutingConfig.substring(colon+1,equalsPos), inputHeaderRoutingConfig.substring(equalsPos+1));
			drd.addRouteConfig(inputHeaderRoutingConfig.substring(0,colon), m);
		}

		if (currentSpan !=null && drd != null) {
			log.info("adding routing info to span {} {}",currentSpan,drd);
			currentSpan.setBaggageItem("devtools",jsonVersionOfDeveloperRoutingDescriptor(drd));
		}

		// Tell the ribbon filter about the routing descriptor it should use to make decisions
		if (currentSpan != null) {
			String inflightDeveloperRoutingDescriptor = currentSpan.getBaggageItem("devtools");
			log.info("attaching routing constraints to ribbon {}",inflightDeveloperRoutingDescriptor);
			RibbonFilterContextHolder.getCurrentContext().add("devtools", inflightDeveloperRoutingDescriptor);
		}
		chain.doFilter(request, response);
	}

//	private String toHeaderNames(Enumeration<String> headerNames) {
//		StringBuilder s= new StringBuilder();
//		while (headerNames.hasMoreElements()) {
//			s.append(headerNames.nextElement());
//			s.append(" ");
//		}
//		return s.toString().trim();
//	}
	
}