/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tightblog.rendering.processors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tightblog.WebloggerTest;
import org.tightblog.service.MediaManager;
import org.tightblog.domain.MediaFile;
import org.tightblog.domain.Weblog;
import org.tightblog.rendering.cache.LazyExpiringCache;
import org.tightblog.rendering.requests.WeblogRequest;
import org.tightblog.repository.WeblogRepository;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MediaFileProcessorTest {

    private static Logger log = LoggerFactory.getLogger(MediaFileProcessorTest.class);

    private MediaFileProcessor processor;
    private WeblogRequest mediaFileRequest;
    private HttpServletRequest mockRequest;
    private HttpServletResponse mockResponse;
    private LazyExpiringCache mockCache;
    private WeblogRepository mockWR;
    private MediaManager mockMFM;

    private static final String TEST_IMAGE = "/hawk.jpg";

    @Before
    public void initializeMocks() throws IOException {
        mockRequest = mock(HttpServletRequest.class);
        // default is resource always needs refreshing
        when(mockRequest.getDateHeader(any())).thenReturn(
                Instant.now().minus(7, ChronoUnit.DAYS).toEpochMilli());
        mockResponse = mock(HttpServletResponse.class);
        ServletOutputStream mockSOS = mock(ServletOutputStream.class);
        when(mockResponse.getOutputStream()).thenReturn(mockSOS);
        mediaFileRequest = new WeblogRequest();
        mediaFileRequest.setExtraPathInfo("abc");
        mockCache = mock(LazyExpiringCache.class);
        mockWR = mock(WeblogRepository.class);
        mockMFM = mock(MediaManager.class);
        processor = new MediaFileProcessor(mockWR, mockCache, mockMFM);
        WeblogRequest.Creator wprCreator = mock(WeblogRequest.Creator.class);
        when(wprCreator.create(mockRequest)).thenReturn(mediaFileRequest);
        processor.setWeblogRequestCreator(wprCreator);
        Weblog weblog = new Weblog();
        weblog.setHandle("myhandle");
        when(mockWR.findByHandleAndVisibleTrue(any())).thenReturn(weblog);
    }

    @Test
    public void test404OnMissingWeblog() throws IOException {
        mediaFileRequest.setWeblogHandle("myhandle");
        when(mockWR.findByHandleAndVisibleTrue("myhandle")).thenReturn(null);
        processor.getMediaFile(mockRequest, mockResponse);
        verify(mockResponse).sendError(SC_NOT_FOUND);
        verify(mockCache, never()).incrementIncomingRequests();
    }

    @Test
    public void test404OnNoPathInfo() throws IOException {
        mediaFileRequest.setExtraPathInfo(null);
        processor.getMediaFile(mockRequest, mockResponse);
        verify(mockMFM, never()).getMediaFileWithContent(anyString());
        verify(mockResponse).sendError(SC_NOT_FOUND);

        Mockito.clearInvocations(mockResponse, mockMFM);
        mediaFileRequest.setExtraPathInfo("/");
        processor.getMediaFile(mockRequest, mockResponse);
        verify(mockMFM, never()).getMediaFileWithContent(anyString());
        verify(mockResponse).sendError(SC_NOT_FOUND);
        verify(mockCache, never()).incrementIncomingRequests();
    }

    @Test
    public void test404OnNoMediaFile() throws IOException {
        processor.getMediaFile(mockRequest, mockResponse);
        verify(mockMFM).getMediaFileWithContent("abc");
        verify(mockResponse).sendError(SC_NOT_FOUND);
        verify(mockCache, never()).incrementIncomingRequests();
    }

    @Test
    public void test304OnNotModified() throws IOException {
        MediaFile fourDayOldMF = new MediaFile();
        Instant now = Instant.now();
        fourDayOldMF.setLastUpdated(now.minus(4, ChronoUnit.DAYS));
        when(mockRequest.getDateHeader(any())).thenReturn(now.minus(2,
                ChronoUnit.DAYS).toEpochMilli());
        when(mockMFM.getMediaFileWithContent(anyString())).thenReturn(fourDayOldMF);
        processor.getMediaFile(mockRequest, mockResponse);
        verify(mockRequest).getDateHeader("If-Modified-Since");
        verify(mockResponse).setStatus(HttpServletResponse.SC_NOT_MODIFIED);
        verify(mockCache).incrementIncomingRequests();
        verify(mockCache).incrementRequestsHandledBy304();
    }

    @Test
    public void testReturn404onFileNotFound() throws IOException {
        MediaFile mediaFile = new MediaFile();
        when(mockMFM.getMediaFileWithContent(anyString())).thenReturn(mediaFile);
        processor.getMediaFile(mockRequest, mockResponse);
        verify(mockResponse).sendError(SC_NOT_FOUND);
        verify(mockCache).incrementIncomingRequests();
        verify(mockCache, never()).incrementRequestsHandledBy304();

        // not found also sent on missing thumbnail requests
        Mockito.clearInvocations(mockResponse);
        mediaFile.setContentType("image/jpeg");
        when(mockRequest.getParameter("tn")).thenReturn("true");
        processor.getMediaFile(mockRequest, mockResponse);
        verify(mockResponse).sendError(SC_NOT_FOUND);
    }

    @Test
    public void testReturnCorrectImage() throws IOException, URISyntaxException {
        Instant now = Instant.now();
        MediaFile mediaFile = new MediaFile();
        mediaFile.setLastUpdated(now);
        when(mockMFM.getMediaFileWithContent(anyString())).thenReturn(mediaFile);
        File regularFile = new File(getClass().getResource(TEST_IMAGE).toURI());
        mediaFile.setContent(regularFile);
        mediaFile.setThumbnail(regularFile);
        mediaFile.setContentType("image/jpeg");
        processor.getMediaFile(mockRequest, mockResponse);
        // image/jpeg: regular image sent
        verify(mockResponse).setContentType("image/jpeg");

        Mockito.clearInvocations(mockResponse);
        when(mockRequest.getParameter("tn")).thenReturn("true");
        processor.getMediaFile(mockRequest, mockResponse);
        // image/png: thumbnail image sent
        verify(mockResponse).setContentType("image/png");

        verify(mockResponse).setHeader("Cache-Control", "no-cache");
        verify(mockResponse).setDateHeader("Last-Modified", now.toEpochMilli());
        verify(mockCache, times(2)).incrementIncomingRequests();
        verify(mockCache, never()).incrementRequestsHandledBy304();
    }

    @Test
    public void testReturn404OnProcessingException() throws IOException, URISyntaxException {
        Instant now = Instant.now();
        MediaFile mediaFile = new MediaFile();
        mediaFile.setLastUpdated(now);
        when(mockMFM.getMediaFileWithContent(anyString())).thenReturn(mediaFile);
        File regularFile = new File(getClass().getResource(TEST_IMAGE).toURI());
        mediaFile.setContent(regularFile);
        WebloggerTest.logExpectedException(log, "IllegalArgumentException");
        when(mockResponse.getOutputStream()).thenThrow(new IllegalArgumentException());
        processor.getMediaFile(mockRequest, mockResponse);
        verify(mockResponse).sendError(SC_NOT_FOUND);
        verify(mockResponse).reset();
        verify(mockCache).incrementIncomingRequests();
        verify(mockCache, never()).incrementRequestsHandledBy304();

        // don't send error code if response committed
        Mockito.clearInvocations(mockResponse);
        when(mockResponse.isCommitted()).thenReturn(true);
        processor.getMediaFile(mockRequest, mockResponse);
        verify(mockResponse, never()).sendError(SC_NOT_FOUND);
        verify(mockResponse, never()).reset();
    }

}