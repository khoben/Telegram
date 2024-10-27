package org.telegram.messenger.cast;

import android.net.Uri;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.github.dkbai.tinyhttpd.nanohttpd.core.protocols.http.IHTTPSession;
import io.github.dkbai.tinyhttpd.nanohttpd.core.protocols.http.NanoHTTPD;
import io.github.dkbai.tinyhttpd.nanohttpd.core.protocols.http.response.Response;
import io.github.dkbai.tinyhttpd.nanohttpd.core.protocols.http.response.Status;
import io.github.dkbai.tinyhttpd.nanohttpd.core.util.ServerRunner;


public class CastShareServer extends NanoHTTPD {

    private static String HOST = "127.0.0.1:4747";
    private static final String PREFIX = "tgcast";
    private static final ConcurrentHashMap<String, CastItem> castItems = new ConcurrentHashMap<>();

    public CastShareServer(String host, int port) {
        super(host, port);
        HOST = "http://" + host + ":" + port;
    }

    public void runServer() {
        ServerRunner.executeInstance(this);
    }

    public static void stopServer() {
        ServerRunner.stopServer();
    }


    @Override
    public void start(int timeout, boolean daemon) throws IOException {
        super.start(timeout, daemon);
    }

    public static String register(String castId, CastItem castItem) {
        castItems.put(castId, castItem);
        return HOST + "/" + PREFIX  + "/" + castId;
    }

    public static void unregisterAll() {
        castItems.clear();
    }

    @Override
    public Response serve(IHTTPSession session) {
        Uri uri = Uri.parse(session.getUri());
        List<String> segments = uri.getPathSegments();
        if (!segments.contains(PREFIX)) {
            return Response.newFixedLengthResponse(Status.NOT_FOUND, "text/plain", "Not found");
        }

        String castId = null;
        try {
            castId = segments.get(segments.indexOf(PREFIX) + 1);
        } catch (IndexOutOfBoundsException ignored) {
        }

        if (castId == null) {
            return Response.newFixedLengthResponse(Status.NOT_FOUND, "text/plain", "Cast ID Not found");
        }

        CastItem castItem = castItems.get(castId);

        if (castItem == null) {
            return Response.newFixedLengthResponse(Status.NOT_FOUND, "text/plain", "Cast Item Not found");
        }

        String range = null;
        Map<String, String> headers = session.getHeaders();
        for (String key : headers.keySet()) {
            if ("range".equals(key)) {
                range = headers.get(key);
                break;
            }
        }
        if (range != null) {
            try {
                return partialResponse(castItem, range);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            return fullResponse(castItem);
        }
    }

    public Response fullResponse(CastItem castItem) {
        try {
            InputStream fis = castItem.getInputStream();
            return Response.newChunkedResponse(Status.OK, castItem.getMime(), fis);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private Response partialResponse(CastItem castItem, String rangeHeader) throws IOException {
        String rangeValue = rangeHeader.trim().substring("bytes=".length());
        long fileLength = castItem.getSize();
        long start, end;
        if (rangeValue.startsWith("-")) {
            end = fileLength - 1;
            start = fileLength - 1
                    - Long.parseLong(rangeValue.substring("-".length()));
        } else {
            String[] range = rangeValue.split("-");
            start = Long.parseLong(range[0]);
            end = range.length > 1 ? Long.parseLong(range[1])
                    : fileLength - 1;
        }
        if (end > fileLength - 1) {
            end = fileLength - 1;
        }
        if (start <= end) {
            long contentLength = end - start + 1;
            InputStream fileInputStream = castItem.getInputStream();
            //noinspection ResultOfMethodCallIgnored
            fileInputStream.skip(start);
            Response response = Response.newFixedLengthResponse(Status.PARTIAL_CONTENT, castItem.getMime(), fileInputStream, contentLength);
            response.addHeader("Content-Length", contentLength + "");
            response.addHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);
            response.addHeader("Content-Type", castItem.getMime());
            return response;
        } else {
            return Response.newChunkedResponse(Status.INTERNAL_ERROR, "text/html", null);
        }
    }
}