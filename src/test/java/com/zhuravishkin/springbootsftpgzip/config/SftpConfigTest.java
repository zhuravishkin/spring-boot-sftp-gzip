package com.zhuravishkin.springbootsftpgzip.config;

import com.ghdiri.abdallah.sftp.DummySftpServerExtension;
import com.ghdiri.abdallah.sftp.SftpGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class SftpConfigTest {
    @SpyBean
    private SftpConfig sftpConfig;

    @RegisterExtension
    static final DummySftpServerExtension extension = DummySftpServerExtension.Builder.create()
            .port(8081)
            .addCredentials("user", "password")
            .build();

    @Test
    void testPayloadUpload(SftpGateway gateway) throws IOException {
        InputStream inputStream = new GZIPInputStream(new FileInputStream("src/test/resources/keanu.txt.gz"));
        gateway.createDirectories(".sftp");
        gateway.putFile(".sftp/keanu.txt.gz", inputStream);
        inputStream.close();
        assertTrue(gateway.fileExists(".sftp/keanu.txt.gz"));
        System.out.println(gateway.getFile(".sftp/keanu.txt.gz"));
        sftpConfig.getFileFromSftpServer();
    }
}