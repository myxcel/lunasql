package lunasql.lib;


import java.io.*;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import jline.ConsoleReader;
import jline.History;
import lunasql.Config;
import lunasql.Console;
import lunasql.cmd.*;
import lunasql.lib.ret.RetPlugin;
import lunasql.sql.SQLCnx;
import lunasql.sql.TypesSGBD;
import lunasql.val.EvalEngine;
import lunasql.val.Valeur;

import static lunasql.cmd.Instruction.*;
//import jline.console.ConsoleReader;
//import jline.console.history.FileHistory;

/**
 * Contexte d'exécution des commandes : connexion SQL et variables d'environnement
 *
 * @author M.P.
 */
public final class Contexte {

   // *** Patrons Regexp de définitions des identifiants de clefs ***
   public static final String  KEY_PATTERN    = "\\p{L}(\\p{L}|\\d|[._-])*"; // pas de ^$
   protected static final Pattern KEY_PATTERN_LEC   = Pattern.compile(
            "^(\\*?)(\\d+|[_:.]?" + KEY_PATTERN + ")([?=~,\\\\/%@&:#]?)([!]?)(.*)$"); // $1, $(*x?ND), $(*x%zzz)...
   // match   1     2                          4              5     6            parmi ?:/,\%#&@
   protected static final Pattern CAR_PATTERN_LEC   = Pattern.compile(
            "^(('(.*)')|([ntegqQcdyYkKsSpPlrbf]|[0-9]+))(\\*(\\d+))?$"); // $<'str'*n>|$<c*n>
   // match   12 3      4                        5   6

   // Autres constantes
   public static final String END_LINE = System.getProperty("line.separator");

   // *** Constantes statiques ***
   /** Etat de sortie : pas de modification */
   public static final String                  STATE_FALSE    = "0";
   /** Etat de sortie : modifications lors de la commande */
   public static final String                  STATE_TRUE     = "1";
   /** Etat de sortie : commande erronée */
   public static final String                  STATE_ERROR    = "E";
   // *** Etats de verbose ***
   /** Etat de verbose : aucun message affiché */
   public static final int                     VERB_SIL       = 0;
   /** Etat de verbose : uniquement les textes demandés et les erreurs */
   public static final int                     VERB_AFF       = VERB_SIL + 1;
   /** Etat de verbose : message AFFICHAGE + messages info de base */
   public static final int                     VERB_MSG       = VERB_AFF + 1;
   /** Etat de verbose : message MESSAGE + commandes et infos supplémentaires */
   public static final int                     VERB_BVR       = VERB_MSG + 1;
   /** Etat de verbose : tous messages, même les inutiles affichés */
   public static final int                     VERB_DBG       = VERB_BVR + 1;
   /** Nombre de verbes */
   public static final int                     VERB_NUMBER    = VERB_DBG + 1;
   /** Tableau des différents états de verbose */
   public static final String[]                VERBLIB        = new String[] {
         "SILENCE", "DISPLAY", "MESSAGE", "CHATTY", "DEBUG"
   };

   /** Quelques couleurs (à modifier plus bas en Contexte et dans CmdPrint) */
   public static final int
         NONE = 0,
         BLACK = NONE + 1,
         RED = BLACK + 1,
         GREEN = RED + 1,
         YELLOW = GREEN + 1,
         BLUE = YELLOW + 1,
         MAGENTA = BLUE + 1,
         CYAN = MAGENTA + 1,
         WHITE = CYAN + 1,
         BR_BLACK = WHITE + 1,
         BR_RED = BR_BLACK + 1,
         BR_GREEN = BR_RED + 1,
         BR_YELLOW = BR_GREEN + 1,
         BR_BLUE = BR_YELLOW + 1,
         BR_MAGENTA = BR_BLUE + 1,
         BR_CYAN = BR_MAGENTA + 1,
         BR_WHITE = BR_CYAN + 1;
   public static final String[] COLORS = {
      /* NONE */ "\u001b[0m",
      /* BLACK */ "\u001b[30m",
      /* RED */ "\u001b[31m",
      /* GREEN */ "\u001b[32m",
      /* YELLOW */ "\u001b[33m",
      /* BLUE */ "\u001b[34m",
      /* MAGENTA */ "\u001b[35m",
      /* CYAN */ "\u001b[36m",
      /* WHITE */ "\u001b[37m",
      /* BR_BLACK */ "\u001b[90m",
      /* BR_RED */ "\u001b[91m",
      /* BR_GREEN */ "\u001b[92m",
      /* BR_YELLOW */ "\u001b[93m",
      /* BR_BLUE */ "\u001b[94m",
      /* BR_MAGENTA */ "\u001b[95m",
      /* BR_CYAN */ "\u001b[96m",
      /* BR_WHITE */ "\u001b[77m"
   };

   // *** Variables d'environnement ***
   /** Autorisation d'édition des options */
   public static final String                  ENV_CONST_EDIT = "_CONST_EDIT";
   /** Autorisation d'édition des options */
   public static final String                  ENV_SQL_UPDATE = "_SQL_UPDATE";
   /** Variable d'environnement : moteur d'évaluation de la console */
   public static final String                  ENV_EVAL_ENG   = "_EVAL_ENGINE";
   /** Variable d'environnement : version de l'application */
   public static final String                  ENV_VERSION    = "_VERSION";
   /** Variable d'environnement : nom de version de l'application */
   public static final String                  ENV_VERS_NAME  = "_VERS_NAME";
   /** Variable d'environnement : libellé du type de la base de données */
   public static final String                  ENV_DB_TYPE    = "_DB_TYPE";
   /** Variable d'environnement : numéro du type de la base de données */
   public static final String                  ENV_DB_NTYPE   = "_DB_NTYPE";
   /** Variable d'environnement : driver de connexion */
   public static final String                  ENV_CNX_DRIVER = "_CNX_DRIVER";
   /** Variable d'environnement : chemin de connexion */
   public static final String                  ENV_CNX_PATH   = "_CNX_PATH";
   /** Variable d'environnement : base de donnée de connexion */
   public static final String                  ENV_CNX_BASE   = "_CNX_BASE";
   /** Variable d'environnement : login de connexion */
   public static final String                  ENV_CNX_LOGIN  = "_CNX_LOGIN";
   /** Variable d'environnement : état de sortie des commandes */
   public static final String                  ENV_CMD_STATE  = "_CMD_STATE";
   /** Variable d'environnement : si on sort du fichier en cas d'erreur */
   public static final String                  ENV_EXIT_ERR   = ":EXIT_ON_ERR";
   /** Variable d'environnement : libellé état de verbose */
   public static final String                  ENV_VERBOSE    = "_VERBOSE";
   /** Variable d'environnement : numéro de l'état de verbose */
   public static final String                  ENV_NVERBOSE   = "_NVERBOSE";
   /** Variable d'environnement : chemin des scripts de la console */
   public static final String                  ENV_SCR_PATH   = ":SCRIPTS_PATH";
   /** Variable d'environnement : chemin des plugins de la console */
   public static final String                  ENV_PLG_PATH   = ":PLUGINS_PATH";
   /** Variable d'environnement : nom du fichier d'initialisation de la console */
   public static final String                  ENV_INIT_FILE  = "_INIT_FILE";
   /** Variable d'environnement : nom du fichier des définitions des bases */
   public static final String                  ENV_BASES_FILE  = "_BASES_FILE";
   /** Variable d'environnement : nom du fichier d'historique de la console */
   public static final String                  ENV_HIST_FILE  = "_HISTORY_FILE";
   /** Variable d'environnement : doublage de l'historique des commandes par "" */
   public static final String                  ENV_HIST_DBL   = ":HISTORY_DBL";
   /** Variable d'environnement : nom du fichier de configuration de l'appli */
   public static final String                  ENV_CONF_FILE  = "_CONFIG_FILE";
   /** Variable d'environnement : si on sauve l'état des variables à la sortie */
   public static final String                  ENV_SAVE_CONF  = ":AUTOSAVE_CFG";
   /** Variable d'environnement : prompt de la console */
   public static final String                  ENV_PROMPT     = ":PROMPT";
   /** Variable d'environnement : cloche (bip) ennervant sur erreur */
   public static final String                  ENV_BEEP_ON    = ":BEEP_ON";
   /** Variable d'environnement : si les alias prennent les arguments de la ligne */
   public static final String                  ENV_ALIAS_ARG  = ":ALIAS_ARG";
   /** Variable d'environnement : date du jour au format JJ/MM/DDDD) */
   public static final String                  ENV_DAY_DATE   = "_DAY_DATE";
   /** Variable d'environnement : date du jour (et rien de plus) */
   public static final String                  ENV_DAY_DATE_F = "_DAY_DATE_F";
   /** Valeur de retour des commandes */
   public static final String                  ENV_RET_VALUE  = "_RET_VALUE";
   /** Valeur de nombre de ligne des commandes de type SELECT */
   public static final String                  ENV_RET_NLINES = "_RET_NLINES";
   /** Variable d'environnement : état de profondeur des WHEN */
   public static final String                  ENV_WHEN_DEEP  = "_WHEN_DEEP";
   /** Variable d'environnement : interaction avec l'utilisateur */
   public static final String                  ENV_INTERACT   = ":FILE_CONFIRM";
   /** Charset d'encoding des fichiers lus */
   public static final String                  ENV_FILE_ENC   = ":ENCODING";
   /** Autorisation d'exécuter les alias récursifs (à vos risques et périls !) */
   public static final String                  ENV_ALLOW_REC  = ":ALLOW_RECUR";
   /** Caractère de fin d'instruction (fin de ligne ou point-virgule) */
   public static final String                  ENV_END_CMD_NL = ":END_CMD_NL";
   /** Taille maximale de largeur de colonne des retour de SELECT */
   public static final String                  ENV_COL_MAX_WTH = ":COL_MAX_WIDTH";
   /** Nombre maximal de lignes à retourner */
   public static final String                  ENV_ROW_MAX_NB = ":ROW_MAX_NB";
   /** Affichage des numéros de ligne */
   public static final String                  ENV_ADD_ROW_NB = ":ADD_ROW_NB";
   /** Mode d'affichage des résultats d'un SELECT : tablulaire 1, ou linéaire 0*/
   public static final String                  ENV_SELECT_ARR = ":SELECT_ARRAY";
   /** Support des substitutions fortes par § */
   public static final String                  ENV_LIST_SUBST = ":LIST_SUBSTIT";
   /** Mesure du temps pour la commande CHRON */
   public static final String                  ENV_EXEC_TIME  = "_EXEC_TIME";
   /** Liste des bibliothèques chargées par commande REQUIRE */
   public static final String                  ENV_LOADED_LIBS = "_LOADED_LIBS";
   /** Gestion des couleurs activée */
   public static final String                  ENV_COLORS_ON  = ":COLORS_ON";
   /** Exécution de code en cas d'erreur */
   public static final String                  ENV_ON_ERROR = ":ON_ERROR";
   /** Exécution de code en cas d'erreur */
   public static final String                  ENV_ON_INIT = ":ON_INIT";
   /** Exécution de code en cas d'erreur */
   public static final String                  ENV_ON_QUIT = ":ON_QUIT";
   /** Tampon de commandes à avoir sous la main */
   public static final String                  ENV_CMD_BUFFER = "_CMD_BUFFER";
   /** Configuration d'un éditeur de texte manuel */
   public static final String                  ENV_EDIT_PATH = ":EDITOR_PATH";
   /** Code de la dernière erreur levée */
   public static final String                  ENV_ERROR_CMD = "_ERROR_CMD";
   /** Message de la dernière erreur levée */
   public static final String                  ENV_ERROR_MSG = "_ERROR_MSG";
   /** Pile lors de la dernière erreur levée */
   public static final String                  ENV_ERROR_STK = "_ERROR_STK";
   /** Identifiant unique de session */
   public static final String                  ENV_SESSION_ID = "_SESSION_ID";
   /** Moment de connexion */
   public static final String                  ENV_LOGIN_MS = "_LOGIN_MS";
   /** Répertoire courant au lancement */
   public static final String                  ENV_WORK_DIR = "_WORKING_DIR";
   /** Autorisation de masquage de commande par les alias */
   public static final String                  ENV_ALLOW_RED = ":ALLOW_REDEF";
   /** Politique de signature de code : 0: pas de vérif, 1: pas de confiance, 2: vérif + confiance */
   public static final String                 ENV_SIGN_POLICY = ":SIGN_POLICY";
   /** Clef privée de signature */
   public static final String                 ENV_SIGN_KEY = ":SIGN_KEY";
   /** Clef privée de signature */
   public static final String                 ENV_SIGN_TRUST = ":SIGN_TRUST";

   /** Variable de Lecteur (locale) pour EVAL -c (attrape erreurs) */
   public static final String                  LEC_CATCH_CODE = "_CATCH_CODE",
                                               LEC_CATCH_LCMD = "_CATCH_LCMD";

   /** Variables de Lecteur pour FOR et WHILE */
   public static final String                  LEC_LOOP_DEEP = "_LOOP_DEEP",
                                               LEC_LOOP_BREAK = "_LOOP_BREAK";

  /** Autres variables de Lecteur spéciales */
   public static final String
          LEC_THIS = "this_scope",
          LEC_SUPER = "super_scope",
          LEC_DEEP = "scope_deep",
          LEC_SCR_NAME = "script_name",
          LEC_SCR_PATH = "script_path",
          LEC_SCR_ARGS = "script_args",
          LEC_SCR_ARG_NB = "script_arg_nb",
          LEC_SCR_ARG_LS = "script_arg_ls",
          LEC_SCR_ARG = "script_arg",
          LEC_VAR_NAME = "macro_name",
          LEC_VAR_ARG_NB = "arg_nb",
          LEC_VAR_ARG_LS = "arg_ls",
          LEC_VAR_ARG = "arg";

   // Cas particulier : mise en System.setProperty
   /** Variable d'environnement : chemin du dossier des logs d'erreur  */
   public static final String                  PROP_LOG_DIR     = "lunasql.log.dir";

   // *** Variables de classe globales ***
   private final Coquille                      contwrap;
   private Valeur                              cmdvalue;
   private final HashMap<String, Instruction>  commands;
   private PrintStream                         writer;
   private PrintStream                         errwriter;
   private String                              wrname;
   private String                              errwrname;
   private SQLCnx                              sqlc;
   private final HashMap<String, String>       environ;     // liste des variables/macros
   private final HashMap<String, String>       varhelp;     // aides des variables/macros
   private final HashSet<String>               varcirc;     // variables exemptes de controle de ref. circ.
   private final HashMap<String, String>       tmpvars;     // var. $_n, $_l, $1, $2...
   private final HashMap<String, Class<Instruction>> plugins;     // liste des plugins chargés
   private final HashMap<Integer, EvalEngine>  engines;     // liste des engines chargés
   private ScriptContext                       enginejscnt; // contexte pour js
   private ScriptEngine                        evaleng;     // engine courant
   private Lecteur                             lecteur;     // lecteur courant
   private final Stack<Boolean>                whentree;    // arbre des WHEN
   private Console                             console;
   private ConsoleReader                       reader;
   private History                             histo;
   //private FileHistory                         histo;
   private int                                 idwhen;
   private int                                 quitStat;
   private long                                chron;
   private boolean                             onerrex;
   private int                                 submode;
   private final HashMap<Integer, FileReader>  filereads;
   private final HashMap<Integer, FileWriter>  filewrites;
   //private HashMap<Integer, Properties>         vardict; // recueil des var. dict
   private boolean                             finish; // roue qui tourne terminée
   private boolean                             httpMode; // mode HTTP (restraint)

   /**
    * Constructeur Contexte
    * @param color la couleur
    * @throws IOException si erreur IO
    */
   public Contexte(boolean color) throws IOException {
      this.idwhen = -1;
      this.submode = 0;
      this.writer = null;
      this.errwriter = null;
      this.wrname = null;
      this.errwrname = null;
      this.onerrex = false; // si :ON_ERROR a déjà été exécuté
      this.httpMode  = false;

      // Environnement et plugins
      this.environ = new HashMap<>(128);
      this.varhelp = new HashMap<>(64);
      this.varcirc = new HashSet<>(64);
      this.tmpvars = new HashMap<>(16);
      //this.vardict = new HashMap<>(16);
      this.filereads = new HashMap<>();
      this.filewrites = new HashMap<>();
      this.engines = EvalEngine.createAllEngines();//new

      this.plugins = new HashMap<>();
      this.whentree = new Stack<>();

      // Chargement des variables d'environnement internes
      Date d = new Date();
      this.environ.put(ENV_WORK_DIR, new File(".").getCanonicalPath());
      this.environ.put(ENV_DAY_DATE, new SimpleDateFormat("dd/MM/yyyy").format(d));
      this.environ.put(ENV_DAY_DATE_F, new SimpleDateFormat("yyyyMMdd").format(d));
      this.environ.put(ENV_CONST_EDIT, Integer.toString(Config.CT_CONST_EDIT));
      this.environ.put(ENV_SQL_UPDATE, Integer.toString(Config.CT_SQL_UPDATE));
      this.environ.put(ENV_EVAL_ENG, Config.CT_EVAL_ENGINE);
      this.environ.put(ENV_VERSION, Config.APP_VERSION_NUM);
      this.environ.put(ENV_VERS_NAME, Config.APP_VERSION_NAME);
      this.environ.put(ENV_CMD_STATE, STATE_FALSE);
      this.environ.put(ENV_EXIT_ERR, Config.CT_EXIT_ON_ERROR ? STATE_TRUE : STATE_FALSE);
      this.environ.put(ENV_VERBOSE, VERBLIB[Config.CT_VERBOSE]);
      this.environ.put(ENV_NVERBOSE, Integer.toString(Config.CT_VERBOSE));
      this.environ.put(ENV_SCR_PATH, Config.CT_SCRIPTS_PATH);
      this.environ.put(ENV_PLG_PATH, Config.CT_PLUGINS_PATH);
      this.environ.put(ENV_INIT_FILE, Config.CT_INIT_FILE);
      this.environ.put(ENV_BASES_FILE, Config.CT_BASES_FILE);
      this.environ.put(ENV_HIST_FILE, Config.CS_HISTORY_FILE);
      this.environ.put(ENV_HIST_DBL, Config.CS_HISTORY_DBL);
      this.environ.put(ENV_CONF_FILE, Config.CT_CONFIG_FILE);
      this.environ.put(ENV_EDIT_PATH, "");
      this.environ.put(ENV_SAVE_CONF, Config.CT_SAVE_CONF ? STATE_TRUE : STATE_FALSE);
      this.environ.put(ENV_PROMPT, Config.CS_PROMPT);
      this.environ.put(ENV_BEEP_ON, Config.CS_BEEP_ON ? STATE_TRUE : STATE_FALSE);
      this.environ.put(ENV_COLORS_ON, Config.CS_COLOR_ON && color ? STATE_TRUE : STATE_FALSE);
      this.environ.put(ENV_ALIAS_ARG, Config.CT_ALIAS_ARG ? STATE_TRUE : STATE_FALSE);
      this.environ.put(ENV_WHEN_DEEP, STATE_FALSE);
      this.environ.put(ENV_RET_VALUE, "0");
      this.environ.put(ENV_RET_NLINES, "0");
      this.environ.put(ENV_INTERACT, Config.CT_INTERACT ? STATE_TRUE : STATE_FALSE);
      this.environ.put(ENV_FILE_ENC, System.getProperty("file.encoding"));
      this.environ.put(ENV_ALLOW_REC, Config.CT_ALLOW_RECUR ? STATE_TRUE : STATE_FALSE);
      this.environ.put(ENV_ALLOW_RED, Config.CT_ALLOW_REDEF ? STATE_TRUE : STATE_FALSE);
      this.environ.put(ENV_END_CMD_NL, Config.CT_END_CMD_NL ? STATE_TRUE : STATE_FALSE);
      this.environ.put(ENV_COL_MAX_WTH, Integer.toString(Config.CT_COL_MAX_WIDTH));
      this.environ.put(ENV_ADD_ROW_NB, Config.CT_ADD_ROW_NB ? STATE_TRUE : STATE_FALSE);
      this.environ.put(ENV_ROW_MAX_NB, "0");
      this.environ.put(ENV_SELECT_ARR, Config.CT_SELECT_ARR ? STATE_TRUE : STATE_FALSE);
      this.environ.put(ENV_LIST_SUBST, Config.CT_LIST_SUBST ? STATE_TRUE : STATE_FALSE);
      this.environ.put(ENV_CMD_BUFFER, "");
      this.environ.put(ENV_EXEC_TIME, "0");
      this.environ.put(ENV_LOADED_LIBS, "");
      this.environ.put(ENV_ON_ERROR, "");
      this.environ.put(ENV_ON_INIT, "");
      this.environ.put(ENV_ON_QUIT, "");
      this.environ.put(ENV_ERROR_CMD, "");
      this.environ.put(ENV_ERROR_MSG, "");
      this.environ.put(ENV_ERROR_STK, "");
      this.environ.put(ENV_SIGN_POLICY, Integer.toString(Config.CT_SIGN_POLICY));
      this.environ.put(ENV_SIGN_KEY, "");
      this.environ.put(ENV_SIGN_TRUST, "");

      // Cas particulier : mise en System.setProperty
      System.setProperty(PROP_LOG_DIR, Config.CT_LOG_DIR);

      // Commandes
      this.commands = new HashMap<>(32);
      // Commandes spéciales
      addCommand(new CmdErr(this));
      addCommand(new CmdErrSyn(this));
      addCommand(new CmdErrInc(this));
      addCommand(new CmdError(this));
      addCommand(new CmdVoid(this));
      // Commandes SQL
      addCommand(new CmdSelect(this));
      addCommand(new CmdUpdate(this));
      addCommand(new CmdInsert(this));
      addCommand(new CmdDelete(this));
      addCommand(new CmdCreate(this));
      addCommand(new CmdAlter(this));
      addCommand(new CmdDrop(this));
      addCommand(new CmdCall(this));
      addCommand(new CmdCommit(this));
      addCommand(new CmdRollback(this));
      addCommand(new CmdGrant(this));
      addCommand(new CmdRevoke(this));
      addCommand(new CmdSet(this));
      addCommand(new CmdTruncate(this));
      addCommand(new CmdComment(this));
      addCommand(new CmdMerge(this));
      addCommand(new CmdExplain(this));
      // Commandes internes
      addCommand(new CmdStr(this));
      addCommand(new CmdFile(this));
      addCommand(new CmdList(this));
      addCommand(new CmdDict(this));
      addCommand(new CmdTime(this));
      addCommand(new CmdArg(this));
      addCommand(new CmdWhen(this));
      addCommand(new CmdIf(this));
      addCommand(new CmdWhile(this));
      addCommand(new CmdFor(this));
      addCommand(new CmdElse(this));
      addCommand(new CmdEnd(this));
      addCommand(new CmdCase(this));
      addCommand(new CmdExit(this));
      addCommand(new CmdReturn(this));
      addCommand(new CmdBreak(this));
      addCommand(new CmdNext(this));
      addCommand(new CmdPut(this));
      addCommand(new CmdAppend(this));
      addCommand(new CmdQuit(this));
      addCommand(new CmdPrint(this));
      addCommand(new CmdInput(this));
      addCommand(new CmdDisp(this));
      addCommand(new CmdSize(this));
      addCommand(new CmdDef(this));
      addCommand(new CmdUndef(this));
      addCommand(new CmdOpt(this));
      addCommand(new CmdLet(this));
      addCommand(new CmdEval(this));
      addCommand(new CmdSeek(this));
      addCommand(new CmdExec(this));
      addCommand(new CmdStart(this));
      addCommand(new CmdExport(this));
      addCommand(new CmdImport(this));
      addCommand(new CmdNeed(this));
      addCommand(new CmdUse(this));
      addCommand(new CmdVerb(this));
      addCommand(new CmdShow(this));
      addCommand(new CmdTree(this));
      addCommand(new CmdBuffer(this));
      addCommand(new CmdHist(this));
      addCommand(new CmdSpec(this));
      addCommand(new CmdSpool(this));
      addCommand(new CmdRand(this));
      addCommand(new CmdEngine(this));
      addCommand(new CmdPlugin(this));
      addCommand(new CmdConfig(this));
      addCommand(new CmdShell(this));
      addCommand(new CmdWait(this));
      addCommand(new CmdView(this));
      addCommand(new CmdEdit(this));
      addCommand(new CmdInfo(this));
      addCommand(new CmdHelp(this));
      addCommand(new CmdNum(this));
      addCommand(new CmdAlias(this));

      // Moteur dévaluation par défaut
      this.contwrap = new Coquille(this);
      setEvalEngineByName(Config.CT_EVAL_ENGINE);

      // Lecture du fichier ressources de fonctions Javascript
      try {
         ScriptEngine se = getEvalEngine("javascript");
         if (se != null) se.eval(new InputStreamReader(getClass().getResourceAsStream("/lunasql/misc/init-se.js")));
      }
      catch (ScriptException ex) {
         if (getVerbose() >= Contexte.VERB_AFF)
            errprintln("\nErreur lors du chargement du script utilitaire JS : " + ex.getMessage());
      }

      // Lecture du fichier ressources de fonctions LunaSQL
      try {
         new Lecteur(this, new InputStreamReader
               (getClass().getResourceAsStream("/lunasql/misc/init-ctx.sql"), StandardCharsets.UTF_8));
      }
      catch (IOException ex) {
         if (getVerbose() >= Contexte.VERB_AFF)
            errprintln("\nErreur lors du chargement du script utilitaire SQL : " + ex.getMessage());
      }
   }// Contexte()

   public Contexte() throws IOException {
      this(true);
   }

   /**
    * Fixe la connexion SQL
    */
   public final void setSQLCnx(SQLCnx sqlc) throws IllegalArgumentException {
      if (sqlc == null) throw new IllegalArgumentException("Connexion SQL nulle");
      this.sqlc = sqlc;
      // Complètement de l'environnement
      String ms = Long.toString(System.currentTimeMillis()), type = TypesSGBD.getSTypeFromArg(sqlc.getType());
      int cid = (int)(Math.random() * 100000000);
      File base = new File(sqlc.getBase());
      this.environ.put(ENV_DB_TYPE, type == null ? "unknown" : type);
      this.environ.put(ENV_DB_NTYPE, Integer.toString(sqlc.getType()));
      this.environ.put(ENV_CNX_DRIVER, sqlc.getDriver());
      this.environ.put(ENV_CNX_PATH, sqlc.getPath());
      this.environ.put(ENV_CNX_BASE, base.getAbsolutePath());
      this.environ.put(ENV_CNX_LOGIN, sqlc.getLogin());
      this.environ.put(ENV_LOGIN_MS, ms);
      this.environ.put(ENV_SESSION_ID, base.getName().replace(' ', '-') + "-" +
              sqlc.getLogin().replace(' ', '-') + "-" + ms + "-" + cid);
   }

   /**
    * Teste s'il y a une connexion SQL
    *
    * @return sqlc != null
    */
   public final boolean hasSQLCnx() {
      return this.sqlc != null;
   }

   /**
    * Retourne une instruction correspondant à la commande spécifiée
    *
    * @param name le nom de la commande de Lecteur
    * @return l'object Instruction correspondant pour exécution
    */
   public Instruction getCommand(String name) { // 3.7
      return this.commands.get(name);
   }

   /**
    * Exécution de la commande lue par Lecteur.<br>
    * Note : le nom de commande n'est pas forcément le premier élément de la liste <tt>lcmd</tt>
    *
    * @param name le nom de la commande
    * @param lcmd la liste correspondant à la commande complète
    * @param nolg le numéro de la ligne
    * @return le code de retour de la commande (continue ou arrêt)
    */
   public int executeCmd(String name, ArrayList<String> lcmd, int nolg) {
      Instruction cmd;
      int verb = getVerbose();
      boolean okexec = canExec(); // WHEN 0 ?

      char c0 = name.length() > 1 ? name.charAt(0) : '\0'; // ne pas couvrir le raccourci de commandes

      // Recherche d'alias
      if (c0 == ':' || c0 == '*') { // ':' force la résolution en tant qu'alias / '*' allow ref. circulaire
         cmd = getCommand("ALIAS");
      }
      else if (c0 == '=') { // '=' force la résolution de commande
         cmd = getCommand(name.substring(1).toUpperCase());
      }
      else if (isLec(name) || isSet(name)) { // c'est une macro (ou un alias si :ALIAS_ARG)
         Instruction cmd2;
         if ((cmd2 = getCommand(name.toUpperCase())) != null) { // commande redéfinie
            if (!getVar(ENV_ALLOW_RED).equals(STATE_TRUE)) { // mais pas autorisé
               errprintln("Note : masquage de commande non autorisé, l'alias est ignoré"
                     + "\n(positionnez " + Contexte.ENV_ALLOW_RED + " à " + Contexte.STATE_TRUE
                     + " pour jouer avec la redéfinition)");
               cmd = cmd2;
            }
            else { // ok, autorisé
               if (getVerbose() >= VERB_BVR)
                  println("Note : alias '" + name + "' masquant la commande du même nom");
               cmd = getCommand("ALIAS");
            }
         }
         else cmd = getCommand("ALIAS");
      }
      // Recherche de commande interne
      else cmd = getCommand(name.toUpperCase());

      // Autres commandes : éval SE ou erreurs
      if (cmd == null) {
        if (!okexec) return RET_CONTINUE; // Pas d'éval si mode WHEN 0
        // Recherche d'expression SE
        Object r;
        StringBuilder ss = new StringBuilder();
        for (String s : lcmd) ss.append(s).append(' ');
        try {
           r = evaluerExpr(ss.toString());
           lcmd = new ArrayList<>();
           lcmd.add(r == null ? "null" : r.toString());
           cmd = getCommand("NUM");
        }
        catch (ScriptException ex) {
           cmd = getCommand("ERRINC");
           ((CmdErr)cmd).setMessage(ex.getMessage());
        }
        catch (Exception ex) {
           cmd = getCommand("ERRSYN");
           ((CmdErr)cmd).setMessage(ex.getMessage());
        }
     }

     // Si aucune commande trouvée, on affecte une erreur de syntaxe
     if (cmd == null) cmd = getCommand("ERRSYN");

     // Préparation de la commande et exécution
     cmd.setCommand(lcmd);
     cmd.setNoLine(nolg);
     clearTmpVars();
     int l = cmd.getLength();
     setTmpVar("_l", cmd.getSCommand(1));
     setTmpVar("_n", Integer.toString(l - 1));
     for (int i = 0; i < l; i++) setTmpVar(Integer.toString(i), cmd.getArg(i));

     // Commandes en erreur et sortie sur WHEN 0 pour commandes hors WHEN
     if (cmd.getType() == Instruction.TYPE_ERR) return cmd.execute();
     if (!okexec) {
        if (cmd.getType() == Instruction.TYPE_MOTC_WH) return cmd.execute();
        return RET_CONTINUE;
     }

     // Affichage et exécution des commandes normales
     if (verb >= VERB_BVR) println(cmd.getSCommand());
     int r = cmd.execute();
     // Valeur retournée si non nulle
     if (!isSubMode() && cmdvalue != null) {
        int color;
        for (int i = VERB_AFF; i <= VERB_BVR; i++) {
           String disp = cmdvalue.getDispValue(i);
           if (verb >= i && disp != null && !disp.isEmpty()) {
              if ((color = cmdvalue.getColor()) >= 0) printlnX(disp, color);
              else println(disp);
           }
        }
     }
     return getVar(ENV_CMD_STATE).equals(STATE_ERROR) && getVar(ENV_EXIT_ERR).equals("1") ?
           Math.max(r, RET_EV_CATCH) : r;
   }

   /**
    * Exécution d'une commande donnée en interne (num de ligne = 1)
    *
    * @param lcmd la liste correspondant à la commande complète
    * @return le code de retour de la commande (continue ou arrêt)
    */
   public int executeCmd(String... lcmd) {
      if (lcmd == null || lcmd.length == 0) throw new IllegalArgumentException("Commande nulle ou vide");

      return executeCmd(lcmd[0], new ArrayList<>(Arrays.asList(lcmd)), 1);
   }

   /**
    * Chargement du fichier init par défaut. Une connexion SQL doit être active Une commande QUIT
    * dans ce fichier interrompt seulement l'exécuton
    * Ne doit pas être appelé dans le constructeur car dépend de l'environnement
    */
   public final void loadInitFile() {
      if (this.sqlc != null) {
         String initf = getVar(ENV_INIT_FILE);
         File f = new File(initf);
         if (f.isFile()) { // existe + est fichier
            if (getVerbose() >= VERB_BVR)
               System.out.println("Lecture du script d'initialisation '" + f.getName() + "'");
            Lecteur lec = new Lecteur(this, "EXEC \"" + f.getAbsolutePath() + "\";");
            if (lec.getCmdState() == RET_SHUTDOWN ||
                  execOnInitQuit(true) == RET_SHUTDOWN) { // traitement spécifique de QUIT
               if (fermerConnex()) System.exit(getQuitStat());
            }
         }
      }
   }

   /**
    * Exécution du contenu d'une variable init ou quit
    *
    * @param ini true :ON_INIT ou false :ON_QUIT
    * @return l'état du lecteur
    */
   private int execOnInitQuit(boolean ini) {
      String var = ini ? ENV_ON_INIT : ENV_ON_QUIT, code = getVar(var);
      if (!code.isEmpty()) {
         if (getVerbose() >= VERB_BVR) System.out.println("Lecture de la variable " + var);
         Lecteur lec = new Lecteur(this);
         lec.setLecVar(Contexte.LEC_THIS, var);
         lec.setLecVar("cancel", STATE_FALSE);
         lec.add(code);
         String cl = lec.getLecVar("cancel");
         lec.fin();

         if (ini) return STATE_TRUE.equals(cl) ? RET_SHUTDOWN : RET_CONTINUE;
         return STATE_TRUE.equals(cl) ? RET_CONTINUE : RET_SHUTDOWN;
      }
      return ini ? RET_CONTINUE : RET_SHUTDOWN;
   }

   /**
    * Chargement du fichier de configuration par défaut.
    * Ne doit pas être appelé dans le constructeur car dépend de l'environnement    *
    * @return la taille chargée
    */
   public final int loadConfigFile() {
      return loadConfigFile(false);
   }

   /**
    * Chargement du fichier de configuration par défaut.
    * Ne doit pas être appelé dans le constructeur car dépend de l'environnement
    * @param sysonly si l'on doit charger uniquement les var. systèmes
    * @return la taille chargée
    */
   public final int loadConfigFile(boolean sysonly) {
      File fcfg = new File(getVar(ENV_CONF_FILE));
      if (!fcfg.isFile() || !fcfg.canRead()) return 0;

      try {
         Properties prop = new Properties();
         FileReader rd = new FileReader(fcfg);
         prop.load(rd);
         for (Map.Entry<Object, Object> kv : prop.entrySet()) {
            String key = (String)kv.getKey(), val = (String)kv.getValue();
            if (key.startsWith("?")) registerPlugin(fcfg, val);
            else if (isSysUser(key) || (!sysonly && valideKey(key))) this.environ.put(key, val);
            else if (getVerbose() >= Contexte.VERB_AFF)
               errprintln(" * " + fcfg.getName() + " : Affectation invalide : " + kv.toString());
         }
         rd.close();
         return prop.size();
      }
      catch (IOException ex) {
         if (getVerbose() >= Contexte.VERB_AFF)
            errprintln(fcfg.getName() + " : IOException : " + ex.getMessage());
         return 0;
      }
   }

   /**
    * Sauvegarde de la configuration en fichier par défaut
    *
    * @param fcfg le nom du fichier de sortie
    * @return le nombre de variables écrites
    * @throws java.io.IOException si erreur IO
    */
   public final int dumpConfigFile(File fcfg) throws IOException {
      return dumpConfigFile(fcfg, false);
   }

   /**
    * Sauvegarde de la configuration en fichier par défaut
    *
    * @param fcfg le nom du fichier de sortie
    * @param sysonly si l'on doit sauver uniquement les var. systèmes
    * @return le nombre de variables écrites
    * @throws java.io.IOException si erreur IO
    */
   public final int dumpConfigFile(File fcfg, boolean sysonly) throws IOException {
      if (fcfg == null) {
         System.err.println("Erreur : fichier de sauvegarde de la config. nul");
         return 0;
      }

      StringBuilder comm = new StringBuilder();
      comm.append("#\n# Fichier de configuration de la console LunaSQL\n");
      comm.append("#\n# Déclaration de variables et modification de constantes de paramétrage\n");
      comm.append("# À noter :\n");
      comm.append("#  1) les options non déclarées ici posséderont leur valeur par défaut\n");
      comm.append("#  2) la définition se fait aussi en fichier 'init.sql' par DEF et OPT\n#\n");
      comm.append("# LunaSQL version : ").append(Config.APP_VERSION_NUM).append("\n\n");
      comm.append("# Options de paramétrage : \\:OPTION = valeur\n");
      comm.append("# Variables/fonctions utilisateur : identifiant = valeur\n");
      comm.append("# Greffons Java compilés : ?plugin = nom.complet.de.la.classe\n#");

      Writer wr = new BufferedWriter(new FileWriter(fcfg));
      Properties prop = new Properties();
      SortedSet<String> sort = new TreeSet<>(getAllVars().keySet());
      Iterator<String> iter = sort.iterator();
      while (iter.hasNext()) {
         String key = iter.next();
         if (isSysUser(key) || (!sysonly && isNonSys(key))) prop.put(key, getGlbVar(key));
      }
      Set<Map.Entry<String, Class<Instruction>>> lplug = getAllPlugins();
      for (Map.Entry<String, Class<Instruction>> entry : lplug) {
         prop.put("?" + entry.getKey(), entry.getValue().getCanonicalName());
      }
      prop.store(wr, comm.toString());
      wr.close();
      return prop.size();
   }

   /**
    * Sauvegarde de la configuration en fichier par défaut
    *
    * @return le nombre de variables écrites
    */
   public final int dumpConfigFile(){
      try {
         return dumpConfigFile(new File(getVar(ENV_CONF_FILE)), false);
      } catch (IOException ex) {
         System.err.println("Sauvegarde config. : IOException : " + ex.getMessage());
         return 0;
      }
   }

   /**
    * Mode HTTP
    *
    * @return le mode HTTP
    */
   public final boolean isHttpMode() {
      return this.httpMode;
   }
   public final void setHttpMode(boolean mode) {
      this.httpMode = mode;
      setVar(ENV_COLORS_ON, STATE_FALSE);
   }

   /**
    * Évaluation en mode substitution
    */
   public void addSubMode() {
      submode++;
   }
   public void remSubMode() {
      submode--;
   }

   /**
    * Retourne l'état du mode substitution
    *
    * @return le mode sub
    */
   public boolean isSubMode() {
      return submode > 0;
   }

   /**
    * Retourne la Valeur d'exécution de commande courante
    *
    * @return la valeur
    */
   public Valeur getValeur() {
      return cmdvalue;
   }

   /**
    * Fixe la Valeur d'exécution de commande courante
    *
    * @param val la valeur
    */
   public void setValeur(Valeur val) {
      this.cmdvalue = val;
   }

   /**
    * Retourne le niveau courant de verbose
    *
    * @return int le niveau
    */
   public final int getVerbose() {
      int vb;
      try {
         vb = Integer.parseInt(getVar(ENV_NVERBOSE));
      } catch (NumberFormatException e) {
         vb = Config.CT_VERBOSE;
      }
      return vb;
   }

   /**
    * Modification de l'état de verbose
    *
    * @param verb l'état de verbose, en numérique ou nominatif parmi les valeurs autorisées
    * @return le nom du nouveau verbose, ou null si ça a raté
    */
   public String setVerbose(String verb) {
      if (verb == null) return null;
      int val = -1;
      String sval = null;
      try {
         if (verb.startsWith("+")) {
            val = getVerbose() + Integer.parseInt(verb.substring(1));
         } else if (verb.startsWith("-")) {
            val = getVerbose() - Integer.parseInt(verb.substring(1));
         } else {
            val = Integer.parseInt(verb);
         }

         if (val >= VERB_SIL && val <= VERB_DBG) sval = Contexte.VERBLIB[val];
         else return null;
      }
      catch (NumberFormatException ex) {
         // Test de reconnaissance du libellé
         for (int i = 0; i < Contexte.VERBLIB.length; i++) {
            if (Contexte.VERBLIB[i].equalsIgnoreCase(verb)) {
               val = i;
               sval = Contexte.VERBLIB[i];
            }
         }// for
         if (val == -1) return null;
      }// catch
       // Enregistrement de la valeur de verbose
      setVar(ENV_VERBOSE, sval);
      setVar(ENV_NVERBOSE, Integer.toString(val));
      return sval;
   }

   /**
    * Affiche un message sur la sortie standard sans fin de ligne
    *
    * @param txt le texte à afficher
    */
   public void print(String txt) {
      getWriterOrOut().print(txt);
   }


   /**
    * Affiche sans niveau de verbose une ligne vide
    */
   public void println() {
      getWriterOrOut().println();
   }

   /**
    * Affiche sans niveau de verbose un message sur la sortie standard
    *
    * @param txt le texte à afficher
    */
   public void println(String txt) { getWriterOrOut().println(txt); }

   /**
    * Affiche un message sur la sortie standard en couleur si couleurs autorisées
    *
    * @param txt le texte à afficher
    * @param color la couleur : int de 0 à 5
    */
   public void printlnX(String txt, int color) {
      printlnX(txt, color, true);
   }

   /**
    * Affiche un message sur la sortie standard en couleur si couleurs autorisées
    *
    * @param txt le texte à afficher
    * @param color la couleur : int de 0 à 15
    * @param nl si l'on affiche un saut de ligne final
    */
   public void printlnX(String txt, int color, boolean nl){
      if (color < 0 || color >= COLORS.length) throw new IllegalArgumentException("Couleur non définie " + color);
      print(getVar(ENV_COLORS_ON).equals(STATE_TRUE) ? COLORS[color] + txt + COLORS[0] : txt);
      if (nl) println();
   }

   /**
    * Affiche un message sur la sortie ERR, à condition que verbose >= AFFICHAGE
    * Si les couleurs sont acceptées, le message est en rouge
    *
    * @param txt le texte à afficher
    */
   public void errprintln(String txt) {
      errprintln(txt, true);
   }

   /**
    * Affiche un message sur la sortie ERR, à condition que verbose >= AFFICHAGE
    * Si la couleur est autorisée, le message est en rouge
    *
    * @param txt le texte à afficher
    * @param nl si l'on affiche un saut de ligne final
    */
   public void errprintln(String txt, boolean nl) {
      if (this.errwriter == null && this.writer == null) {
         System.err.print (getVar(ENV_COLORS_ON).equals(STATE_TRUE) ? COLORS[BR_RED] + txt + COLORS[0] : txt);
         if (nl) System.err.println();
      }
      else {
         PrintStream pr = this.errwriter == null ? this.writer : this.errwriter; // copie de la référence
         pr.print(txt);
         if (nl) pr.println();
      }
   }

   /**
    * Pile d'exécution (appel de macros ou fichiers)
    * 
    * @return une chaîne représentant la pile
    */
   public String getCallStack() {
      StringBuilder sb = new StringBuilder();
      Lecteur l = getCurrentLecteur();
      while (l != null) {
         String lnom = l.getLecVar(Contexte.LEC_THIS),
                lsup = l.getLecVar(Contexte.LEC_SUPER);
         if (lnom != null && lnom.length() > 0) {
            sb.append(lnom);
            if (lsup != null && lsup.length() > 0) sb.append(':').append(lsup);
            sb.append(" < ");
         }
         l = l.getFather();
      }
      if (sb.length() > 0) sb.delete(sb.length() - 3, sb.length());
      return sb.toString();
   }

   /**
    * Return la liste de commandes chargées
    *
    * @return hashmap
    */
   public HashMap<String, Instruction> getAllCommands() {
      return this.commands;
   }

   /**
    * Return la liste de commandes chargées (en un seul exemplaire s'il te plaît)
    *
    * @return hashmap
    */
   public TreeMap<String, Instruction> getAllCommandsTree() {
      TreeMap<String, Instruction> tree = new TreeMap<>();
      for (String key : this.commands.keySet()) {
         Instruction ins = this.commands.get(key);
         if (!tree.containsKey(ins.getName())) tree.put(ins.getName(),ins);
      }
      return tree;
   }

   /**
    * Return le writer pour la classe Log ou bien, si null, la sortie standard
    *
    * @return writer
    */
   public PrintStream getWriterOrOut(){
      return this.writer == null ? System.out : this.writer;
   }

   /**
    * Return le writer pour la classe Log
    *
    * @return writer
    */
   public PrintStream getWriter() {
      return this.writer;
   }

   /**
    * Return le writer des erreurs pour la classe Log
    *
    * @return writer
    */
   public PrintStream getErrWriter() {
      return this.errwriter;
   }

   /**
    * Return le nom du fichier du writer pour la classe Log
    *
    * @return writer name
    */
   public String getWriterName() {
      return this.wrname;
   }

   /**
    * Return le nom du fichier du writer des erreurs pour la classe Log
    *
    * @return writer name
    */
   public String getErrWriterName() {
      return this.errwrname;
   }

   /**
    * Fixe le writer
    *
    * @param writer le nouveau writer
    */
   public void setWriter(PrintStream writer) {
      this.writer = writer;
   }

   /**
    * Fixe le writer pour les erreurs
    *
    * @param errwriter le nouveau writer pour les erreurs
    */
   public void setErrWriter(PrintStream errwriter) {
      this.errwriter = errwriter;
   }

   /**
    * Fixe le writer avec le nom du fichier correspondant
    *
    * @param writer le nouveau writer
    * @param wname le nom du fichier du nouveau writer
    */
   public void setWriter(PrintStream writer, String wname) {
      this.writer = writer;
      this.wrname = wname;
   }

   /**
    * Fixe le writer des erreurs avec le nom du fichier correspondant
    *
    * @param errwriter le nouveau writer pour les erreurs
    * @param errwname le nom du nouveau writer
    */
   public void setErrWriter(PrintStream errwriter, String errwname) {
      this.errwriter = errwriter;
      this.errwrname = errwname;
   }

   /**
    * Return l'objet ScriptEngine d'évaluation
    *
    * @return ScriptEngine
    */
   public ScriptEngine getEvalEngine() {
      return this.evaleng;
   }

   /**
    * Return la liste des noms de ScriptEngine d'évaluation instanciés
    *
    * @return ScriptEngineFactory
    */
   public HashMap<Integer, EvalEngine> getAllEngines() {
      return this.engines;
   }

   /**
    * Return l'objet ScriptEngine d'évaluation par id
    *
    * @param id le numéro
    * @return ScriptEngine
    */
   public ScriptEngine getEvalEngine(int id) {
      EvalEngine ee = this.engines.get(id);
      return ee == null ? null : ee.getEngine();
   }

   /**
    * Return l'objet ScriptEngine d'évaluation par nom
    *
    * @param name le nom
    * @return ScriptEngine
    */
   public ScriptEngine getEvalEngine(String name) {
      for (Map.Entry<Integer, EvalEngine> entry : this.engines.entrySet()) {
         EvalEngine ee = entry.getValue();
         if (ee.getNames().contains(name)) return ee.getEngine();
      }
      return null;
   }

   /**
    * Return le nom du ScriptEngine d'évaluation
    *
    * @return ScriptEngine
    */
   public String getEvalEngineName() {
      return this.evaleng == null ? null : this.evaleng.getFactory().getEngineName();
   }

   /**
    * Return l'extension du ScriptEngine d'évaluation
    *
    * @return ScriptEngine
    */
   public String getEvalEngineExtens() {
      return this.evaleng == null ? null : this.evaleng.getFactory().getExtensions().get(0);
   }

   /**
    * Return le nom du ScriptEngine d'évaluation
    *
    * @return ScriptEngine
    */
   public String getEvalEngineAlias() {
      return this.evaleng == null ? null : this.evaleng.getFactory().getNames().get(0);
   }

   /**
    * Fixe l'objet ScriptEngine d'évaluation
    *
    * @param name le nom de l'engine
    */
   private void setEvalEngineByName(String name) {
      setEvalEngine(getEvalEngine(name));
   }

   /**
    * Fixe l'objet ScriptEngine d'évaluation
    *
    * @param se le nouvel engine
    */
   public void setEvalEngine(ScriptEngine se) {
      this.evaleng = se;
      if (this.evaleng == null)
         errprintln(Config.CT_EVAL_ENGINE + " : ScriptEngine inconnu");
      else {
         if ("js".equals(getEvalEngineExtens())) {
            if (this.enginejscnt == null) this.enginejscnt = this.evaleng.getContext();
            else this.evaleng.setContext(this.enginejscnt);
         }
         this.evaleng.put("lunasql", this.contwrap);
         this.evaleng.put("engine",  this.evaleng);
      }
      setVar(ENV_EVAL_ENG, getEvalEngineName());
   }

   /**
    * Attribue un lecteur courant au contexte d'exécution
    *
    * @param lec le lecteur nouveau
    */
   public void setCurrentLecteur(Lecteur lec) {
      this.lecteur = lec;
   }

   /**
    * Retourne le lecteur courant
    *
    * @return Lecteur
    */
   public Lecteur getCurrentLecteur() {
      return this.lecteur;
   }

   /**
    * Return l'objet Console
    *
    * @return Console
    */
   public Console getConsole() {
      return this.console;
   }

   /**
    * Fixe le Console (facultatif)
    *
    * @param console la nouvelle console
    */
   public void setConsole(Console console) {
      this.console = console;
   }

   /**
    * Return l'objet ConsoleReader
    *
    * @return ConsoleReader
    */
   public ConsoleReader getConsoleReader() {
      return this.reader;
   }

   /**
    * Fixe le ConsoleReader (facultatif)
    *
    * @param console la nouvelle console reader
    */
   public void setConsoleReader(ConsoleReader console) {
      this.reader = console;
   }

   /**
    * Return l'objet History
    *
    * @return History
    */
   //public FileHistory getHistory() {
   public History getHistory() {
      return this.histo;
   }

   /**
    * Emet un biip bien énervant
    *
    * @throws IOException si erreur IO
    */
   public void playBeep() throws IOException {
      if (reader != null) this.reader.beep();
   }

   /**
    * Efface le contenu de la console (semble de pas marcher terrible)
    *
    * @throws IOException si erreur IO
    */
   public void clearConsole() throws IOException {
      if (reader != null) this.reader.clearScreen();
   }

   /**
    * Fixe le History
    *
    * @param histo le nouvel histo
    */
   public void setHistory(History histo) {
   //public void setHistory(FileHistory histo) {
      this.histo = histo;
   }

   /**
    * Return le plugin répondant au nom fourni
    *
    * @param pname le nom du plugin
    * @return class
    */
   public Class<Instruction> getPlugin(String pname) {
      return this.plugins.get(pname);
   }

   /**
    * Return le plugin répondant au nom fourni
    *
    * @param pname le nom du plugin
    * @return class
    */
   public boolean hasPlugin(String pname) {
      return this.plugins.containsKey(pname);
   }

   /**
    * Return la liste des plugins activés
    *
    * @return class
    */
   public Set<Map.Entry<String, Class<Instruction>>> getAllPlugins() {
      return this.plugins.entrySet();
   }

   /**
    * Return la taille de la liste des plugins activés
    *
    * @return int
    */
   public int getPluginsNb() {
      return this.plugins.size();
   }

   /**
    * Ajoute un plugin
    *
    * @param lng la ligne en cours
    * @param cl la classe
    * @return {valeur continue, succes 1/0, nom de commande}
    */
   public RetPlugin addPlugin(Class<Instruction> cl, int lng) {
      int ret = RET_CONTINUE, success = 0;
      String pname = null;
      try {
         Constructor<Instruction> ctor = cl.getConstructor(Contexte.class);
         Instruction cmd = ctor.newInstance(this);
         pname = cmd.getName();
         if (pname == null) throw new IllegalArgumentException("commande nulle");
         if (cmd.getType() != Instruction.TYPE_CMDPLG)
            ret = erreur("PLUGIN", "la classe " + cl.toString() + " doit être du type TYPE_CMDPLG", lng);
         else {
            HashMap<String, Instruction> cmds = getAllCommands();
            for (Map.Entry<String, Instruction> me : cmds.entrySet())
               if(me.getKey() != null && me.getKey().equals(pname.toUpperCase())){
                  ret = erreur("PLUGIN", "Conflit de nom du greffon avec la commande " + pname, lng);
               }
            // Si pas de collision avec un nom de commande
            addCommand(cmd);
            this.plugins.put(pname, cl);
            success = 1;
         }
      }
      catch (Exception ex) {
         ret = erreur("PLUGIN", "Impossible d'instancier le greffon : " + ex.getMessage(), lng);
      }
      //return new int[]{ret, success};
      return new RetPlugin(ret, success, pname);
   }

   /**
    * Supprime un plugin
    *
    * @param pname le nom du plugin
    * @return le plugin supprimé
    */
   public Class<Instruction> removePlugin(String pname) {
      removeCommand(pname);
      return this.plugins.remove(pname);
   }

   /**
    * Vide la liste des plugins
    */
   public void clearAllPlugins() {
      removePlgCommand();
      this.plugins.clear();
   }

   /**
    * Obtient le nombre max de colonnes
    *
    * @param lng la ligne en cours
    * @return le nombre
    */
   public int getColMaxWidth(int lng){
      int r;
      try {
         r = Integer.parseInt(getVar(ENV_COL_MAX_WTH));
         if (r < 5) r = Config.CT_COL_MAX_WIDTH;
      }
      catch(NumberFormatException ex){// erreur prévisible > cont.erreur
         erreur("Contexte", "Impossible de définir la taille max. de colonne : "
               + ex.getMessage(), lng);
         r = Config.CT_COL_MAX_WIDTH;
      }
      return r;
   }

   /**
    * Obtient le nombre max de lignes à aller chercher
    *
    * @param lng la ligne en cours
    * @return le nombre (ou 1000 si la viariable n'est pas définie ou nulle)
    */
   public int getRowMaxNumber(int lng){
      int r;
      try {
         r = Integer.parseInt(getVar(ENV_ROW_MAX_NB));
         if (r <= 0) r = 1000;
      }
      catch(NumberFormatException ex){// erreur prévisible > cont.erreur
         erreur("Contexte", "Impossible de définir le nombre max. de lignes : " + ex.getMessage(), lng);
         r = 1000;
      }
      return r;
   }

   /**
    * Évaluation d'une expression arithmétique (moteur JavaScript)
    *
    * @param expr l'expression arithmétique JS
    * @return le résultat
    * @throws ScriptException en cas de problème de script
    */
   public Object evaluerExpr(String expr) throws ScriptException {
      if (expr == null || expr.isEmpty()) return null;
      if (evaleng != null) return evaleng.eval(Tools.removeBracketsIfAny(expr).trim());

      // TODO: Si aucun SE disponible
      //erreur("Contexte", "Aucun moteur d'évaluation disponible", lng);
      return null;
   }

   /**
    * Évaluation d'une expression booléenne par SE pour IF, WHILE, WHEN.
    *
    * @param expr l'expression booléenne
    * @return valeur bool
    * @throws ScriptException en cas de problème de script
    */
   public boolean evaluerBool(String expr) throws ScriptException,
         IllegalArgumentException {
      if (expr == null || expr.isEmpty()) return false;

      // Substitution initiale par Lecteur et évaluation par SE avec sortie normale sur erreur
      Object r = evaluerExpr(getCurrentLecteur().substituteExt(expr));
      if (r == null) return false;
      if (r instanceof Boolean) return ((Boolean)r);
      if (r instanceof Integer) return ((Integer)r) != 0;
      if (r instanceof Double) return ((Double)r) != 0.0;
      String s = r.toString();
      return !s.isEmpty() && !s.equals("0") && !s.equals("0.0");
   }

   /**
    * Évaluation d'une expression booléenne par nouveau Lecteur pour DICT et LIST
    *
    * @param expr l'expression booléenne
    * @param vars les variables locales à définir
    * @return valeur int[0/1, CONTINUE]
    */
   public int[] evaluerBoolLec(String expr, HashMap<String, String> vars) throws IllegalArgumentException {
      final int[] ret0 = new int[]{0, RET_CONTINUE}, ret1 = new int[]{1, RET_CONTINUE};

      // Prétraitement des [] {}
      if (expr == null || expr.isEmpty()) return ret0;
      if ((expr = Tools.removeBracketsIfAny(expr).trim()).isEmpty()) return ret0;

      // Analyse première en Lecteur
      addSubMode();
      Lecteur lec = new Lecteur(this, expr, vars);
      remSubMode();
      if (lec.getCmdState() != RET_CONTINUE) return new int[]{0, lec.getCmdState()};

      // Test de la valeur résultante et falsehoods
      String r;
      Valeur v = getValeur();
      if (v == null || (r = v.getSubValue()) == null || r.isEmpty()) return ret0;
      if (r.equals("0") || r.equals("0.0") || r.equalsIgnoreCase("false")) return ret0;
      return ret1;
   }

   /**
    * Évaluation d'un bloc par Lecteur pour FOR ou WHILE
    * Le lecteur ne doit pas être instancié ici, mais en classe appelante
    * 
    * @param lec le lecteur
    * @param cmds les commandes à évaluer
    * @param vars les variables locales
    * @return statut
    */
   public int evaluerBlockFor(Lecteur lec, String cmds, HashMap<String, String> vars) {
      if (!cmds.isEmpty()) {
         lec.add(cmds, vars);
         lec.doCheckWhen();
      }

      // Commande NEXT appelée
      int ret = lec.getCmdState();
      if (ret == RET_NEXT_LP) {
         lec.setCmdState(RET_CONTINUE);
         ret = RET_CONTINUE;
      }
      return ret;
   }

   /**
    * Évaluation d'un bloc par Lecteur pour IF, CASE
    * Le lecteur ne doit pas être instancié ici, mais en classe appelante
    *
    * @param lec le lecteur
    * @param cmds les commandes à évaluer
    * @return statut
    */
   public int evaluerBlockIf(Lecteur lec, String cmds) {
      if (!cmds.isEmpty()) {
         lec.add(cmds);
         lec.doCheckWhen();
      }
      return lec.getCmdState();
   }

   /**
    * Lit une saisie utilisateur pour INPUT, ARG et Lecteur $``
    * 
    * @param prompt la prompt à afficher
    * @param echo le caractère à "échoer"
    * @return la saisie (String)
    * @throws IOException si erreur IO
    */
   public String getInput(String prompt, Character echo) throws IOException {
      if (isHttpMode()) return prompt; // fonction désactivée si mode HTTP

      String val = null;
      if (this.reader == null) {
         if (getVerbose() >= Contexte.VERB_AFF && prompt.length() > 0) println(prompt); // print non supporté
         java.io.Console c = System.console();
         if (c != null) val = c.readLine();
      }
      else val = this.reader.readLine(prompt, echo);
      return val;
   }

   /**
    * Affiche un message d'erreur
    *
    * @param cmd la commande en erreur
    * @param msg le message d'erreur
    * @param lng le numéro de ligne
    * @return état
    */
   public int erreur(String cmd, String msg, int lng) {
      setValeur(null);
      String stk = getCallStack();

      // Biip
      if (getVar(Contexte.ENV_BEEP_ON).equals(Contexte.STATE_TRUE)) {
         try { playBeep(); } catch (IOException ex) {
            errprintln("Note: lecture de son impossible : " + ex.getMessage());
         }
      }

      // Recherche de la commande en erreur
      String scmd = getLecVar(LEC_CATCH_LCMD), lnom = getLecVar(LEC_THIS);
      boolean okcmd = true;
      if (scmd != null) {
         String[] lcmd = scmd.length() > 0 ? scmd.split(",") : new String[0];
         okcmd = lcmd.length == 0;
         for (String c : lcmd) if (c.equalsIgnoreCase(cmd) || c.equals(lnom)) {
            okcmd = true;
            break;
         }
      }
      // Traitement si commande en liste à attraper
      String key; // nom de variable en cas de EVAL -c
      if (okcmd && (key = getLecVar(LEC_CATCH_CODE)) != null) { // bloc code err fourni
         unsetLecVar(LEC_CATCH_CODE);
         // Exécution du code
         HashMap<String, String> vars = new HashMap<>();
         vars.put(Contexte.LEC_SUPER, "eval:catch");
         vars.put("err_lng", Integer.toString(lng));
         vars.put("err_cmd", cmd);
         vars.put("err_msg", msg);
         vars.put("err_stk", stk);
         addSubMode();
         Lecteur lec = new Lecteur(this, key, vars);
         remSubMode();
         int ret = lec.getCmdState();
         return ret == RET_CONTINUE ? (getVar(ENV_EXIT_ERR).equals(STATE_TRUE) ? RET_EV_CATCH : RET_CONTINUE) : ret;
      }

      // Affichage de l'erreur non attrapée
      String errmsg = cmd + " (ligne " + lng + ") : " + msg;
      if (getVerbose() >= VERB_AFF) {
         errprintln(errmsg);
         errprintln("Pile des appels : " + stk);
      }
      setVar(ENV_RET_VALUE, "0");
      setVar(ENV_RET_NLINES, "0");
      setVar(ENV_ERROR_CMD, cmd);
      setVar(ENV_ERROR_MSG, errmsg);
      setVar(ENV_ERROR_STK, stk);

      // Exécution du contenu de l'option :ON_ERROR
      if (!onerrex) { // pour éviter le bouclage de la mort en cas d'erreur en bloc :ON_ERROR
         String code = getVar(ENV_ON_ERROR);
         if (code.length() > 0) {
            setVar(ENV_CMD_STATE, STATE_FALSE);
            HashMap<String, String> vars = new HashMap<>();
            vars.put(Contexte.LEC_SUPER, ENV_ON_ERROR);
            vars.put("err_lng", Integer.toString(lng));
            vars.put("err_cmd", cmd);
            vars.put("err_msg", msg);
            vars.put("err_stk", stk);
            onerrex = true;
            addSubMode();
            Lecteur lec = new Lecteur(this, code, vars);
            remSubMode();
            onerrex = false;
            return getVar(ENV_EXIT_ERR).equals(STATE_TRUE) ? RET_EV_CATCH : lec.getCmdState();
         }
         else setVar(ENV_CMD_STATE, STATE_ERROR);
      }
      else setVar(ENV_CMD_STATE, STATE_ERROR);
      return getVar(ENV_EXIT_ERR).equals(STATE_TRUE) ? RET_EV_CATCH : RET_CONTINUE;
   }

   /**
    * Affiche un message d'exception
    *
    * @param cmd la commande en erreur
    * @param msg le message d'erreur
    * @param lng le numéro de ligne
    * @param ex  l'exception lancée
    * @return état
    */
   public int exception(String cmd, String msg, int lng, Throwable ex) {
      if (getVerbose() == VERB_DBG) {
         StringBuilder result = new StringBuilder("DEBUG: ");
         result.append(ex.toString()).append("\nPile d'exécution :");
         for (StackTraceElement elemt : ex.getStackTrace()) result.append("\n  ").append(elemt);
         println(result.toString());
      }
      return erreur(cmd, msg, lng);
   }

   /**
    * Mise à jour du complètement
    */
   public void setCompletors() {
      if (console != null) console.setCompletors();
   }

   /**
    * Ajout d'une variable à l'environnement, ou écrasement si existe déjà.
    * La console prend en outre en compte la nouvelle var. pour le complètement
    * Si la variable est une constante, teste si pas déjà déclarée
    *
    * @param key le nom de la variable
    * @param val la valeur de la variable
    */
   public void setVar2(String key, String val) {
      setVar2(key, val, false);
   }

   /**
    * Ajout d'une variable à l'environnement, ou écrasement si existe déjà.
    * La console prend en outre en compte la nouvelle var. pour le complètement
    * Si la variable est une constante, teste si pas déjà déclarée
    *
    * @param key le nom de la variable
    * @param val la valeur de la variable
    * @param docirc si la var. est exempte de contrôle de ref. circ.
    */
   public void setVar2(String key, String val, boolean docirc) {
      if (Character.isUpperCase(key.charAt(0)) && isSet(key) && getVerbose() >= VERB_MSG)
         errprintln("Note : redéclaration de la constante '" + key + "'");
      setVar(key, val);
      boolean hascirc = varcirc.contains(key);
      if (!hascirc && docirc) varcirc.add(key);
      else if (hascirc && !docirc) varcirc.remove(key);
   }

   /**
    * Ajout d'une variable à l'environnement global, ou écrasement si existe déjà<br>
    * Attention : pas de contrôle validité de la variable
    *
    * @param key le nom de la variable
    * @param val la valeur de la variable
    */
   public void setVar(String key, String val) {
      this.environ.put(key, val);
   }

   /**
    * Suppression d'une variable d'environnement global
    *
    * @param key le nom de la variable
    * @return true si suppression a fonctionné, false sinon
    */
   public boolean unsetVar(String key) {
      if (!isSet(key)) return false;
      else {
         this.environ.remove(key);
         this.varcirc.remove(key);
         removeVarHelp(key);
         return true;
      }
   }

   /**
    * Suppression de toutes les variable d'envoronnement non systeme
    * @return nb de var. supprimées
    */
   public int unsetAllVars() {
      SortedSet<String> sort = new TreeSet<>(getAllVars().keySet());
      Iterator<String> iter = sort.iterator();
      int nbr = 0;
      while (iter.hasNext()) {
         String key = iter.next();
         if (isNonSys(key)) {
            this.environ.remove(key);
            removeVarHelp(key);
            nbr ++;
         }
      }
      return nbr;
   }

   /**
    * Ajout ou met à jour d'une variable à l'environnement temporaire : (_l), $(_n), $(1)...<br>
    *
    * @param key le nom de la variable
    * @param val la valeur de la variable
    */
   public void setTmpVar(String key, String val) {
      this.tmpvars.put(key, val);
   }

   /**
    * Ajout ou met à jour d'une variable à l'environnement lecteur local<br>
    *
    * @param key le nom de la variable
    * @param val la valeur de la variable
    */
   public void setLecVar(String key, String val) {
      if (Character.isUpperCase(key.charAt(0)) && isLec(key) && getVerbose() >= VERB_MSG)
         errprintln("Note : redéclaration de la constante '" + key + "'");
      if (this.lecteur != null) this.lecteur.setLecVar(key, val);
   }

   /**
    * Lie une variable locale à l'environnement du lecteur père direct<br>
    *
    * @param key le nom de la variable
    */
   public void setLinkVar(String key) {
      if (this.lecteur != null) this.lecteur.addLink(key);
   }

   /**
    * Lie une variable locale à l'environnement du lecteur père dans les ancètres<br>
    *
    * @param key le nom de la variable
    * @param level le niveau de remontée dans les ancêtres
    */
   public void setLinkVar(String key, int level) {
      Lecteur lec = this.lecteur;
      while (level-- > 0 && lec != null) {
         lec.addLink(key);
         lec = lec.getFather();
      }
   }

   /**
    * Suppression d'une variable lecteur
    *
    * @param key le nom de la variable
    */
   public void unsetLecVar(String key) {
      this.lecteur.unsetLecVar(key);
   }

   /**
    * Obtient la valeur d'une variable : test si locale, sinon globale
    *
    * @param key le nom de la variable
    * @return la valeur, ou null si elle n'est pas déficie
    */
   public final String getVar(String key) {
      String val = getLecVar(key);
      if (val == null) val = getGlbVar(key);
      return val;
   }

   /**
    * Obtient la valeur d'une variable globale (normale)
    *
    * @param key le nom de la variable
    * @return la valeur, ou null si elle n'est pas déficie
    */
   public final String getGlbVar(String key) {
      if (!isSet(key)) return null;
      return this.environ.get(key);
   }

   /**
    * Obtient la valeur d'une variable temporaire
    *
    * @param key le nom de la variable
    * @return la valeur, ou null si elle n'est pas déficie
    */
   public final String getTmpVar(String key) {
      if (!isTmp(key)) return null;
      return this.tmpvars.get(key);

   }

   /**
    * Obtient la valeur d'une variable locale de Lecteur
    *
    * @param key le nom de la variable
    * @return la valeur, ou null si elle n'est pas définie
    */
   public final String getLecVar(String key) {
      if (!isLec(key)) return null;
      return this.lecteur.getLecVar(key);
   }

   /**
    * Obtient la valeur d'une variable locale de Lecteur dans les ancètres
    *
    * @param key le nom de la variable
    * @param level le niveau de remontée dans les ancêtres
    * @return la valeur, ou null si elle n'est pas définie
    */
   public final String getLinkVar(String key, int level) {
      Lecteur lec = this.lecteur;
      String r = null;
      while (level-- >= 0 && lec != null) {
         r = lec.getLecVar(key);
         lec = lec.getFather();
      }
      return r;
   }

   /**
    * Suppression des variables temporaires
    */
   public void clearTmpVars() {
      tmpvars.clear();
   }

   /**
    * Obtient l'ensemble des variables d'environnement
    *
    * @return les variables d'environnement sous HashMap
    */
   public final HashMap<String, String> getAllVars() {
      return this.environ;
   }

   /**
    * Obtient une copie des variables locales du lecteur en cours
    * On fait une copie pour ne pas modifier les var. lecteur du lecteur père
    *
    * @return les variables lecteur sous HashMap
    */
   public final HashMap<String, String> getAllLecVars() {
      if (this.lecteur == null) return null;
      HashMap<String, String> vars = this.lecteur.getAllLecVars();
      return vars == null ? null : new HashMap<>(vars);
   }

   /**
    * Teste si la variable est fixée en var. ou opt. normale (globale)
    *
    * @param key le nom de la variable
    * @return true si variable existe, false sinon
    */
   public final boolean isSet(String key) {
      return key != null && this.environ.containsKey(key);
   }

   /**
    * Obtient l'aide de la variable (si définie)
    *
    * @param key le nom de la variable
    * @return aide si var définie, null sinon
    */
   public final String getVarHelp(String key){
      if (key == null) return null;
      return this.varhelp.get(key);
   }

   /**
    * Fixe le couple variable-aide associée, avec formatage ou non en paragraphes
    *
    * @param key le nom de la variable aide
    * @param help l'aide associée
    * @param frm s'il doit y avoir formatage
    */
   public final void addVarHelp(String key, String help, boolean frm) {
      if (key == null || help == null || help.isEmpty()) return;
      this.varhelp.put(key, frm ? Tools.textToLines(help, "  ") : help);
   }

   /**
    * Fixe le couple variable-aide associée, avec formatage en paragraphes
    *
    * @param key le nom de la variable aide
    * @param help l'aide associée
    */
   public final void addVarHelp(String key, String help) {
      addVarHelp(key, help, true);
   }

   /**
    * Obtient toutes les aides des variables
    *
    * @return la liste
    */
   public final HashMap<String, String> getAllVarHelps() {
      return this.varhelp;
   }

   /**
    * Supprime une variable-aide
    *
    * @param key le nom de la variable aide
    */
   public final void removeVarHelp(String key){
      if (key == null) return;
      this.varhelp.remove(key);
   }

   /**
    * Return la liste des aides (en un seul exemplaire s'il te plaît)
    *
    * @return hashmap
    */
   public TreeMap<String, String> getAllVarHelpTree() {
      TreeMap<String, String> tree = new TreeMap<>();
      for (String key : this.varhelp.keySet()) {
         String help = this.varhelp.get(key);
         if (!tree.containsKey(key)) tree.put(key, help);
      }
      return tree;
   }

   /**
    * Détermine si le nom de macro est ignoré par la contrôle de réf. circulaire
    *
    * @param key le nom
    * @return true si oui
    */
   public boolean isCtrlCircVar(String key) {
      if (key == null) return false;
      return this.varcirc.contains(key);
   }

   /**
    * Retourne tous les noms de macros exemptes de contrôle de ref. circ.
    *
    * @return le tableau
    */
   public HashSet<String> getAllCircVars() {
      return this.varcirc;
   }

   /**
    * Teste si la variable est fixée en var temporaire
    *
    * @param key le nom de la variable
    * @return true si variable existe, false sinon
    */
   public final boolean isTmp(String key) {
      return key != null && this.tmpvars.containsKey(key);
   }

   /**
    * Teste si la variable est fixée en var locale de lecteur
    *
    * @param key le nom de la variable
    * @return true si variable existe, false sinon
    */
   public final boolean isLec(String key) {
      return this.lecteur != null && this.lecteur.isSet(key);
   }

   /**
    * Teste si la variable est système (existe et commence par _)
    *
    * @param key le nom de la variable
    * @return true si variable existe et est système, false sinon
    */
   public final boolean isSys(String key) {
      return isSet(key) && key.startsWith("_");
   }

   /**
    * Teste si la variable est système éditable (existe et commence par :) mais éditable
    *
    * @param key le nom de la variable
    * @return true si variable existe et est système éditable, false sinon
    */
   public final boolean isSysUser(String key) {
      return isSet(key) && key.startsWith(":");
   }

   /**
    * Teste si la variable est non système (ne commence pas par _ ni par :)
    *
    * @param key le nom de la variable
    * @return true si variable existe et est non système, false sinon
    */
   public final boolean isNonSys(String key) {
      return key != null && !key.startsWith("_") && !key.startsWith(":");
   }

   /**
    * Vérifie si un nom de variable utilisateur est valide
    *
    * @param key le nom de la variable
    * @return true si OK, false sinon
    */
   public final boolean valideKey(String key) {
      if (key == null || key.length() == 0) return false;
      // Caractères acceptés dans la clef
      return key.matches(KEY_PATTERN);
   }

   /**
    * Vérifie si un nom de variable système OU utilisateur est valide
    *
    * @param key le nom de la variable
    * @return true si OK, false sinon
    */
   public final boolean valideSysKey(String key) {
      if (key == null || key.length() == 0) return false;
      // Caractères acceptés dans la clef
      return key.matches("[_:]?" + KEY_PATTERN);
   }

   /**
    * Valide l'affectation d'un nom de plugin
    *
    * @param key le nom de la variable
    * @return true si OK, false sinon
    */
   public final boolean valideCmdName(String key) {
      if (key == null || key.length() == 0) return false;
      // Caractères acceptés dans la clef
      return key.matches("[a-zA-Z][a-zA-Z0-9_]*");
   }

   /**
    * Retourne la variable _LOOP_DEEP des boucles FOR et WHILE pour le lecteur
    * @return la valeur
    */
   public int getLoopDeep() {
      String sd = getLecVar(Contexte.LEC_LOOP_DEEP);
      int deep;
      if (sd == null || sd.length() == 0) deep = 0;
      else deep = Integer.parseInt(sd);
      return deep;
   }

   /**
    * Incrémente la variable _LOOP_DEEP des boucles FOR et WHILE pour le lecteur
    * @return la nouvelle valeur
    */
   public String incrLoopDeep() {
      return Integer.toString(getLoopDeep() + 1);
   }

   /**
    * Connexion en cours
    *
    * @return la connexion
    */
   public SQLCnx getConnex() {
      return this.sqlc;
   }

   /**
    * Ferme la connexion à la base
    *
    * @return true si ça a fonctionné
    */
   public boolean fermerConnex() {
      try {
         // Contenu de :ON_QUIT
         boolean ret = execOnInitQuit(false) != RET_CONTINUE;

         // Connexion SQL
         if (this.sqlc != null) this.sqlc.fermer();
         // Fichier in/out
         for (InputStreamReader is : filereads.values()) is.close();
         for (OutputStreamWriter os : filewrites.values()) os.close();
         // Fichiers SPOOL
         PrintStream pr = this.errwriter == null ? this.writer : this.errwriter;
         if (pr != null) pr.close();
         return ret;
      }
      catch (SQLException ex) {
         exception("ERREUR SQLException", "erreur de fermeture : " + ex.getMessage(), 0, ex);
         return false;
      }
      catch (IOException ex) {
         exception("ERREUR IOException", "erreur de fermeture : " + ex.getMessage(), 0, ex);
         return false;
      }
   }

   /**
    * Statut de la commande QUIT
    *
    * @return statut
    */
   public int getQuitStat() {
      return this.quitStat;
   }

   /**
    * Fixe le statut de la commande QUIT
    *
    * @param stat le statut
    */
   public void setQuitStat(int stat) {
      this.quitStat = stat;
   }

   /*
    * Méthodes de gestion des conditionnelles en contexte d'exécution par WHEN
    */

   /**
    * Ajout d'un nouveau WHEN sur la pile
    *
    * @param val la valeur 0/1
    */
   public void addWhen(boolean val) {
      whentree.push(val);
      setVar(ENV_WHEN_DEEP, Integer.toString(whentree.size()));
      if (val && canIncWhen()) idwhen++;
      if (getVerbose() == VERB_DBG) printWhenState("addWhen");
   }

   /**
    * Inversion du WHEN sur la pile
    */
   public void invWhen() {
      if (hasIf()) {
         Boolean b = whentree.pop();
         whentree.push(!b);
         if (!b && canIncWhen()) idwhen++;
      }
      if (getVerbose() == VERB_DBG) printWhenState("invWhen");
   }

   /**
    * Suppression du WHEN sur la pile
    */
   public void endWhen() {
      if (hasIf()) {
         whentree.pop();
         setVar(ENV_WHEN_DEEP, Integer.toString(whentree.size()));
         if (whentree.size() == idwhen) idwhen--;
      }
      if (getVerbose() == VERB_DBG) printWhenState("endWhen");
   }

   /**
    * Est-on dans une structure WHEN ?
    *
    * @return true si oui
    */
   public boolean hasIf() {
      return !whentree.isEmpty();
   }

   /**
    * Peut-on exécuter une commande en fonction de l'état des WHEN ?
    *
    * @return true si oui
    */
   public boolean canExec() {
      return !hasIf() || (whentree.lastElement() && whentree.size() - 1 == idwhen);
   }

   /**
    * Suppression de tout l'arbre des WHEN
    */
   public void supprWhen() {
      whentree.clear();
      idwhen = -1;
      setVar(ENV_WHEN_DEEP, "0");
   }

   public int getNbWhen() {
      return whentree.size();
   }

   public void setNbWhen(int nb) {
      if (nb <= 0) {
         supprWhen();
         return;
      }
      int l = whentree.size();
      for (int i = nb; i < l; i++) endWhen();
   }

   /**
    * Commande CHRON : mesure du temps
    * déclenche un chrono et retourne le temps qui a passé depuis le dernier
    *
    * @return temps qui a passé
    */
   public long setChrono(){
      long tm = System.currentTimeMillis();
      long timexec = chron == 0 ? 0 : tm - chron;
      chron = tm;
      return timexec;
   }

   /*
    * Méthodes de gestion des fichiers ouverts
    */

   /**
    * Liste des fichiers ouverts en lecture
    * @return liste
    */
   public HashMap<Integer, FileReader> getAllFilereads() {
      return this.filereads;
   }

   /**
    * Liste des fichiers ouverts en écriture
    * @return liste
    */
   public HashMap<Integer, FileWriter> getAllFilewrites() {
      return this.filewrites;
   }

   /**
    * Vérifie l'existance d'un fichier et demande pour écrasement
    * @param f le fichier
    * @return true si ok pour écriture
    * @throws IOException si erreur qelque part
    */
   public boolean askWriteFile(File f) throws IOException {
      if (f.exists() && getVerbose() >= Contexte.VERB_AFF &&
            getVar(Contexte.ENV_INTERACT).equals(Contexte.STATE_TRUE)) {
         ConsoleReader reader = getConsoleReader();
         String prompt = "Un fichier portant ce nom existe. Continuer ? (O/N) ";
         String val;
         if (reader == null) {
            if (getVerbose() >= Contexte.VERB_AFF) print(prompt);
            val = new BufferedReader(new InputStreamReader(System.in)).readLine();
         }
         else val = reader.readLine(prompt);
         return val.equalsIgnoreCase("O");
      }
      return true;
   }
   public boolean askWriteFile(String fname) throws IOException {
      return askWriteFile(new File(fname));
   }

   /**
    * Ajout d'un fichier en modes écriture ou lecture
    *
    * @param f le fichier
    * @param m mode écriture : 'w', lecture : 'r', append : 'a'
    * @return le descripteur de fichier (0-999 : write, >= 1000 : read)
    * @throws IOException si erreur IO
    */
   public int addFile(File f, char m) throws IOException {
      int h = 0;
      if (m == 'w' || m == 'a') { // mode écriture
         h = newFileHandle(filewrites, 0);
         filewrites.put(h, new FileWriter(f, m == 'a'));
      }
      else if (m == 'r') { // mode lecture
         h = newFileHandle(filewrites, 1000);
         filereads.put(h, new FileReader(f));
      }
      return h;
   }
   private static int newFileHandle(HashMap<Integer, ?> m, int x) throws IOException {
      int h, i = 0;
      while (i++ < 200) {
         h = ((int) (Math.random() * 1000)) + x;
         if (!m.containsKey(h)) return h;
      }
      throw new IOException("nombre limite de fichiers ouverts atteint");
   }

   public FileReader getFileR(int h) throws IOException {
      // mode lecture
      FileReader r = filereads.get(h);
      if (r == null) throw new IOException("aucun fichier portant le descripteur : " + h);
      return r;
   }
   public FileWriter getFileW(int h) throws IOException {
      // mode lecture
      FileWriter r = filewrites.get(h);
      if (r == null) throw new IOException("aucun fichier portant le descripteur : " + h);
      return r;
   }

   public void delFile(int h) throws IOException {
      if (h < 1000) { // mode écriture
         FileWriter o = filewrites.remove(h);
         if (o == null) throw new IOException("aucun fichier portant le descripteur : " + h);
         else o.close();
      }
      else if (h < 2000) { // mode lecture
         FileReader o = filereads.remove(h);
         if (o == null) throw new IOException("aucun fichier portant le descripteur : " + h);
         else o.close();
      }
   }

   /**
    * Affiche les infos de signature
    * @param pk la clef publique
    * @param ms la date de signature (millis)
    */
   public void printSignInfos(String pk, String ms) {
      if (getVerbose() < Contexte.VERB_BVR) return;

      try {
         ms = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date(Long.parseLong(ms,16)));
      } catch (NumberFormatException ex) {
         ms = "ERREUR";
      }
      println("Signé le : " + ms + "\npar : " + pk + " (" + pk.substring(0, 10) + ")");
   }

   // PRIVATE

   private void addCommand(Instruction cmd){
      if (!valideCmdName(cmd.getName()))
         throw new IllegalArgumentException("nom de commande incorrect : '"+ cmd.getName() + "'");
      commands.put(cmd.getName(), cmd);
      if (cmd.getAlias() != null) commands.put(cmd.getAlias(), cmd);
   }

   private void removeCommand(String cmdname){
      removeCommand(getCommand(cmdname));
   }

   private void removeCommand(Instruction cmd){
      if (cmd.getAlias() != null) commands.remove(cmd.getAlias());
      commands.remove(cmd.getName());
   }

   private void removePlgCommand() {
      for (String key : plugins.keySet()) removeCommand(key);
   }

   private boolean canIncWhen() {
      return (whentree.size() == 1 && idwhen == -1) ||
             (idwhen >= 0 && idwhen < whentree.size() - 1 && whentree.get(idwhen) && whentree.get(idwhen + 1));
   }

   private void printWhenState(String s) {
      StringBuilder sb = new StringBuilder().append(s).append(':');
      for (Boolean b : whentree) sb.append(b ? "1" : "0");
      sb.append(" (i=").append(idwhen).append(", n=").append(whentree.size()).append(")");
      System.out.println(sb.toString());
   }

   /*
    * Pour enregistrer les plugins
    */
   @SuppressWarnings("unchecked")
   private void registerPlugin(File f, String val) {
      try {
         Class<?> cl = Class.forName(val);
         if (Instruction.class.isAssignableFrom(cl))
            addPlugin((Class<Instruction>) cl, 0);
         else if (getVerbose() >= Contexte.VERB_AFF)
               errprintln(f.getName() + " : Greffon invalide : " + cl);
      }
      catch (ClassNotFoundException ex) {
         if (getVerbose() >= Contexte.VERB_AFF)
            errprintln(f.getName() + " : Plugin introuvable : " + val);
      }
   }

   /**
    * Pour afficher une roue qui tourne
    */
   public void showWheel() {
      if (getVerbose() == Contexte.VERB_SIL) return;
      finish = false;
      Thread wheel = new Thread(() -> {
         String c = "|/-\\";
         int i = 0;
         while (! finish) {
            try {
               System.out.print("\r" + c.charAt(i++ % c.length()) + " ");
               Thread.sleep(100);
            } catch (InterruptedException ex) {}
         }
      });
      wheel.start();
   }
   public void hideWheel() {
      if (getVerbose() == Contexte.VERB_SIL) return;
      finish = true;
      System.out.print("\r");
   }

}// class

