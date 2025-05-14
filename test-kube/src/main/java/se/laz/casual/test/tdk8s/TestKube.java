/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
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
 * <br/>
 * The ability to interact with the k8s resources during the test are also provided
 * <ul>
 *     <li>connections to resources</li>
 *     <li>access logs</li>
 *     <li>transfer files</li>
 *     <li>execute command</li>
 * </ul>
 * Resources should be created at the start of tests and destroyed at the end.
 * <br/>
 * To allow maximum flexibility you can use the fabric8 api to define you resources.
 * These resources are then managed by the TestKube instance.
 * For more complex resource definitions it is advised to implement your own mapping
 * code which produces you own fabric8 {@link io.fabric8.kubernetes.client.dsl.Resource}
 * objects.
 * <br/>
 * Note: Currently only Pod and Service resources can be managed, though additional resources should be
 * added later e.g. ConfigMap, Deployment.
 * <br/>
 * All managed resources are labeled with a unique label, this label can be configured using the
 * builder.
 * <br/>
 * By default, the cluster connectivity is provided using a {@link io.fabric8.kubernetes.client.KubernetesClient}
 * with no additional configuration. If you require more complex setup, you can provide the appropriate
 * {@link io.fabric8.kubernetes.client.KubernetesClient} instance into the builder.
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
     * <br/>
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
         * <br/>
         * The alias provided can be used to retrieve and does not have to match
         * the actual name of the Pod provided.
         *
         * @param alias of the pod.
         * @param pod   to be managed.
         * @return the builder.
         */
        public Builder addPod( String alias, Pod pod )
        {
            this.pods.put( alias, pod );
            return this;
        }

        /**
         * Add a service to be managed.
         * <br/>
         * The alias provided can be used to retrieve and does not have to match
         * the actual name of the Service provided.
         *
         * @param alias   of the service.
         * @param service to be managed.
         * @return the builder.
         */
        public Builder addService( String alias, Service service )
        {
            this.services.put( alias, service );
            return this;
        }

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
            this.resourcesStore.putServices( services );
            this.resourcesStore.putProvisioningProbes( initProbes );

            return new TestKube( this );
        }
    }
}
