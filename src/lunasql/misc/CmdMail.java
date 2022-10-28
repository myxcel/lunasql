package lunasql.misc;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import lunasql.cmd.Instruction;
import lunasql.lib.Contexte;
import lunasql.val.ValeurDef;

/**
 * Commande MAIL <br>
 * (Interne) Envoi d'un mail au destinataire avec la bibliothèque JavaMail si présente
 * @author M.P.
 */
public class CmdMail extends Instruction {

   public static final String FROM_ADDR = "mail-cmd@lunasql.net",
                              SMTP_HOST  = "smtp.lunasql.net";
   public static final int SMTP_TLS_PORT = 587,
                           SMTP_DEF_PORT = 25;

   private final OptionParser parser;
   private String host, user, pswd, from;
   private int port;

   public CmdMail(Contexte cont){
      super(cont, TYPE_CMDPLG, "MAIL", null);
      // Option parser
      parser = new OptionParser();
      parser.allowsUnrecognizedOptions();
      parser.accepts("?", "aide sur la commande");
      parser.accepts("h", "serveur hôte SMTP").withRequiredArg().ofType(String.class)
         .describedAs("host");
      parser.accepts("o", "port SMTP").withRequiredArg().ofType(Integer.class)
         .describedAs("port");
      parser.accepts("a", "nécessite authentification");
      parser.accepts("t", "nécessite chiffrement TLS");
      parser.accepts("u", "nom d'utilisateur SMTP").withRequiredArg().ofType(String.class)
         .describedAs("user");
      parser.accepts("p", "mot de passe SMTP").withRequiredArg().ofType(String.class)
         .describedAs("pswd");
      parser.accepts("e", "adresse expéditeur").withRequiredArg().ofType(String.class)
         .describedAs("adresse");
      parser.accepts("d", "adresses destinataires").withRequiredArg().ofType(String.class)
         .describedAs("adresses").withValuesSeparatedBy(',');
      parser.accepts("c", "adresses destinataires CC").withRequiredArg().ofType(String.class)
         .describedAs("adresses").withValuesSeparatedBy(',');
      parser.accepts("i", "adresses destinataires CCI").withRequiredArg().ofType(String.class)
         .describedAs("adresses").withValuesSeparatedBy(',');
      parser.accepts("f", "fichiers attachés").withRequiredArg().ofType(File.class)
         .describedAs("fichiers").withValuesSeparatedBy(',');
      parser.accepts("b", "sujet du courrier").withRequiredArg().ofType(String.class)
         .describedAs("subject");
      parser.accepts("y", "message en mode hypertexte (HTML)");
      parser.accepts("g", "mode DEBUG JavaMail");
      parser.nonOptions("corps_message").ofType(String.class);

      // Paramètres du mail
      host = SMTP_HOST;
      port = 0;
      user = "";
      pswd = "";
      from = FROM_ADDR;
   }

   @Override
   public int execute() {
      OptionSet options;
      try {
         options = parser.parse(getCommandA1());
         // Aide sur les options
         if (options.has("?")) {
            parser.printHelpOn(cont.getWriterOrOut());
            cont.setValeur(null);
            return RET_CONTINUE;
         }
      }
      catch (OptionException|IOException ex) {
         return cont.exception("MAIL", "erreur d'option : " + ex.getMessage(), lng, ex);
      }

      // Exécution avec autres options. Merci à :
      // http://algorithmes.cofares.net/reseau/javamail
      // http://www.tutorialspoint.com/java/java_sending_email.htm
      // http://www.codejava.net/java-ee/javamail/send-e-mail-with-attachment-in-java
      try {
         Properties props = System.getProperties();
         if (options.has("h")) host = (String) options.valueOf("h");
         if (options.has("o")) port = ((Integer) options.valueOf("o"));
         else if (port == 0) port = options.has("t") ? SMTP_TLS_PORT : SMTP_DEF_PORT;
         if (options.has("e")) from = (String) options.valueOf("e");
         if (options.has("a")) {
            if (options.has("u")) user = (String) options.valueOf("u");
            if (options.has("p")) pswd = (String) options.valueOf("p");
            props.setProperty("mail.user", user);
            props.setProperty("mail.password", pswd);
         }
         props.setProperty("mail.debug", options.has("g") ? "true" : "false");
         props.setProperty("mail.smtp.auth", options.has("a") ? "true" : "false");
         props.setProperty("mail.smtp.starttls.enable", options.has("t") ? "true" : "false");
         props.setProperty("mail.smtp.host", host);
         props.setProperty("mail.smtp.port", Integer.toString(port));
         
         // Avec autentifification
         javax.mail.Session session = javax.mail.Session.getDefaultInstance(props);
         if (!user.isEmpty()) session.getTransport("smtp").connect(host, port, user, pswd);

         // Préparation des entêtes
         javax.mail.internet.MimeMessage message = new javax.mail.internet.MimeMessage(session);
         message.setSentDate(new Date());
         message.setSubject((String) options.valueOf("b"));
         message.setFrom(new javax.mail.internet.InternetAddress(from));
         for (Object o : options.valuesOf("d"))
            message.addRecipients(javax.mail.Message.RecipientType.TO,
               javax.mail.internet.InternetAddress.parse((String) o));
         for (Object o : options.valuesOf("c"))
             message.addRecipients(javax.mail.Message.RecipientType.CC,
                javax.mail.internet.InternetAddress.parse((String) o));
         for (Object o : options.valuesOf("i"))
             message.addRecipients(javax.mail.Message.RecipientType.BCC,
                javax.mail.internet.InternetAddress.parse((String) o));
         if (message.getAllRecipients() == null)
            return cont.erreur("MAIL", "au moins un destinataire est attendu", lng);

         // Préparation du contenu (texte ou HTLM)
         javax.mail.Multipart multi = new javax.mail.internet.MimeMultipart();
         javax.mail.internet.MimeBodyPart mbody = new javax.mail.internet.MimeBodyPart();
         String body = listToString(options.nonOptionArguments());
         if (options.has("y")) mbody.setContent(body, "text/html");
         else mbody.setText(body);
         multi.addBodyPart(mbody);
         // Pièces jointes
         for (Object o : options.valuesOf("f")) {
            javax.mail.internet.MimeBodyPart mattach = new javax.mail.internet.MimeBodyPart();
            mattach.attachFile((File) o);
            multi.addBodyPart(mattach);
         }

         // Préparation et envoi
         message.setContent(multi);
         javax.mail.Transport.send(message);

         cont.setValeur(new ValeurDef(cont, "Courrier envoyé", Contexte.VERB_MSG, "1"));
         cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_TRUE);
         return RET_CONTINUE;
      }
      catch (IOException ex) {
         return cont.exception("MAIL", "ERREUR IOException : " + ex.getMessage(), lng, ex);
      }
      catch (javax.mail.MessagingException ex) {
         return cont.exception("MAIL", "ERREUR MessagingException : " + ex.getMessage(), lng, ex);
      }
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  mail        Envoi d'un courrier électronique par javamail\n";
   }
}// class
