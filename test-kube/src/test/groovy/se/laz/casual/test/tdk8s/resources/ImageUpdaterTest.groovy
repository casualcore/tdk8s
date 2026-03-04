/*
 * Copyright (c) 2026, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.resources

import io.fabric8.kubernetes.api.model.Pod
import io.fabric8.kubernetes.api.model.apps.Deployment
import se.laz.casual.test.tdk8s.sample.NginxResources
import se.laz.casual.test.tdk8s.store.ResourceNotFoundException
import spock.lang.Specification

class ImageUpdaterTest extends Specification
{
    def "Change pods first container image to the one provided"()
    {
        given:
        Pod pod = NginxResources.SIMPLE_NGINX_POD
        Pod expected = NginxResources.SIMPLE_NGINX_POD.edit()
                .editSpec().editContainer( 0 )
                .withImage( image )
                .endContainer().endSpec().build()

        when:
        Pod actual = ImageUpdater.setImage( pod, image )

        then:
        actual == expected

        where:
        image << [
                "nginx-updated:5.2.0",
                "192.168.0.1:5000/repo/image:version",
                "ghcr.io/busybox:0.0.1",
                "quay.io/wildfly/wildfly:32.0.1.Final-jdk21"
        ]
    }

    def "Change pods specific container image to the one provided."()
    {
        given:
        Pod pod = NginxResources.SIMPLE_NGINX_POD.edit().editSpec()
                .addNewContainer()
                .withName( "additional" )
                .withImage( "tobechanged." )
                .endContainer().endSpec().build()

        Pod expected = NginxResources.SIMPLE_NGINX_POD.edit().editSpec()
                .addNewContainer()
                .withName( "additional" )
                .withImage( image )
                .endContainer().endSpec().build()

        when:
        Pod actual = ImageUpdater.setImage( pod, image, "additional" )

        then:
        actual == expected

        where:
        image << [
                "nginx-updated:5.2.0",
                "192.168.0.1:5000/repo/image:version",
                "ghcr.io/busybox:0.0.1",
                "quay.io/wildfly/wildfly:32.0.1.Final-jdk21"
        ]
    }

    def "Change deployments first container image to the one provided"()
    {
        given:
        Deployment deployment = NginxResources.SIMPLE_NGINX_DEPLOYMENT
        Deployment expected = NginxResources.SIMPLE_NGINX_DEPLOYMENT.edit()
                .editSpec().editTemplate(  ).editSpec(  ).editContainer( 0 )
                .withImage( image )
                .endContainer().endSpec(  )
                .endTemplate(  ).endSpec(  )
                .build()

        when:
        Deployment actual = ImageUpdater.setImage( deployment, image )

        then:
        actual == expected

        where:
        image << [
                "nginx-updated:5.2.0",
                "192.168.0.1:5000/repo/image:version",
                "ghcr.io/busybox:0.0.1",
                "quay.io/wildfly/wildfly:32.0.1.Final-jdk21"
        ]
    }

    def "Change deployments specific container image to the one provided."()
    {
        given:
        Deployment deployment = NginxResources.SIMPLE_NGINX_DEPLOYMENT.edit().editSpec()
                .editTemplate(  ).editSpec(  )
                .addNewContainer()
                .withName( "additional" )
                .withImage( "tobechanged." )
                .endContainer().endSpec()
                .endTemplate(  ).endSpec(  )
                .build()

        Deployment expected = NginxResources.SIMPLE_NGINX_DEPLOYMENT.edit().editSpec()
                .editTemplate(  ).editSpec(  )
                .addNewContainer()
                .withName( "additional" )
                .withImage( image )
                .endContainer().endSpec()
                .endTemplate(  ).endSpec(  )
                .build()

        when:
        Deployment actual = ImageUpdater.setImage( deployment, image, "additional" )

        then:
        actual == expected

        where:
        image << [
                "nginx-updated:5.2.0",
                "192.168.0.1:5000/repo/image:version",
                "ghcr.io/busybox:0.0.1",
                "quay.io/wildfly/wildfly:32.0.1.Final-jdk21"
        ]
    }

    def "Change pod container image, container name doesn't exist"()
    {
        when:
        ImageUpdater.setImage( NginxResources.SIMPLE_NGINX_POD, "update", "invalid" )

        then:
        thrown ResourceNotFoundException
    }

    def "Change image pod nulls."()
    {
        when:
        ImageUpdater.setImage( pod as Pod, image )

        then:
        thrown NullPointerException

        where:
        pod                             | image
        NginxResources.SIMPLE_NGINX_POD | null
        null                            | "imagename:version"
        null                            | null
    }

    def "Change image pod nulls."()
    {
        when:
        ImageUpdater.setImage( pod as Pod, image, container )

        then:
        thrown NullPointerException

        where:
        pod                             | image               | container
        null                            | "imagename:version" | "container"
        NginxResources.SIMPLE_NGINX_POD | null                | "container"
        NginxResources.SIMPLE_NGINX_POD | "imagename:version" | null
        null                            | null                | null
    }

    def "Change pod container image, container name doesn't exist"()
    {
        when:
        ImageUpdater.setImage( NginxResources.SIMPLE_NGINX_DEPLOYMENT, "update", "invalid" )

        then:
        thrown ResourceNotFoundException
    }

    def "Change image deployment nulls."()
    {
        when:
        ImageUpdater.setImage( deployment as Deployment, image )

        then:
        thrown NullPointerException

        where:
        deployment                             | image
        NginxResources.SIMPLE_NGINX_DEPLOYMENT | null
        null                                   | "imagename:version"
        null                                   | null
    }

    def "Change image deployment nulls."()
    {
        when:
        ImageUpdater.setImage( deployment as Deployment, image, container )

        then:
        thrown NullPointerException

        where:
        deployment | image | container
        null                                   | "imagename:version" | "container"
        NginxResources.SIMPLE_NGINX_DEPLOYMENT | null                | "container"
        NginxResources.SIMPLE_NGINX_DEPLOYMENT | "imagename:version" | null
        null                                   | null                | null
    }

}
