/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.k8s;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import se.laz.casual.test.k8s.connection.KubeConnection;
import se.laz.casual.test.k8s.controller.KubeController;
import se.laz.casual.test.k8s.store.ResourcesStore;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Facade / Abstraction providing mechanisms to
 * provision kubernetes resources for the purpose of test.
 * <br/>
 * Resources should be created at the start of tests and destroyed at the end.
 * <br/>
 * The ability to interact with the k8s resources during the test are also provided
 * <ul>
 *     <li>check logs</li>
 *     <li>network connection details</li>
 *     <li>status, readiness</li>
 *     <li>modify / redeploy</li>
 *     <li>delete</li>
 *     <li>monitor</li>
 * </ul>
 * To allow maximum flexibility you can use the fabric8 api to define you resources.
 * These resources are then managed by the TestKube instance.
 * For more complex resource definitions it is advised to implement your own mapping
 * code which produces you own fabric8 {@link io.fabric8.kubernetes.client.dsl.Resource}
 * objects.
 * <br/>
 * By default, the TestKube instance will use a {@link io.fabric8.kubernetes.client.KubernetesClient}
 * with no additional configuration. If you require more complex setup, you can provide the appropriate
 * {@link io.fabric8.kubernetes.client.KubernetesClient} instance into the TestKube builder.
 *
 */
public class TestKube
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
        this.kubeController = builder.kubeController;
    }

    public KubernetesClient getClient()
    {
        return client;
    }

    public String getLabel()
    {
        return label;
    }

    public Map<String,Pod> getPods()
    {
        return this.resourcesStore.getPods();
    }

    public Map<String, Service> getServices()
    {
        return this.resourcesStore.getServices();
    }

    public ResourcesStore getResourcesStore( )
    {
        return this.resourcesStore;
    }

    /**
     * To gain more control over the TestKube, you can access the
     * {@link KubeController} for this TestKube instance here,
     * providing you with more methods for example:
     * <ul>
     *     <li>{@link KubeController#initAsync()}</li>
     *     <li>{@link KubeController#destroyAsync()}</li>
     * </ul>
     *
     * @return controller instance for this TestKube.
     */
    public KubeController getController()
    {
        return kubeController;
    }

    public void init()
    {
        this.kubeController.init();
    }

    public void destroy()
    {
        this.kubeController.destroy();
    }

    public KubeConnection getConnection( String resource, int targetPort )
    {
        return this.kubeController.getConnection( resource, targetPort );
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
        private Map<String,Pod> pods = new HashMap<>();
        private Map<String,Service> services = new HashMap<>();
        private KubeController kubeController;
        private ResourcesStore resourcesStore;

        public Builder client( KubernetesClient client )
        {
            this.client = client;
            return this;
        }

        public Builder label( String label )
        {
            this.label = label;
            return this;
        }

        public Builder addPod( String alias, Pod pod )
        {
            this.pods.put( alias, pod );
            return this;
        }

        public Builder addService( String alias, Service service )
        {
            this.services.put( alias, service );
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

            if( this.kubeController == null )
            {
                this.kubeController = KubeController.newBuilder()
                        .client( this.client )
                        .label( this.label )
                        .resourcesStore( this.resourcesStore )
                        .build();
            }
            return new TestKube( this );
        }
    }
}
