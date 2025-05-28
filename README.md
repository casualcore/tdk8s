# `tdk8s` - Test Driven Kubernetes

Test Driven Kubernetes (`tdk8s`) is designed to simplify the testing of software running on 
kubernetes (`k8s`) compatible platforms.

Too often, running and maintaining tests on `k8s` is arduous, inflexible and brittle.

`tdk8s` is designed to reduce that burden, ensuring that the `k8s` System Under Test (`SUT`)
is exactly as required for your different test scenarios. Providing a simplified,
flexible and stable developer experience for writing and debugging your tests, whilst also speeding 
up the test development feedback loop, by allowing test execution directly from within your IDE.<sup>[*](#connectivity)</sup>

TODO: build status badges etc

## Contents

* [Overview](#overview) 
* [Usage](#usage)
  * [Provisioning](#provisioning)
  * [Connectivity](#connectivity)
* [Advanced Usage](#advanced-usages)
  * [Execute Commands](#execute-commands)
  * [Retrieve Log Files](#retrieve-log-files)
  * [Transfer Files](#transfer-files)
  * [`port-forward`](#connect-via-port-forward) 
  * [Asynchronous Provisioning](#asynchronous-provisioning)
  * [`k8s` Resources](#k8s-resources)

## Overview

The rise of GitOps and CI/CD pipelines has undoubtedly improved configuration management and stability for 
production releases, but it has led to developers experiencing some negative side effects:
* Slow feedback loops.
* Complexity when debugging failing test.
* Vanishing resources and logs after tests complete.
* Lack of flexibility over the `SUT` configuration for testing different test scenarios.
* Onerous maintenance of often verbose manifest files e.g. yaml.

`tdk8s` aims to address these issues:
* Simplicity while developing and maintaining tests.
* Flexibility over creating test scenarios.
* Stability for running tests repeatably and frequently.

### Simplicity

It must be possible to run/re-run CI/CD pipeline tests from the developers local machine/IDE upon the `k8s` `SUT`, without 
modifications to the tests and without needing to manually ensure the `k8s` `SUT` is configured in a specific way prior to 
running the tests.

The test scenarios must be able to define which `k8s` resources are required and their configuration in the `k8s` `SUT`.
It must be possible for this environment to be built from scratch, provisioning all necessary
resources and destroying them all once testing is complete.

If required though, it should also be possible to retain the `k8s` resources provisioned by the test scenario within the 
`k8s` `SUT`, allowing for faster test re-run, test fix iterations, remote debugging etc.

NOTE: `tdk8s` is **NOT** expected to be used for unit testing; it will most likely be too slow due to the time
taken by the target `k8s` platform to provision and remove resources. It is more suited for things like functional 
integration testing.

`tkd8s` simplifies the developer experience for testing your software running on `k8s` by providing
a wrapper around the provisioning and destruction of `k8s` resources. This works whether the tests are run within a 
CI/CD pipeline or from a developers local machine within their IDE.

### Flexibility

There must be flexibility over the `k8s` resources and their configurations in the `k8s` `SUT`, allowing testing of
different test scenarios with different configuration permutations, whilst still ensuring stability over the
test execution and reliability of test results.

The definition and configuration of `k8s` resources must be possible directly from within the test code without needing to
manually maintain handwritten verbose manifest files.

Test must be able to run on any flavour of kubernetes, be that `k3s`, `minikube`, `KinD`, vanilla `k8s`, `openshift`
etc, so long as it is a `k8s` compatible platform and allow remote access via kubectl. This ensures flexibility of 
choice for difference scenarios whilst also making for more robust, stable test scenarios.

`tdk8s` embraces this very same flexibility within its own integration test suite, which is a good place to check for usage
examples.

### Stability

Whilst flexibility is important, it must not come at the sacrifice of test reliability, as this would reduce the benefits 
of GitOps and CI/CD pipelines.

`tdk8s` allows test code to specify which `k8s` resources are required for each test scenario and waits for resources
to become available/ready prior to test execution. Once complete, all `k8s` resources provisioned by the test scenario will be
deleted with test scenarios finishing once this is confirmed.

Using `tdk8s` from within your test code, ensures the `k8s` `SUT` is exactly as your test codes wants it to be, everytime your 
tests are run; before, during and after execution, whether run locally from your development machine or within a CI/CD 
pipeline.

Thereby ensuring stable and repeatable test execution.

## Usage

The entry point for `tdk8s` is the `TestKube`. 

Within each `TestKube` you add the `k8s` resources you wish to be managed (provisioned and destroyed) during
your test(s) and interact with these resources through the `TestKube` facilitated connections.

`tdk8s` utilises the [`fabric8` kubernetes client](https://github.com/fabric8io/kubernetes-client) for `k8s` connectivity and 
defining `k8s` resources. Helper classes should be used to simplify resource creation.

As part of the development of `tdk8s` there are numerous integration tests, which also serve as functional examples
of how to utilise `tdk8s`, see [`tdk8s` integration tests](./test-kube/src/integration) for more.

The majority of test scenarios will be possible with just provisioning/destruction and connectivity functionality.

The following is a simplified example from the `tdk8s` integration tests using the `Spock` test framework to:
* Initialise a `Pod` and `Service` within the `k8s` `SUT`
* Connect to the `Pod` via the `Service`.
* Destroy both the `Service` and `Pod` once complete.

```groovy
class NginxConnectivityIntTest extends Specification
{
    @Shared
    TestKube instance
    @Shared
    Pod pod // Fabric8 kubernetes client API Pod definition.
    @Shared
    Service service // Fabric8 kubernetes client API Service definition.
    
    def setupSpec()
    {

        instance = TestKube.newBuilder()
                .addPod( "nginx-tdk8s", pod )
                .addService( "nginx-service", service )
                .build()

        instance.init()
    }

    def cleanupSpec()
    {
        instance.destroy()
    }
    
    def "Connect to nginx pod via service, confirm result"()
    {
        when:
        int status
        String body
        
        try( KubeConnection connection = instance.getConnection( "nginx-service", 80 ) )
        {
            String host = connection.getHostName()
            int port = connection.getPort()

            HttpClient httpClient = HttpClient.newBuilder(  ).build(  )
            HttpRequest request = HttpRequest.newBuilder( )
                    .uri( URI.create( "http://" + host + ":" + port +"/" ) )
                    .GET( )
                    .build(  )

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            status = response.statusCode(  )
            body = response.body()
        }

        then:
        status == 200
        body != ""
        body.containsIgnoreCase( "nginx" )
    }
    
}
```

### Provisioning

The `TestKube` is used to define which `k8s` resources need to be provisioned and ensures they are available 
prior to running your test scenario by utilising the `init()` and `destroy()` methods respectively.

`k8s` resources are expected to be valid `fabric8` kubernetes java client api resource definitions.
These resources should ideally be created with the use of helper methods to simplify their maintenance and modifications.

The `k8s` resources are added via the `addXXX` methods of the `TestKube` Builder.

```java
TestKube instance = TestKube.newBuilder()
        .addPod( "podAlias", pod )
        .addService( "serviceAlias", service )
        .build();
```
When adding a resource, the `String` provided is used as an alias to the resource, which can be used later when 
interacting with the resources. This helps readability of test code and simplifies access to resources.

See more advanced provisioning options see [here](#asynchronous-provisioning).

### Connectivity

Connectivity to the `k8s` `SUT` is provided through the use of a [`fabric8` kubernetes client](https://github.com/fabric8io/kubernetes-client)
`io.fabric8.kubernetes.client.KubernetesClient` instance.

As a general rule, to ensure connectivity works:
* when running tests locally, ensure your local user's `~/kube/config` allows a connection using `kubectl` to the `k8s` `SUT`.
* when running inside a CI/CD pipeline, ensure that an "appropriate" service account is used to run the container, with credentials mounting enabled.

However, if these default configurations are not appropriate, there are 
[additional fabric8 client configuration options described here](https://github.com/fabric8io/kubernetes-client?tab=readme-ov-file#configuring-the-client) 
which will be honoured.

For example, you can create your own custom instance of `KubernetesClient` and provide it to the `TestKube` 
builder for `tdk8s` to be used for communicating with the `k8s` `SUT`:

```java
TestKube.newBuilder().client( myClient ).build()
```

The `TestKube` object, once created, facilitates establishing a connection to the `k8s` resources via `Service` resources using the `getConnection` 
method which returns `KubeConnection` which contains the `hostname` and `port` you should use for connecting within
your test code.

```java
TestKube instance; // defined.

try( KubeConnection connection: instance.getConnection( "myService", 8080 ) )
{
    String host = connection.getHostName();
    int port = connection.getPort();
    // your test
}
```

When the `getConnection` method is run within the `k8s` environment i.e. inside CI/CD pipelines, it will connect through the 
service. Though when run from outside the `k8s` environment i.e. from a local developer machine / IDE, if the services is not
accessible, it will seamlessly provide a `port-forward` to the service. Allowing the same tests to be run without any
modifications.

Therefore, you should always use the `KubeConnection` object returned to determine the connection details, otherwise seamless
connectivity will not work. `KubeConnection` objects are `AutoCloseable` so should ideally be used within `try-with-resources`
blocks to ensure they are closed correctly.

NOTE: This seamless `port-forward` mechanism has been added to simplify writing and maintaining test code, though `port-forward` 
should **NOT** be used when running performance tests, other than for debugging, as load balancing will **NOT** work as expected.
A log WARNING will be emitted when the seamless `port-forward` mechanism has been used to ensure you are aware. 

## Advanced Usages

The main functionality provided by the `TestKube` is resource management (provisioning and destruction) and connectivity.

The majority of tests should be possible with just this functionality.

However, there are more advanced functions provided via the `KubeController`, which can be accessed 
via the `getController()` method of the `TestKube` object.

This provides the ability to:
* access [managed resources](#access-managed-resources).
* [execute commands](#execute-commands) upon pods.
* [retrieve log files](#retrieve-log-files) from pods.
* [transfer file](#transfer-files) to and from pods.
* Connect via [`port-forward`](#connect-via-port-forward).
* [asynchronous provisioning](#asynchronous-provisioning).
* run [custom provisioning probes](#custom-provisioning-probes).

### Access Managed Resources

Resources that are provisioned and destroyed by `tdk8s` are considered managed resources. These are added to the `TestKube`
with an alias using the respective `addXXX` methods of the builder. 

Managed Resources are stored within the `TestKube` resource store which can be accessed via the `getResourcesStore` method.

```java
Pod p = instance.getResourcesStore().getPod( "my_pod_alias" );
Service s = instance.getResourcesStore().getService( "my_service_alias" );
Deployment d = instance.getResourcesStore().getDeployment( "my_deployment_alias" );
```

When referring to `k8s` resources by name, `tdk8s` will initially look within the `ResourcesStore`. 
Though if there is no match for the alias it will revert back to the `k8s` `SUT` to try and find an 
exact match for the name provided. 

This is provided for convenience to:
* Simplify naming of resources to improve readability in test code. 
* Providing access to a [`Deployment`'s `Pod`s](#pod-aliases).
* Allow tests to be written initially without initial provisioning and destruction.
* Simplify debugging scenarios.

Though ideally all resources required by the test scenario should really be managed resources, otherwise
tests become less portable and likely more brittle over time.

#### Pod aliases

In addition to the above rules for [resource alias matching](#access-managed-resources), functionality related to `Pod`s, 
for example executing commands, is also possible for the `Pod` of a matching `Deployment`.

The search order when matching aliases for a `Pod` is as follows:
* Check resource store for `Pod` matching the alias.
* Check `k8s` `SUT` for a `Pod` with name matching alias.
* Check resource store for `Deployment` matching the alias.
* Check `k8s` `SUT` for matching `Deployment` with the alias.

In the event that a `Pod` match is found, it will be used.
In the event that a `Deployment` match is found, one of the deployments `Pod`s will be used at random, with a WARNING logged
if the deployment has more than 1 `Pod` associated, i.e. multiple replicas.

This is implemented to simplify the developer experience so it works in the majority of scenarios as expected e.g. a `Deployment` with 1 replica.

If you want more control over which `Pod` in a multi replica `Deployment` is used, you can retrieve the `Deployment`'s `Pod`s from
the `TestKube` resource store first, before then calling the function with the exact name of the `Deployment`'s `Pod`.

### Execute Commands

If you need to execute a command against a running pod you can use the following `KubeController` methods:

* `executeCommand` - waits for completion of command.
* `executeCommandAsync` - returns CompletableFuture of command.

The methods return an `ExecResult` object which wraps the `output` and `exitCode`.

Synchronous example:
```java
ExecResult result = instance.getController().executeCommand( podAlias, "sh", "-c", "echo -n hello" );
```

Asynchronous example:
```java
CompletableFuture<ExecResult> future = instance.getController().executeCommand( podAlias, "sh", "-c", "sleep 5;echo -n hello" );
// do your work
ExecResult result = future.join();
```

### Retrieve Log Files

If you need to retrieve the log files from a running pod you can use the following `KubeController` methods:

* `getLog` - retrieve full log as a String. 
* `getLogTail` - retrieve the last n lines of the log file.
* `getLogSince` - retrieve log files lines since the provided time.

Examples:
```java
// Get full log.
String fullLog = instance.getController().getLog( podAlias );

// Get last 10 lines of log. 
String lastTen = instance.getController().getLogTail( podName, 10 );

// Get lines from log after provided date.
ZoneDateTime afterInit = ZonedDateTime.now();
String since = afterInit.format( DateTimeFormatter.ISO_OFFSET_DATE_TIME );
String sinceLog = instance.getController().getLogSince( podAlias, since );
```

### Transfer Files

If you need to transfer files to or from a running pod you can use the following `KubeController` methods:

* `download` - download a file from the pod to local file system.
* `upload` - upload a local file system file to the pod file system.

Both methods return a `boolean` value indicating if the operations was successful once complete.

Examples:
```java
boolean downloadSuccess = instance.getController().download( podAlias, "/tmp/podfile", localDstFile.toPath() );
boolean uploadSuccess = instance.getController().upload( podAlias, "./localfile.txt", podFile.toPath() );
```

### Connect via `port-forward`

You should probably not use this functionality other than for simplifying/enabling remote debugging. Though...

If you wish to connect to a pod via `port-forward` you can use the following `KubeController` method:

* `getPortForwardConnection` 

This can be used for connections to a pod, deployment or service resource. 
The target port is used to determine how to create the `port-forward`, the actual local port used will be 
dynamically assigned.

As with `getConnection` the returned `KubeConnection` object should be used to retrieve `hostname` and `port` and
should be ideally used within a `try-with-resources` block to ensure the connection is closed correctly.

```java
TestKube instance; // defined.

try( KubeConnection connection: instance.getPortForwardConnection( "mypod", 8080 ) )
{
    String host = connection.getHostName();
    int port = connection.getPort();
    // your test
}
```

### Asynchronous Provisioning

The `TestKube`, `init` and `destroy` are both synchronous calls which wait until the resources are all "ready".

Ready is determined based on the `k8s` status and can therefore be controlled by readiness probes where appropriate.

If you wish to perform operations whilst waiting for the managed resources provisioning or deletion you can also use the asynchronous
alternatives available via the `KubeController`:

* `initAsync` - start provision resources, though do not wait for them to be "ready".
* `waitUntilReady` - wait for all provisioned managed resources to be "ready".
* `destroyAsync` - start destruction of all provisioned managed resources, though do not wait for them to be deleted.
* `waitUntilDestroyed` - wait for all provisioned managed resources to be deleted.

Asynchronous initialisation:
```java
TestKube instance; // defined

instance.getController().initAsync();
// do your work.
instance.getController().waitUntilReady();
```

Asynchronous destruction:
```java
TestKube instance; // defined

instance.getController().destroyAsync();
// do your work.
instance.getController().waitUntilDestroyed();
```

### Custom Provisioning Probes

The majority of the time, standard `k8s` readiness probes are sufficient to ensure test execution can begin.
`tdk8s` managed resources are waited upon until ready within the `TestKube` `init()` or `waitUntilReady()` methods.

However, some `k8s` resources do not allow readiness probes. So there can be timing issues, for example with 
newly created `Service` resources, the IP details may not have propagated within the cluster yet, leading to 
occasional initial connection refused responses, resulting in instability with repeated test execution.

To prevent the need for arbitrary `sleep()`s or retries within the test code, a `ProvisioningProbe` mechanism is provided.
These can be added to the `TestKube` as part of the builder as a lambda, executing after the managed resources readiness probes
have all successfully returned.

All registered probes are run during the `init()` or `waitUntilReady()` methods in parallel. Failing probes, those that return 
false, are retried with a backoff until successful or a timeout has occurred.

Probes should not result in side effects and must be able to be run multiple times in the event they do not succeed.
They should also ideally be short running to ensure they do not delay test execution unnecessarily.

Probes are all provided access to the current `TestKube` instance making it possible to use from within during the probe execution.

Calling a service from within another pod using curl via the `TestKube` [execute command](#execute-commands) functionality:
```groovy
TestKube instance = TestKube.newBuilder()
    .label( id )
    .addPod( "pod1", NginxResources.SIMPLE_NGINX_POD )
    .addPod( "pod2", NginxResources.SIMPLE_NGINX_POD2 )
    .addService( NginxResources.SIMPLE_NGINX_SERVICE_NAME, NginxResources.SIMPLE_NGINX_SERVICE )
    .addProvisioningProbe( "service check.", (tk)-> {
        String[] command = ["sh", "-c", "curl -s http://" + NginxResources.SIMPLE_NGINX_SERVICE_NAME +":"+80 ]
        ExecResult actual = tk.getController(  ).executeCommandAsync( "pod2", command )
                .get( 5, TimeUnit.SECONDS)
        return actual.getExitCode(  ) == 0
    } )
    .build()

instance.init( )
```

NOTE: Care should be taken to consider where the probe is executing. When running from a local machine accessing the cluster 
remotely it will not have the same network access as when running within the cluster for example within a CI/CD pipeline.
Probes must be created to ensure they work in both scenarios to prevent issues.

### `k8s` Resources

#### Definitions

`k8s` managed resources are expected to be valid `fabric8` kubernetes java client api resource definitions.
These resources should ideally be created with the use of helper methods to simplify their maintenance and modifications.
Though there is technically nothing stopping you from using a `k8s` manifest file as a starting point which you then "edit".

#### Labelling

`tdk8s` automatically adds a unique `tdk8s` label populated with a random `uuid` to all `k8s` resources created so they
are clearly identifiable, though if you wish to control this value you can also specify this when defining the `TestKube`.

```java
TestKube.newBuilder().label("my value").build();
```

#### Grouping

If you wish to have different groups of resources for more granular control over longer living resources, you can create
multiple `TestKube` instances: 
* initialised before all tests (`Spock` `setupSpec`, `Junit` `@BeforeClass`) and destroyed  after all test (`Spock` `cleanupSpec`, `Junit` `@AfterClass`).
* initialised before each test (`Spock` `setup`, `Junit` `@Before`) and destroyed after each test (`Spock` `cleanup`, `Junit` `@After`) 
* or even manually within the test itself.

For a working example of this see the [MultiTestKube Integration Test](./test-kube/src/integration/groovy/se/laz/casual/test/tdk8s/integration/MultiTestKubesIntTest.groovy). 

NOTE: Be aware that the time taken to provision `k8s` resources is dependent upon among other things, container image sizes, their readiness requirements and
available `k8s` compute resources. Therefore, you can quickly end up with very **slow tests executions** if you are not careful.