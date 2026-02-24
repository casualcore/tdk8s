/*
 * Copyright (c) 2025 - 2026, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.integration

import io.fabric8.kubernetes.api.model.ConfigMap
import io.fabric8.kubernetes.api.model.ConfigMapBuilder
import io.fabric8.kubernetes.api.model.Pod
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.KubernetesClientBuilder
import se.laz.casual.test.tdk8s.TestKube
import se.laz.casual.test.tdk8s.sample.NginxResources
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path

import static se.laz.casual.test.tdk8s.TestKube.RESOURCE_LABEL_NAME

class ConfigMapMountIntTest extends Specification
{
    @Shared
    KubernetesClient client = new KubernetesClientBuilder().build()
    @Shared
    String id = ConfigMapMountIntTest.class.getSimpleName()
    @Shared
    TestKube instance
    @Shared
    String podName = NginxResources.SIMPLE_NGINX_POD_NAME
    @Shared
    String mapName = "config-cm"
    @Shared
    Path configFile = new File( "./src/integration/resources/configFile.txt").toPath(  )
    @Shared
    String configFileName = configFile.getFileName(  ).toString(  )
    @Shared
    String mountPath = "/data/" + configFileName
    @Shared
    String volumeName = "data"


    def setupSpec()
    {

        List<Pod> pods = client.pods().withLabel( RESOURCE_LABEL_NAME, id ).list().getItems()
        List<ConfigMap> cms = client.configMaps(  ).withLabel( RESOURCE_LABEL_NAME, id ).list().getItems()

        assert pods.size() == 0
        assert cms.size() == 0

        ConfigMap map = new ConfigMapBuilder().withNewMetadata(  )
                .withName( mapName )
                .addToLabels( RESOURCE_LABEL_NAME, id )
                .endMetadata(  )
                .addToData( configFileName, Files.readString( configFile ) )
        .build(  )

        Pod pod = NginxResources.SIMPLE_NGINX_POD
        pod = pod.edit(  ).editSpec(  )
                .addNewVolume(  )
                    .withName( volumeName )
                    .withNewConfigMap(  )
                        .withName( map.getMetadata(  ).getName(  ) )
                    .endConfigMap(  )
                .endVolume(  )
                .editContainer( 0 )
                    .addNewVolumeMount(  )
                        .withName( volumeName )
                        .withMountPath( mountPath )
                        .withSubPath( configFileName )
                    .endVolumeMount(  )
                .endContainer(  )
            .endSpec(  )
            .build(  )

        instance = TestKube.newBuilder()
                .label( id )
                .addConfigMap( mapName, map )
                .addPod( podName, pod )
                .build()

        instance.init()

    }

    def cleanupSpec()
    {
        instance.destroy()

        List<Pod> pods = client.pods().withLabel( RESOURCE_LABEL_NAME, id ).list().getItems()
        List<ConfigMap> cms = client.configMaps(  ).withLabel( RESOURCE_LABEL_NAME, id ).list().getItems()

        assert pods.size() == 0
        assert cms.size(  ) == 0
    }

    def "Download mounted config file from the running pod."()
    {
        given:
        Path p = Files.createTempFile( "FileTransferIntTest", "txt" )

        when:
        boolean successful = instance.getController().download( podName, mountPath, p )

        String actual = new String( Files.readAllBytes( p ) )

        then:
        successful
        actual != ""
        actual == Files.readString( configFile )

        cleanup:
        p.toFile().delete()
    }
}
