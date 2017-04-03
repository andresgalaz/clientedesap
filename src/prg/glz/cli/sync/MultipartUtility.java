package prg.glz.cli.sync;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

import prg.glz.cli.ws.AbstractFrwk;

/**
 * This utility class provides an abstraction layer for sending multipart HTTP POST requests to a web server.
 * 
 * @author www.codejava.net
 * 
 */
public class MultipartUtility {
    private static String     EOL = "\r\n";
    private final String      boundary;
    private HttpURLConnection httpConn;
    private String            charset;
    private OutputStream      outputStream;
    private PrintWriter       writer;

    /**
     * This constructor initializes a new HTTP POST request with content type is set to multipart/form-data
     * 
     * @param requestURL
     * @param charset
     * @throws IOException
     */
    public MultipartUtility(String requestURL, String charset) throws IOException {
        this.charset = charset;

        // creates a unique boundary based on time stamp
        boundary = "===" + System.currentTimeMillis() + "===";

        URL url = new URL( requestURL );
        httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setUseCaches( false );
        httpConn.setDoOutput( true ); // indicates POST method
        httpConn.setDoInput( true );
        httpConn.setRequestProperty( "Content-Type", "multipart/form-data; boundary=" + boundary );
        httpConn.setRequestProperty( "User-Agent", "CodeJava Agent" );
        outputStream = httpConn.getOutputStream();
        writer = new PrintWriter( new OutputStreamWriter( outputStream, charset ), true );
    }

    /**
     * Adds a form field to the request
     * 
     * @param name
     *            field name
     * @param value
     *            field value
     */
    public void addFormField(String name, Integer value) {
        addFormField( name, value.toString() );
    }

    public void addFormField(String name, String value) {
        writer.append( "--" + boundary + EOL );
        writer.append( "Content-Disposition: form-data; name=\"" + name + '"' + EOL );
        writer.append( "Content-Type: text/plain; charset=" + charset + EOL + EOL );
        writer.append( value ).append( EOL );
        writer.flush();
    }

    /**
     * Adds a upload file section to the request
     * 
     * @param fieldName
     *            name attribute in <input type="file" name="..." />
     * @param uploadFile
     *            a File to be uploaded
     * @throws IOException
     */
    public void addFilePart(String fieldName, File uploadFile) throws IOException {
        String fileName = uploadFile.getName();
        writer.append( "--" + boundary + EOL );
        writer.append( "Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + fileName + '"' + EOL );
        writer.append( "Content-Type: " + URLConnection.guessContentTypeFromName( fileName ) + EOL );
        writer.append( "Content-Transfer-Encoding: binary" + EOL + EOL );
        writer.flush();

        FileInputStream inputStream = new FileInputStream( uploadFile );
        byte[] buffer = new byte[4096];
        int bytesRead = -1;
        boolean bEmpty = true;
        while ((bytesRead = inputStream.read( buffer )) > 0) {
            outputStream.write( buffer, 0, bytesRead );
            bEmpty = false;
        }
        outputStream.flush();
        inputStream.close();

        // writer.append( EOL );
        writer.flush();

        if (bEmpty)
            throw new IOException( "El archivo está vacío o no se puedo enviar" );
    }

    /**
     * Adds a header field to the request.
     * 
     * @param name
     *            - name of the header field
     * @param value
     *            - value of the header field
     */
    public void addHeaderField(String name, String value) {
        writer.append( name + ": " + value ).append( EOL );
        writer.flush();
    }

    /**
     * Completes the request and receives response from the server.
     * 
     * @return a list of Strings as response in case the server returned status OK, otherwise an exception is thrown.
     * @throws IOException
     */
    public Map<String, Object> finish() throws IOException {
        writer.append( EOL ).flush();
        writer.append( "--" + boundary + "--" + EOL );
        writer.close();

        // checks server's status code first
        int status = httpConn.getResponseCode();
        if (status == HttpURLConnection.HTTP_OK) {
            try {
                return AbstractFrwk.getJsonMap( httpConn.getInputStream() );
            } finally {
                httpConn.disconnect();
            }
        } else {
            throw new IOException( "Server returned non-OK status: " + status );
        }
    }
}
