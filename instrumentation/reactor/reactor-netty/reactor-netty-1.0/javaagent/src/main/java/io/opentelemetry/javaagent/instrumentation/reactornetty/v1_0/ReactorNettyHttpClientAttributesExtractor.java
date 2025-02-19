/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.reactornetty.v1_0;

import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientAttributesExtractor;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import javax.annotation.Nullable;
import reactor.netty.http.client.HttpClientConfig;
import reactor.netty.http.client.HttpClientResponse;

final class ReactorNettyHttpClientAttributesExtractor
    extends HttpClientAttributesExtractor<HttpClientConfig, HttpClientResponse> {

  @Override
  protected String url(HttpClientConfig request) {
    String uri = request.uri();
    if (isAbsolute(uri)) {
      return uri;
    }

    // use the baseUrl if it was configured
    String baseUrl = request.baseUrl();
    if (baseUrl != null) {
      if (baseUrl.endsWith("/") && uri.startsWith("/")) {
        baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
      }
      return baseUrl + uri;
    }

    // otherwise, use the host+port config to construct the full url
    SocketAddress hostAddress = request.remoteAddress().get();
    if (hostAddress instanceof InetSocketAddress) {
      InetSocketAddress inetHostAddress = (InetSocketAddress) hostAddress;
      return (request.isSecure() ? "https://" : "http://")
          + inetHostAddress.getHostName()
          + ":"
          + inetHostAddress.getPort()
          + (uri.startsWith("/") ? "" : "/")
          + uri;
    }

    return uri;
  }

  private static boolean isAbsolute(String uri) {
    return uri.startsWith("http://") || uri.startsWith("https://");
  }

  @Nullable
  @Override
  protected String flavor(HttpClientConfig request, @Nullable HttpClientResponse response) {
    if (response != null) {
      String flavor = response.version().text();
      if (flavor.startsWith("HTTP/")) {
        flavor = flavor.substring("HTTP/".length());
      }
      return flavor;
    }
    return null;
  }

  @Override
  protected String method(HttpClientConfig request) {
    return request.method().name();
  }

  @Override
  protected List<String> requestHeader(HttpClientConfig request, String name) {
    return request.headers().getAll(name);
  }

  @Nullable
  @Override
  protected Long requestContentLength(
      HttpClientConfig request, @Nullable HttpClientResponse response) {
    return null;
  }

  @Nullable
  @Override
  protected Long requestContentLengthUncompressed(
      HttpClientConfig request, @Nullable HttpClientResponse response) {
    return null;
  }

  @Override
  protected Integer statusCode(HttpClientConfig request, HttpClientResponse response) {
    return response.status().code();
  }

  @Nullable
  @Override
  protected Long responseContentLength(HttpClientConfig request, HttpClientResponse response) {
    return null;
  }

  @Nullable
  @Override
  protected Long responseContentLengthUncompressed(
      HttpClientConfig request, HttpClientResponse response) {
    return null;
  }

  @Override
  protected List<String> responseHeader(
      HttpClientConfig request, HttpClientResponse response, String name) {
    return response.responseHeaders().getAll(name);
  }
}
