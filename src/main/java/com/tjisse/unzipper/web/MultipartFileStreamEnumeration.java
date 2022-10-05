package com.tjisse.unzipper.web;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.function.Function;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;

public class MultipartFileStreamEnumeration implements Enumeration<InputStream> {

    private FileItemIterator fileItemIterator;
    private Function<FileItemStream, InputStream> fileItemStreamInterceptor;

    public MultipartFileStreamEnumeration(
            FileItemIterator fileItemIterator,
            Function<FileItemStream, InputStream> fileItemStreamInterceptor) {
        this.fileItemIterator = fileItemIterator;
        this.fileItemStreamInterceptor = fileItemStreamInterceptor;
    }

    @Override
    public boolean hasMoreElements() {
        try {
            return fileItemIterator.hasNext();
        } catch (FileUploadException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public InputStream nextElement() {
        try {
            return fileItemStreamInterceptor.apply(fileItemIterator.next());
        } catch (IOException | FileUploadException e) {
            throw new IllegalStateException(e);
        }
    }
}
