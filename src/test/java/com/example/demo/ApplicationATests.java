//package com.example.demo;
//
//import static org.junit.Assert.assertEquals;
//
//import java.util.List;
//
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.context.TestConfiguration;
//import org.springframework.boot.test.web.client.TestRestTemplate;
//import org.springframework.cloud.client.loadbalancer.LoadBalanced;
//import org.springframework.cloud.devtools.DeveloperRoutingDescriptor;
//import org.springframework.context.annotation.Bean;
//import org.springframework.http.client.ClientHttpRequestInterceptor;
//import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
//import org.springframework.web.client.RestTemplate;
//
////@RunWith(SpringRunner.class)
////@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,classes= {ApplicationA.class, RoutingConfig.class})
////@ContextConfiguration(loader=AnnotationConfigContextLoader.class)
//@RunWith(SpringJUnit4ClassRunner.class)
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//public class ApplicationATests {
//
//	@TestConfiguration
//	static class RoutingConfig {
//		@Bean
//		DeveloperRoutingDescriptor me() {
//			DeveloperRoutingDescriptor drd = new DeveloperRoutingDescriptor();
//			drd.addRouteConstraint("c","user=kryten");
//			return drd;
//		}
//	}
//	
//	@Autowired
//	TestRestTemplate restTemplate;
//	
//	@Test
//	public void test1() {
////		List<ClientHttpRequestInterceptor> interceptors = rt.getInterceptors();
////		System.out.println("RT in test: "+System.identityHashCode(rt));
//		String response = restTemplate.getForObject("http://devex-appa.cfapps.io/abc", String.class);
//		assertEquals("CCCCBBBBAAAA",response);
//	}
//	
//}
