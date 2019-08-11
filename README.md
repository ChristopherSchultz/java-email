# java-email
A simple API for building MIME messages for delivery through a JavaMail provider.

Please see the API javadoc for details. It's easy to get started:

    MailSession session = ...;
    Transport transport = ...;

    MailMessage message = new MailMessage();
    message.setTo("Mom", "mom@example.com");
    message.setFrom("Me", "me@example.com");
    message.setPlainText("Happy Mother's Day!");

    message.send(session, transport);

The `MailMessage` class makes it easy to add other things, such as an HTML version of your message.

    message.setHTMLText("<p>Happy Mother's Day!</p>");

And add attachments:

    File selfie = new File("selfie.jpg");
    message.attach(selfie);

## Building

Use Maven

    mvn package

