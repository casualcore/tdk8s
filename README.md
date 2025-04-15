# tdk8s - Test Driven Kubernetes

Test Driven Kubernetes (`tdk8s`) is designed to simplify the testing of software running on 
kubernetes (k8s) compatible platforms.

Too often running and maintaining tests on k8s is arduous, inflexible and brittle.

`tdk8s` is designed to reduce that burden, ensuring that the k8s System Under Test (SUT)
is exactly as required for your different test scenarios, whilst providing a simplified,
flexible and stable developer experience for writing and debugging your tests whilst also speeding 
up the test development feedback loop from allowing for test execution from within your IDE.

TODO: build status badges etc

## Contents

* [Overview](#overview) 
* [Usage](#usage)
  * [Provisioning](#provisioning)
  * [Connectivity](#connectivity)
* [Advanced Usage](#advanced-usages)
  * [Execute Commands](#execute-commands)
  * [Retrieve Log Files](#retrieve-log-files)
  * [`port-forward`](#connect-via-port-forward) 
  * [Asynchronous Provisioning](#asynchronous-provisioning)
  * [k8s Resources](#k8s-resources)

## Overview

The rise of GitOps and CI/CD pipelines has undoubtedly improved configuration management and stability for 
production releases, but it has led to developers experiencing some negative side effects:
* Slow feedback loops.
* Complexity when debugging failing test.
* Vanishing resources and logs after tests complete.
* Lack of flexibility over the SUT configuration for testing different test scenarios.
* Onerous maintenance of often verbose manifest files e.g. yaml.

`tdk8s` aims to address these issues:
* Simplicity while developing and maintaining tests.
* Flexibility over creating test scenarios.
* Stability for running tests repeatably and frequently.

### Simplicity

It must be possible to run/re-run CI/CD pipeline tests from the developers local machine/IDE upon the k8s SUT, without 
modifications to the tests and without needing to manually ensure the k8s SUT is configured in a specific way prior to 
running the tests.

The test scenarios must be able to define which k8s resources and their configuration in the k8s SUT are required.
It must be possible for this environment to be built from scratch, provisioning all necessary
resources and destroying them all once complete.

If required though, it should also be possible to retain the k8s resources provisioned by the test scenario within the 
k8s SUT, allowing for faster test re-run, test fix iterations, remote debugging etc.

NOTE: `tdk8s` is **NOT** expected to be used for unit testing; it will most likely be too slow due to the time
taken by the target k8s platform to provision and remove resources. It is more suited for things like functional 
integration testing.

tkd8s simplifies the developer experience for testing your software running on k8s by providing
a wrapper around the provision and destruction of k8s resources. This works whether the tests are run within a 
CI/CD pipeline or from a developers local machine within their IDE.

### Flexibility

There must be flexibility over the k8s resources and their configurations in the k8s SUT, allowing testing of
different test scenarios with different configuration permutations, whilst still ensuring stability over the
test execution and reliability of test results.

The definition and configuration of k8s resources must be possible to directly from test code without needing to
maintain manually written verbose manifest files.

Test must be able to run on any flavour of kubernetes, be that k3s, minikube, KinD, vanilla k8s, openshift etc, as long
as they are k8s compatible platforms and allow remote access via kubectl. This ensures flexibility of choice for
difference scenarios whilst also making for more robust, stable test scenarios.

`tdk8s` embraces this very same flexibility within its own integration test suite, which is a good place to check for usage
examples.

### Stability

Whilst flexibility is important, it must not come at the sacrifice of test reliability, as this would reduce the benefits 
of GitOps and CI/CD pipelines.

`tdk8s` allows test code to specify which k8s resources are required for each test scenario and waits for resources
to become available prior to test execution. Once complete, all k8s resources provisioned by the test scenario will be
deleted with test scenarios finishing once this is confirmed.

Using `tdk8s` from within your test code, ensures the k8s SUT is exactly as your test codes wants it to be, everytime your 
tests are run; before, during and after execution, whether run locally from your development machine or within a CI/CD 
pipeline.

Thereby ensuring stable and repeatable test execution.

## Usage

`tdk8s` utilises the fabric8 kubernetes client for defining k8s resources. Helper classes should be used to simplify
their creation.

As part of the development of `tdk8s` there are numerous integration tests, which serve as functional examples
of how to utilise `tdk8s`, see [tdk8s integration tests](./test-kube/src/integration) for more.

The following is a simplified example from the `tdk8s` integration tests using `Spock` to:
* Initialise a Pod and Service within the k8s SUT
* Connect to the pod via the service.
* Destroy both the Service and Pod once complete.

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

The main entry point for `tdk8s` is the `TestKube` definition.

This is used to define the different k8s resources that need to be provisioned and ensures they are available 
prior to running your test case by utilising the `init()` and `destroy()` methods respectively.

k8s resources are expected to be valid fabric8 kubernetes java client definitions.
These resources should ideally be created with the use of helper methods to simplify their maintenance and modifications.

See more advanced provisioning options see [here](#asynchronous-provisioning).

### Connectivity

Connectivity to the k8s SUT is provided through the use of fabric8 kubernetes java client `io.fabric8.kubernetes.client.KubernetesClient`.
If the default configuration is not appropriate, you can create your own instance of this and provide it to the `TestKube` 
builder:

```java
TestKube.newBuilder().client( myClient ).build()
```

The `TestKube` object also facilitates establishing a connection to the k8s resources via services using the `getConnection` 
method which returns `KubeConnection` which contains the `hostname` and `port` you should use for connecting within
your test code.

When the `getConnection` method is run within the k8s environment i.e. inside CI/CD pipelines, it will connect through the 
service. Though when run from outside the k8s environment i.e. from a local developer machine / IDE, if the services is not
accessible, it will seamlessly provide a `port-forward` to the service. Allowing the same tests to be run without any
modifications.

Therefore you should always use the `KubeConnection` object returned to determine the connection details, otherwise seamless
connectivity will not work. `KubeConnection` objects are `AutoCloseable` so should ideally be used within `try-with-resources`
blocks to ensure they are closed correctly.

Note - this seamless `port-forward` mechanism has been added to simplify writing and maintaining test code, though `port-forward` 
should **NOT** be used for performance testing as load balancing will not work as expected.

## Advanced Usages

The main functionality provided by TestKube is provisioning and connectivity.

The majority of tests should be possible with just this functionality.

However, there are more advanced functions provided via the `KubeController`, which can be accessed 
via `getController()` method of the `TestKube` object.

This provides the ability to:
* execute commands upon pods.
* retrieve log files from pods.
* Connect via `port-forward`
* asynchronous provisioning.

When referring to pods by name, it will initially look for the pod alias provided when added using `TestKubeBuilder#addPod`,
this aims to remove complexity around pods with generated names. Though it will also find exact matches too if required
when a match to the alias is not found.

### Execute Commands

If you need to execute a command against a running pod you can use the following `KubeController` methods:

* `executeCommand` - waits for completion of command.
* `executeCommandAsync` - returns CompletableFuture of command.

The methods return an `ExecResult` object which wraps the `output` and `exitCode`.

Synchronous example:
```java
ExecResult result = instance.getController().executeCommand( podName, "sh", "-c", "echo -n hello" );
```

Asynchronous example:
```java
CompletableFuture<ExecResult> future = instance.getController().executeCommand( podName, "sh", "-c", "sleep 5;echo -n hello" );
// do your work
ExecResult result = future.join();
```

### Retrieve Log Files

If you need to retrieve the log files from a running pod you can use the following 'KubeController' methods:

* `getLog` - retrieve full log as a String. 
* `getLogTail` - retrieve the last n lines of the log file.
* `getLogSince` - retrieve log files lines since the provided time.

Examples:
```java
// Get full log.
String fullLog = instance.getController().getLog( podName );

// Get last 10 lines of log. 
String lastTen = instance.getController().getLogTail( podName, 10 );

// Get lines from log after provided date.
ZoneDateTime afterInit = ZonedDateTime.now();
String since = afterInit.format( DateTimeFormatter.ISO_OFFSET_DATE_TIME );
String sinceLog = instance.getController().getLogSince( podName, since );
```

### Connect via `port-forward`

You should probably not use this functionality other than for simplifying/enabling remote debugging. Though...

If you wish to connect to a pod via `port-forward` you can use the following `KubeController` method:

* `getPortForwardConnection` 

This can be used for connections to pod and service resources. 
The target port is used to determine how to create the `port-forward`.

As with `getConnection` the returned `KubeConnection` object should be used to retrieve `hostname` and `port` and
should be ideally used within a `try-with-resources` block to ensure the connection is closed correctly.

### Asynchronous Provisioning

`init()` and `destroy()` are both synchronous calls which wait until the resources are all "ready". If you wish to
perform operations whilst waiting for the resources provisioning or deletion you can also use the asynchronous
alternatives available via the `KubeController`:

Asynchronous initialisation:
```java
TestKube instance; // defined

instance.getController().initAsync();
// do your work.
instance.getController().waitUntilReady();
```

Asynchronous destruction:
```java
TestKube instance; // define

instance.getController().destroyAsync();
// do your work.
instance.getController().waitUntilDestroyed();
```

### k8s Resources

#### Definitions

k8s resources are expected to be valid fabric8 kubernetes java client definitions.
These resources should ideally be created with the use of helper methods to simplify their maintenance and modifications.
Though there is technically nothing stopping you from using a k8s manifest file as a starting point which you then "edit".

#### Labelling

`tdk8s` will automatically add a unique `tdk8s` label populated with a `uuid` to all resources created so they
are clearly identifiable, though if you wish to control this value you can also specify this when defining the `TestKube`.

```java
TestKube.newBuilder().label("my value").build();
```

#### Grouping

If you wish to have different groups of resources for more granular control over longer living resources, you can create
multiple `TestKube` instances, for example with some initialised before all tests (Spock `setupSpec`, Junit `@BeforeClass`)
and others initialised and destroyed before each test (Spock `setup`, Junit `@Before`) or even within the test itself.

Just be aware that the time taken to provision resources is dependent upon container image sizes, their readiness requirements and
available k8s compute resources. Therefore you can quickly end up with very slow tests executions if you are not careful.