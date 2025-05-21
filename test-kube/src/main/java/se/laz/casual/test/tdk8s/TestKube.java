/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import se.laz.casual.test.tdk8s.connection.KubeConnection;
import se.laz.casual.test.tdk8s.controller.Connectable;
import se.laz.casual.test.tdk8s.controller.KubeController;
import se.laz.casual.test.tdk8s.controller.Provisionable;
import se.laz.casual.test.tdk8s.probe.ProvisioningProbe;
import se.laz.casual.test.tdk8s.store.ResourcesStore;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages kubernetes resources for the purpose of test.
 * <p>
 * The ability to interact with the k8s resources during the test are also provided
 * <ul>
 *     <li>connections to resources</li>
 *     <li>access logs</li>
 *     <li>transfer files</li>
 *     <li>execute command</li>
 * </ul>
 * Resources should be created at the start of tests and destroyed at the end.
 * </p>
 * <p>
 * To allow maximum flexibility you can use the fabric8 api to define you resources.
 * These resources are then managed by the TestKube instance.
 * For more complex resource definitions it is advised to implement your own mapping
 * code which produces you own fabric8 {@link io.fabric8.kubernetes.client.dsl.Resource}
 * objects.
 * </p>
 * <p>
 * Note: Currently only Pod and Service resources can be managed, though additional resources should be
 * added later e.g. ConfigMap, Deployment.
 * </p>
 * <p>
 * All managed resources are labeled with a unique label, this label can be configured using the
 * builder.
 * </p>
 * <p>
 * By default, the cluster connectivity is provided using a {@link io.fabric8.kubernetes.client.KubernetesClient}
 * with no additional configuration. If you require more complex setup, you can provide the appropriate
 * {@link io.fabric8.kubernetes.client.KubernetesClient} instance into the builder.
 * </p>
 */
public class TestKube implements Provisionable, Connectable
{
    public static final String RESOURCE_LABEL_NAME = "tdk8s";
    private final KubernetesClient client;
    private final String label;
    private final ResourcesStore resourcesStore;

    private final KubeController kubeController;

    private TestKube(final Builder builder )
    {
        this.client = builder.client;
        this.label = builder.label;

        this.resourcesStore = builder.resourcesStore;

        KubeController kc = builder.kubeController;
        if( kc == null )
        {
            kc = KubeController.newBuilder()
                    .testKube( this )
                    .client( this.client )
                    .label( this.label )
                    .resourcesStore( this.resourcesStore )
                    .build();
        }

        this.kubeController = kc;
    }

    /**
     * Gets the client associated with this TestKube.
     * If you need to perform more advanced operations.
     *
     * @return the client.
     */
    public KubernetesClient getClient()
    {
        return client;
    }

    /**
     * Gets the {@link TestKube#RESOURCE_LABEL_NAME} label value for this TestKube.
     *
     * @return label value.
     */
    public String getLabel()
    {
        return label;
    }

    /**
     * Gets the pods managed by this TestKube.
     *
     * @return map of managed pods.
     */
    public Map<String,Pod> getPods()
    {
        return this.resourcesStore.getPods();
    }

    /**
     * Gets the deployments managed by this TestKube.
     *
     * @return map of managed deployments.
     */
    public Map<String,Deployment> getDeployments()
    {
        return this.resourcesStore.getDeployments();
    }

    /**
     * Gets the services managed by this TestKube.
     *
     * @return map of managed services.
     */
    public Map<String, Service> getServices()
    {
        return this.resourcesStore.getServices();
    }

    /**
     * Gets the resources store for this TesTKube.
     *
     * @return resources store.
     */
    public ResourcesStore getResourcesStore( )
    {
        return this.resourcesStore;
    }

    /**
     * Gets the controller for this TestKube.
     * <p>
     * To gain more control over the TestKube, you can access the
     * {@link KubeController} for this TestKube instance here,
     * providing you with more functionality, for example:
     * <ul>
     *     <li>{@link KubeController#initAsync()}</li>
     *     <li>{@link KubeController#destroyAsync()}</li>
     *     <li>{@link KubeController#getLog(String)}</li>
     *     <li>{@link KubeController#executeCommand(String, String...)}</li>
     *     <li>{@link KubeController#download(String, String, Path)}</li>
     *     <li>{@link KubeController#upload(String, String, Path)}</li>
     * </ul>
     * </p>
     *
     * @return controller instance for this TestKube.
     */
    public KubeController getController()
    {
        return kubeController;
    }

    @Override
    public void init()
    {
        this.kubeController.init();
    }

    @Override
    public void destroy()
    {
        this.kubeController.destroy();
    }

    @Override
    public KubeConnection getConnection( String service, int targetPort )
    {
        return this.kubeController.getConnection( service, targetPort );
    }

    @Override
    public String toString()
    {
        return "TestKube{" +
                "client=" + client +
                ", label='" + label + '\'' +
                ", resourcesStore=" + resourcesStore +
                ", kubeController=" + kubeController +
                '}';
    }

    public static Builder newBuilder()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private KubernetesClient client;
        private String label = UUID.randomUUID().toString();
        private Map<String, Pod> pods = new HashMap<>();
        private Map<String, Service> services = new HashMap<>();
        private Map<String, Deployment> deployments = new HashMap<>();
        private Map<String, ProvisioningProbe> initProbes = new HashMap<>();
        private KubeController kubeController;
        private ResourcesStore resourcesStore;

        /**
         * Provide a configured KubernetesClient when the default created,
         * is not sufficient.
         *
         * @param client configured.
         * @return the builder.
         */
        public Builder client( KubernetesClient client )
        {
            this.client = client;
            return this;
        }

        /**
         * Provide a more specific label value for managed resources, if the
         * default random UUID is not sufficient.
         *
         * @param label for the managed resources.
         * @return the builder.
         */
        public Builder label( String label )
        {
            this.label = label;
            return this;
        }

        /**
         * Add a pod to be managed.
         * <p>
         * The alias provided can be used to retrieve and does not have to match
         * the actual name of the Pod provided.
         * </p>
         *
         * @param alias of the pod.
         * @param pod to be managed.
         * @return the builder.
         */
        public Builder addPod( String alias, Pod pod )
        {
            this.pods.put( alias, pod );
            return this;
        }

        /**
         * Add a service to be managed.
         * <p>
         * The alias provided can be used to retrieve and does not have to match
         * the actual name of the Service provided.
         * </p>
         *
         * @param alias of the service.
         * @param service to be managed.
         * @return the builder.
         */
        public Builder addService( String alias, Service service )
        {
            this.services.put( alias, service );
            return this;
        }

        /**
         * Add a deployment to be managed.
         * <p>
         * The alias provided can be used to retrieve and does not have to match
         * the actual name of the Deployment provided.
         * </p>
         * @param alias of the deployment.
         * @param deployment to be managed.
         * @return the builder.
         */
        public Builder addDeployment( String alias, Deployment deployment )
        {
            this.deployments.put( alias, deployment );
            return this;
        }

        /**
         * Add a custom provisioning probe to be executed at the end of the initialisation process.
         * <p>
         * Probes are executed repeatedly until they succeed (return true) or a timeout has occurred.
         * Therefore, they should not result in side effect and can be executed multiple times without issue.
         * They should also ideally be short running to ensure they do not unnecessarily delay completion
         * of the initialisation process prior to test execution.
         * </p>
         * <p>
         * <b>Note:</b> These should be avoided where possible as in most scenarios correct usage of readiness
         * probes within the kubernetes resources should be sufficient to ensure that the managed
         * resources are all ready.
         * </p>
         * <p>
         * However, they can be useful for example when new service IP details take time to propagate
         * within the cluster such that sleeps and retries are not required within the test code,
         * ensuring stable, repeatable test execution.
         * </p>
         * The current TestKube instances is made available to the probe to utilise as necessary.
         * For example, probe using curl from one pod through a service to another pod:
         * <pre>
         * {@code
         *    builder.addProvisioningProbe( "service-check", (tk)-> {
         *        ExecResult result = tk.getController()
         *              .executeCommandAsync( "pod-alias", "sh", "-c", "curl -s http://<service>:<port>" )
         *              .get( 5, TimeUnit.SECONDS );
         *        return result.getExitCode() == 0 && result.getOutput() == expected;
         *    });
         * }
         * </pre>*
         * </p>
         * <p>
         * <b>Note:</b> Care should be taken to consider where the probe is executing. When running from
         * a local machine accessing the cluster remotely it will not have the same network access as
         * when running within the cluster for example within a CI/CD pipeline.
         * Probes must be created to ensure they work in both scenarios to prevent issues.
         * <br/>
         * </p>
         *
         * @param alias of the probe.
         * @param probe to be executed.
         * @return the builder.
         */
        public Builder addProvisioningProbe( String alias, ProvisioningProbe probe )
        {
            this.initProbes.put( alias, probe );
            return this;
        }

        Builder kubeController( KubeController kubeController )
        {
            this.kubeController = kubeController;
            return this;
        }

        public TestKube build()
        {
            if( this.client == null )
            {
                this.client = new KubernetesClientBuilder().build();
            }

            this.resourcesStore = new ResourcesStore();
            this.resourcesStore.putPods( pods );
            this.resourcesStore.putDeployments( deployments );
            this.resourcesStore.putServices( services );
            this.resourcesStore.putProvisioningProbes( initProbes );

            return new TestKube( this );
        }
    }
}
