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

package org.apache.inlong.agent.plugin.sources;

import org.apache.inlong.agent.conf.InstanceProfile;
import org.apache.inlong.agent.conf.OffsetProfile;
import org.apache.inlong.agent.constant.DataCollectType;
import org.apache.inlong.agent.constant.TaskConstants;
import org.apache.inlong.agent.core.FileStaticManager;
import org.apache.inlong.agent.core.FileStaticManager.FileStatic;
import org.apache.inlong.agent.core.task.OffsetManager;
import org.apache.inlong.agent.except.FileException;
import org.apache.inlong.agent.metrics.audit.AuditUtils;
import org.apache.inlong.agent.plugin.sources.file.AbstractSource;
import org.apache.inlong.agent.plugin.utils.file.FileDataUtils;
import org.apache.inlong.agent.utils.AgentUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import static org.apache.inlong.agent.constant.TaskConstants.SOURCE_DATA_CONTENT_STYLE;

/**
 * Read text files
 */
public class LogFileSource extends AbstractSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogFileSource.class);
    private final Integer SIZE_OF_BUFFER_TO_READ_FILE = 64 * 1024;
    private final Long INODE_UPDATE_INTERVAL_MS = 1000L;
    private final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // 设置格式

    private String fileName;
    private File file;
    private byte[] bufferToReadFile;
    public volatile long linePosition = 0;
    public volatile long bytePosition = 0;
    private boolean isIncrement = false;
    private volatile boolean fileExist = true;
    private String inodeInfo;
    private volatile long lastInodeUpdateTime = 0;
    private RandomAccessFile randomAccessFile;

    public LogFileSource() {
    }

    @Override
    protected void initSource(InstanceProfile profile) {
        try {
            LOGGER.info("LogFileSource init: {}", profile.toJsonStr());
            fileName = profile.getInstanceId();
            bufferToReadFile = new byte[SIZE_OF_BUFFER_TO_READ_FILE];
            isIncrement = isIncrement(profile);
            file = new File(fileName);
            inodeInfo = profile.get(TaskConstants.INODE_INFO);
            lastInodeUpdateTime = AgentUtils.getCurrentTime();
            linePosition = getInitLineOffset(isIncrement, taskId, instanceId, inodeInfo);
            bytePosition = getBytePositionByLine(linePosition);
            randomAccessFile = new RandomAccessFile(file, "r");
        } catch (Exception ex) {
            stopRunning();
            throw new FileException("error init stream for " + file.getPath(), ex);
        }
    }

    @Override
    protected boolean doPrepareToRead() {
        if (isInodeChanged()) {
            fileExist = false;
            LOGGER.info("inode changed, instance will restart and offset will be clean, file {}",
                    fileName);
            return false;
        }
        if (file.length() < bytePosition) {
            fileExist = false;
            LOGGER.info("file rotate, instance will restart and offset will be clean, file {}",
                    fileName);
            return false;
        }
        return true;
    }

    @Override
    protected List<SourceData> readFromSource() {
        try {
            return readFromPos(bytePosition);
        } catch (FileNotFoundException e) {
            fileExist = false;
            LOGGER.error("readFromPos file deleted error: ", e);
        } catch (IOException e) {
            LOGGER.error("readFromPos error: ", e);
        }
        return null;
    }

    @Override
    protected void printCurrentState() {
        LOGGER.info("path is {}, linePosition {}, bytePosition is {} file len {}", file.getName(), linePosition,
                bytePosition, file.length());
    }

    @Override
    protected String getThreadName() {
        return "log-file-source-" + taskId + "-" + fileName;
    }

    private List<SourceData> readFromPos(long pos) throws IOException {
        List<byte[]> lines = new ArrayList<>();
        List<SourceData> dataList = new ArrayList<>();
        bytePosition = readLines(randomAccessFile, pos, lines, BATCH_READ_LINE_COUNT, BATCH_READ_LINE_TOTAL_LEN, false);
        for (int i = 0; i < lines.size(); i++) {
            linePosition++;
            dataList.add(new SourceData(lines.get(i), Long.toString(linePosition)));
        }
        return dataList;
    }

    private int getRealLineCount(String fileName) {
        try (LineNumberReader lineNumberReader = new LineNumberReader(new FileReader(instanceId))) {
            lineNumberReader.skip(Long.MAX_VALUE);
            return lineNumberReader.getLineNumber();
        } catch (IOException ex) {
            LOGGER.error("getRealLineCount error {} file {}", ex.getMessage(), fileName);
            return 0;
        }
    }

    private long getInitLineOffset(boolean isIncrement, String taskId, String instanceId, String inodeInfo) {
        long offset = 0;
        if (offsetProfile != null && offsetProfile.getInodeInfo().compareTo(inodeInfo) == 0) {
            offset = Long.parseLong(offsetProfile.getOffset());
            int fileLineCount = getRealLineCount(instanceId);
            if (fileLineCount < offset) {
                LOGGER.info("getInitLineOffset inode no change taskId {} file rotate, offset set to 0, file {}", taskId,
                        fileName);
                offset = 0;
            } else {
                LOGGER.info("getInitLineOffset inode no change taskId {} from offset store {}, file {}", taskId, offset,
                        fileName);
            }
        } else {
            if (isIncrement) {
                offset = getRealLineCount(instanceId);
                LOGGER.info("getInitLineOffset taskId {} for new increment read from {} file {}", taskId,
                        offset, fileName);
            } else {
                offset = 0;
                LOGGER.info("getInitLineOffset taskId {} for new all read from 0 file {}", taskId, fileName);
            }
        }
        return offset;
    }

    public File getFile() {
        return file;
    }

    private boolean isIncrement(InstanceProfile profile) {
        if (profile.hasKey(TaskConstants.TASK_FILE_CONTENT_COLLECT_TYPE) && DataCollectType.INCREMENT
                .equalsIgnoreCase(profile.get(TaskConstants.TASK_FILE_CONTENT_COLLECT_TYPE))) {
            return true;
        }
        return false;
    }

    private long getBytePositionByLine(long linePosition) throws IOException {
        long pos = 0;
        long readCount = 0;
        RandomAccessFile input = null;
        try {
            input = new RandomAccessFile(file, "r");
            while (readCount < linePosition) {
                List<byte[]> lines = new ArrayList<>();
                pos = readLines(input, pos, lines, Math.min((int) (linePosition - readCount), BATCH_READ_LINE_COUNT),
                        BATCH_READ_LINE_TOTAL_LEN, true);
                readCount += lines.size();
                if (lines.size() == 0) {
                    LOGGER.error("getBytePositionByLine LineNum {} larger than the real file");
                    break;
                }
            }
        } catch (Exception e) {
            LOGGER.error("getBytePositionByLine error: ", e);
        } finally {
            if (input != null) {
                input.close();
            }
        }
        LOGGER.info("getBytePositionByLine {} LineNum {} position {}", fileName, linePosition, pos);
        return pos;
    }

    /**
     * Read new lines.
     *
     * @param reader The file to read
     * @return The new position after the lines have been read
     * @throws IOException if an I/O error occurs.
     */
    private long readLines(RandomAccessFile reader, long pos, List<byte[]> lines, int maxLineCount, int maxLineTotalLen,
            boolean isCounting)
            throws IOException {
        if (maxLineCount == 0) {
            return pos;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        reader.seek(pos);
        long rePos = pos; // position to re-read
        int num;
        int lineTotalLen = 0;
        boolean overLen = false;
        while ((num = reader.read(bufferToReadFile)) != -1) {
            LOGGER.debug("read size {}", num);
            int i = 0;
            for (; i < num; i++) {
                byte ch = bufferToReadFile[i];
                switch (ch) {
                    case '\n':
                        if (isCounting) {
                            lines.add(null);
                        } else {
                            lines.add(baos.toByteArray());
                            lineTotalLen += baos.size();
                        }
                        rePos = pos + i + 1;
                        if (overLen) {
                            LOGGER.warn("readLines over len finally string len {}",
                                    new String(baos.toByteArray()).length());
                            long auditTime = 0;
                            if (isRealTime) {
                                auditTime = AgentUtils.getCurrentTime();
                            } else {
                                auditTime = profile.getSinkDataTime();
                            }
                            AuditUtils.add(AuditUtils.AUDIT_ID_AGENT_READ_FAILED, inlongGroupId, inlongStreamId,
                                    auditTime, 1, maxPackSize, auditVersion);
                            AuditUtils.add(AuditUtils.AUDIT_ID_AGENT_READ_FAILED_REAL_TIME, inlongGroupId,
                                    inlongStreamId, AgentUtils.getCurrentTime(), 1, maxPackSize, auditVersion);
                        }
                        baos.reset();
                        overLen = false;
                        break;
                    case '\r':
                        break;
                    default:
                        if (baos.size() < maxPackSize) {
                            baos.write(ch);
                        } else {
                            overLen = true;
                        }
                }
                if (lines.size() >= maxLineCount || lineTotalLen >= maxLineTotalLen) {
                    break;
                }
            }
            if (lines.size() >= maxLineCount || lineTotalLen >= maxLineTotalLen) {
                break;
            }
            if (i == num) {
                pos = reader.getFilePointer();
            }
        }
        baos.close();
        reader.seek(rePos); // Ensure we can re-read if necessary
        return rePos;
    }

    private boolean isInodeChanged() {
        if (AgentUtils.getCurrentTime() - lastInodeUpdateTime > INODE_UPDATE_INTERVAL_MS) {
            try {
                return FileDataUtils.getInodeInfo(fileName).compareTo(inodeInfo) != 0;
            } catch (IOException e) {
                LOGGER.error("check inode change file {} error", fileName, e);
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean isRunnable() {
        return runnable && fileExist;
    }

    @Override
    public boolean sourceExist() {
        return fileExist;
    }

    @Override
    protected void releaseSource() {
        if (randomAccessFile != null) {
            try {
                if (FileStaticManager.getInstance() == null) {
                    return;
                }
                FileStatic data = new FileStatic();
                data.setTaskId(taskId);
                data.setRetry(String.valueOf(profile.isRetry()));
                data.setContentType(profile.get(SOURCE_DATA_CONTENT_STYLE));
                data.setGroupId(profile.getInlongGroupId());
                data.setStreamId(profile.getInlongStreamId());
                data.setDataTime(format.format(profile.getSinkDataTime()));
                data.setFileName(profile.getInstanceId());
                data.setFileLen(String.valueOf(randomAccessFile.length()));
                data.setReadBytes(String.valueOf(bytePosition));
                data.setReadLines(String.valueOf(linePosition));
                OffsetProfile offsetProfile = OffsetManager.getInstance().getOffset(taskId, instanceId);
                if (offsetProfile == null) {
                    return;
                }
                data.setSendLines(offsetProfile.getOffset());
                FileStaticManager.getInstance().putStaticMsg(data);
                randomAccessFile.close();
            } catch (IOException e) {
                LOGGER.error("close randomAccessFile error", e);
            }
        }
    }
}
