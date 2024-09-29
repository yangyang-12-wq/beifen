/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.dataproxy.config;

import org.apache.inlong.dataproxy.config.holder.ConfigUpdateCallback;
import org.apache.inlong.dataproxy.consts.AttrConstants;

import com.google.common.base.Splitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.inlong.dataproxy.config.ConfigManager.CONFIG_HOLDER_MAP;

public abstract class ConfigHolder {

    public static final Splitter.MapSplitter MAP_SPLITTER = Splitter.on(AttrConstants.SEPARATOR)
            .trimResults().withKeyValueSeparator(AttrConstants.KEY_VALUE_SEPARATOR);
    private static final Logger LOG = LoggerFactory.getLogger(ConfigHolder.class);
    private final String fileName;
    private final AtomicBoolean fileChanged = new AtomicBoolean(false);
    // list of callbacks for this holder
    private final List<ConfigUpdateCallback> callbackList = new ArrayList<>();
    private long lastModifyTime = 0;
    private String filePath;
    private File configFile;

    public ConfigHolder(String fileName) {
        this.fileName = fileName;
        setFilePath(fileName);
        CONFIG_HOLDER_MAP.put(this, System.currentTimeMillis());
    }

    /**
     * add callback
     *
     * @param callback - callback
     */
    public void addUpdateCallback(ConfigUpdateCallback callback) {
        callbackList.add(callback);
    }

    /**
     * execute callbacks
     */
    public void executeCallbacks() {
        for (ConfigUpdateCallback callback : callbackList) {
            callback.update();
        }
    }

    /**
     * load from file to holder
     *
     * @return - true if the configure file read, otherwise it will be false.
     */
    protected abstract boolean loadFromFileToHolder();

    /**
     * check updater
     *
     * @return - true if updated
     */
    public boolean checkAndUpdateHolder() {
        if (fileChanged.compareAndSet(true, false)
                || (configFile != null && configFile.lastModified() != this.lastModifyTime)) {
            long startTime = System.currentTimeMillis();
            if (loadFromFileToHolder()) {
                boolean initialized = (this.lastModifyTime != 0L);
                if (configFile != null) {
                    this.lastModifyTime = configFile.lastModified();
                }
                if (initialized) {
                    LOG.info("File {} has changed, reload from local file, wast {} ms",
                            this.fileName, (System.currentTimeMillis() - startTime));
                } else {
                    LOG.info("File {} has imported, reload from local file, wast {} ms",
                            this.fileName, (System.currentTimeMillis() - startTime));
                }
                return true;
            }
            LOG.warn("File {} has changed, but reload content failure", this.fileName);
        }
        return false;
    }

    public String getFileName() {
        return fileName;
    }

    /**
     * get file name
     *
     * @return file name with prefix
     */
    public String getNextBackupFileName() {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
        String dateStr = format.format(new Date(System.currentTimeMillis()));
        return getFilePath() + "." + dateStr;
    }

    /**
     * file name with base path.
     */
    public String getFilePath() {
        return filePath;
    }

    private void setFilePath(String fileName) {
        URL url = getClass().getClassLoader().getResource(fileName);
        if (url != null) {
            this.filePath = url.getPath();
            this.configFile = new File(this.filePath);
            LOG.info("Set {} file path, lastTime: {}, currentTime: {}",
                    fileName, lastModifyTime, configFile.lastModified());
        }
    }

    public void setFileChanged() {
        fileChanged.set(true);
    }
}
