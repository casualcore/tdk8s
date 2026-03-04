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
    private final String volume;
    private final String container;

    private FileMount( ConfigMap configMap, String mountPath, String volume, String containerName )
    {
        this.configMap = configMap;
        this.mountPath = mountPath;
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
         * Optional - volume name to mount the file.
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

            return new FileMount( configMap, mountPath, volume, container );
        }
    }
}
