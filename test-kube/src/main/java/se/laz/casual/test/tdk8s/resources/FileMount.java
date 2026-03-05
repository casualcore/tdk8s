/*
 * Copyright (c) 2026, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.resources;

import io.fabric8.kubernetes.api.model.ConfigMap;

import java.util.Objects;

/**
 * Definition of the file mounted from a ConfigMap on a k8s resource e.g. Pod, Deployment.
 */
public class FileMount
{
    public static final String DEFAULT_VOLUME_NAME = "vol-01";

    private final ConfigMap configMap;
    private final String mountPath;
    private final String subPath;
    private final String volume;
    private final String container;

    private FileMount( ConfigMap configMap, String mountPath, String subPath, String volume, String containerName )
    {
        this.configMap = configMap;
        this.mountPath = mountPath;
        this.subPath = subPath;
        this.volume = volume;
        this.container = containerName;
    }

    /**
     * ConfigMap containing the file data.
     * @return ConfigMap.
     */
    public ConfigMap getConfigMap()
    {
        return configMap;
    }

    /**
     * The container mountPath for the file.
     *
     * @return container mountPath.
     */
    public String getMountPath()
    {
        return mountPath;
    }

    /**
     * The entry of the ConfigMap to use as the file.
     *
     * @return ConfigMap entry subpath.
     */
    public String getSubPath()
    {
        return subPath;
    }

    /**
     * The container volume name to use for the mount.
     *
     * @return container volume name.
     */
    public String getVolume()
    {
        return volume;
    }

    /**
     * The named container to mount the file upon.
     *
     * @return container to mount upon.
     */
    public String getContainer()
    {
        return container;
    }

    public static Builder newBuilder()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private ConfigMap configMap;
        private String mountPath;
        private String subPath;
        private String volume = DEFAULT_VOLUME_NAME;
        private String container;

        /**
         * Mandatory - populated ConfigMap containing the file data to mount.
         *
         * @param configMap containing the file data to mount.
         * @return Builder.
         */
        public Builder configMap( ConfigMap configMap )
        {
            this.configMap = configMap;
            return this;
        }

        /**
         * Mandatory - container path for file mount e.g. `/mnt/file.txt`
         *
         * @param mountPath container path to mount the file.
         * @return Builder.
         */
        public Builder mountPath( String mountPath )
        {
            this.mountPath = mountPath;
            return this;
        }

        /**
         * Optional - ConfigMap entry (subpath) to use as the file contents.
         * <br/>
         * If not provided, the ConfigMap must have a single entry, which will be used.
         *
         * @param subPath of the ConfigMap to use.
         * @return Builder.
         */
        public Builder subPath( String subPath )
        {
            this.subPath = subPath;
            return this;
        }

        /**
         * Optional - volume name to mount the file.
         * <br/>
         * If not provided a default name will be used.
         *
         * @param volume name of the volume.
         * @return Builder.
         */
        public Builder volume( String volume )
        {
            this.volume = volume;
            return this;
        }

        /**
         * Optional - name of the container to mount the file upon.
         *<br/>
         * If not provided the first container will be used.
         *
         * @param container name to mount the file upon.
         * @return Builder.
         */
        public Builder container( String container )
        {
            this.container = container;
            return this;
        }

        public FileMount build()
        {
            Objects.requireNonNull( configMap, "ConfigMap is null." );
            Objects.requireNonNull( mountPath, "Mount path is null."  );

            this.subPath = confirmValidSubPath( );

            return new FileMount( configMap, mountPath, subPath, volume, container );
        }

        private String confirmValidSubPath( )
        {
            if( this.subPath == null )
            {
                if( this.configMap.getData().size() != 1 )
                {
                    throw new IllegalArgumentException( "ConfigMap contains multiple entries without a specified subpath." );
                }
                return this.configMap.getData().keySet().iterator().next();
            }

            if( !this.configMap.getData().containsKey( this.subPath ) )
            {
                throw new IllegalArgumentException( "ConfigMap does not contain an entry: " + this.subPath );
            }
            return this.subPath;
        }
    }
}
