package net.christopherschultz.mail;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Properties;

import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;

/**
 * A command-line driver to send email messages using the MailMessage class.
 * Run with "--help" for usage information.
 *
 * @version 1.0a 2009-09-17
 * @author Chris Schultz
 */
public class Mailer
{
    private Mailer() { }

    public static void main(String[] args)
        throws Exception
    {
        Properties properties = null;

        MailMessage message = new MailMessage();

        int i;

        for(i=0; i<args.length; ++i)
        {
            String arg = args[i];

            if(!arg.startsWith("-") || "--".equals(arg))
                break;

            if("-help".equals(arg) || "--help".equals(arg))
            {
                printHelp(System.out);

                System.exit(0);
            }
            else if("-a".equals(arg))
            {
                message.attach(new File(args[++i]));
            }
            else if("-b".equals(arg))
            {
                message.addBcc(new InternetAddress(args[++i], true));
            }
            else if("-c".equals(arg))
            {
                message.addCc(new InternetAddress(args[++i], true));
            }
            else if("-e".equals(arg))
            {
                String filename = args[++i];
                String contentType = args[++i];
                String contentId = args[++i];

                File file = new File(filename);

                message.embedHTML(filename,
                                  null,
                                  contentType,
                                  contentId,
                                  file);
            }
            else if("-f".equals(arg))
            {
                message.setFrom(new InternetAddress(args[++i], true));
            }
            else if("-h".equals(arg))
            {
                String argstr = args[++i];

                int split = argstr.indexOf(':');

                if(0 < split)
                {
                    message.addHeader(argstr.substring(0, split),
                                      argstr.substring(split+1));
                }
                else
                {
                    System.err.println("Bad header: " + arg);
                    System.exit(1);
                }
            }
            else if("-p".equals(arg))
            {
                if(null == properties)
                    properties = new Properties();

                loadProperties(properties, new File(args[++i]), false);
            }
            else if("-s".equals(arg))
            {
                message.setSubject(args[++i]);
            }
            else if("-H".equals(arg))
            {
                message.setHTMLText(readContent(new InputStreamReader(new FileInputStream(args[++i]), Charset.defaultCharset())));
            }
            else
            {
                System.out.println("Unknown option: " + arg);
                System.out.println();

                printUsage(System.out);

                System.exit(1);
            }
        }

        for(; i<args.length; ++i)
            message.addTo(new InternetAddress(args[i], true));

        // Now, read the message from stdin
        message.setPlainText(readContent(new InputStreamReader(System.in, Charset.defaultCharset())));

        // Get default properties if none are specified
        if(null == properties)
        {
            properties = new Properties();

            // Some defaults. Note that the user's properties are loaded
            // after these defaults, and, of course, they may be overridden.
            properties.put("mail.user", System.getProperty("user.name"));
            properties.put("mail.smtp.port", "25");
            properties.put("mail.host", "localhost");
            properties.put("mail.transport.protocol", "smtp");

            loadProperties(properties);
            loadProperties(properties, new File("mail.properties"), true);
        }

        if(null != properties.getProperty("mail.X-Mailer"))
            message.addHeader("X-Mailer", properties.getProperty("mail.X-Mailer"));

        // Check for a reply-to header
        if(null != System.getProperty("mail.reply-to")) // Prefer system property
            message.setReplyTo(new InternetAddress(System.getProperty("mail.reply-to")));
        else if(null != System.getenv("REPLYTO")) // then prefer REPLYTO environment var
            message.setReplyTo(new InternetAddress(System.getenv("REPLYTO")));
        else if(null != properties.getProperty("mail.reply-to")) // fall-back to file-based property
            message.setReplyTo(new InternetAddress(properties.getProperty("mail.reply-to")));

        // Write the message to stdout (debug)
        message.buildMimeMessage(null).writeTo(System.out);

        // Send the message using JavaMail
        Session session = Session.getDefaultInstance(properties);

        String protocol = properties.getProperty("mail.transport.protocol");
        Transport transport = null;

        try
        {
            transport = session.getTransport(protocol);

            connect(transport, protocol, properties);

            message.send(session, transport);
        }
        finally
        {
            if(null != transport) try { transport.close(); }
            catch (MessagingException me) { me.printStackTrace(); }
        }
    }

    private static void connect(Transport transport,
                                String protocol,
                                Properties properties)
        throws MessagingException
    {
        String hostname = "localhost";
        int port = -1;
        String username;
        String password;

        if("true".equalsIgnoreCase(properties.getProperty("mail." + protocol + ".auth")))
        {
            username = properties.getProperty("mail." + protocol + ".user");

            if(null == username)
                username = properties.getProperty("mail.user");

            password = properties.getProperty("mail." + protocol + ".password");

            if(null == password)
                password = properties.getProperty("mail.password");
        }
        else
        {
            // No authentication necessary
            username = null;
            password = null;
        }

        hostname = properties.getProperty("mail." + protocol + ".host");

        if(null == hostname)
            hostname = properties.getProperty("mail.host");

        String portStr = properties.getProperty("mail." + protocol + ".port");

        if(null == portStr)
            port = -1;
        else
            port = Integer.parseInt(portStr);

        transport.connect(hostname, port, username, password);
    }
    /*
    private static int getIntProperty(Properties properties,
                                      String key,
                                      int defaultValue)
    {
        String value = properties.getProperty(key);

        if(null == value)
            return defaultValue;
        else
            return Integer.parseInt(value);
    }

    private static String getProperty(Properties properties,
                                      String key,
                                      String defaultValue)
    {
        String value = properties.getProperty(key);

        if(null == value)
            value = defaultValue;

        return value;
    }
    */

    private static void loadProperties(Properties properties)
    {
        InputStream in = null;

        try
        {
            in = Mailer.class.getClassLoader()
                .getResourceAsStream("mail.properties");

            if(null != in)
                properties.load(in);
        }
        catch (IOException ioe)
        {
            // Ignore
        }
        finally
        {
            if(null != in) try { in.close(); }
            catch (IOException ioe) { ioe.printStackTrace(); }
        }
    }

    private static void loadProperties(Properties properties,
                                       File file,
                                       boolean ignoreErrors)
        throws IOException
    {
        FileInputStream in = null;

        try
        {
            in = new FileInputStream(file);

            properties.load(in);
        }
        catch (IOException ioe)
        {
            if(!ignoreErrors)
                throw ioe;
        }
        finally
        {
            if(null != in) try { in.close(); }
            catch (IOException ioe) { ioe.printStackTrace(); }
        }
    }

    private static String readContent(Reader r)
        throws IOException
    {
        StringBuilder sb = new StringBuilder();

        char[] buffer = new char[1024];

        int read;

        while(-1 != (read = r.read(buffer)))
            sb.append(buffer, 0, read);

        return sb.toString();
    }
    
    private static void printUsage(PrintStream out)
        throws IOException
    {
        out.print("Usage: java ");
        out.print(Mailer.class.getName());
        out.println(" [-a file] [-b addr] [-c addr] [-f addr] [-h header] [-H HTML message file] [-i file] [-p properties] [-s subject] addr [...]");
    }

    private static void printHelp(PrintStream out)
        throws IOException
    {
        printUsage(out);

        out.println();

        out.println("options:");
        out.println("  -a <file>      attach a file to the message using MIME");
        out.println("  -b <address>   specify a blond carbon-copy (BCC) address");
        out.println("  -c <address>   specify a carbon-copy (CC) address");
        //        out.println("  -i <file>      ");
        out.println("  -f <address>   specify the sender of the message");
        out.println("  -h <header>    specify an SMTP header in the form name:value");
        out.println("  -H             specify a file to read the HTML portion of the message from");
        out.println("  -p <file>      specify a properties file to read (default:mail.properties)");
        out.println("  -s <subj>      the subject of the message");
    }
}
