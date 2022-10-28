package lunasql.cmd;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.*;
import java.util.regex.Pattern;

import lunasql.lib.Contexte;
import lunasql.lib.Security;
import lunasql.sql.SQLCnx;
import lunasql.val.Valeur;
import lunasql.val.ValeurDef;

/**
 * Commande INFO <br>
 * (Interne) Affiche des informations sur le système
 * @author M.P.
 */
public class CmdInfo extends Instruction {

   public CmdInfo(Contexte cont) {
      super(cont, TYPE_CMDINT, "INFO", "&");
   }

   @Override
   public int execute() {
      // Exécution avec autres options
      Valeur vr = new ValeurDef(cont);
      StringBuilder sb;
      switch (getLength()) {
         case 1 :
         sb = new StringBuilder();
         sb.append(SQLCnx.frm("Propriété", 35, ' ')).append(SQLCnx.frm("Valeur", 40, ' ')).append('\n');
         sb.append(SQLCnx.frm("", 75, '-')).append('\n');

         // Liste des infos de connexion
         sb.append(SQLCnx.frm("Connection path", 35, ' '))
            .append(SQLCnx.frm(cont.getConnex().getPath(), 35, ' ')).append('\n');
         sb.append(SQLCnx.frm("Connection database", 35, ' '))
            .append(SQLCnx.frm(cont.getConnex().getBase(), 35, ' ')).append('\n');
         sb.append(SQLCnx.frm("Connection driver", 35, ' '))
            .append(SQLCnx.frm(cont.getConnex().getDriver(), 35, ' ')).append('\n');
         sb.append(SQLCnx.frm("Connection login", 35, ' '))
            .append(SQLCnx.frm(cont.getConnex().getLogin(), 35, ' ')).append('\n');
         // Liste des paramètres système
         sb.append(SQLCnx.frm("Operating system name", 35, ' '))
            .append(SQLCnx.frm(System.getProperty("os.name"), 35, ' ')).append('\n');
         sb.append(SQLCnx.frm("Operating system version", 35, ' '))
            .append(SQLCnx.frm(System.getProperty("os.version"), 35, ' ')).append('\n');
         sb.append(SQLCnx.frm("Operating system arch.", 35, ' '))
            .append(SQLCnx.frm(System.getProperty("os.arch"), 35, ' ')).append('\n');
         sb.append(SQLCnx.frm("Java Version", 35, ' '))
            .append(SQLCnx.frm(System.getProperty("java.version"), 35, ' ')).append('\n');
         sb.append(SQLCnx.frm("Java VM", 35, ' '))
            .append(SQLCnx.frm(System.getProperty("java.vm.info"), 35, ' ')).append('\n');
         sb.append(SQLCnx.frm("Java Home", 35, ' '))
            .append(SQLCnx.frm(System.getProperty("java.home"), 35, ' ')).append('\n');
         sb.append(SQLCnx.frm("Java Vendor", 35, ' '))
            .append(SQLCnx.frm(System.getProperty("java.vendor"), 35, ' ')).append('\n');
         sb.append(SQLCnx.frm("Java Vendor URL", 35, ' '))
            .append(SQLCnx.frm(System.getProperty("java.vendor.url"), 35, ' ')).append('\n');
         sb.append(SQLCnx.frm("User name", 35, ' '))
            .append(SQLCnx.frm(System.getProperty("user.name"), 35, ' ')).append('\n');
         sb.append(SQLCnx.frm("User home", 35, ' '))
            .append(SQLCnx.frm(System.getProperty("user.home"), 35, ' ')).append('\n');
         sb.append(SQLCnx.frm("User directory", 35, ' '))
            .append(SQLCnx.frm(System.getProperty("user.dir"), 35, ' ')).append('\n');
         sb.append(SQLCnx.frm("Default file encoding", 35, ' '))
            .append(SQLCnx.frm(System.getProperty("file.encoding"), 35, ' ')).append('\n');

         // System memory
         System.gc();   //-- make sure we get almost reliable memory usage information.
         NumberFormat numbfrm = NumberFormat.getNumberInstance(Locale.FRANCE);
         numbfrm.setMinimumFractionDigits(2);
         numbfrm.setMaximumFractionDigits(2);
         Runtime rt = Runtime.getRuntime();
         double totmem = rt.totalMemory() / 1024;
         double fremem = rt.freeMemory() / 1024;
         double maxmem = rt.maxMemory() / 1024;
         double usemem = totmem - fremem;
         sb.append(SQLCnx.frm("Maximum memory [KB]", 35, ' '))
            .append(SQLCnx.frmI(numbfrm.format(maxmem), 15, ' ')).append('\n');
         sb.append(SQLCnx.frm("Allocated memory [KB]", 35, ' '))
            .append(SQLCnx.frmI(numbfrm.format(totmem), 15, ' ')).append('\n');
          sb.append(SQLCnx.frm("Free memory [KB]", 35, ' '))
            .append(SQLCnx.frmI(numbfrm.format(fremem), 15, ' ')).append('\n');
          sb.append(SQLCnx.frm("Used memory [KB]", 35, ' '))
            .append(SQLCnx.frmI(numbfrm.format(usemem), 15, ' '));

         vr.setDispValue(sb.toString(), Contexte.VERB_AFF);
         vr.setSubValue("20"); // c'est le nombre de propritétés affichées
         cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
         break;

         case 2:
         String[] tp = getArg(1).split("\\,");
         for (String p : tp) {
            String val, p2 = p.toUpperCase();
            if (p2.equals("COMMANDS")) {
               sb = new StringBuilder();
               TreeMap<String, Instruction> cmds = cont.getAllCommandsTree();
               for (Map.Entry<String, Instruction> me : cmds.entrySet()) {
                  Instruction ins = me.getValue();
                  int t = ins.getType();
                  if (t == Instruction.TYPE_CMDSQL || t == Instruction.TYPE_CMDINT ||
                      t == Instruction.TYPE_CMDPLG || t == Instruction.TYPE_MOTC_WH)
                     sb.append(ins.getName()).append(' ');
               }
               val = sb.toString();
            }
            else if (p2.equals("GLOBALS")) {
               sb = new StringBuilder();
               HashMap<String, String> vars = cont.getAllVars();
               for (String key : vars.keySet()) {
                  if (cont.isNonSys(key)) sb.append(key).append(' ');
               }
               val = sb.toString();
            }
            else if (p2.equals("LOCALS")) {
               sb = new StringBuilder();
               HashMap<String, String> vars = cont.getAllLecVars();
               if (vars != null) {
                  for (String key : vars.keySet()) sb.append(key).append(' ');
               }
               val = sb.toString();
            }
            else if (p2.equals("OPTIONS")) {
               sb = new StringBuilder();
               HashMap<String, String> vars = cont.getAllVars();
               for (String key : vars.keySet()) {
                  if (cont.isSys(key) || cont.isSysUser(key)) sb.append(key).append(' ');
               }
               val = sb.toString();
            }
            else if (p2.equals("DATABASE")) {
               try {
                  DatabaseMetaData dMeta = cont.getConnex().getMetaData();
                  Properties prop = new Properties();
                  prop.put("db-major", Integer.toString(dMeta.getDatabaseMajorVersion()));
                  prop.put("db-minor", Integer.toString(dMeta.getDatabaseMinorVersion()));
                  prop.put("product", dMeta.getDatabaseProductName());
                  prop.put("product-vers", dMeta.getDatabaseProductVersion());
                  prop.put("driver", dMeta.getDriverName());
                  prop.put("driver-major", Integer.toString(dMeta.getDriverMajorVersion()));
                  prop.put("driver-minor", Integer.toString(dMeta.getDriverMinorVersion()));
                  prop.put("driver-vers", dMeta.getDriverVersion());
                  prop.put("jdbc-major", Integer.toString(dMeta.getJDBCMajorVersion()));
                  prop.put("jdbc-minor", Integer.toString(dMeta.getJDBCMinorVersion()));

                  // Autres resultsets
                  StringBuilder s = new StringBuilder();
                  // Catalogs
                  ResultSet rscat = dMeta.getCatalogs();
                  while (rscat.next()) s.append(rscat.getString("TABLE_CAT")).append(' ');
                  rscat.close();
                  prop.put("db-catalogs", s.toString());
                  s.setLength(0);
                  // Schémas
                  rscat = dMeta.getSchemas();
                  while (rscat.next()) {
                     String cat = rscat.getString("TABLE_CATALOG");
                     if (cat != null) s.append(cat).append('.');
                     s.append(rscat.getString("TABLE_SCHEM")).append(' ');
                  }
                  rscat.close();
                  prop.put("db-schemas", s.toString());

                  ByteArrayOutputStream os = new ByteArrayOutputStream();
                  prop.store(os, "Propriétés du SGBD");
                  val = os.toString(cont.getVar(Contexte.ENV_FILE_ENC));
               }
               catch (UnsupportedOperationException ex) {
                  return cont.erreur("INFO", "ERREUR UnsupportedOperationException : " + ex.getMessage(), lng);
               }
               catch (SQLException ex) {
                  return cont.erreur("INFO", "ERREUR SQLException : " + ex.getMessage(), lng);
               }
               catch (IOException ex) {
                  return cont.erreur("INFO", "ERREUR IOException : " + ex.getMessage(), lng);
               }
            }
            else if (p2.equals("TABLES")) {
               try {
                  DatabaseMetaData dMeta = cont.getConnex().getMetaData();
                  ResultSet result = dMeta.getTables(null, null, null, new String[] {
                     "TABLE", "GLOBAL TEMPORARY", "LOCAL TEMPORARY", "ALIAS", "SYNONYM"});
                  sb = new StringBuilder();
                  while(result.next()) sb.append(result.getString(3)).append(' ');
                  val = sb.toString();
               } catch (SQLException ex) {
                  return cont.erreur("INFO", "ERREUR SQLException : " + ex.getMessage(), lng);
               }
            }
            else if (p2.equals("SYSTABLES")) {
               try {
                  DatabaseMetaData dMeta = cont.getConnex().getMetaData();
                  ResultSet result = dMeta.getTables(null, null, null, new String[] {"SYSTEM TABLE"});
                  sb = new StringBuilder();
                  while(result.next()) sb.append(result.getString(3)).append(' ');
                  val = sb.toString();
               } catch (SQLException ex) {
                  return cont.erreur("INFO", "ERREUR SQLException : " + ex.getMessage(), lng);
               }
            }
            else if (p2.equals("ENVIRON")) {
               Map<String, String> env = System.getenv();
               Properties prop = new Properties();
               prop.putAll(env);
               try {
                  ByteArrayOutputStream os = new ByteArrayOutputStream();
                  prop.store(os, "Variables d'environnement");
                  val = os.toString(cont.getVar(Contexte.ENV_FILE_ENC));
               } catch (IOException ex) {
                  return cont.erreur("INFO", "ERREUR IOException : " + ex.getMessage(), lng);
               }
            }
            else if (p2.equals("STACK")) {
               val = cont.getCallStack();
            }
            else if (p2.equals("REDEFINED")) {
               sb = new StringBuilder();
               HashMap<String, String> vars = cont.getAllVars();
               for (String key : vars.keySet()) {
                  if (cont.isNonSys(key) && cont.getCommand(key.toUpperCase()) != null)  sb.append(key).append(' ');
               }
               vars = cont.getAllLecVars();
               if (vars != null) {
                  for (String key : vars.keySet()) {
                     if (cont.getCommand(key.toUpperCase()) != null) sb.append(key).append(' ');
                  }
               }
               val = sb.toString();
            }
            else if (p2.equals("NOCIRCCTRL")) {
               sb = new StringBuilder();
               for (String s : cont.getAllCircVars()) sb.append(s).append(' ');
               val = sb.toString();
            }
            else if (p2.equals("NETWORK")) {
               try {
                  sb = new StringBuilder();
                  Enumeration<NetworkInterface> linf = NetworkInterface.getNetworkInterfaces();
                  while (linf.hasMoreElements()) {
                     NetworkInterface inf = linf.nextElement();
                     Enumeration<InetAddress> adrList = inf.getInetAddresses();
                     while (adrList.hasMoreElements()) {
                        InetAddress adr = adrList.nextElement();
                        sb.append('{').append(adr.getHostAddress());
                        NetworkInterface net = NetworkInterface.getByInetAddress(adr);
                        if (net != null) sb.append(' ').append(Security.hexencode(net.getHardwareAddress()));
                        sb.append("}\n");
                     }
                  }
                  val = sb.toString();
               }
               catch (SocketException ex) {
                  return cont.erreur("INFO", "ERREUR SocketException : " + ex.getMessage(), lng);
               }
            }
            else {
               val = System.getProperty(p);
               if (val == null) val = System.getenv(p);
               if (val == null) return cont.erreur("INFO",
                     "nom de propriété système / environnement inconnu : " + p, lng);
            }
            // Affichage ?
            if (tp.length > 1) vr.appendDispValue(SQLCnx.frm(p, 35, ' ') + SQLCnx.frm(val, 35, ' '),
                    Contexte.VERB_AFF, true);
            vr.setSubValue(val);
         }
         cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
         break;

         case 3:
         String cmd = getArg(1).toUpperCase(), arg = getArg(2), val;
         if (cmd.equals("WHATIS")) {
            if (cont.getLecVar(arg) != null) {
               char c0;
               if ((c0 = arg.charAt(0)) == ':' || c0 == '_') val = "syslocal";
               else val = "local";
            }
            else if (cont.getGlbVar(arg) != null) {
               char c0;
               if ((c0 = arg.charAt(0)) == ':' || c0 == '_') val = "sysglobal";
               else val = "global";
            }
            else if (cont.getPlugin(arg.toUpperCase()) != null) val = "plugin";
            else if (cont.getCommand(arg.toUpperCase()) != null) val = "intern";
            else val = "nodef";
         }
         else if (cmd.equals("FIND")) {
            val = "";
            sb = new StringBuilder();
            Pattern ptv = Pattern.compile(".*" + arg + ".*", Pattern.DOTALL);
            HashMap<String, String> vlec = cont.getAllLecVars();
            for (Map.Entry<String, String> e : vlec.entrySet()) {
               String k = e.getKey(), v = e.getValue();
               if (cont.isNonSys(k) && ptv.matcher(v).matches()) {
                  sb.append(SQLCnx.frm(k + " *")).append(' ').append(v).append("\n");
                  val = v;
               }
            }
            HashMap<String, String> vars = cont.getAllVars();
            for (Map.Entry<String, String> e : vars.entrySet()) {
               String k = e.getKey(), v = e.getValue();
               if (cont.isNonSys(k) && ptv.matcher(v).matches()) {
                  sb.append(SQLCnx.frm(k)).append(' ').append(v).append("\n");
                  val = v;
               }
            }
            if (sb.length() > 0) sb.deleteCharAt(sb.length() - 1);
            vr.setDispValue(sb.toString(), Contexte.VERB_AFF);
         }
         else return cont.erreur("INFO", "nom de commande inconnu : " + cmd, lng);

         // Affichage
         vr.setSubValue(val);
         cont.setVar(Contexte.ENV_CMD_STATE, Contexte.STATE_FALSE);
         break;

         default: return cont.erreur("INFO", "2 arguments maximum sont attendus", lng);
      } 

      vr.setRet();
      cont.setValeur(vr);
      return RET_CONTINUE;
   }

   /*
    * (non-Javadoc)
    * @see lunasql.cmd.Instruction#getHelp()
    */
   @Override
   public String getDesc(){
      return "  info, &     Affiche des informations sur le système\n";
   }
}// class
