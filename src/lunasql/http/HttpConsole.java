package lunasql.http;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Properties;

import lunasql.Config;
import lunasql.lib.Contexte;
import lunasql.lib.Lecteur;
import lunasql.lib.Security;
import lunasql.lib.Tools;
import lunasql.sql.SQLCnx;
import lunasql.val.Valeur;

/**
 * Console en HTTP
 */
public class HttpConsole extends NanoHTTPD {

   private final Contexte cont;

   /**
    * Constructeur HttpConsole (Contexte, String, String, String, String)
    *
    * @param cont le contexte
    * @param path la chaine entière de connexion
    * @param driver le chemin de la classe du driver
    * @param login nom d'utilisateur
    * @param mdp mot de passe de connexion
    * @param port le port d'écoute du serveur
    * @throws java.io.IOException si erreur IO
    * @throws java.sql.SQLException si erreur SQL
    */
   public HttpConsole(Contexte cont, String path, String driver, String login, String mdp, int port)
           throws IOException, SQLException, IllegalArgumentException {
      super(port);
      // Application du contexte et connexion
      this.cont = cont;
      this.cont.setSQLCnx(new SQLCnx(path, driver, login, mdp));
      this.cont.loadInitFile(); // Si présence d'un fichier INIT, exécution du fichier
   }

   /**
    * Constructeur HttpConsole (Contexte, int, String, String, String, String, String, int)
    *
    * @param cont le contexte
    * @param type type de la base
    * @param base nom ou chemin de la base de données
    * @param login nom d'utilisateur
    * @param mdp mot de passe de connexion
    * @param opts les options des connexions (ex : crypt)
    * @param host l'hôte
    * @param dbport le port de la base de données
    * @param svport le port d'écoute du serveur
    * @throws java.io.IOException si erreur IO
    * @throws java.sql.SQLException si erreur SQL
    */
   public HttpConsole(Contexte cont, int type, String base, String login, String mdp, String opts, String host,
                      int dbport, int svport) throws IOException, SQLException {
      super(svport);
      // Application du contexte et connexion
      this.cont = cont;
      this.cont.setSQLCnx(new SQLCnx(type, base, login, mdp, opts, host, dbport));
      this.cont.loadInitFile(); // Si présence d'un fichier INIT, exécution du fichier
   }

   @Override
   public Response serve(String uri, String method, Properties header, Properties parms, Properties files) {
      String sql = parms.getProperty("sqlquery");
      System.out.println("request: " + method + " at " + uri + ":\n" + sql);
      //TODO: ne supporte pas les requêtes CORS

      // GET|POST '/'
      if (uri.equals("/")) {
         // TODO: ajouter attribut  onkeydown='history()' pour l'historique
         String frm = parms.getProperty("frmsql");
         HashMap<String, String> htmlvars = new HashMap<>();
         htmlvars.put("version", Config.APP_VERSION_NUM);
         htmlvars.put("lsqlcode", HTMLEntities(sql));
         htmlvars.put("frmchecked", frm == null ? "" : " checked");
         StringBuilder msg = new StringBuilder();
         if (sql != null && !sql.isEmpty()) {
            if (frm != null) sql = Tools.cleanSQLCode(cont, sql);
            // Zone de saisie
            msg.append("<p><b>Commande SQL</b> :</p>\n");
            msg.append("<div id='stdin'>\n<pre><code class='sql'>").append(HTMLEntities(sql));
            msg.append("</code></pre>\n</div>\n");
            ByteArrayOutputStream baos = new ByteArrayOutputStream(), errbaos = new ByteArrayOutputStream();
            cont.setWriter(new PrintStream(baos));
            cont.setErrWriter(new PrintStream(errbaos));
            new Lecteur(cont, sql, new HashMap<String, String>() {{ put(Contexte.LEC_THIS, "(http-console)"); }});

            // Valeur de retour
            Valeur vr = cont.getValeur();
            String sub = null;
            if (vr != null) sub = vr.getSubValue();
            if (sub != null)
               msg.append("<p><b>Retour</b> : <span id='cmdret'><tt>")
                     .append(HTMLEntities(sub)).append("</tt></span></p>\n");

            String r = baos.toString(), e = errbaos.toString();
            msg.append("<p><b>Affichage en console</b> :</p>\n");
            // Sortie erreurs
            if (!e.trim().isEmpty())
               msg.append("<div id='stderr'>\n<pre>").append(HTMLEntities(e)).append("</pre>\n</div>\n");
            // Sortie standard
            if (!r.trim().isEmpty())
               msg.append("<div id='stdout'>\n<pre>").append(HTMLEntities(r)).append("</pre>\n</div>\n");

            // Pied
            msg.append("<br><hr>\n<p>&nbsp;</p>\n<p><a href=''>Recharger</a></p>\n");
         }
         else {
            msg.append("<div id='stdout' style='display:none'></div>\n");
            msg.append("<div id='stderr' style='display:none'></div>\n");
         }
         htmlvars.put("returndata", msg.toString());
         return new NanoHTTPD.Response(HTTP_OK, MIME_HTML, getTxtFile("index.html", htmlvars));
      }

      // GET|POST '/api'
      if (uri.equals("/api")) {
         if (sql == null || sql.isEmpty())
            return new NanoHTTPD.Response(HTTP_BADREQUEST, MIME_PLAINTEXT,
                  "No SQL code provided. Please set the `sqlcode` property in the body content.");

         ByteArrayOutputStream baos = new ByteArrayOutputStream(), errbaos = new ByteArrayOutputStream();
         cont.setWriter(new PrintStream(baos));
         cont.setErrWriter(new PrintStream(errbaos));
         new Lecteur(cont, sql, new HashMap<String, String>() {{ put(Contexte.LEC_THIS, "(http-api)"); }});

         // TODO get return value and send a JSON/XML object
         String r = baos.toString(), e = errbaos.toString();
         if (!e.isEmpty()) r += '\n' + e;
         return new NanoHTTPD.Response(HTTP_OK, MIME_PLAINTEXT, r) {{
            header = new Properties() {{ put("Content-Type", "text/plain; charset=UTF-8"); }};
         }};
      }

      return new NanoHTTPD.Response(HTTP_NOTFOUND, MIME_PLAINTEXT, uri + " not found");
   }

   /**
    * Ouvre une ressource binaire (image) et en retourne l'URL base64
    * Merci à http://stackoverflow.com/questions/1264709/convert-inputstream-to-byte-array-in-java
    *
    * @param fich le nom du fichier
    * @return la chaîne Base64
    */
   private String getImgFile(String fich) {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      try {
         InputStream is = getClass().getResourceAsStream("/lunasql/http/res/" + fich);
         if (is == null) throw new IOException("ressource introuvable");
         byte[] buf = new byte[1024];
         int b;
         while ((b = is.read(buf, 0, buf.length)) != -1) out.write(buf, 0, b);
         out.flush();
      }
      catch (IOException ex) {
         cont.errprintln("Erreur HttpConsole : " + ex.getMessage());
      }
      return "data:image/gif;base64," + Security.b64encode(out.toByteArray());
   }

   /**
    * Ouvre une ressource texte et en retourne le contenu en remplaçant les variables
    * ${txtfile:xxx}, ${imgfile:xxx} et ${var:xxx} par la liste de varibles fournie
    *
    * @param fich le nom du fichier en fichier jar
    * @param vars la liste de variables à remplacer
    * @return le contenu
    */
   public String getTxtFile(String fich, HashMap<String, String> vars) {
      StringBuilder sb = new StringBuilder(), sbvar = new StringBuilder();
      try {
         InputStream is =  getClass().getResourceAsStream("/lunasql/http/res/" + fich);
         if (is == null) throw new IOException("ressource introuvable");
         BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
         int i;
         while ((i = br.read()) != -1) {
            if ((char) i == '$') {
               if ((char) (i = br.read()) == '{') {
                  while ((i = br.read()) != -1 && i != '}') sbvar.append((char) i);
                  String var = sbvar.toString(), key = var.substring(4), val;
                  if (var.startsWith("var=")) {
                     if (vars == null || (val = vars.get(key)) == null)
                        sb.append("[undefined: ").append(key).append("]");
                     else sb.append(val);
                  } else if (var.startsWith("txt=")) sb.append(getTxtFile(key, vars));
                  else if (var.startsWith("img=")) sb.append(getImgFile(key));
                  else sb.append("[invalid substitution: ").append(var).append("]");
                  sbvar.setLength(0);
               }
               else sb.append('$').append((char) i);
            }
            else sb.append((char) i);
         }
         br.close();
      }
      catch (IOException ex) {
         cont.errprintln("Erreur HttpConsole : " + ex.getMessage());
      }
      return sb.toString();
   }

   /**
    * Remplacement de tous les caractères HTML bizarres en leut équivalent sécurisés
    * @param s la chaîne à traiter
    * @return la chaîne sécurisée
    */
   public static String HTMLEntities(String s){
      if (s == null || s.isEmpty()) return "";
      return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\u001B[1;30m", "<span class='txtc0'>")
            .replace("\u001B[1;31m", "<span class='txtc1'>")
            .replace("\u001B[1;32m", "<span class='txtc2'>")
            .replace("\u001B[1;33m", "<span class='txtc3'>")
            .replace("\u001B[1;34m", "<span class='txtc4'>")
            .replace("\u001B[1;37m", "<span class='txtc5'>")
            .replace("\u001B[m", "</span>");
   }
}

