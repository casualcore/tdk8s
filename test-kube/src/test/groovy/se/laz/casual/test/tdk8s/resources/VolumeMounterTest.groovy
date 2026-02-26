/*
 * Copyright (c) 2026, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.resources

import io.fabric8.kubernetes.api.model.ConfigMap
import io.fabric8.kubernetes.api.model.ConfigMapBuilder
import io.fabric8.kubernetes.api.model.Pod
import se.laz.casual.test.tdk8s.sample.NginxResources
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Files

class VolumeMounterTest extends Specification
{
    @Shared
    String mapName = "config-map-1"

    @Shared
    ConfigMap map = new ConfigMapBuilder()
            .withNewMetadata(  ).withName( mapName ).endMetadata(  )
            .addToData( "test.txt", Files.readString( new File( "./src/test/resources/test.txt").toPath(  ) ) )
            .build()


    def "Add pod volume mount for configmap."()
    {
        given:
        String volumeName = "tdk8s-vol-0"
        String mountPath = "/data/test.txt"
        String subPath = "test.txt"
        Pod pod = NginxResources.SIMPLE_NGINX_POD

        Pod expected = NginxResources.SIMPLE_NGINX_POD.edit(  )
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
        Pod actual = new PodVolumeMounter( pod )
                    .configMap( mapName )
                    .name( volumeName )
                    .mountPath( mountPath )
                    .subPath( subPath )
                .mount()

        then:
        actual == expected
    }

    def "Add pod volume with defaults."()
    {
        given:
        String mountPath = "/data/test.txt"

        Pod pod = NginxResources.SIMPLE_NGINX_POD

        when:
        Pod actual = new PodVolumeMounter( pod )
                .configMap( mapName )
                .mountPath( mountPath )
            .mount()

        then:
        actual.getSpec(  ).getVolumes(  ).size(  ) == 1
        actual.getSpec(  ).getContainers(  ).get( 0 ).getVolumeMounts(  ).size(  ) == 1
    }

    def "Add volume, both pvc and configmap provided - fails."()
    {
        given:
        VolumeMounter<?> instance = initial
            .configMap( cm )
            .persistentVolumeClaim( "other" )
            .mountPath( mountPath )

        when:
        instance.mount()

        then:
        thrown IllegalStateException

        where:
        cm        | pvc    | mountPath | initial
        "config1" | "pvc1" | "/data"   | new PodVolumeMounter( NginxResources.SIMPLE_NGINX_POD )
        "config2" | "pvc2" | "/config" | new DeploymentVolumeMounter( NginxResources.SIMPLE_NGINX_DEPLOYMENT )
    }

}
