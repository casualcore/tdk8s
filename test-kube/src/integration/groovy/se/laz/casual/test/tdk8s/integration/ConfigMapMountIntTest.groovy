/*
 * Copyright (c) 2025 - 2026, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.integration

import io.fabric8.kubernetes.api.model.ConfigMap
import io.fabric8.kubernetes.api.model.Pod
import io.fabric8.kubernetes.api.model.apps.Deployment
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.KubernetesClientBuilder
import se.laz.casual.test.tdk8s.TestKube
import se.laz.casual.test.tdk8s.resources.ConfigMapFactory
import se.laz.casual.test.tdk8s.resources.FileMount
import se.laz.casual.test.tdk8s.resources.VolumeMounter
import se.laz.casual.test.tdk8s.sample.NginxResources
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

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
    String deploymentName = NginxResources.SIMPLE_NGINX_DEPLOYMENT_NAME
    @Shared
    String mapName = "config-cm"
    @Shared
    Path configFile = Paths.get("./src/integration/resources/configFile.txt")
    @Shared
    String mountPath = "/data/configFile2.txt"


    def setupSpec()
    {
        List<Pod> pods = client.pods().withLabel( RESOURCE_LABEL_NAME, id ).list().getItems()
        List<ConfigMap> cms = client.configMaps(  ).withLabel( RESOURCE_LABEL_NAME, id ).list().getItems()

        assert pods.size() == 0
        assert cms.size() == 0

        Pod pod = NginxResources.SIMPLE_NGINX_POD
        Deployment deployment = NginxResources.SIMPLE_NGINX_DEPLOYMENT

        ConfigMap map = ConfigMapFactory.fromFile( mapName, configFile )

        FileMount mount = FileMount.newBuilder().configMap( map ).mountPath( mountPath ).build()

        pod = VolumeMounter.mount( pod, mount )
        deployment = VolumeMounter.mount( deployment, mount )

        instance = TestKube.newBuilder()
                .label( id )
                .addConfigMap( mapName, map )
                .addPod( podName, pod )
                .addDeployment( deploymentName, deployment )
                .build()

        instance.init()
    }

    def cleanupSpec()
    {
        instance.destroy()

        List<Pod> pods = client.pods().withLabel( RESOURCE_LABEL_NAME, id ).list().getItems()
        List<ConfigMap> cms = client.configMaps(  ).withLabel( RESOURCE_LABEL_NAME, id ).list().getItems()

        assert pods.size() == 0
        assert cms.size() == 0
    }

    def "Download mounted config file from the running pod."()
    {
        given:
        Path localPath = Files.createTempFile( "FileTransferIntTest", "txt" )

        when:
        boolean successful = instance.getController().download( podName, mountPath, localPath )

        String actual = new String( Files.readAllBytes( localPath ) )

        then:
        successful
        actual != ""
        actual == Files.readString( configFile )

        cleanup:
        Files.delete( localPath )
    }

    def "Download mounted config file from the running deployment pod."()
    {
        given:
        Path localPath = Files.createTempFile( "FileTransferIntTest", "txt" )

        when:
        boolean successful = instance.getController().download( deploymentName, mountPath, localPath )

        String actual = new String( Files.readAllBytes( localPath ) )

        then:
        successful
        actual != ""
        actual == Files.readString( configFile )

        cleanup:
        Files.delete( localPath )
    }
}
