
/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.fabric.jaas;


import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.apache.felix.utils.properties.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

public class ZookeeperProperties extends Properties implements NodeCacheListener {

    private static final Logger LOG = LoggerFactory.getLogger(ZookeeperProperties.class);

    private final String path;
    private CuratorFramework curator;
    private NodeCache nodeCache;

    public ZookeeperProperties(String path) throws Exception {
        this.path = path;
    }

    @Override
    public void save() throws IOException {
        if (curator == null) {
            throw new IOException("Curator not bound");
        }
        StringWriter writer = new StringWriter();
        saveLayout(writer);
        try {
            curator.setData().forPath(path, writer.toString().getBytes());
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public void nodeChanged() throws Exception {
        fetchData();
    }

    protected void fetchData() throws Exception {
        byte[] data = nodeCache.getCurrentData().getData();
        String value = new String(data);
        if (value != null) {
            clear();
            load(new StringReader(value));
        }
    }

    public void bind(CuratorFramework curator) throws Exception {
        this.curator = curator;
        this.nodeCache = new NodeCache(curator, path);
        this.nodeCache.getListenable().addListener(this);
        this.nodeCache.start();
    }

    public void unbind(CuratorFramework curator) throws IOException {
        this.nodeCache.close();
        this.curator = null;
    }
}