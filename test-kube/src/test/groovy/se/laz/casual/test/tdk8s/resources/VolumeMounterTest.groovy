/*
 * Copyright (c) 2026, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.resources

import io.fabric8.kubernetes.api.model.ConfigMap
import io.fabric8.kubernetes.api.model.Pod
import io.fabric8.kubernetes.api.model.apps.Deployment
import se.laz.casual.test.tdk8s.sample.NginxResources
import se.laz.casual.test.tdk8s.store.ResourceNotFoundException
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Paths

class VolumeMounterTest extends Specification
{
    @Shared
    String mapName = "config-map-1"

    @Shared
    ConfigMap map = ConfigMapFactory.fromFile( mapName, Paths.get( "src/test/resources/test.txt") )


    def "Add pod volume mount for configmap."()
    {
        given:
        String volumeName = "tdk8s-vol-01"
        String container = NginxResources.NGINX_CONTAINER_NAME
        String mountPath = "/data/test.txt"
        String subPath = "test.txt"
        Pod pod = NginxResources.SIMPLE_NGINX_POD

        FileMount mount = FileMount.newBuilder().configMap( map )
                .mountPath( mountPath )
                .container( container )
                .volume( volumeName )
                .build()

        Pod expected = pod.edit(  )
                .editSpec(  )
                .addNewVolume(  )
                    .withName( volumeName )
                    .withNewConfigMap(  )
                        .withName( mapName )
                    .endConfigMap(  )
                .endVolume(  )
                .editContainer( 0 )
                    .addNewVolumeMount(  )
                        .withName( volumeName )
                        .withMountPath( mountPath )
                        .withSubPath( subPath )
                    .endVolumeMount(  )
                .endContainer(  )
                .endSpec(  )
                .build(  )

        when:
        Pod actual = VolumeMounter.mount( pod, mount )

        then:
        actual == expected
    }

    def "Add pod volume mount without container name for configmap."()
    {
        given:
        String volumeName = "tdk8s-vol-01"
        String mountPath = "/data/test.txt"
        String subPath = "test.txt"
        Pod pod = NginxResources.SIMPLE_NGINX_POD

        FileMount mount = FileMount.newBuilder().configMap( map )
                .mountPath( mountPath )
                .volume( volumeName )
                .build()

        Pod expected = pod.edit(  )
                .editSpec(  )
                .addNewVolume(  )
                .withName( volumeName )
                .withNewConfigMap(  )
                .withName( mapName )
                .endConfigMap(  )
                .endVolume(  )
                .editContainer( 0 )
                .addNewVolumeMount(  )
                .withName( volumeName )
                .withMountPath( mountPath )
                .withSubPath( subPath )
                .endVolumeMount(  )
                .endContainer(  )
                .endSpec(  )
                .build(  )

        when:
        Pod actual = VolumeMounter.mount( pod, mount )

        then:
        actual == expected
    }

    def "Add pod volume mount invalid container name, throws ResourceNotFound."()
    {
        given:
        String volumeName = "tdk8s-vol-01"
        String container = "invalid"
        String mountPath = "/data/test.txt"

        Pod pod = NginxResources.SIMPLE_NGINX_POD

        FileMount mount = FileMount.newBuilder().configMap( map )
                .mountPath( mountPath )
                .container( container )
                .volume( volumeName )
                .build()

        when:
        VolumeMounter.mount( pod, mount )

        then:
        thrown ResourceNotFoundException
    }

    def "Add pod volume with nulls, throws NullPointerException."()
    {
        when:
        VolumeMounter.mount( pod as Pod, mount )

        then:
        thrown NullPointerException

        where:
        pod                             | mount
        NginxResources.SIMPLE_NGINX_POD | null
        null                            | FileMount.newBuilder().configMap( map ).mountPath( "/tmp/t.log" ).build()
        null                            | null
    }

    def "Add pod volume mount with different mountpath file name for configmap."()
    {
        given:
        String volumeName = "tdk8s-vol-01"
        String container = NginxResources.NGINX_CONTAINER_NAME
        String mountPath = "/data/test2.txt"
        String subPath = "test.txt"
        Pod pod = NginxResources.SIMPLE_NGINX_POD

        FileMount mount = FileMount.newBuilder().configMap( map )
                .mountPath( mountPath )
                .container( container )
                .volume( volumeName )
                .build()

        Pod expected = pod.edit(  )
                .editSpec(  )
                .addNewVolume(  )
                .withName( volumeName )
                .withNewConfigMap(  )
                .withName( mapName )
                .endConfigMap(  )
                .endVolume(  )
                .editContainer( 0 )
                .addNewVolumeMount(  )
                .withName( volumeName )
                .withMountPath( mountPath )
                .withSubPath( subPath )
                .endVolumeMount(  )
                .endContainer(  )
                .endSpec(  )
                .build(  )

        when:
        Pod actual = VolumeMounter.mount( pod, mount )

        then:
        actual == expected
    }

    def "Add deployment volume mount for configmap."()
    {
        given:
        String volumeName = "tdk8s-vol-0"
        String container = NginxResources.NGINX_CONTAINER_NAME
        String mountPath = "/data/test.txt"
        String subPath = "test.txt"
        Deployment deployment = NginxResources.SIMPLE_NGINX_DEPLOYMENT

        FileMount mount = FileMount.newBuilder().configMap( map )
                .mountPath( mountPath )
                .container( container )
                .volume( volumeName )
                .build()

        Deployment expected = deployment.edit(  )
                .editSpec(  )
                .editTemplate(  ).editSpec(  )
                .addNewVolume(  )
                .withName( volumeName )
                .withNewConfigMap(  )
                .withName( mapName )
                .endConfigMap(  )
                .endVolume(  )
                .editContainer( 0 )
                .addNewVolumeMount(  )
                .withName( volumeName )
                .withMountPath( mountPath )
                .withSubPath( subPath )
                .endVolumeMount(  )
                .endContainer(  )
                .endSpec(  )
                .endTemplate(  ).endSpec(  )
                .build(  )

        when:
        Deployment actual = VolumeMounter.mount( deployment, mount )

        then:
        actual == expected
    }

    def "Add deployment volume mount without container name for configmap."()
    {
        given:
        String volumeName = "tdk8s-vol-0"
        String mountPath = "/data/test.txt"
        String subPath = "test.txt"
        Deployment deployment = NginxResources.SIMPLE_NGINX_DEPLOYMENT

        FileMount mount = FileMount.newBuilder().configMap( map )
                .mountPath( mountPath )
                .volume( volumeName )
                .build()

        Deployment expected = deployment.edit(  )
                .editSpec(  )
                .editTemplate(  ).editSpec(  )
                .addNewVolume(  )
                .withName( volumeName )
                .withNewConfigMap(  )
                .withName( mapName )
                .endConfigMap(  )
                .endVolume(  )
                .editContainer( 0 )
                .addNewVolumeMount(  )
                .withName( volumeName )
                .withMountPath( mountPath )
                .withSubPath( subPath )
                .endVolumeMount(  )
                .endContainer(  )
                .endSpec(  )
                .endTemplate(  ).endSpec(  )
                .build(  )

        when:
        Deployment actual = VolumeMounter.mount( deployment, mount )

        then:
        actual == expected
    }

    def "Add pod volume mount invalid container name, throws ResourceNotFound."()
    {
        given:
        String volumeName = "tdk8s-vol-01"
        String container = "invalid"
        String mountPath = "/data/test.txt"

        Deployment deployment = NginxResources.SIMPLE_NGINX_DEPLOYMENT

        FileMount mount = FileMount.newBuilder().configMap( map )
                .mountPath( mountPath )
                .container( container )
                .volume( volumeName )
                .build()

        when:
        VolumeMounter.mount( deployment, mount )

        then:
        thrown ResourceNotFoundException
    }

    def "Add deployment volume with nulls, throws NullPointerException."()
    {
        when:
        VolumeMounter.mount( deployment as Deployment, mount )

        then:
        thrown NullPointerException

        where:
        deployment | mount
        NginxResources.SIMPLE_NGINX_DEPLOYMENT | null
        null                                   | FileMount.newBuilder().configMap( map ).mountPath( "/tmp/t.log" ).build()
        null                                   | null
    }

    def "Add deployment volume mount with different mountpath file name for configmap."()
    {
        given:
        String volumeName = "tdk8s-vol-0"
        String container = NginxResources.NGINX_CONTAINER_NAME
        String mountPath = "/data/test2.txt"
        String subPath = "test.txt"
        Deployment deployment = NginxResources.SIMPLE_NGINX_DEPLOYMENT

        FileMount mount = FileMount.newBuilder().configMap( map )
                .mountPath( mountPath )
                .container( container )
                .volume( volumeName )
                .build()

        Deployment expected = deployment.edit(  )
                .editSpec(  )
                .editTemplate(  ).editSpec(  )
                .addNewVolume(  )
                .withName( volumeName )
                .withNewConfigMap(  )
                .withName( mapName )
                .endConfigMap(  )
                .endVolume(  )
                .editContainer( 0 )
                .addNewVolumeMount(  )
                .withName( volumeName )
                .withMountPath( mountPath )
                .withSubPath( subPath )
                .endVolumeMount(  )
                .endContainer(  )
                .endSpec(  )
                .endTemplate(  ).endSpec(  )
                .build(  )

        when:
        Deployment actual = VolumeMounter.mount( deployment, mount )

        then:
        actual == expected
    }

}
