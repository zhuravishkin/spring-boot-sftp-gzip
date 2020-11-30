package com.zhuravishkin.springbootsftpgzip.config;

import com.jcraft.jsch.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Vector;
import java.util.zip.GZIPInputStream;

@Slf4j
@Configuration
@EnableScheduling
public class SftpConfig {
    @Scheduled(fixedDelay = 10_000)
    public void getFileFromSftpServer() {
        log.info("Loading file from sftp-server");
        ChannelSftp channelSftp = null;
        Channel channel = null;
        Session session = null;
        try {
            JSch jSch = new JSch();
            session = jSch.getSession("user", "localhost", 8081);
            session.setPassword("password");
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
            channel = session.openChannel("sftp");
            channel.connect();
            channelSftp = (ChannelSftp) channel;
            List<String> fileNameList = new ArrayList<>();
            List<String> newFileNameList = new ArrayList<>();
            String path = ".sftp";
            Vector<ChannelSftp.LsEntry> filelist = channelSftp.ls(path);
            for (ChannelSftp.LsEntry entry : filelist) {
                String fileName = entry.getFilename();
                if (fileName.endsWith(".gz")) {
                    fileNameList.add(fileName);
                }
            }
            if (!fileNameList.isEmpty()) {
                for (String fileName : fileNameList) {
                    String newFileName = null;
                    try {
                        newFileName = UUID.randomUUID().toString() + ".gz";
                        channelSftp.rename(path + "/" + fileName, path + "/" + newFileName);
                        newFileNameList.add(newFileName);
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                    try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                            new GZIPInputStream(channelSftp.get(path + "/" + newFileName))))) {
                        String line;
                        while ((line = bufferedReader.readLine()) != null) {
                            System.out.println(line);
                        }
                    } catch (IOException e) {
                        log.error(e.getMessage(), e);
                    }
                }
            }
            if (!newFileNameList.isEmpty()) {
                for (String fileName : newFileNameList) {
                    channelSftp.rm(path + "/" + fileName);
                }
            }
        } catch (JSchException | SftpException e) {
            log.error(e.getMessage(), e);
        } finally {
            if (channelSftp != null) {
                channelSftp.exit();
            }
            if (channel != null) {
                channel.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }
    }
}
