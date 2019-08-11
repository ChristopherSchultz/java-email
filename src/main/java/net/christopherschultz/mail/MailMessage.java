package net.christopherschultz.mail;

import java.io.IOException;
import java.io.File;
import java.io.UnsupportedEncodingException;

import java.util.ArrayList;
import java.util.List;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.activation.DataHandler;

/**
 * Provides a nice wrapper around the JavaMail API for building
 * an email message to send.
 *
 * Supports plain text {@link #setPlainText} and HTML bodies
 * {@link #setHTMLText}, attachments {@link #attach}, and embedded files
 * for use within the HTML body {@link #embedHTML}.
 *
 * Uses UTF-8 as the character set to encode addresses.
 * Indirectly uses (javamail does this) the content-type specified
 * by " ;charset=" in calls to {@link #setPlainText(String,String)}.
 *
 * @author Chris Schultz
 * @version 1.0
 */
public class MailMessage
{
    /**
     * A content disposition indicating that an attachment is to be attached
     * as a separate file.
     */
    public static final String DISPOSITION_ATTACHMENT = Part.ATTACHMENT;

    /**
     * A content disposition indicating that an attachment is to be
     * placed inline.
     */
    public static final String DISPOSITION_INLINE = Part.INLINE;

    private String _multipartPreamble = "This is a multi-part message in MIME format.";
    private Address _sender;
    private List<Address> _from;
    private List<Address> _replyTo; 
    private List<Address> _to;
    private List<Address> _cc;
    private List<Address> _bcc;
    private List<NameValuePair> _headers;
    private String _subject;
    private String _plainText;
    private String _plainContentType;
    private String _plainCharset; // TODO: Just use content-type?
    private String _htmlText;
    private String _htmlContentType;
    private List<Attachment> _attachments;
    private List<Attachment> _embeddedFiles;

    private String _addressCharset = "UTF-8";
    private int _contentIdCounter = 0;

    /**
     * Creates a new MailMessage.
     */
    public MailMessage()
    {
    }

    /**
     * Sets the multipart preamble for this message. The multipart
     * preamble is the text included as the body for the top-level
     * multipart portion of the message.
     *
     * If set, and a multipart message will be generated, then this text
     * will be used as the multipart body. The default preamble is
     * "This is a multi-part message in MIME format.".
     *
     * @param multipartPreamble The preamble to use for multipart
     * messages.
     *
     * @return This MailMessage
     */
    public MailMessage setMultipartPreamble(String multipartPreamble)
    {
        _multipartPreamble = multipartPreamble;

        return this;
    }

    /**
     * Sets the sender of this message.
     *
     * <i>Sender</i> is distinct from <i>from</i> in that the sender is
     * typically the actual sender acting on behalf of the <i>from</i>
     * address.
     *
     * @param email The email address of the sender.
     *
     * @return This MailMessage
     *
     * @throws AddressException If there is a problem with the email address.
     *
     * @see #setFrom(String)
     */
    public MailMessage setSender(String email)
        throws UnsupportedEncodingException, AddressException
    {
        return setSender(email, null);
    }

    /**
     * Sets the sender of this message.
     *
     * <i>Sender</i> is distinct from <i>from</i> in that the sender is
     * typically the actual sender acting on behalf of the <i>from</i>
     * address.
     *
     * @param email The email address of the sender.
     * @param name The name of the sender.
     *
     * @return This MailMessage
     *
     * @throws AddressException If there is a problem with the email address.
     *
     * @see #setFrom(String,String)
     */
    public MailMessage setSender(String email, String name)
        throws UnsupportedEncodingException, AddressException
    {
        return setSender(makeInternetAddress(email, name, _addressCharset));
    }

    /**
     * Sets the sender of this message.
     *
     * <i>Sender</i> is distinct from <i>from</i> in that the sender is
     * typically the actual sender acting on behalf of the <i>from</i>
     * address.
     *
     * @param address The address of the sender.
     *
     * @return This MailMessage
     *
     * @see #setFrom(Address)
     */
    public MailMessage setSender(Address address)
    {
        _sender = address;

        return this;
    }

    /**
     * Sets the sender of this message.
     *
     * <i>Sender</i> is distinct from <i>from</i> in that the sender is
     * typically the actual sender acting on behalf of the <i>from</i>
     * address.
     *
     * @param email The email address of the sender.
     *
     * @return This MailMessage
     *
     * @throws AddressException If there is a problem with the email address.
     *
     * @see #setSender(String)
     */
    public MailMessage setFrom(String email)
        throws UnsupportedEncodingException, AddressException
    {
        return setFrom(email, null);
    }

    /**
     * Sets the sender of this message.
     *
     * <i>Sender</i> is distinct from <i>from</i> in that the sender is
     * typically the actual sender acting on behalf of the <i>from</i>
     * address.
     *
     * @param email The email address of the sender.
     * @param name The name of the sender.
     *
     * @return This MailMessage
     *
     * @throws AddressException If there is a problem with the email address.
     *
     * @see #setSender(String,String)
     */
    public MailMessage setFrom(String email, String name)
        throws UnsupportedEncodingException, AddressException
    {
        return setFrom(makeInternetAddress(email, name, _addressCharset));
    }

    /**
     * Sets the sender of this message.
     *
     * <i>Sender</i> is distinct from <i>from</i> in that the sender is
     * typically the actual sender acting on behalf of the <i>from</i>
     * address.
     *
     * @param address The address of the sender.
     *
     * @return This MailMessage
     *
     * @see #setSender(Address)
     */
    public MailMessage setFrom(Address address)
    {
        _from = new ArrayList<Address>();

        _from.add(address);

        return this;
    }

    /**
     * Sets the reply-to address for this message.
     *
     * @param email The reply-to email address for this message.
     *
     * @return This MailMessage
     *
     * @throws AddressException If there is a problem with the email address.
     *
     * @see #addReplyTo(String)
     */
    public MailMessage setReplyTo(String email)
        throws UnsupportedEncodingException, AddressException
    {
        return setReplyTo(email, null);
    }

    /**
     * Sets the reply-to address for this message.
     *
     * @param email The reply-to email address for this message.
     * @param name The name of the reply-to recipient.
     *
     * @return This MailMessage
     *
     * @throws AddressException If there is a problem with the email address.
     *
     * @see #addReplyTo(String,String)
     */
    public MailMessage setReplyTo(String email, String name)
        throws UnsupportedEncodingException, AddressException
    {
        return setReplyTo(makeInternetAddress(email, name, _addressCharset));
    }

    /**
     * Sets the reply-to address for this message.
     *
     * @param address The reply-to address for this message.
     *
     * @return This MailMessage
     *
     * @see #addReplyTo(Address)
     */
    public MailMessage setReplyTo(Address address)
    {
        _replyTo = null;

        return addReplyTo(address);
    }

    /**
     * Adds a reply-to recipient to this message.
     *
     * @param email The email address of the reply-to recipient.
     *
     * @return This MailMessage
     *
     * @throws AddressException If there is a problem with the email address.
     *
     * @see #setReplyTo(String)
     */
    public MailMessage addReplyTo(String email)
        throws UnsupportedEncodingException, AddressException
    {
        return addReplyTo(email, null);
    }

    /**
     * Adds a reply-to recipient to this message.
     *
     * @param email The email address of the reply-to recipient.
     * @param name The name of the reply-to recipient.
     *
     * @return This MailMessage
     *
     * @throws AddressException If there is a problem with the email address.
     *
     * @see #setReplyTo(String,String)
     */
    public MailMessage addReplyTo(String email, String name)
        throws UnsupportedEncodingException, AddressException
    {
        return addReplyTo(makeInternetAddress(email, name, _addressCharset));
    }

    /**
     * Adds a reply-to recipient to this message.
     *
     * @param address The address of the reply-to recipient.
     *
     * @return This MailMessage
     *
     * @see #setReplyTo(Address)
     */
    public MailMessage addReplyTo(Address address)
    {
        if(null == _replyTo)
            _replyTo = new ArrayList<Address>();

        _replyTo.add(address);

        return this;
    }

    /**
     * Sets the recipient of this message.
     *
     * @param email The email address of the recipient.
     *
     * @return This MailMessage
     *
     * @throws AddressException If there is a problem with the email address.
     *
     * @see #addTo(String)
     */
    public MailMessage setTo(String email)
        throws UnsupportedEncodingException, AddressException
    {
        return setTo(email, null);
    }

    /**
     * Sets the recipient of this message.
     *
     * @param email The email address of the recipient.
     * @param name The name of the recipient.
     *
     * @return This MailMessage
     *
     * @throws AddressException If there is a problem with the email address.
     *
     * @see #addTo(String,String)
     */
    public MailMessage setTo(String email, String name)
        throws UnsupportedEncodingException, AddressException
    {
        return addTo(makeInternetAddress(email, name, _addressCharset));
    }

    /**
     * Sets the recipient of this message.
     *
     * @param address The address of the recipient.
     *
     * @return This MailMessage
     *
     * @see #addTo(Address)
     */
    public MailMessage setTo(Address address)
    {
        _to = null;

        return addTo(address);
    }

    /**
     * Adds a recipient to this message.
     *
     * @param email The email address of the recipient.
     *
     * @return This MailMessage
     *
     * @throws AddressException If there is a problem with the email address.
     *
     * @see #setTo(String)
     */
    public MailMessage addTo(String email)
        throws UnsupportedEncodingException, AddressException
    {
        return addTo(email, null);
    }

    /**
     * Adds a recipient to this message.
     *
     * @param email The email address of the recipient.
     * @param name The name of the recipient.
     *
     * @return This MailMessage
     *
     * @throws AddressException If there is a problem with the email address.
     *
     * @see #setTo(String,String)
     */
    public MailMessage addTo(String email, String name)
        throws UnsupportedEncodingException, AddressException
    {
        return addTo(makeInternetAddress(email, name, _addressCharset));
    }

    /**
     * Adds a recipient to this message.
     *
     * @param address The address of the recipient.
     *
     * @return This MailMessage
     *
     * @see #setTo(Address)
     */
    public MailMessage addTo(Address address)
    {
        if(null == _to)
            _to = new ArrayList<Address>();

        _to.add(address);

        return this;
    }

    /**
     * Sets the carbon-copy (CC) recipient of this message.
     *
     * @param email The email address of the recipient.
     *
     * @return This MailMessage
     *
     * @throws AddressException If there is a problem with the email address.
     *
     * @see #addCc(String,String)
     */
    public MailMessage setCc(String email)
        throws UnsupportedEncodingException, AddressException
    {
        return setCc(email, null);
    }

    /**
     * Sets the carbon-copy (CC) recipient of this message.
     *
     * @param email The email address of the recipient.
     * @param name The name of the recipient.
     *
     * @return This MailMessage
     *
     * @throws AddressException If there is a problem with the email address.
     *
     * @see #addCc(String,String)
     */
    public MailMessage setCc(String email, String name)
        throws UnsupportedEncodingException, AddressException
    {
        _cc = null;

        return addCc(email, name);
    }

    /**
     * Sets the carbon-copy (CC) recipient of this message.
     *
     * @param address The address of the recipient.
     *
     * @return This MailMessage
     *
     * @throws AddressException If there is a problem with the email address.
     *
     * @see #addCc(Address)
     */
    public MailMessage setCc(Address address)
    {
        _cc = null;

        return addCc(address);
    }

    /**
     * Adds a carbon-copy (CC) recipient to this message.
     *
     * @param email The email address of the recipient.
     *
     * @return This MailMessage
     *
     * @throws AddressException If there is a problem with the email address.
     *
     * @see #setCc(String)
     */
    public MailMessage addCc(String email)
        throws UnsupportedEncodingException, AddressException
    {
        return addCc(email, null);
    }

    /**
     * Adds a carbon-copy (CC) recipient to this message.
     *
     * @param email The email address of the recipient.
     * @param name The name of the recipient.
     *
     * @return This MailMessage
     *
     * @throws AddressException If there is a problem with the email address.
     *
     * @see #setCc(String,String)
     */
    public MailMessage addCc(String email, String name)
        throws UnsupportedEncodingException, AddressException
    {
        return addCc(makeInternetAddress(email, name, _addressCharset));
    }

    /**
     * Adds a carbon-copy (CC) recipient to this message.
     *
     * @param address The address of the recipient.
     *
     * @return This MailMessage
     *
     * @see #setCc(Address)
     */
    public MailMessage addCc(Address address)
    {
        if(null == _cc)
            _cc = new ArrayList<Address>();

        _cc.add(address);

        return this;
    }

    /**
     * Sets the blind carbon-copy (BCC) recipient for this message.
     *
     * @param email The email address of the recipient.
     *
     * @return This MailMessage
     *
     * @throws AddressException If there is a problem with the email address.
     *
     * @see #addBcc(String,String)
     */
    public MailMessage setBcc(String email)
        throws UnsupportedEncodingException, AddressException
    {
        _bcc = null;

        return addBcc(email);
    }

    /**
     * Sets the blind carbon-copy (BCC) recipient for this message.
     *
     * @param email The email address of the recipient.
     * @param name The name of the recipient.
     *
     * @return This MailMessage
     *
     * @throws AddressException If there is a problem with the email address.
     *
     * @see #addBcc(String,String)
     */
    public MailMessage setBcc(String email, String name)
        throws UnsupportedEncodingException, AddressException
    {
        _bcc = null;

        return addBcc(email, name);
    }

    /**
     * Sets the blind carbon-copy (BCC) recipient for this message.
     *
     * @param address The address of the recipient.
     *
     * @return This MailMessage
     *
     * @see #addBcc(Address)
     */
    public MailMessage setBcc(Address address)
    {
        _bcc = null;

        return addBcc(address);
    }

    /**
     * Adds a blind carbon-copy (BCC) recipient to this message.
     *
     * @param email The email address of the recipient.
     *
     * @return This MailMessage
     *
     * @throws AddressException If there is a problem with the email address.
     *
     * @see #setBcc(String)
     */
    public MailMessage addBcc(String email)
        throws UnsupportedEncodingException, AddressException
    {
        return addBcc(email, null);
    }

    /**
     * Adds a blind carbon-copy (BCC) recipient to this message.
     *
     * @param email The email address of the recipient.
     * @param name The name of the recipient.
     *
     * @return This MailMessage
     *
     * @throws AddressException If there is a problem with the email address.
     *
     * @see #setBcc(String,String)
     */
    public MailMessage addBcc(String email, String name)
        throws UnsupportedEncodingException, AddressException
    {
        return addBcc(makeInternetAddress(email, name, _addressCharset));
    }

    /**
     * Adds a blind carbon-copy (BCC) recipient to this message.
     *
     * @param address The address of the recipient.
     *
     * @return This MailMessage
     *
     * @see #setBcc(Address)
     */
    public MailMessage addBcc(Address address)
    {
        if(null == _bcc)
            _bcc = new ArrayList<Address>();

        _bcc.add(address);

        return this;
    }

    /**
     * Sets the subject of this message.
     *
     * @param subject The subject of this message.
     *
     * @return This MailMessage
     */
    public MailMessage setSubject(String subject)
    {
        _subject = subject;

        return this;
    }

    /**
     * Sets the plain-text body of this message.
     *
     * @param plainText The text to use as the body of this message.
     *
     * @return This MailMessage
     */
    public MailMessage setPlainText(String plainText)
    {
        return setPlainText(plainText, "text/plain; charset=UTF-8");
    }

    /**
     * Sets the plain-text body of this message.
     *
     * @param plainText The text to use as the body of this message.
     * @param contentType The MIME type to use for the message body.
     *
     * @return This MailMessage
     */
    public MailMessage setPlainText(String plainText, String contentType)
    {
        _plainText = plainText;
        _plainContentType = contentType;

        return this;
    }

    /**
     * Sets the HTML body of this message.
     *
     * @param htmlText The HTML text to use as the body of this message.
     *
     * @return This MailMessage
     */
    public MailMessage setHTMLText(String htmlText)
    {
        return setHTMLText(htmlText, "text/html; charset=UTF-8");
    }

    /**
     * Sets the HTML body of this message.
     *
     * @param htmlText The HTML text to use as the body of this message.
     * @param contentType The MIME type to use for the message body.
     *
     * @return This MailMessage
     */
    public MailMessage setHTMLText(String htmlText, String contentType)
    {
        _htmlText = htmlText;
        _htmlContentType = contentType;

        return this;
    }

    /**
     * Attaches the specified file to this message.
     *
     * @param file The file to attach.
     *
     * @return This MailMessage
     */
    public MailMessage attach(File file)
    {
        return attach(file.getName(), null, null, DISPOSITION_ATTACHMENT, file);
    }

    /**
     * Attaches the specified file to this message.
     *
     * @param name The name of the attachment.
     * @param file The file to attach.
     *
     * @return This MailMessage
     */
    public MailMessage attach(String name, File file)
    {
        return attach(name, null, null, DISPOSITION_ATTACHMENT, file);
    }

    /**
     * Attaches the specified file to this message.
     *
     * @param name The name of the attachment.
     * @param description The description of the attachment.
     * @param file The file to attach.
     *
     * @return This MailMessage
     */
    public MailMessage attach(String name,
                              String description,
                              File file)
    {
        return attach(name, description, null, DISPOSITION_ATTACHMENT, file);
    }

    /**
     * Attaches the specified file to this message.
     *
     * @param name The name of the attachment.
     * @param description The description of the attachment.
     * @param contentType The MIME type of the attachment.
     * @param disposition Either {@link #DISPOSITION_INLINE} or
     *        {@link #DISPOSITION_ATTACHMENT}
     * @param file The file to attach.
     *
     * @return This MailMessage
     */
    public MailMessage attach(String name,
                              String description,
                              String contentType,
                              String disposition,
                              File file)
    {
        if(null == _attachments)
            _attachments = new ArrayList<Attachment>();

        _attachments.add(new Attachment(name,
                                        description,
                                        contentType,
                                        disposition,
                                        null,
                                        file));

        return this;
    }

    /**
     * Attaches the specified DataSource to this message.
     *
     * @param dataSource The dataSource representing the data to attach.
     * @param name The name of the attachment.
     *
     * @return This MailMessage
     */
    public MailMessage attach(DataSource dataSource,
                              String name)
    {
        return attach(dataSource, name, null, DISPOSITION_ATTACHMENT);
    }

    /**
     * Attaches the specified DataSource to this message.
     *
     * @param dataSource The dataSource representing the data to attach.
     * @param name The name of the attachment.
     * @param description The description of the attachment.
     *
     * @return This MailMessage
     */
    public MailMessage attach(DataSource dataSource,
                              String name,
                              String description)
    {
        return attach(dataSource, name, description, DISPOSITION_ATTACHMENT);
    }

    /**
     * Attaches the specified DataSource to this message.
     *
     * @param dataSource The dataSource representing the data to attach.
     * @param name The name of the attachment.
     * @param description The description of the attachment.
     * @param disposition Either {@link #DISPOSITION_INLINE} or
     *        {@link #DISPOSITION_ATTACHMENT}
     *
     * @return This MailMessage
     */
    public MailMessage attach(DataSource dataSource,
                              String name,
                              String description,
                              String disposition)
    {
        if(null == _attachments)
            _attachments = new ArrayList<Attachment>();

        _attachments.add(new Attachment(dataSource,
                                        name,
                                        description,
                                        disposition,
                                        null));

        return this;
    }

    /**
     * Embeds the specified DataSource in this message's HTML part.
     *
     * @param dataSource The dataSource representing the data to embed.
     *
     * @return The content id, suitable for use within the HTML body for
     *         reference.
     */
    public String embedHTML(DataSource dataSource)
    {
        return embedHTML(dataSource, dataSource.getName());
    }

    /**
     * Embeds the specified DataSource in this message's HTML part.
     *
     * @param dataSource The dataSource representing the data to embed. 
     * @param name The name of the embedded data.
     *
     * @return The content id, suitable for use within the HTML body for
     *         reference.
     */
    public String embedHTML(DataSource dataSource, String name)
    {
        return embedHTML(dataSource, name, null);
    }

    /**
     * Embeds the specified DataSource in this message's HTML part.
     *
     * @param dataSource The dataSource representing the data to embed.
     * @param name The name of the embedded data.
     * @param description The description of the data.
     *
     * @return The content id, suitable for use within the HTML body for
     *         reference.
     */
    public String embedHTML(DataSource dataSource,
                            String name,
                            String description)
    {
        return embedHTML(dataSource, name, description, generateContentId());
    }

    /**
     * Embeds the specified DataSource in this message's HTML part.
     *
     * @param dataSource The dataSource representing the data to embed.
     * @param name The name of the embedded data.
     * @param contentId The content id to use for the embedded data.
     *
     * @return The content id, suitable for use within the HTML body for
     *         reference.
     */
    public String embedHTML(DataSource dataSource,
                            String name,
                            String description,
                            String contentId)
    {
        if(null == _embeddedFiles)
            _embeddedFiles = new ArrayList<Attachment>();

        Attachment embedded = new Attachment(dataSource,
                                             name,
                                             description,
                                             DISPOSITION_ATTACHMENT,
                                             contentId);

        _embeddedFiles.add(embedded);

        return contentId;
    }

    /**
     * Embeds the specified File in this message's HTML part.
     *
     * @param file The file to embed.
     *
     * @return The content id, suitable for use within the HTML body for
     *         reference.
     */
    public String embedHTML(File file)
    {
        return embedHTML(null, null, null, file);
    }

    /**
     * Embeds the specified File in this message's HTML part.
     *
     * @param name The name of the embedded data.
     * @param file The file to embed.
     *
     * @return The content id, suitable for use within the HTML body for
     *         reference.
     */
    public String embedHTML(String name,
                            File file)
    {
        return embedHTML(name, null, null, file);
    }

    /**
     * Embeds the specified File in this message's HTML part.
     *
     * @param name The name of the embedded data.
     * @param description The description of the embedded file.
     * @param file The file to embed.
     *
     * @return The content id, suitable for use within the HTML body for
     *         reference.
     */
    public String embedHTML(String name,
                            String description,
                            File file)
    {
        return embedHTML(name, description, null, file);
    }

    /**
     * Embeds the specified File in this message's HTML part.
     *
     * @param name The name of the embedded data.
     * @param description The description of the embedded file.
     * @param contentType The MIME type of the embedded file.
     * @param file The file to embed.
     *
     * @return The content id, suitable for use within the HTML body for
     *         reference.
     */
    public String embedHTML(String name,
                            String description,
                            String contentType,
                            File file)
    {
        return embedHTML(name, description, contentType, generateContentId(), file);
    }

    /**
     * Embeds the specified File in this message's HTML part.
     *
     * @param name The name of the embedded data.
     * @param description The description of the embedded file.
     * @param contentType The MIME type of the embedded file.
     * @param contentId The content id to use for the embedded data.
     * @param file The file to embed.
     *
     * @return The content id, suitable for use within the HTML body for
     *         reference.
     */
    public String embedHTML(String name,
                            String description,
                            String contentType,
                            String contentId,
                            File file)
    {
        if(null == _embeddedFiles)
            _embeddedFiles = new ArrayList<Attachment>();

        Attachment embedded = new Attachment(name,
                                             description,
                                             contentType,
                                             DISPOSITION_ATTACHMENT,
                                             contentId,
                                             file);

        _embeddedFiles.add(embedded);

        return contentId;
    }

    /**
     * Adds an SMTP header to this MailMessage.
     *
     * @param name The name of the header.
     * @param value The value of the header.
     *
     * @return This MailMessage
     */
    public MailMessage addHeader(String name,
                                 String value)
    {
        if(null == _headers)
            _headers = new ArrayList<NameValuePair>();

        _headers.add(new NameValuePair(name, value));

        return this;
    }

    /**
     * Sends this message using the specified Session and Transport.
     *
     * @param session The JavaMail session to use.
     * @param transport The JavaMail transport to use.
     *
     * @throws MessagingException If there is a problem building or sending
     * this message.
     *
     * @return The MimeMessage that was sent.
     */
    public MimeMessage send(Session session, Transport transport)
        throws MessagingException
    {
        MimeMessage message = buildMimeMessage(session);

        // TODO: Here, or in buildMimeMessage?
        message.saveChanges();

        transport.sendMessage(message, message.getAllRecipients());

        return message;
    }

    /**
     * Builds a MimeMessage object from this MailMessage.
     *
     * @param session The JavaMail session in use. <code>null</code> seems to
     *                work okay, too.
     *
     * @return A MimeMessage including everything from this MailMessage.
     *
     * @throws MessagingException If there is a problem converting this
     *                            MailMessage into a MimeMessage.
     */
    public MimeMessage buildMimeMessage(Session session)
        throws MessagingException
    {
        MimeMessage message = new MimeMessage(session);

        if(null != _sender)
            message.setSender(_sender);

        if(null != _from)
            message.addFrom(_from.toArray(new Address[_from.size()]));

        if(null != _replyTo)
            message.setReplyTo(_replyTo.toArray(new Address[_replyTo.size()]));

        if(null != _to)
            message.setRecipients(Message.RecipientType.TO,
                                  _to.toArray(new Address[_to.size()]));

        if(null != _cc)
            message.setRecipients(Message.RecipientType.TO,
                                  _cc.toArray(new Address[_cc.size()]));

        if(null != _bcc)
            message.setRecipients(Message.RecipientType.TO,
                                  _bcc.toArray(new Address[_bcc.size()]));

        if(null != _subject)
            message.setSubject(_subject);

        if(null != _headers)
            for(NameValuePair header:_headers)
                message.addHeader(header.getName(), header.getValue());

        //
        // Now, get down to the real work: the content.
        //
        // Messages with both text/plain and text/html
        // need to have a structure like this:
        //
        // multipart/alternate { text/plain, text/html }
        // - text/plain
        // - text/html
        //
        // Messages with attachments need to look like this:
        //
        // multipart/mixed { (actual message), att1, att2, ... }
        // - text/plain
        // - other/mimetype
        // ...
        //
        // Messages with both plain+HTML and attachments look like this:
        //
        // multipart/mixed
        // - multipart/alternative
        //   - text/plain
        //   - text/html
        // - other/mimetype
        // ...
        //
        // Messages with embedded files for use within the HTML body look like
        // this:
        //
        // - multipart/alternative
        //   - text/plain
        //   - multipart/related
        //     - text/html
        //     - text/css
        //     - image/png
        //     ...
        //
        // Messages with embedded files for use within the HTML body along with
        // attachments look like this:
        //
        // multipart/mixed
        // - multipart/alternative
        //   - text/plain
        //   - multipart/related
        //     - text/html
        //     - text/css
        //     - image/png
        //     ...
        // - other/mimetype
        // ...
        //
        // First, figure out what the top-level looks like.
        // If we have attachments, it's multipart/mixed.
        //
        if(null != _attachments && 0 < _attachments.size())
        {
            MimeMultipart topLevelMultipart = new MimeMultipart("mixed");

            if(null != _multipartPreamble)
                topLevelMultipart.setPreamble(_multipartPreamble);

            setBody(topLevelMultipart, false);

            for(Attachment a : _attachments)
                a.attach(topLevelMultipart);

            message.setContent(topLevelMultipart);
        }
        else
        {
            setBody(message, true);
        }

        return message;
    }

    protected InternetAddress makeInternetAddress(String email,
                                                  String name,
                                                  String charset)
        throws UnsupportedEncodingException, AddressException
    {
        if(null == charset)
        {
            if(null == name)
                return new InternetAddress(email);
            else
                return new InternetAddress(email, name);
        }
        else
        {
        if(null == name)
            return new InternetAddress(email, null, charset);
        else
            return new InternetAddress(email, name, charset);
        }
    }

    protected String generateContentId()
    {
        return new StringBuffer("attachment.")
            .append(_contentIdCounter++)
            .toString();
    }

    protected void setBody(Object messageContainer,
                           boolean includeMultipartPreamble)
        throws MessagingException
    {
        if(null != _plainText && null != _htmlText)
        {
            //
            // Body is complex
            //
            MimeMultipart bodyMultipart = new MimeMultipart("alternative");

            if(includeMultipartPreamble && null != _multipartPreamble)
                bodyMultipart.setPreamble(_multipartPreamble);

            // Create the PLAIN TEXT portion of the body
            MimeBodyPart bodyPart = new MimeBodyPart();

            // TODO: Decide what the best way is to set the content
            // TODO: type /and/ encoding
            if(null != _plainCharset)
            {
                if(_plainContentType.startsWith("text/"))
                {
                    // Use setText and indicate charset
                    bodyPart.setText(_plainText, _plainCharset,
                                     _plainContentType.substring(6));
                }
                else
                {
                    String contentType = _plainContentType;

                    // TODO: Is this legal?
                    contentType = contentType
                            + "; charset=" + _plainCharset;

                    bodyPart.setContent(_plainText, contentType);
                }
            }
            else
            {
                bodyPart.setContent(_plainText, _plainContentType);
            }

            bodyMultipart.addBodyPart(bodyPart);

            // Create the HTML portion of the body
            bodyPart = createHTMLBodyPart(false);

            bodyMultipart.addBodyPart(bodyPart);

            Part enclosingPart;

            if(messageContainer instanceof Part)
            {
                enclosingPart = (Part)messageContainer;
            }
            else if(messageContainer instanceof Multipart)
            {
                // Wrap the body multipary in a BodyPart
                enclosingPart = new MimeBodyPart();

                ((Multipart)messageContainer)
                    .addBodyPart((BodyPart)enclosingPart);
            }
            else
            {
                throw new MessagingException("Unknown message container: " + messageContainer.getClass().getName());
            }

            enclosingPart.setContent(bodyMultipart);
        }
        else if(null != _plainText)
        {
            setSimpleBody(messageContainer, _plainText, _plainContentType);
        }
        else if(null != _htmlText)
        {
            setSimpleBody(messageContainer, _htmlText, _htmlContentType);
        }
        else
        {
            throw new MessagingException("Message has no body.");
        }
    }

    protected MimeBodyPart createHTMLBodyPart(boolean includeMultipartPreamble)
        throws MessagingException
    {
        MimeBodyPart htmlBodyPart = new MimeBodyPart();

        htmlBodyPart.setContent(_htmlText, _htmlContentType);

        if(null != _embeddedFiles && 0 < _embeddedFiles.size())
        {
            // Wrap the HTML body along with the embedded files together
            MimeMultipart htmlMultipart = new MimeMultipart("related");

            if(includeMultipartPreamble && null != _multipartPreamble)
                htmlMultipart.setPreamble(_multipartPreamble);

            htmlMultipart.addBodyPart(htmlBodyPart);

            for(Attachment e:_embeddedFiles)
            {
                MimeBodyPart embedded;

                e.attach(htmlMultipart);
            }

            MimeBodyPart container = new MimeBodyPart();
            container.setContent(htmlMultipart);

            return container;
        }
        else
        {
            return htmlBodyPart;
        }
    }

    protected void setSimpleBody(Object messageContainer,
                                 String text,
                                 String contentType)
        throws MessagingException
    {
        Part enclosingPart;

        if(messageContainer instanceof Part)
        {
            enclosingPart = (Part)messageContainer;
        }
        else if (messageContainer instanceof Multipart)
        {
            // Wrap the body multipary in a BodyPart
            enclosingPart = new MimeBodyPart();

            ((Multipart)messageContainer)
                .addBodyPart((BodyPart)enclosingPart);
        }
        else
        {
            throw new MessagingException("Unknown message container: " + messageContainer.getClass().getName());
        }

        enclosingPart.setContent(text, contentType);
    }

    private static class NameValuePair
    {
        private String _name;
        private String _value;

        public NameValuePair(String name, String value)
        {
            _name = name;
            _value = value;
        }

        public String getName() { return _name; }
        public String getValue() { return _value; }
    }

    private static class Attachment
    {
        private String _name;
        private String _description;
        private String _contentType;
        private String _contentId;
        private String _filename;
        private String _disposition; // Inline or Attachment

        // One of the following will be non-null
        // TODO: Always use DataSource for everything?
        private File _file;
        private DataSource _dataSource;

        public Attachment(DataSource dataSource,
                          String name,
                          String description,
                          String disposition,
                          String contentId)
        {
            if(!(DISPOSITION_INLINE.equals(disposition)
                 || DISPOSITION_ATTACHMENT.equals(disposition)))
                throw new IllegalArgumentException("Unknown disposition: "
                                                   + disposition);

            _dataSource = dataSource;
            _name = name;
            _description = description;
            _disposition = disposition;
            _contentId = contentId;
            _contentType = dataSource.getContentType();
        }

        public Attachment(String name,
                          String description,
                          String contentType,
                          String disposition,
                          String contentId,
                          File file)
        {
            if(null == file)
                throw new IllegalArgumentException("File must not be null");

            if(!(DISPOSITION_INLINE.equals(disposition)
                 || DISPOSITION_ATTACHMENT.equals(disposition)))
                throw new IllegalArgumentException("Unknown disposition: "
                                                   + disposition);

            _name = name;
            _description = description;
            _contentType = contentType;
            _disposition = disposition;
            _contentId = contentId;
            _file = file;
            _filename = _file.getName();
        }

        public String getName()
        {
            return _name;
        }

        public String getDescription()
        {
            return _description;
        }

        public String getContentId()
        {
            return _contentId;
        }

        public String getContentType()
        {
            return _contentType;
        }

        protected void attach(Multipart multipart)
            throws MessagingException
        {
            MimeBodyPart mbp = new MimeBodyPart();
 
            if(null != _dataSource)
            {
                // NOTE: DataSource contains its own content type
                //       and should not have a different one set, here
                mbp.setDataHandler(new DataHandler(_dataSource));
            }
            else if(null != _contentType)
            {
                DataSource ds = null;

                // Overriding the content-type requires some heroics
                if(null != _file)
                {
                    ds = new FileDataSource(_file)
                        {
                            public String getContentType()
                            {
                                return _contentType;
                            }
                        };
                }
                else if(null != _filename)
                {
                    ds = new FileDataSource(_filename)
                        {
                            public String getContentType()
                            {
                                return _contentType;
                            }
                        };
                }

                mbp.setDataHandler(new DataHandler(ds));
            }
            else
            {
                try
                {
                    if(null != _file)
                        mbp.attachFile(_file);
                    else if (null != _filename)
                        mbp.attachFile(_filename);
                }
                catch (IOException ioe)
                {
                    throw new MessagingException("Cannot attach file", ioe);
                }
            }

            if(null != _description)
                mbp.setDescription(_description);

            if(null != _contentId)
                mbp.setContentID("<" + _contentId + ">"); // commons-email explicitly includes the '<' and '>', too

            if(null != _name)
                mbp.setFileName(_name);

            mbp.setDisposition(_disposition);

            multipart.addBodyPart(mbp);
        }
    }
}
