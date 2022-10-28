package lunasql.misc;

import lunasql.cmd.Instruction;
import lunasql.lib.Contexte;
import lunasql.lib.Lecteur;

import java.io.*;

import lunasql.val.Valeur;
import lunasql.val.ValeurDef;

/**
 * Commande REPORT <br>
 * (Interne) Génére un rapport en HTML
 * @author M.P.
 */
public class CmdReport extends Instruction {

   public CmdReport(Contexte cont) {
      super(cont, TYPE_CMDPLG, "REPORT", null);
   }

   @Override
   public int execute() {
      if (getLength() < 3)
         return cont.erreur("REPORT", "deux noms de fichers HTML (entrée + sortie) sont attendus", lng);

      File fsrc = new File(getArg(1)), fout = new File(getArg(2));
      if (!fsrc.canRead())
         return cont.erreur("REPORT", "le fichier HTML '" + getArg(1) + "' est inaccessible", lng);

      try {
         if (!cont.askWriteFile(fout)) {
            cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
            return RET_CONTINUE;
         }

         Lecteur lec = new Lecteur(cont);
         StringBuilder code = new StringBuilder(), html = new StringBuilder();
         int c, c0 = 0, ret = RET_CONTINUE;
         boolean iscode = false;

         BufferedReader read = new BufferedReader(new FileReader(getArg(1)));
         while ((c = read.read()) > -1 && ret == RET_CONTINUE) {
            if (!iscode && c0 == '<' && c == '%') { // début de code
               iscode = true;
               html.deleteCharAt(html.length() - 1);
            }
            else if (iscode && c0 =='%' && c == '>') { // fin de code
               iscode = false;
               if (code.length() > 0) {
                  lec.add(code.deleteCharAt(code.length() - 1).toString());
                  code.setLength(0);
                  ret = lec.getCmdState();
                  Valeur v = cont.getValeur();
                  if (v != null) html.append(v.getSubValue());
               }
            }
            else if (iscode) code.append((char)c);
            else html.append((char)c);
            c0 = c;
         }
         lec.fin();
         read.close();
         if (iscode) cont.erreur("REPORT", "Balise fermante absente", lng);

         // Écriture en fichier de sortie
         PrintWriter writer = new PrintWriter(fout, cont.getVar(Contexte.ENV_FILE_ENC));
         writer.println(html.toString());
         writer.close();

         cont.setValeur(new ValeurDef(cont, null, Integer.toString(html.length())));
         cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_TRUE);
       }
       catch (IOException ex) {
          return cont.exception("REPORT", "ERREUR IOException : " + ex.getMessage(), lng, ex);
       }

      return RET_CONTINUE;
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  report      Génération de rapports HTML simples\n";
   }
}// class
