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
import java.util.Vector;
import java.util.zip.GZIPInputStream;

@Slf4j
@Configuration
@EnableScheduling
public class SftpConfig {
    @Scheduled(fixedDelay = 10_000)
    private void getFileFromSftpServer() {
        log.info("Loading file from sftp-server");
        ChannelSftp channelSftp = null;
        Channel channel = null;
        Session session = null;
        try {
            JSch jSch = new JSch();
            session = jSch.getSession("user", "192.168.0.13", 22);
            session.setPassword("password");
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
            channel = session.openChannel("sftp");
            channel.connect();
            channelSftp = (ChannelSftp) channel;
            List<String> fileNameList = new ArrayList<>();
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
                    try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                            new GZIPInputStream(channelSftp.get(path + "/" + fileName))))) {
                        String line;
                        while ((line = bufferedReader.readLine()) != null) {
                            System.out.println(line);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (!fileNameList.isEmpty()) {
                for (String fileName : fileNameList) {
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
