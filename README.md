# spring-cloud-devtools

An experiment in what we might do to help developers working with Spring Cloud apps.

This currently features two things:

- There is support for a developer controller routing header/bean.  By publishing service registries entries
with particular metadata we can use the routing header or bean to specify a specific route we should
take through a service call chain. This based on custom headers added with spring sleuth.

- There is support for tunneling to a special type of Proxy app that can be published to CF that will stand
in for any spring boot app. By setting the name of the target app in a property, some autoconfig will kick
in to setup the ssh tunnels necessary to support this. This makes it very easy to run a local app that is
exposed via cloud foundry.  I am still wondering if the tunnels should be done here or via a `cf cli` extension.

