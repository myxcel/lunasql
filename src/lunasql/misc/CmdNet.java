package lunasql.misc;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import lunasql.cmd.Instruction;
import lunasql.lib.Contexte;
import lunasql.lib.Tools;
import lunasql.val.Valeur;
import lunasql.val.ValeurDef;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Commande NET <br>
 * (Interne) Ouvre une connexion HTTP GET, POST, PUT, DELETE
 * @author M.P.
 */
public class CmdNet extends Instruction {

   private final OptionParser parser;

   public CmdNet(Contexte cont) {
      super(cont, TYPE_CMDPLG, "NET", null);
      // Option parser
      parser = new OptionParser();
      parser.allowsUnrecognizedOptions();
      parser.accepts("?", "aide sur la commande");
      parser.accepts("h", "dictionnaire des entêtes").withRequiredArg().ofType(String.class)
            .describedAs("headers");
      parser.accepts("o", "nom du fichier de sortie").withRequiredArg().ofType(File.class)
            .describedAs("fichier");
      parser.accepts("i", "nom du fichier d'entrée").withRequiredArg().ofType(File.class)
            .describedAs("fichier");
      parser.nonOptions("nom_cmd url").ofType(String.class);
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
         return cont.exception("NET", "erreur d'option : " + ex.getMessage(), lng, ex);
      }

      // Exécution avec autres options
      try {
         List<?> lf = options.nonOptionArguments();
         if (lf.size() < 2) return cont.erreur("NET", "au moins une cmd et une URL sont attendues", lng);

         String cmd = ((String) lf.get(0)).toUpperCase();
         Valeur vr = new ValeurDef(cont);
         URL url = new URL((String) lf.get(1));
         HttpURLConnection conn = (HttpURLConnection) url.openConnection();
         conn.setConnectTimeout(15000); // 15s

         if (options.has("h")) {
            Properties prop = Tools.getProp((String) options.valueOf("h"));
            if (prop == null) return cont.erreur("NET", "dictionnaire invalide" , lng);
            for (Map.Entry<Object, Object> me : prop.entrySet())
               conn.setRequestProperty((String) me.getKey(), (String) me.getValue());
         }

         // thanks to https://techndeck.com/how-to-send-http-get-post-request-in-java/
         if (cmd.equals("GET")) { // HTTP GET
            conn.setRequestMethod("GET");
         }
         else if (cmd.equals("POST")) { // HTTP POST
            boolean optf = options.has("o");
            if (lf.size() < 3 && !optf)
               return cont.erreur("NET", "POST: une cmd, une URL et des données sont attendues", lng);

            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            DataOutputStream bodystr = new DataOutputStream(conn.getOutputStream());
            if (optf) {
               BufferedInputStream stream =
                     new BufferedInputStream(new FileInputStream((File) options.valueOf("i")));
               int i;
               while ((i = stream.read()) != -1) bodystr.write(i);
               stream.close();
            }
            else bodystr.writeBytes((String) lf.get(2));
            bodystr.flush();
            bodystr.close();
         }
         //else if (cmd.equals("PUT")) { // HTTP PUT
         //}
         //else if (cmd.equals("DELETE")) { // HTTP DELETE
         //}
         else return cont.erreur("NET", "commande invalide : " + cmd, lng);

         // Lecture de la réponse dans tous les cas
         int res = conn.getResponseCode();
         vr.appendDispValue("Code de réponse : " + res, Contexte.VERB_BVR, true);

         BufferedInputStream stream = new BufferedInputStream(conn.getInputStream());
         int i;
         if (options.has("o")) {
            File f = (File) options.valueOf("o");
            FileOutputStream output = new FileOutputStream(f);
            while ((i = stream.read()) != -1) output.write(i);
            output.flush();
            output.close();

            if (res == HttpURLConnection.HTTP_OK) vr.setSubValue(Long.toString(f.length()));
            else return cont.erreur("NET", "code erreur renvoyé : " + res + " " +
                     conn.getResponseMessage(), lng);
         } else {
            StringBuilder sb = new StringBuilder();
            while ((i = stream.read()) != -1) sb.append((char) i);

            if (res == HttpURLConnection.HTTP_OK) vr.setSubValue(sb.toString());
            else return cont.erreur("NET", "code erreur renvoyé : " + res + " " +
                     conn.getResponseMessage() + "\n" + sb.toString(), lng);
         }
         stream.close();

         vr.setRet();
         cont.setValeur(vr);
         cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
         return RET_CONTINUE;
      }
      catch (IOException ex) {
         return cont.erreur("NET", "GET : ERREUR " + ex.getClass().getSimpleName() + " : " +
               ex.getMessage(), lng);
      }
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  net,        Ouvre et envoie une requête HTTP\n";
   }
}// class
