/*
 * Copyright (c) 2025 - 2026, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.controller.provisioning;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import se.laz.casual.test.tdk8s.store.ResourcesStore;
import se.laz.casual.test.tdk8s.watchers.DeleteResourceWatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.lang.System.Logger.Level.DEBUG;
import static se.laz.casual.test.tdk8s.TestKube.RESOURCE_LABEL_NAME;

/**
 * Controls the provisioning of ConfigMap resources.
 */
public class ProvisioningConfigMapController implements ProvisionableAsync
{
    private static final System.Logger logger = System.getLogger( ProvisioningConfigMapController.class.getName());

    private final KubernetesClient client;
    private final ResourcesStore resourcesStore;
    private final String labelValue;
    private final List<DeleteResourceWatcher<ConfigMap>> deleteWatchers = new ArrayList<>();

    public ProvisioningConfigMapController( KubernetesClient client, ResourcesStore resourcesStore, String labelValue )
    {
        this.client = client;
        this.resourcesStore = resourcesStore;
        this.labelValue = labelValue;
    }

    @Override
    public void initAsync()
    {
        for( Map.Entry<String, ConfigMap> entry : resourcesStore.getConfigMaps().entrySet() )
        {
            String name = entry.getKey();
            ConfigMap cm = entry.getValue();
            ConfigMap updated = cm.edit().editMetadata().addToLabels( RESOURCE_LABEL_NAME, labelValue ).endMetadata().build();
            updated = client.configMaps().resource( updated ).serverSideApply();
            resourcesStore.putConfigMap( name, updated );
            logger.log( DEBUG, ()-> "ConfigMap applied: " + name );
        }
    }

    @Override
    public void waitUntilReady()
    {
        //no-op
    }

    @Override
    public void destroyAsync()
    {
        for( Map.Entry<String,ConfigMap> entry : resourcesStore.getConfigMaps().entrySet() )
        {
            Resource<ConfigMap> configMapResource = client.configMaps().resource( entry.getValue() );
            deleteWatchers.add( new DeleteResourceWatcher<>( configMapResource ) );

            configMapResource.delete();
            logger.log( DEBUG, ()-> "ConfigMap deleted: " + entry.getKey() );
        }
    }

    @Override
    public void waitUntilDestroyed()
    {
        for( DeleteResourceWatcher<ConfigMap> watcher : deleteWatchers )
        {
            watcher.waitUntilDeleted();
        }
        deleteWatchers.clear();
    }
}
