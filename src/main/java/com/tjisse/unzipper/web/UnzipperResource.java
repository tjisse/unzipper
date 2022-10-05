package com.tjisse.unzipper.web;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.io.SequenceInputStream;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;

@RestController
public class UnzipperResource {

    private final static byte[] SPANNING_SIGNATURE = { 0x50, 0x4b, 0x07, 0x08 };
    private final static Logger logger = LoggerFactory.getLogger(UnzipperResource.class);

    @Autowired
    private AmazonS3 s3client;

    @PostMapping("/upload")
    private ResponseEntity<Object> methodName(HttpServletRequest request) throws IOException, FileUploadException {
        var upload = new ServletFileUpload();
        var fileItemIterator = upload.getItemIterator(request);
        var enumeration = new MultipartFileStreamEnumeration(fileItemIterator, this::removeSpanningSignatureFromStream);

        var filesProcessed = 0L;
        try (var zipInputStream = new ZipInputStream(new SequenceInputStream(enumeration))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                var bytes = zipInputStream.readAllBytes();

                var metadata = new ObjectMetadata();
                metadata.setContentLength(bytes.length);

                var fileName = entry.getName();
                logger.info("Uploading {}", fileName);
                s3client.putObject("zipper", fileName, new ByteArrayInputStream(bytes), metadata);
                filesProcessed++;
            }
        }

        if (filesProcessed == 0) {
            return ResponseEntity.badRequest().body("Bad request, no files processed");
        }
        return ResponseEntity.ok().body(filesProcessed + " files processed");
    }

    private InputStream removeSpanningSignatureFromStream(FileItemStream fileItemStream) {
        try {
            var buf4 = new byte[4];
            var pushbackStream = new PushbackInputStream(fileItemStream.openStream(), buf4.length);
            if (pushbackStream.read(buf4) != buf4.length) {
                throw new IOException(fileItemStream.getName() + " is too small for a zip file/segment");
            }
            if (Arrays.equals(buf4, SPANNING_SIGNATURE)) {
                logger.info("Spanning signature removed from {}", fileItemStream.getName());
            } else {
                pushbackStream.unread(buf4, 0, buf4.length);
            }
            return pushbackStream;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
