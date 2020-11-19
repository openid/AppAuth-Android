package net.openid.appauth.connectivity;

import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;

public interface HttpConnection {

    /**
     * Set the method for the URL request, one of:
     *
     *    * GET
     *    * POST
     *    * HEAD
     *    * OPTIONS
     *    * PUT
     *    * DELETE
     *    * TRACE
     *
     * are legal, subject to protocol restrictions. The default method is GET.
     * @param method - the HTTP method
     */
    void setRequestMethod(String method) throws ProtocolException;

    /**
     * Sets the value of the doInput field for this URLConnection to the specified value.
     *
     * A URL connection can be used for input and/or output. Set the DoInput flag to true if you intend to use the URL connection for input, false if not. The default is true.
     * @param doInput - the new value.
     */
    void setDoInput(boolean doInput);

    /**
     * Sets the value of the doOutput field for this URLConnection to the specified value.
     *
     * A URL connection can be used for input and/or output. Set the DoOutput flag to true if you intend to use the URL connection for output, false if not. The default is false.
     * @param doOutput - the new value.
     */
    void setDoOutput(boolean doOutput);

    /**
     * Sets the general request property. If a property with the key already exists, overwrite its value with the new value.
     *
     * NOTE: HTTP requires all request properties which can legally have multiple instances with the same key to use a comma-separated list syntax which enables multiple properties to be appended into a single property.
     * @param key - the keyword by which the request is known (e.g., "Accept").
     * @param value - the value associated with it.
     */
    void setRequestProperty(String key, String value);

    /**
     * Sets a specified timeout value, in milliseconds, to be used when opening a communications link to the resource referenced by this URLConnection. If the timeout expires before the connection can be established, a java.net.SocketTimeoutException is raised. A timeout of zero is interpreted as an infinite timeout.
     *
     * Some non-standard implementation of this method may ignore the specified timeout. To see the connect timeout set, please call getConnectTimeout().
     * @param timeoutMilliSeconds - an int that specifies the connect timeout value in milliseconds
     */
    void setConnectTimeout(int timeoutMilliSeconds);

    /**
     * Sets the read timeout to a specified timeout, in milliseconds. A non-zero value specifies the timeout when reading from Input stream when a connection is established to a resource. If the timeout expires before there is data available for read, a java.net.SocketTimeoutException is raised. A timeout of zero is interpreted as an infinite timeout.
     *
     * Some non-standard implementation of this method ignores the specified timeout. To see the read timeout set, please call getReadTimeout().
     * @param readTimeoutMilliSeconds - if the timeout parameter is negative
     */
    void setReadTimeout(int readTimeoutMilliSeconds);

    /**
     * public void setInstanceFollowRedirects(boolean followRedirects)
     *
     * Sets whether HTTP redirects (requests with response code 3xx) should be automatically followed by this HttpURLConnection instance.
     *
     * The default value comes from followRedirects, which defaults to true.
     * @param followRedirects - a boolean indicating whether or not to follow HTTP redirects.
     */
    void setInstanceFollowRedirects(boolean followRedirects);

    /**
     * Returns an input stream that reads from this open connection. A SocketTimeoutException can be thrown when reading from the returned input stream if the read timeout expires before data is available for read.
     * @return an input stream that reads from this open connection.
     */
    InputStream getInputStream() throws IOException;

    /**
     * Returns an output stream that writes to this connection.
     * @return an output stream that writes to this connection.
     * @throws IOException - if an I/O error occurs while creating the output stream.
     */
    OutputStream getOutputStream() throws IOException;

    /**
     * Returns the value of the named general request property for this connection.
     * @param key - the keyword by which the request is known (e.g., "Accept").
     * @return the value of the named general request property for this connection. If key is null, then null is returned.
     * @throws IllegalStateException - if already connected
     */
    String getRequestProperty(String key) throws IllegalStateException;

    /**
     * Gets the status code from an HTTP response message. For example, in the case of the following status lines:
     *
     *  HTTP/1.0 200 OK
     *  HTTP/1.0 401 Unauthorized
     *
     *
     * It will return 200 and 401 respectively. Returns -1 if no code can be discerned from the response (i.e., the response is not valid HTTP).
     * @return the HTTP Status-Code, or -1
     * @throws IOException - if an error occurred connecting to the server.
     */
    int getResponseCode() throws IOException;

    /**
     * Returns the error stream if the connection failed but the server sent useful data nonetheless. The typical example is when an HTTP server responds with a 404, which will cause a FileNotFoundException to be thrown in connect, but the server sent an HTML help page with suggestions as to what to do.
     *
     * This method will not cause a connection to be initiated. If the connection was not connected, or if the server did not have an error while connecting or if the server had an error but no error data was sent, this method will return null. This is the default.
     * @return an error stream if any, null if there have been no errors, the connection is not connected or the server sent no useful data.
     */
    InputStream getErrorStream();

    /**
     * Opens a communications link to the resource referenced by this URL, if such a connection has not already been established.
     *
     * If the connect method is called when the connection has already been opened (indicated by the connected field having the value true), the call is ignored.
     *
     * URLConnection objects go through two phases: first they are created, then they are connected. After being created, and before being connected, various options can be specified (e.g., doInput and UseCaches). After connecting, it is an error to try to set them. Operations that depend on being connected, like getContentLength, will implicitly perform the connection, if necessary.
     * @throws SocketTimeoutException - if the timeout expires before the connection can be established
     * @throws IOError - if an I/O error occurs while opening the connection.
     */
    void connect() throws IOException, IOError;
}
