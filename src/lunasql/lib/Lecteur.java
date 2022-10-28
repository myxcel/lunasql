package lunasql.lib;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import lunasql.val.Valeur;

import static lunasql.cmd.Instruction.*;

//import jline.console.ConsoleReader;

/**
 * Classe Lecteur<br> Lecture caractère par caractère d'une ligne d'instruction
 * Coeur du programme : analyse des commandes et exécution
 * 
 * Dates révision : 21/12/2014  LunaSQL v4.2 nouvelle fonction substitute récursive
 *                  26/12/2014  LunaSQL v4.2 bugs traitement des ^ en chaînes "" [] {} `` et $$
 *                  14/01/2015  LunaSQL v4.3 toute substitution d'évaluation se fait par $[]
 *                  31/12/2015  LunaSQL v4.4 substitution par §, et amélioration de non exec
 *                  01/08/2016  LunaSQL v4.5 suppression des !!, et ajout de *$
 *                  31/01/2017  LunaSQL v4.6 commentaires --~ et ; finaux facultatifs
 *                  21/02/2017  LunaSQL v4.6 ; supprimé en mode noexec
 *                  16/04/2017  LunaSQL v4.7 suppression de easy et deep
 *                  06/06/2018  LunaSQL v4.7 ajout de NEXT : cmdstat est modifiable
 *                  13/07/2018  LunaSQL v4.7 ajout de deep
 * 
 * @author Micaël P.
 */
public final class Lecteur {

   private final Contexte cont;
   private final Lecteur father; // lecteurs pères
   private final List<String> links; // liens des variables locales des pères
   private final HashMap<String, String> lecvars;

   private final int nbWhen; // à la sortie, même profondeur de WHEN
   private boolean checkWhen = true;
   private final ArrayList<String> lcmdcour = new ArrayList<>();
   private ArrayList<ArrayList<String>> allcmds;
   private final StringBuilder argcour = new StringBuilder();
   //private final StringBuilder command = new StringBuilder();
   private final Map<String, String> prompts = new HashMap<>();

   // process
   private int ccour = -1, cprec = -1;
   private boolean isgui1 = false, // guillemets simples
            isgui2 = false, // guillemets doubles
            iswhi = false, // espaces blancs
            iscom1 = false, iscom2 = false, // commentaires simple et multi ligne
            ischdur = false, // paramètres en chaîne dure $$ $$
            ispar = false,  // ()
            isacco = false, // {}
            iscroc = false, // []
            isbquo = false; // ``
   private int cmdstat = 0;
   private int nbpar = 0, // nb de parenthèses ouvertes
               nbacc = 0, // nb d'accolades ouvertes
               nbcro = 0, // nombre de crochets ouverts
               nolgn = 1; // numéro de ligne
   private boolean toexec = true,  // commande à exécuter
                   modecs = false; // mode console
   private char endlchar;

   /**
    * Constructeur Lecteur<br> Lecteur par défaut, doit être utilisé avec
    * <code>add</code><br>
    * Mode sub désactivé
    *
    * @param cont le contexte
    */
   public Lecteur(Contexte cont) {
      this.cont = cont;
      this.nbWhen = cont.getNbWhen();
      this.father = cont.getCurrentLecteur();
      this.links = new ArrayList<>();
      this.lecvars = new HashMap<>();
      addLecVar(cont.getAllLecVars()); // variables du lecteur père
      // DEEP
      String sdp = getLecVar(Contexte.LEC_DEEP);
      int dp = 0;
      if (sdp != null) {
         try { dp = Integer.parseInt(sdp); }
         catch (NumberFormatException ex) {}
      }
      setLecVar(Contexte.LEC_DEEP, Integer.toString(dp + 1));
      // Fin
      cont.setCurrentLecteur(this);
   }

   /**
    * Constructeur Lecteur<br>
    * Mode sub désactivé
    *
    * @param cont le contexte
    * @param fichier le fichier SQL à lire
    * @throws java.io.IOException si impossible à lire
    */
   public Lecteur(Contexte cont, File fichier) throws IOException {
      this(cont, new FileReader(fichier), null);
   }

   /**
    * Constructeur Lecteur<br>
    * Mode sub désactivé
    *
    * @param cont le contexte
    * @param reader le reader à lire
    * @throws java.io.IOException si impossible à lire
    */
   public Lecteur(Contexte cont, Reader reader) throws IOException {
      this(cont, reader, null);
   }

   /**
    * Constructeur Lecteur sur flux reader<br>
    * Mode sub désactivé
    *
    * @param cont le contexte
    * @param reader le reader à lire
    * @param vars les variables de lecteur
    * @throws java.io.IOException si impossible à lire
    */
   public Lecteur(Contexte cont, Reader reader, HashMap<String, String> vars) throws IOException {
      this(cont);
      addLecVar(vars);
      // Lecture du fichier
      BufferedReader buf = new BufferedReader(reader);
      String line;
      while ((line = buf.readLine()) != null && cmdstat == RET_CONTINUE) add(line);
      buf.close();
      fin();
   }

   /**
    * Constructeur Lecteur sur chaîne de caractères.<br>
    * Mode sub selon contexte
    *
    * @param cont le contexte
    * @param instr l'instruction à exécuter
    */
   public Lecteur(Contexte cont, String instr) throws IllegalArgumentException {
      this(cont, instr, null, true);
   }

   /**
    * Constructeur Lecteur sur chaîne de caractères.<br>
    * Lecteur utilisé par la commande CmdAlias à exécution déterminée
    * Mode sub selon contexte
    *
    * @param cont le contexte
    * @param instr l'instruction à exécuter
    * @param toexec si le code doit être exécuté
    */
   public Lecteur(Contexte cont, String instr, boolean toexec) throws IllegalArgumentException {
      this(cont, instr, null, toexec);
   }

   /**
    * Constructeur Lecteur.
    *
    * @param cont le contexte
    * @param instr l'instruction à exécuter
    * @param vars les variables de lecteur
    */
   public Lecteur(Contexte cont, String instr, HashMap<String, String> vars){
      this(cont, instr, vars, true);
   }
   public Lecteur(Contexte cont, String instr, HashMap<String, String> vars, boolean toexec)
         throws IllegalArgumentException {
      this(cont);
      this.toexec = toexec;
      if (!toexec) allcmds  = new ArrayList<>();
      addLecVar(vars);
      add(instr);
      fin();
   }

   /**
    * Ajout de commande à exécuter
    *
    * @param instr l'instruction ajoutée
    * @return true si pas d'erreur à la fin de l'exécution
    */
   public final boolean add(String instr) throws IllegalArgumentException {
      if (instr == null) throw new IllegalArgumentException("La commande est NULLE");
      endlchar = (cont.getVar(Contexte.ENV_END_CMD_NL).equals(Contexte.STATE_TRUE) ? '\n' : ';');
      instr += '\n'; // fin de ligne indépendante de endlchar ';'

      // Présubstitution (précoce par §)
      if (cont.getVar(Contexte.ENV_LIST_SUBST).equals(Contexte.STATE_TRUE)) instr = substitute(instr, '§');
      // Traitement
      for (int i = 0; i < instr.length(); i++) {
         if (cmdstat == RET_CONTINUE) {
            ccour = instr.charAt(i);
            process();
         }
         else {
            lcmdcour.clear();
            break;
         }
      }
      return oki();
   }

   /**
    * Exécution en mode console (comportement des fins de lignes un peu différent)
    */
   public final void setConsoleMode() {
      this.modecs = true;
   }

   /**
    * Ajout de commande à exécuter
    *
    * @param instr l'instruction ajoutée
    * @param vars les variables de lecteur
    * @return true si pas d'erreur à la fin de l'exécution
    */
   public final boolean add(String instr, HashMap<String, String> vars) {
      addLecVar(vars);
      return add(instr);
   }

   /**
    * Fixation d'une variable locale volabile Lecteur
    * 
    * @param key le nom de la variable
    * @param val la valeur de la variable
    */
   public void setLecVar(String key, String val) {
      lecvars.put(key, val);
   }

   /**
    * Suppression d'une variable locale volabile Lecteur
    * 
    * @param key le nom de la variable
    * @return true si OK, false sinon
    */
   public boolean unsetLecVar(String key) {
      //if (lecvars == null) return false;
      return lecvars.remove(key) != null;
   }

   /**
    * Obtient la valeur de la variable locale lecteur nommée
    * 
    * @param key le nom de la variable
    * @return la valeur String
    */
   public String getLecVar(String key) {
      //return lecvars == null ? null : lecvars.get(key);
      return lecvars.get(key);
   }

   /**
    * Obtient la liste complète des variables locales de ce lecteur
    * 
    * @return HashMap les var. locales lecteur
    */
   public HashMap<String, String> getAllLecVars(){
      return lecvars;
   }

   /**
    * Teste si la variable est fixée en var locale de lecteur
    * 
    * @param key le nom de la variable
    * @return true si variable existe, false sinon
    */
   public boolean isSet(String key){
      //return key != null && lecvars != null && lecvars.containsKey(key);
      return key != null && lecvars.containsKey(key);
   }

   /**
    * Retourne l'état des commandes
    * 
    * @return RET_CONTINUE, RET_EXIT_SCR, RET_SHUTDOWN...
    */
   public int getCmdState() {
      return cmdstat;
   }

   /**
    * Fixe l'état des commandes, pour Contaxte.evaluerBlock
    *
    * @param s le nouvel état
    */
   public void setCmdState(int s) {
      cmdstat = s;
   }

   /**
    * Sommes-nous dans une expression à parenthèses ? Alors renvoyons le nombre de parenthèses
    * ouvertes
    *
    * @return nb parenthèses
    */
   public int getNP() {
      return nbpar;
   }

   /**
    * Sommes-nous dans une expression à accolades ? Alors renvoyons le nombre d'accolades
    * ouvertes
    *
    * @return nb accolades
    */
   public int getNA() {
      return nbacc;
   }

   /**
    * Sommes-nous dans une expression à crochets ? Alors renvoyons le nombre de crochets
    * ouvertes
    *
    * @return nb crochets
    */
   public int getNC() {
      return nbcro;
   }

   /**
    * Dans quelle forme de chaîne sommes-nous ?
    * 
    * @return le type de chaîne à afficher
    */
   public char getMLineChar() {
      if (ispar) return '(';
      if (isgui2) return '"'; 
      if (isgui1) return '\''; 
      if (isacco) return '{'; 
      if (iscroc) return '['; 
      if (isbquo) return '`'; 
      if (ischdur) return '$';
      if (iscom2) return '*';
      return '-';
   }

   /**
    * Retourne la commande complète en cours du Lecteur courant<br>
    * Utilisé comme parseur de liste pour commande Array
    *
    * @return ArrayList si toexec==false, null sinon
    */
   public ArrayList<ArrayList<String>> getCurrentCmd() {
      return allcmds;
   }

   /**
    * Retourne le Lecteur parent
    *
    * @return Lecteur père
    */
   public Lecteur getFather() {
      return father;
   }

   /**
    * Fixe si à la fin la correspondance WHEN/END est vérifiée
    * @param b si on a vérifié
    */
   public void setCheckWhile(boolean b) {
      checkWhen = b;
   }

   /**
    * Force la vérification de la correspondance WHEN/END
    */
   public void doCheckWhen() {
      if (cont.getNbWhen() != nbWhen) {
         cmderr("WHEN/END : " + cont.getVar(Contexte.ENV_WHEN_DEEP) + " END manquant(s)");
         cont.setNbWhen(nbWhen);
      }
   }

   /**
    * Terminaison du processus
    */
   public final void fin() {
      if (!modecs && !lcmdcour.isEmpty()) runInst(); // ';' final facultatif pour sous-lecteurs et listes
      if (toexec && !oki()) {
         ArrayList<String> ar = new ArrayList<>();
         ar.add(Tools.arrayToString(lcmdcour, false) +
               (argcour.length() > 0 ? " " + argcour.toString() : ""));
         cont.executeCmd("ERRSYN", ar, nolgn);
      }
      // Vérification de la correspondance des WHEN
      if (checkWhen) doCheckWhen();
      // Liaison éventuelle des variables locales au père
      if (father != null) for (String key : links) {
         String val = getLecVar(key);
         father.setLecVar(key, val);
      }
      // Nettoyage et retour au lecteur père
      lecvars.clear();
      links.clear();
      cont.setCurrentLecteur(father);
      // Sortie seulement du script en cours si cmd RETURN
      if (cmdstat == RET_RETR_SCR) cmdstat = RET_CONTINUE;
   }

   /**
    * Ajoute au lecteur courant les variables locales vars
    * @param vars les vars à ajouter
    */
   private void addLecVar(HashMap<String, String> vars){
      if (vars != null) this.lecvars.putAll(vars);
   }

   /**
    * Ajoute un lien vers une variable locale du lecteur père
    * @param key le nom de la variable à lier
    */
   public void addLink(String key){
      links.add(key);
   }

   /**
    * Teste si l'instruction est bien finie
    *
    * @return si c'ets la cas
    */
   private boolean oki() {
      return !isgui2 && !isgui1 && !iscom2 && !ispar && !ischdur && !isacco && !iscroc
          && !isbquo && argcour.length() == 0 && (!modecs || lcmdcour.isEmpty());
   }

   /**
    * Mise à jour de cmdstat si variable EXIT_ON_ERR fixée à 1
    */
   private void cmderr(String msg) {
      cont.erreur("Lecteur", msg, nolgn);
      //if (cont.getVar(Contexte.ENV_EXIT_ERR).equals(Contexte.STATE_TRUE)) cmdstat = RET_EXIT_SCR;
      if (cont.getVar(Contexte.ENV_EXIT_ERR).equals(Contexte.STATE_TRUE)) cmdstat = RET_EV_CATCH;
   }

   /**
    * Bestiolle absconse qui décide si le caractère courant est à inclure dans la commande<br /> Prend en
    * compte les commentaires et les guillements. Met à jour le status du parsing
    */
   private void process() {
      // Caractères spéciaux : délimitation de chaînes dures par $$...$$
      if (!ischdur && cprec == '$' && ccour == '$' &&    // $ littéral, pour $$ de H2
            !iscom1 && !iscom2 && !isgui1 && !isgui2 && !isacco && !iscroc && !isbquo) {
         if (argcour.length() > 0) argcour.deleteCharAt(argcour.length() - 1);
         ischdur = true;
         addArg();
         argcour.append((char) ccour).append((char) ccour);
      }
      else if (ischdur && cprec == '$' && ccour == '$') {
         argcour.append((char) ccour);
         ischdur = false;
         addArg(false); // $$ $$ est séparateur d'arguments initial et terminal

      // Caractères spéciaux : guillemets doubles
      }
      else if (!ischdur && !isgui1 && !isacco && !iscroc && !isbquo &&
            ccour == '"') {
         if (cprec == '^') {
            if (argcour.length() > 0 && !iscom1 && !iscom2){
               if (toexec) argcour.deleteCharAt(argcour.length() - 1);
               argcour.append((char) ccour);
            }
         }
         else {
            if (iscom1 || iscom2) ;
            else if (isgui2) {
               isgui2 = false;
               if (!toexec) argcour.append((char) ccour);
               addArg(); // " est séparateur d'arguments terminal
            }
            else {
               isgui2 = true;
               addArg(); // " est séparateur d'arguments initial
               if (toexec) argcour.append((char) 0);
               else argcour.append((char) ccour);
            }
         }
      }

      // Caractères spéciaux : guillemets simples SQL
      else if (!ischdur && !isgui2 && !isacco && !iscroc && !isbquo &&
            ccour == '\'') {
         if (cprec == '^') {
            if (argcour.length() > 0 && !iscom1 && !iscom2){
               if (toexec) argcour.deleteCharAt(argcour.length() - 1);
               argcour.append((char) ccour);
            }
         }
         else if (iscom1 || iscom2) ;
         else {
            argcour.append((char) ccour);
            if (isgui1) iswhi = isgui1 = false;
            else isgui1 = true;
         }
      }

      // Caractères spéciaux : accolades {}
      else if (!ischdur && !isgui1 && !isgui2 && !iscroc && !isbquo
            && (ccour == '{' || ccour == '}')) {
         if (cprec == '^') {
            if (argcour.length() > 0 && !iscom1 && !iscom2){
               if (toexec) argcour.deleteCharAt(argcour.length() - 1);
               argcour.append((char) ccour);
            }
         }
         else if (iscom1 || iscom2) ;
         else {
            if (ccour == '{') {
               if (!isacco) addArg(); // } est séparateur d'arguments initial si premier '}'
               if (!toexec || isacco) argcour.append((char) ccour);
               else argcour.append((char) 0);
               nbacc++;
            }
            else {
               iswhi = false;
               nbacc--;
               if (!toexec || nbacc > 0) argcour.append((char) ccour);
               if (nbacc == 0) addArg(false); // } est séparateur d'arguments terminal si dernier '}'
            }
            isacco = nbacc > 0;
         }
      }

      // Caractères spéciaux : crochets []
      else if (!ischdur && !isgui1 && !isgui2 && !isacco && !isbquo
            && (ccour == '[' || ccour == ']')) {
         if (cprec == '^') {
            if (argcour.length() > 0 && !iscom1 && !iscom2){
               if (toexec) argcour.deleteCharAt(argcour.length() - 1);
               argcour.append((char) ccour);
            }
         }
         else if (iscom1 || iscom2) ;
         else {
            argcour.append((char) ccour);
            if (ccour == '[') nbcro++;
            else {
               iswhi = false;
               nbcro--;
            }
            iscroc = nbcro > 0;
         }
      }

      // Caractères spéciaux : backquotes ``
      else if (!ischdur && !isgui1 && !isgui2 && !isacco && !iscroc &&
            ccour == '`') {
         if (cprec == '^') {
            if (argcour.length() > 0 && !iscom1 && !iscom2){
               if (toexec) argcour.deleteCharAt(argcour.length() - 1);
               argcour.append((char) ccour);
            }
         }
         else if (iscom1 || iscom2) ;
         else {
            argcour.append((char) ccour);
            if (isbquo) iswhi = isbquo = false;
            else isbquo = true;
         }
      }

      else if (ischdur || isacco) 
         argcour.append((char) ccour); // séquence échappée par {} ou !!

      else if (isgui2 || isgui1 || iscroc || isbquo) {
         if (ccour == endlchar && cprec == '^' && toexec)  // Fin de ligne en chaîne
            argcour.deleteCharAt(argcour.length() - 1);
         else
            argcour.append((char) ccour); // séquence échappée par guillemets ou [] ou ``
      }

      // Caractères spéciaux : commentaires doubles et simples
      else if (cprec == '/' && ccour == '*' && !iscom1 && !iscom2) { // commentaire type multi SQL
         if (argcour.length() > 0) argcour.deleteCharAt(argcour.length() - 1);
         iscom2 = true;
      }
      else if (cprec == '*' && ccour == '/' && iscom2) { // fin commentaire type multi
         iscom2 = false;
         addArg();
      }
      else if (cprec == '-' && ccour == '-' && !iscom2 && !iscom1) { // commentaire type simple SQL
         if (argcour.length() > 0) argcour.deleteCharAt(argcour.length() - 1);
         iscom1 = true;
      }
      else if (cprec == '-' && ccour == '~' && iscom1) iscom1 = false; // commentaires actifs --~

      // OK, ce n'est pas un commentaire multi ni un guillemet double
      else if (!iscom1 && !iscom2) {
         if (ccour == ';') { // Fin de commande absolu
            if (cprec == '^') {
               if (argcour.length() > 0 && toexec) argcour.deleteCharAt(argcour.length() - 1);
               argcour.append((char) ccour);
            }
            else {
               //if (!toexec) argcour.append((char) ccour);
               runInst();
               ispar = false;
            }
         }
         else if (ccour == endlchar) { // Fin de commande
            if (cprec == '^') {
               if (argcour.length() > 0) argcour.deleteCharAt(argcour.length() - 1);
            } else if (ispar) {
               // Saut de ligne ajouté en argument (un seul saut ajouté)
               if (cprec != endlchar) argcour.append((char) ccour);
               addArg();
            } else runInst();
         }
         else if (Character.isWhitespace(ccour)) {
            // Caractère blanc
            if (!iswhi) {
               addArg();
               iswhi = true;
            }
         }
         else {
            if (ccour == '(') nbpar++;
            else if (ccour == ')') nbpar--;
            ispar = nbpar > 0;
            // Ajout du caractère courant
            argcour.append((char) ccour);
            iswhi = false;
         }
      }
      // Caractères spéciaux : fin de ligne (indépendamment du paramétrage)
      if (ccour == '\n') {
         if(iscom1) {
            iscom1 = false;
            process(); // pour valider fin de comm simple ligne.
         }
         else nolgn++;
      }
      // Conclusion
      cprec = ccour;
   }

   /**
    * Ajout d'argument si conditions remplies avec substitution
    */
   private void addArg() {
      addArg(true);
   }

   /**
    * Ajout d'argument si conditions remplies avec substitution
    * @param tosub s'il faut substituer
    */
   private void addArg(boolean tosub) {
      if (argcour.length() > 0) {
         if (argcour.charAt(0) == (char) 0) argcour.deleteCharAt(0);

         // Conclusion : ajout en commande temporaire (avec ou sans substitutions)
         if (tosub && toexec && cont.canExec()) {
            String s = substitute(argcour.toString(), '$');
            if (s.length() > 2 && s.charAt(0) == '^' && s.charAt(1) == '~')
               lcmdcour.add(s.substring(1));
            else if (s.length() > 2 && s.charAt(0) == '~')
               lcmdcour.addAll(Arrays.asList(Tools.blankSplitLec(cont, s.substring(1))));
            else lcmdcour.add(s);
         }
         else {
            //int l = argcour.length() - 1;
            //if (l > 0 && argcour.charAt(l) == ';') argcour.deleteCharAt(l);
            lcmdcour.add(argcour.toString());
         }
      }
      argcour.setLength(0);
   }

   /**
    * Substitution des $ en argument, appelée en externe à process
    * 
    * @param arg l'argument
    * @return l'argument éventuellement substitué
    */
   public String substituteExt(String arg) {
      return substitute(arg, '$');
   }

   /*
    * Substitution générale récursive
    */
   private String substitute(String arg, char sub) {
      // Sortie si pas de $ ou si arg est '$'
      if (arg.indexOf(sub) < 0 || arg.equals("" + sub)) return arg;

      StringBuilder ret = new StringBuilder(), keysb = new StringBuilder();
      char cc = 0, c1 = 0, c2; // caractères courant et précédents 1 et 2
      boolean issub = false, nodel = false;
      char subcarop = 0, subcarcl = 0; // caractères délimiteurs de substitution
      int idsubcar, nbsubcar = 0;

      for (int i = 0; i < arg.length(); i++) { // parcours de la chaîne arg
         c2 = c1; // précédent 2
         c1 = cc; // précédent 1
         cc = arg.charAt(i);

         if (issub) { // en cours de construction de clef
            if (subcarop == '[' && c1 == '^' && (cc == '[' || cc == ']')) { // ex. $[print ^^]]
               if (keysb.length() > 0) keysb.deleteCharAt(keysb.length() - 1);
               keysb.append(cc);
            }
            else {
               if (cc == subcarcl) nbsubcar--; // -- avant ++ à cause de ``
               else if (cc == subcarop) nbsubcar++;
               // Fin de la clef : substitution
               if (nbsubcar == 0) {
                  String key;
                  if (subcarop == '[') {
                     // pas de récursivité pour les commandes $[] car descente naturelle
                     key = substitCmd(keysb.toString());
                  }
                  else {
                     key = substitute(keysb.toString(), sub);
                     switch (subcarop) {
                        case '(': key = substitDef(key); break;
                        case '<': key = substitCar(key); break;
                        case '`': key = substitPrm(key); break;  // pas d'imbrication
                     }
                  }
                  ret.append(key);
                  keysb.setLength(0);
                  issub = false;
               }
               else keysb.append(cc);
            }
         }
         else if (cc == sub && c1 == '^') { // Gestion des $ échappés
            if (ret.length() > 0) ret.deleteCharAt(ret.length() - 1);
            ret.append(cc);
         }
         else if (cc == sub && keysb.length() == 0) ; // Suppression du $
         else if (c1 == sub && c2 != '^' && (isVarChar(cc) || cc == ':')) {
            if (keysb.length() > 0) {
               ret.append(substitDef(keysb.toString()));
               keysb.setLength(0);
            }
            keysb.append(cc);
            nodel = true;
            if (i == arg.length() - 1) ret.append(substitDef(keysb.toString()));
         }
         else if (nodel) {
            nodel = isVarChar(cc);
            if (nodel) {
               keysb.append(cc);
               if (i == arg.length() - 1) ret.append(substitDef(keysb.toString()));
            }
            else {
               ret.append(substitDef(keysb.toString()));
               keysb.setLength(0);
               i--;
            }
         }
         else if (c1 == sub && c2 != '^' && (idsubcar = "<([`".indexOf(cc)) >= 0) {
            subcarop = cc;
            subcarcl = ">)]`".charAt(idsubcar);
            nbsubcar = 1;
            issub = true;
         }
         else ret.append(cc);
      }// for
      if (issub)
         cont.errprintln("Substitution : clef '" + keysb.toString() + "' : fin attendue : " + subcarcl);
      return ret.toString();
   }

   /*
    * Substitution de caractères
    */
   private String substitCar(String key) {
      if (key.isEmpty()) {
         cmderr("absence de code caractère");
         return "";
      }

      String scar, val;
      int nbrep;
      boolean isstr;
      try {
         Matcher m = Contexte.CAR_PATTERN_LEC.matcher(key);
         if (m.matches()) {
            isstr = m.group(4) == null;
            scar = m.group(isstr ? 3 : 4);
            //for (int i=1; i<=6; i++) cont.println("group="+i + ": " + m.group(i)); // debug
            nbrep = m.group(6) == null ? 1 : Integer.parseInt(m.group(6));
            if (nbrep < 1) throw new NumberFormatException();
         }
         else {
            cmderr("code caractère incorrect : " + key);
            return "";
         }

         // Répétition selon chaîne saisie
         if (isstr) {
            StringBuilder sb = new StringBuilder(scar);
            for (int i = 1; i < nbrep; i++) sb.append(scar);
            val = sb.toString();
         }
         else {
            switch (scar.charAt(0)) {
               case 'n': val = Contexte.END_LINE; break;
               case 't': val = "\t"; break;
               case 'e': val = " ";  break;
               case 'g': val = "\""; break;
               case 'q': val = "'";  break;
               case 'Q': val = "`";  break;
               case 'c': val = "^";  break;
               case 'd': val = "$";  break;
               case 'y': val = "}";  break;
               case 'Y': val = "{";  break;
               case 'k': val = "]";  break;
               case 'K': val = "[";  break;
               case 's': val = ">";  break;
               case 'S': val = "<";  break;
               case 'p': val = ")";  break;
               case 'P': val = "(";  break;
               case 'l': val = "\u0007"; break;
               case 'r': val = "\r"; break;
               case 'b': val = "\b"; break;
               case 'f': val = "\f"; break;
               default: val = "" + (char) Integer.parseInt(scar);
            }
            val = new String(new char[nbrep]).replace("\0", val);
         }
      }
      catch (NumberFormatException ex) {
         cmderr("code ASCII incorrect : " + key + " (" + ex.getMessage() + ")");
         return "";
      }
      return val;
   }

   /*
    * Substitution de variables
    */
   private String substitDef(String key) {
      char traitcar = 0;
      boolean modifref = false, noerr = false;
      String traitstr = "";

      Matcher m = Contexte.KEY_PATTERN_LEC.matcher(key);
      if (m.matches()) {
         if (m.group(1).equals("*")) noerr = true;       // *
         key = m.group(2);                               // clef
         if (!m.group(4).isEmpty()) traitcar = m.group(4).charAt(0); // ?:/\%#&@
         if(m.group(5).equals("!")) modifref = true;     // !
         if (!m.group(6).isEmpty()) traitstr = m.group(6); // chaîne traitement
      }
      else {
         cmderr("identifiant '" + key + "' invalide");
         return "";
      }

      // Gestion des variables temporaires, puis lecteur, puis contexte
      String val = cont.getTmpVar(key);   // var éphémère : (_l), $(_n), $(1)...
      int typvar = 0;
      if (val == null) {
         val = cont.getLecVar(key);  // var locale
         typvar = 1;
      }
      if (val == null) {
         val = cont.getGlbVar(key);     // var globale
         typvar = 2;
      }

      if (val == null) {   // aucune var trouvée
         val = (traitcar == '?' ? traitstr : "");
         typvar = 1;
         if (!noerr) cmderr("variable '" + key + "' non définie");
      }
      else {
         // Application du post-traitement
         switch (traitcar) {
            case 0:  // pas de modificateur ou modificateur invalide
               if (!traitstr.isEmpty())
                  cmderr("modificateur de variable invalide : '" + traitstr + "'");
            case '?':
               if (!noerr && val.isEmpty()) val =  traitstr;
               break;
            case '=':
               val = val.equals(traitstr) ? "1" : "0";
               break;
            case '~':
               val = val.equals(traitstr) ? "0" : "1";
               break;
            case '\\':
               int nb;
               if (traitstr.equals("g")) nb = -1;
               else {
                  try {
                     nb = Integer.parseInt(traitstr);
                     if (nb < 0) throw new NumberFormatException(nb + " (nombre positif attendu)");
                  } catch (NumberFormatException ex) {
                     cmderr("numéro de lecteur père incorrect : " + ex.getMessage());
                     nb = 0;
                  }
               }
               // recherche globale si 'g' ou si link(nb) non definie
               val = nb == -1 ? cont.getGlbVar(key) : cont.getLinkVar(key, nb);
               if (val == null) val = cont.getGlbVar(key);
               if (val == null) {
                  val = "";
                  cmderr("variable '" + key + "' non définie");
               }
               break;
            case ',' :
               Properties prop = Tools.getProp(val);
               //Properties prop = cont.getVarDict(val);
               if (prop == null) cmderr("dictionnaire invalide ou non défini");
               else val = prop.getProperty(traitstr, "");
               break;
            case '/':
               try { val = val.matches(traitstr) ? "1" : "0"; }
               catch (PatternSyntaxException ex) {
                  cmderr("erreur de syntaxe en patron regexp : " + ex.getMessage());
               }
               break;
            case '%':
               try {
                  Object r = cont.evaluerExpr(val + traitstr);
                  val = r == null ? "null" : r.toString();
               }
               catch (Exception e) {
                  cmderr("impossible d'évaluer '" + val + traitstr + "' : " + e.getMessage());
               }
               break;
            case '@':
               try { val = Tools.subList(cont, val, traitstr); }
               catch(NumberFormatException ex) {
                  cmderr("erreur de format de sous-liste : " + traitstr);
               }
               break;
            case '&':
               try { val = Tools.subString(val, traitstr); }
               catch(NumberFormatException ex) {
                  cmderr("erreur de format de sous-chaîne : " + traitstr);
               }
               break;
            case ':':
               for (String kv : traitstr.replace("^,", "%<v>")
                                        .replace("^=", "%<e>").split(",")) {
                  int j;
                  kv = kv.replace("%<v>", ",");
                  if ((j = kv.indexOf('=')) > 0) {
                     String k = kv.substring(0, j).replace("%<e>", "="),
                            v = kv.substring(j + 1).replace("%<e>", "=");
                     try { val = val.replaceAll(k, v); }
                     catch(PatternSyntaxException ex) {
                        cmderr("erreur de syntaxe en patron regexp : " + ex.getMessage());
                     }
                  }
                  else cmderr("erreur de format de substitution : '" + traitstr + "'");
               }
               break;
            case '#':
               // Extraction des arguments des fonctions
               String[] sargs;
               int a1 = traitstr.indexOf('|'), a2 = traitstr.lastIndexOf('|');
               if(a1 > 0 && a2 > a1){
                  sargs = traitstr.substring(a1 + 1, a2).replace("^,", "%<v>").split(",");
                  for(int j=0; j<sargs.length; j++)
                     sargs[j] = sargs[j].replace("%<v>", ",");
                  traitstr = traitstr.substring(0, a1);
               }
               else sargs = null;

               // Recherche de la fonction ad hoc
               if (traitstr.equalsIgnoreCase("inc!")) {
                  try { val = Integer.toString(Integer.parseInt(val) + 1); }
                  catch (NumberFormatException ex) {
                     cmderr("commande inc : nombre entier invalide : " + val);
                  }
                  modifref = true;
               }
               else if (traitstr.equalsIgnoreCase("dec!")) {
                  try { val = Integer.toString(Integer.parseInt(val) - 1); }
                  catch (NumberFormatException ex) {
                     cmderr("commande dec : nombre entier invalide : " + val);
                  }
                  modifref = true;
               }
               else if (traitstr.equalsIgnoreCase("len")) {
                  val = Integer.toString(val.length());
               }
               else if (traitstr.equalsIgnoreCase("empty?")) {
                  val = val.isEmpty() ? "1" : "0";
               }
               else if (traitstr.equalsIgnoreCase("size")) {
                  String[] tval = Tools.blankSplitLec(cont, val);
                  val = Integer.toString(tval.length);
               }
               else if (traitstr.equalsIgnoreCase("index")) {
                  if (sargs == null || sargs.length != 1) cmderr("commande index : arguments invalides");
                  else val = Integer.toString(val.indexOf(sargs[0]));
               }
               else if (traitstr.equalsIgnoreCase("lstindex")) {
                  if (sargs == null || sargs.length != 1) cmderr("commande lstindex : arguments invalides");
                  else {
                     String[] tval = Tools.blankSplitLec(cont, val);
                     val = "-1";
                     for (int i = 0; i < tval.length; i++)
                        if (tval[i].equals(sargs[0])) {
                           val = Integer.toString(i); break;
                        }
                  }
               }
               else if (traitstr.equalsIgnoreCase("flat")) {
                  val = Tools.flatList(val);
               }
               else if (traitstr.equalsIgnoreCase("shift")) {
                  String[] tval = Tools.blankSplitLec(cont, val);
                  if (tval.length > 1){
                     tval[0] = "";
                     StringBuilder sb = new StringBuilder();
                     for(String s : tval) if (s.length() > 0) sb.append(Tools.putBraces(s)).append(' ');
                     val = sb.deleteCharAt(sb.length() - 1).toString();
                  } else val = "";
               }
               else if (traitstr.equalsIgnoreCase("shift!")) {
                  if (cont.isNonSys(key)) {
                     String[] tval = Tools.blankSplitLec(cont, val);
                     if (tval.length > 1){
                        val = tval[0];
                        tval[0] = "";
                        StringBuilder sb = new StringBuilder();
                        for(String s : tval) if (s.length() > 0) sb.append(Tools.putBraces(s)).append(' ');
                        if (typvar == 1) cont.setLecVar(key, sb.deleteCharAt(sb.length() - 1).toString());
                        else if (typvar == 2) cont.setVar2(key, sb.deleteCharAt(sb.length() - 1).toString());
                     }
                     else if (tval.length == 1){
                        val = tval[0];
                        if (typvar == 1) cont.setLecVar(key, "");
                        else if (typvar == 2) cont.setVar2(key, "");
                     }
                     else val = "";
                  }
                  else cmderr("affectation de variable invalide");
               }
               else if (traitstr.equalsIgnoreCase("pop!")) {
                  if (cont.isNonSys(key)) {
                     String[] tval = Tools.blankSplitLec(cont, val);
                     if (tval.length > 1){
                        val = tval[tval.length - 1];
                        tval[tval.length - 1] = "";
                        StringBuilder sb = new StringBuilder();
                        for(String s : tval) if (s.length() > 0) sb.append(Tools.putBraces(s)).append(' ');
                        if (typvar == 1) cont.setLecVar(key, sb.deleteCharAt(sb.length() - 1).toString());
                        else if (typvar == 2) cont.setVar2(key, sb.deleteCharAt(sb.length() - 1).toString());
                     }
                     else if (tval.length == 1){
                        val = tval[0];
                        if (typvar == 1) cont.setLecVar(key, "");
                        else if (typvar == 2) cont.setVar2(key, "");
                     }
                     else val = "";
                  }
                  else cmderr("affectation de variable invalide");
               }
               else if (traitstr.equalsIgnoreCase("next")) {
                  try { val = stringInc(val); }
                  catch (IllegalArgumentException ex) {
                     cmderr("commande next : " + ex.getMessage());
                  }
               }
               else if (traitstr.equalsIgnoreCase("eval")) {
                  HashMap<String, String> vars = new HashMap<>();
                  vars.put(Contexte.LEC_THIS, key);
                  cont.addSubMode();
                  Lecteur lec = new Lecteur(cont, val, vars);
                  cmdstat = lec.getCmdState();
                  cont.remSubMode();
                  Valeur vr = cont.getValeur();
                  String r;
                  val = vr == null ? "" : ((r = vr.getSubValue()) == null ? "" : r);
               }
               else if (traitstr.equalsIgnoreCase("sub")) {
                  Lecteur lec = new Lecteur(cont);
                  lec.setLecVar(Contexte.LEC_SUPER, "sub");
                  val = lec.substituteExt(val);
                  cmdstat = lec.getCmdState();
               }
               // ...autres commandes
               else cmderr("commande de variable inconnue : '" + traitstr + "'");
               break;
            //...
            // autres modificateurs (à ajouter en Contexte.KEY_PATTERN_LEC)
         }
      }
      if (modifref) { // modification de la référence si autorisée
         if (cont.isNonSys(key)) {
            if (typvar == 1)
               cont.setLecVar(key, val);
            else if (typvar == 2)
               cont.setVar2(key, val);
         }
         else
            cmderr("affectation de variable invalide");
      }
      return val;
   }

   /*
    * Substitution de commande
    */
   private String substitCmd(String key) {
      cont.addSubMode();
      HashMap<String, String> vars = new HashMap<>();
      vars.put(Contexte.LEC_SUPER, "$[]");

      Lecteur lec = new Lecteur(cont, key, vars); // pas ajouter de ';' car c'est une commande
      cmdstat = lec.getCmdState();
      Valeur vr = cont.getValeur();
      cont.remSubMode();
      String r;
      return vr == null ? "" : ((r = vr.getSubValue()) == null ? "" : r);
   }

   /*
    * Substitution de prompt
    */
   private String substitPrm(String key) {
      String val = "";
      boolean isprt = false, ismsk = false;
      if (key.length() > 0) {
         Matcher m = Pattern.compile("^(\\?)?(\\*)?(.*)").matcher(key);
         if (m.matches()) {
            isprt = m.group(1) != null; // ?
            ismsk = m.group(2) != null; // *
            key = m.group(3);
            //for (int i=1; i<=2; i++) cont.println("group="+i + ": " + m.group(i)); // debug
         }
      }
      if (prompts.containsKey(key)) val = prompts.get(key);
      else {
         try {
            val = cont.getInput(isprt ? "Entrer '" + key + "' : " : key, ismsk ? '*' : null);
            if (val == null) val = "";
            prompts.put(key, val);
         }
         catch (IOException ex) {
            cmderr("erreur IOException en lecture de paramètre : " + ex.getMessage());
         }
      }
      return val;
   }

   /**
    * Teste si un caractère peut faire partie d'un nom de variable
    * @param c la caractère à tester
    * @return si c'est la cas
    */
   private boolean isVarChar(char c) {
      return Character.isLetterOrDigit(c) || c == '_';
   }

   /**
    * Ajout d'instruction si conditions remplies
    */
   private void runInst() {
      addArg();
      if (!lcmdcour.isEmpty()) {
         if (toexec) {
            if (cmdstat == RET_CONTINUE) {
               ArrayList<String> ar = new ArrayList<>(lcmdcour);
               cmdstat = cont.executeCmd(ar.get(0), ar, nolgn);
            }
         }
         else allcmds.add(new ArrayList<>(lcmdcour));
      }
      lcmdcour.clear();
      iswhi = false;
   }

   /*
    * Incrémentation de chaîne
    */
   public static String stringInc(String s) {
      int l;
      if (s == null || (l = s.length()) == 0) return null;

      char c = s.charAt(s.length() - 1);
      String r;
      if (Character.isLetter(c)) {
         if (l == 1) {
            if (c == 'z') return "aa";
            else if (c == 'Z') return "AA";
         }
         r = s.substring(0, l - 1);
         if (c == 'z') return stringInc(r) + 'a';
         else if (c == 'Z') return stringInc(r) + 'A';
      }
      else if (Character.isDigit(c)) {
         if (c == '9' && l == 1) return "10";
         r = s.substring(0, l - 1);
         if (c == '9') return stringInc(r) + '0';
      }
      else throw new IllegalArgumentException("caractère illégal : '" + c + "'");
      c++;
      return r + c;
   }
}// class
