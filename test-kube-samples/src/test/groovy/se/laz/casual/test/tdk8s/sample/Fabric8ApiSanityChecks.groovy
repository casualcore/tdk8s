/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.sample

import io.fabric8.kubernetes.api.model.Pod
import io.fabric8.kubernetes.api.model.Service
import io.fabric8.kubernetes.api.model.apps.Deployment
import spock.lang.Specification

class Fabric8ApiSanityChecks extends Specification
{
    def "Pod objects equal."()
    {
        expect:
        WildflyResources.SIMPLE_WILDFLY_POD == WildflyResources.SIMPLE_WILDFLY_POD
        NginxResources.SIMPLE_NGINX_POD == NginxResources.SIMPLE_NGINX_POD
        NginxResources.SIMPLE_NGINX_POD2 == NginxResources.SIMPLE_NGINX_POD2
        AlpineResources.SIMPLE_ALPINE_POD == AlpineResources.SIMPLE_ALPINE_POD

        WildflyResources.SIMPLE_WILDFLY_POD != NginxResources.SIMPLE_NGINX_POD
        NginxResources.SIMPLE_NGINX_POD != AlpineResources.SIMPLE_ALPINE_POD
        NginxResources.SIMPLE_NGINX_POD != NginxResources.SIMPLE_NGINX_POD2

        WildflyResources.SIMPLE_WILDFLY_POD.hashCode(  ) == WildflyResources.SIMPLE_WILDFLY_POD.hashCode(  )
        NginxResources.SIMPLE_NGINX_POD.hashCode(  ) == NginxResources.SIMPLE_NGINX_POD.hashCode(  )
        AlpineResources.SIMPLE_ALPINE_POD.hashCode(  ) == AlpineResources.SIMPLE_ALPINE_POD.hashCode(  )

        WildflyResources.SIMPLE_WILDFLY_POD.hashCode(  ) != NginxResources.SIMPLE_NGINX_POD.hashCode(  )
        NginxResources.SIMPLE_NGINX_POD.hashCode(  ) != AlpineResources.SIMPLE_ALPINE_POD.hashCode(  )
    }

    def "Pod objects edited, create new, original not effected."()
    {
        given:
        String originalName = WildflyResources.SIMPLE_WILDFLY_POD.getMetadata(  ).getName(  )

        when:
        Pod updated = WildflyResources.SIMPLE_WILDFLY_POD.edit(  )
                .editOrNewMetadata(  )
                .withName( "updated" )
                .endMetadata(  )
                .build(  )

        then:
        updated.getMetadata(  ).getName(  ) != originalName
        WildflyResources.SIMPLE_WILDFLY_POD.getMetadata(  ).getName(  ) == originalName
        WildflyResources.SIMPLE_WILDFLY_POD != updated
    }

    def "Pod object editied without changes still equal."()
    {
        when:
        Pod copy = WildflyResources.SIMPLE_WILDFLY_POD.edit(  ).build(  )

        then:
        copy == WildflyResources.SIMPLE_WILDFLY_POD
    }

    def "Service objects equal"()
    {
        expect:
        WildflyResources.SIMPLE_WILDFLY_SERVICE == WildflyResources.SIMPLE_WILDFLY_SERVICE
        WildflyResources.EXTERNAL_WILDFLY_SERVICE == WildflyResources.EXTERNAL_WILDFLY_SERVICE
        NginxResources.SIMPLE_NGINX_SERVICE == NginxResources.SIMPLE_NGINX_SERVICE
        NginxResources.SIMPLE_NGINX_SERVICE2 == NginxResources.SIMPLE_NGINX_SERVICE2
        NginxResources.SIMPLE_NGINX_SERVICE3 == NginxResources.SIMPLE_NGINX_SERVICE3
        NginxResources.EXTERNAL_NGINX_SERVICE == NginxResources.EXTERNAL_NGINX_SERVICE

        WildflyResources.SIMPLE_WILDFLY_SERVICE.hashCode(  ) == WildflyResources.SIMPLE_WILDFLY_SERVICE.hashCode(  )
        NginxResources.SIMPLE_NGINX_SERVICE.hashCode(  ) == NginxResources.SIMPLE_NGINX_SERVICE.hashCode(  )

        WildflyResources.SIMPLE_WILDFLY_SERVICE != NginxResources.SIMPLE_NGINX_SERVICE
        WildflyResources.EXTERNAL_WILDFLY_SERVICE != NginxResources.EXTERNAL_NGINX_SERVICE

        WildflyResources.SIMPLE_WILDFLY_SERVICE.hashCode(  ) != NginxResources.SIMPLE_NGINX_SERVICE.hashCode(  )
        WildflyResources.EXTERNAL_WILDFLY_SERVICE.hashCode(  ) != NginxResources.EXTERNAL_NGINX_SERVICE.hashCode(  )
    }

    def "Service edited, creates new, original not effected."()
    {
        given:
        String originalName = WildflyResources.EXTERNAL_WILDFLY_SERVICE.getMetadata(  ).getName(  )

        when:
        Service updated = WildflyResources.EXTERNAL_WILDFLY_SERVICE.edit(  )
                .editOrNewMetadata(  )
                .withName( "updated" )
                .endMetadata(  )
                .build(  )

        then:
        updated.getMetadata(  ).getName(  ) != originalName
        WildflyResources.EXTERNAL_WILDFLY_SERVICE.getMetadata(  ).getName(  ) == originalName
        updated != WildflyResources.EXTERNAL_WILDFLY_SERVICE
    }

    def "Service object edited without changes still equal."()
    {
        when:
        Service copy = WildflyResources.EXTERNAL_WILDFLY_SERVICE.edit(  ).build(  )

        then:
        copy == WildflyResources.EXTERNAL_WILDFLY_SERVICE
    }

    def "Deployment object equal."()
    {
        expect:
        NginxResources.SIMPLE_NGINX_DEPLOYMENT == NginxResources.SIMPLE_NGINX_DEPLOYMENT
    }

    def "Deployment object edited without changes still equal."()
    {
        when:
        Deployment copy = NginxResources.SIMPLE_NGINX_DEPLOYMENT.edit(  ).build(  )

        then:
        copy == NginxResources.SIMPLE_NGINX_DEPLOYMENT
    }

}
